package com.bytesforge.linkasanote.laano.notes.conflictresolution;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.cloud.CloudNotes;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.laano.notes.NoteId;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class NotesConflictResolutionPresenter implements
        NotesConflictResolutionContract.Presenter {

    private final Repository repository; // NOTE: for cache control
    private final LocalNotes localNotes;
    private final CloudNotes cloudNotes;
    private final NotesConflictResolutionContract.View view;
    private final NotesConflictResolutionContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;

    private String noteId;

    @NonNull
    private final CompositeDisposable localDisposable;

    @NonNull
    private final CompositeDisposable cloudDisposable;

    @Inject
    NotesConflictResolutionPresenter(
            Repository repository, LocalNotes localNotes, CloudNotes cloudNotes,
            NotesConflictResolutionContract.View view,
            NotesConflictResolutionContract.ViewModel viewModel,
            BaseSchedulerProvider schedulerProvider, @NoteId String noteId) {
        this.repository = repository;
        this.localNotes = localNotes;
        this.cloudNotes = cloudNotes;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.noteId = noteId;
        localDisposable = new CompositeDisposable();
        cloudDisposable = new CompositeDisposable();
    }

    @Inject
    void setupView() {
        view.setViewModel(viewModel);
        viewModel.setPresenter(this);
    }

    @Override
    public void subscribe() {
        populate();
    }

    @Override
    public void unsubscribe() {
        localDisposable.clear();
        cloudDisposable.clear();
    }

    private void populate() {
        if (viewModel.isLocalPopulated() && viewModel.isCloudPopulated()) {
            return;
        }
        loadLocalNote(); // first step, then cloud one will be loaded
    }

    @Override
    public Single<Boolean> autoResolve() {
        return Single.fromCallable(() -> {
            Note note = localNotes.getNote(noteId).blockingGet();
            if (note.isDuplicated()) {
                try {
                    // TODO: remove
                    localNotes.getMainNote(note.getNote()).blockingGet();
                } catch (NoSuchElementException e) {
                    SyncState state = new SyncState(SyncState.State.SYNCED);
                    int numRows = localNotes.updateNote(noteId, state).blockingGet();
                    if (numRows == 1) {
                        repository.refreshNotes();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void loadLocalNote() {
        localDisposable.clear();
        Disposable disposable = localNotes.getNote(noteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(note -> {
                    if (!note.isConflicted()) {
                        repository.refreshNotes(); // NOTE: maybe there is a problem with cache
                        view.finishActivity();
                    } else {
                        populateLocalNote(note);
                    }
                }, throwable -> {
                    if (throwable instanceof NoSuchElementException) {
                        repository.refreshNotes(); // NOTE: maybe there is a problem with cache
                        view.finishActivity(); // NOTE: no item, no problem
                    } else {
                        viewModel.showDatabaseError();
                        loadCloudNote();
                    }
                });
        localDisposable.add(disposable);
    }

    private void populateLocalNote(@NonNull final Note note) {
        checkNotNull(note);

        if (note.isDuplicated()) {
            viewModel.populateCloudNote(note);
            localNotes.getMainNote(note.getNote())
                    .subscribeOn(schedulerProvider.computation())
                    .observeOn(schedulerProvider.ui())
                    // NOTE: recursion, but mainNote is not duplicated by definition
                    .subscribe(this::populateLocalNote, throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            // NOTE: main position is empty, so the conflict can be resolved automatically
                            // TODO: remove in favor of autoResolve
                            SyncState state = new SyncState(SyncState.State.SYNCED);
                            int numRows = localNotes.updateNote(noteId, state).blockingGet();
                            if (numRows == 1) {
                                repository.refreshNotes();
                                view.finishActivity();
                            } else {
                                view.cancelActivity();
                            }
                        } else {
                            viewModel.showDatabaseError();
                            loadCloudNote();
                        }
                    });
        } else {
            viewModel.populateLocalNote(note);
            if (!viewModel.isCloudPopulated()) {
                loadCloudNote();
            }
        } // if
    }

    private void loadCloudNote() {
        cloudDisposable.clear();
        Disposable disposable = cloudNotes.downloadNote(noteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(viewModel::populateCloudNote, throwable -> {
                    if (throwable instanceof NoSuchElementException) {
                        viewModel.showCloudNotFound();
                    } else {
                        viewModel.showCloudDownloadError();
                    }
                });
        cloudDisposable.add(disposable);
    }

    @Override
    public void onLocalDeleteClick() {
        viewModel.deactivateButtons();
        if (viewModel.isStateDuplicated()) {
            localNotes.getMainNote(viewModel.getLocalName())
                    .subscribeOn(schedulerProvider.computation())
                    .observeOn(schedulerProvider.ui())
                    .subscribe(
                            note -> replaceNote(note.getId(), noteId),
                            throwable -> view.cancelActivity());
        } else {
            deleteNote(noteId);
        }
    }

    private void replaceNote(
            @NonNull final String mainNoteId, @NonNull final String noteId) {
        checkNotNull(mainNoteId);
        checkNotNull(noteId);

        // DB operation is blocking; Cloud is on computation
        cloudNotes.deleteNote(mainNoteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    boolean isSuccess = false;
                    if (result.isSuccess()) {
                        int numRows = localNotes.deleteNote(mainNoteId).blockingGet();
                        isSuccess = (numRows == 1);
                    }
                    if (isSuccess) {
                        repository.deleteCachedNote(mainNoteId);
                        SyncState state = new SyncState(SyncState.State.SYNCED);
                        int numRows = localNotes.updateNote(noteId, state).blockingGet();
                        isSuccess = (numRows == 1);
                    }
                    if (isSuccess) {
                        repository.refreshNotes();
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    private void deleteNote(@NonNull final String noteId) {
        checkNotNull(noteId);

        cloudNotes.deleteNote(noteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    boolean isSuccess = false;
                    if (result.isSuccess()) {
                        int numRows = localNotes.deleteNote(noteId).blockingGet();
                        isSuccess = (numRows == 1);
                    }
                    if (isSuccess) {
                        repository.deleteCachedNote(noteId);
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    @Override
    public void onCloudDeleteClick() {
        viewModel.deactivateButtons();
        deleteNote(noteId);
    }

    @Override
    public void onCloudRetryClick() {
        viewModel.showCloudLoading();
        loadCloudNote();
    }

    @Override
    public void onLocalUploadClick() {
        viewModel.deactivateButtons();
        Note note = localNotes.getNote(noteId).blockingGet();
        cloudNotes.uploadNote(note)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    boolean isSuccess = false;
                    if (result.isSuccess()) {
                        JsonFile jsonFile = (JsonFile) result.getData().get(0);
                        SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                        int numRows = localNotes.updateNote(note.getId(), state)
                                .blockingGet();
                        isSuccess = (numRows == 1);
                    }
                    if (isSuccess) {
                        repository.refreshNotes();
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    @Override
    public void onCloudDownloadClick() {
        viewModel.deactivateButtons();
        cloudNotes.downloadNote(noteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(note -> {
                    long rowId = localNotes.saveNote(note).blockingGet();
                    if (rowId > 0) {
                        repository.refreshNotes();
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }
}
