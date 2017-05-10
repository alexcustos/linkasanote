package com.bytesforge.linkasanote.laano.notes.conflictresolution;

import android.support.annotation.NonNull;
import android.util.Log;

import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.laano.notes.NoteId;
import com.bytesforge.linkasanote.settings.Settings;
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

    private static final String TAG = NotesConflictResolutionPresenter .class.getSimpleName();

    private final Repository repository; // NOTE: for cache control
    private final Settings settings;
    private final LocalNotes<Note> localNotes;
    private final CloudItem<Note> cloudNotes;
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
            Repository repository, Settings settings,
            LocalNotes<Note> localNotes, CloudItem<Note> cloudNotes,
            NotesConflictResolutionContract.View view,
            NotesConflictResolutionContract.ViewModel viewModel,
            BaseSchedulerProvider schedulerProvider, @NoteId String noteId) {
        this.repository = repository;
        this.settings = settings;
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

    private void loadLocalNote() {
        localDisposable.clear();
        Disposable disposable = localNotes.get(noteId)
                .subscribeOn(schedulerProvider.computation()) // local
                .observeOn(schedulerProvider.ui())
                .subscribe(note -> {
                    if (!note.isConflicted()) {
                        // NOTE: to make sure that there is no problem with the cache
                        repository.refreshNotes();
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
        viewModel.populateLocalNote(note);
        if (!viewModel.isCloudPopulated()) {
            loadCloudNote();
        }
    }

    private void loadCloudNote() {
        cloudDisposable.clear();
        Disposable disposable = cloudNotes.download(noteId)
                .subscribeOn(schedulerProvider.io())
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
        viewModel.showProgressOverlay();
        deleteNote(noteId);
    }

    private void replaceNote(
            @NonNull final String mainNoteId, @NonNull final String noteId) {
        checkNotNull(mainNoteId);
        checkNotNull(noteId);
        deleteNoteSingle(mainNoteId)
                .subscribeOn(schedulerProvider.io())
                .map(success -> {
                    if (success) {
                        SyncState state = new SyncState(SyncState.State.SYNCED);
                        success = localNotes.update(noteId, state).blockingGet();
                    }
                    return success;
                })
                .observeOn(schedulerProvider.ui())
                .subscribe(success -> {
                    if (success) {
                        repository.refreshNotes(); // OPTIMIZATION: reload one item
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    private void deleteNote(@NonNull final String noteId) {
        checkNotNull(noteId);
        deleteNoteSingle(noteId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(success -> {
                    if (success) {
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    private Single<Boolean> deleteNoteSingle(@NonNull final String noteId) {
        checkNotNull(noteId);
        return cloudNotes.delete(noteId)
                .map(result -> {
                    boolean success = false;
                    if (result.isSuccess()) {
                        success = localNotes.delete(noteId).blockingGet();
                    } else {
                        Log.e(TAG, "There was an error while deleting the Note from the cloud storage [" + noteId + "]");
                    }
                    return success;
                })
                .doOnSuccess(success -> {
                    if (success) {
                        repository.deleteCachedNote(noteId);
                        settings.resetNoteFilter(noteId);
                    }
                });
    }

    @Override
    public void onCloudDeleteClick() {
        viewModel.deactivateButtons();
        viewModel.showProgressOverlay();
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
        viewModel.showProgressOverlay();
        Note note = localNotes.get(noteId).blockingGet();
        cloudNotes.upload(note)
                .subscribeOn(schedulerProvider.io())
                .map(result -> {
                    boolean success = false;
                    if (result.isSuccess()) {
                        JsonFile jsonFile = (JsonFile) result.getData().get(0);
                        SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                        success = localNotes.update(note.getId(), state).blockingGet();
                    }
                    return success;
                })
                .observeOn(schedulerProvider.ui())
                .subscribe(success -> {
                    if (success) {
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
        viewModel.showProgressOverlay();
        cloudNotes.download(noteId)
                .subscribeOn(schedulerProvider.io())
                .map(note -> localNotes.save(note).blockingGet())
                .observeOn(schedulerProvider.ui())
                .subscribe(success -> {
                    if (success) {
                        repository.refreshNotes();
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }
}
