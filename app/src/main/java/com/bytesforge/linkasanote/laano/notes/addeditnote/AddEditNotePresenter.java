package com.bytesforge.linkasanote.laano.notes.addeditnote;

import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.ClipboardService;
import com.bytesforge.linkasanote.laano.links.LinkId;
import com.bytesforge.linkasanote.laano.notes.NoteId;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class AddEditNotePresenter implements AddEditNoteContract.Presenter {

    private static final String TAG = AddEditNotePresenter.class.getSimpleName();

    private final Repository repository;
    private final AddEditNoteContract.View view;
    private final AddEditNoteContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final Settings settings;

    private String noteId; // NOTE: can be reset to null if NoSuchElementException
    private String linkId;

    @NonNull
    private final CompositeDisposable tagsDisposable;

    @NonNull
    private final CompositeDisposable noteDisposable;

    @NonNull
    private final CompositeDisposable linkDisposable;

    @Inject
    AddEditNotePresenter(
            Repository repository, AddEditNoteContract.View view,
            AddEditNoteContract.ViewModel viewModel, BaseSchedulerProvider schedulerProvider,
            Settings settings,
            @Nullable @NoteId String noteId, @Nullable @LinkId String linkId) {
        this.repository = repository;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.settings = settings;
        this.noteId = noteId;
        this.linkId = linkId;
        tagsDisposable = new CompositeDisposable();
        noteDisposable = new CompositeDisposable();
        linkDisposable = new CompositeDisposable();
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
        viewModel.setPresenter(this);
    }

    @Override
    public void subscribe() {
        populateLink();
        loadTags();
    }

    @Override
    public void unsubscribe() {
        tagsDisposable.clear();
        noteDisposable.clear();
        linkDisposable.clear();
    }

    @Override
    public void loadTags() {
        // TODO: get rid of EspressoIdlingResource in favor of schedulerProvider mock
        EspressoIdlingResource.increment();
        tagsDisposable.clear(); // stop previous requests

        Disposable disposable = repository.getTags()
                .toList()
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doOnError(throwable -> view.swapTagsCompletionViewItems(new ArrayList<>()))
                .doFinally(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                })
                .subscribe((tags, throwable) -> {
                    if (tags != null) view.swapTagsCompletionViewItems(tags);
                });
        tagsDisposable.add(disposable);
    }

    @Override
    public boolean isNewNote() {
        return noteId == null;
    }

    @Override
    public void populateNote() {
        if (noteId == null) {
            throw new RuntimeException("populateNote() was called but noteId is null");
        }
        EspressoIdlingResource.increment();
        noteDisposable.clear();

        Disposable disposable = repository.getNote(noteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doFinally(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                })
                .subscribe(note -> {
                    viewModel.populateNote(note);
                    linkId = note.getLinkId();
                    populateLink();
                }, throwable -> {
                    noteId = null;
                    viewModel.showNoteNotFoundSnackbar();
                });
        noteDisposable.add(disposable);
    }

    private void populateLink() {
        if (linkId == null) {
            view.setUnboundTitle(isNewNote());
            return;
        }
        viewModel.showLinkStatusLoading();
        EspressoIdlingResource.increment();
        linkDisposable.clear();

        Disposable disposable = repository.getLink(linkId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doFinally(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                })
                .subscribe(link -> {
                    view.setBoundTitle(isNewNote());
                    viewModel.populateLink(link);
                }, throwable -> {
                    linkId = null;
                    if (isNewNote()) {
                        view.setUnboundTitle(true);
                        viewModel.hideLinkStatus();
                    } else {
                        viewModel.showLinkStatusNoteWillBeUnbound();
                    }
                });
        linkDisposable.add(disposable);
    }

    @Override
    public void saveNote(String noteNote, List<Tag> noteTags) {
        if (isNewNote()) {
            createNote(noteNote, noteTags);
        } else {
            updateNote(noteNote, noteTags);
        }
    }

    private void createNote(String noteNote, List<Tag> noteTags) {
        saveNote(new Note(noteNote, linkId, noteTags));
    }

    private void updateNote(String noteNote, List<Tag> noteTags) {
        if (noteId == null) {
            throw new RuntimeException("updateNote() was called but noteId is null");
        }
        // NOTE: state eTag will NOT be overwritten if null
        saveNote(new Note(noteId, noteNote, linkId, noteTags)); // UNSYNCED
    }

    private void saveNote(@NonNull final Note note) {
        checkNotNull(note);

        if (note.isEmpty()) {
            viewModel.showEmptyNoteSnackbar();
            return;
        }
        try {
            repository.saveNote(note);
            view.finishActivity(noteId, linkId);
        } catch (SQLiteConstraintException e) {
            viewModel.showDuplicateKeyError();
        }
    }

    @Override
    public void onClipboardChanged(int clipboardType) {
        view.setNotePaste(clipboardType);
    }

    @Override
    public void onClipboardLinkExtraReady() {
        view.setNotePaste(ClipboardService.CLIPBOARD_EXTRA);
    }

    @Override
    public void setShowFillInFormInfo(boolean show) {
        settings.setShowFillInFormInfo(show);
    }

    @Override
    public boolean isShowFillInFormInfo() {
        return settings.isShowFillInFormInfo();
    }
}
