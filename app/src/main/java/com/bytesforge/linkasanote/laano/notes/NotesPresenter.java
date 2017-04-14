package com.bytesforge.linkasanote.laano.notes;

import android.support.annotation.NonNull;
import android.util.Log;

import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Strings;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public final class NotesPresenter implements NotesContract.Presenter {

    private static final String TAG = NotesPresenter.class.getSimpleName();

    private final Repository repository;
    private final NotesContract.View view;
    private final NotesContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final LaanoUiManager laanoUiManager;
    private final Settings settings;

    @NonNull
    private final CompositeDisposable disposable;

    private boolean firstLoad = true;

    @Inject
    NotesPresenter(
            Repository repository, NotesContract.View view,
            NotesContract.ViewModel viewModel, BaseSchedulerProvider schedulerProvider,
            LaanoUiManager laanoUiManager, Settings settings) {
        this.repository = repository;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.laanoUiManager = laanoUiManager;
        this.settings = settings;
        disposable = new CompositeDisposable();
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
        viewModel.setPresenter(this);
        viewModel.setLaanoUiManager(laanoUiManager);
    }

    @Override
    public void subscribe() {
        loadNotes(false);
    }

    @Override
    public void unsubscribe() {
        disposable.clear();
    }

    @Override
    public void onTabSelected() {
    }

    @Override
    public void onTabDeselected() {
        view.disableActionMode();
    }

    @Override
    public void addNote() {
        view.showAddNote();
    }

    @Override
    public void loadNotes(final boolean forceUpdate) {
        loadNotes(forceUpdate || firstLoad, true);
        firstLoad = false;
    }

    private void loadNotes(boolean forceUpdate, final boolean showLoading) {
        EspressoIdlingResource.increment();
        disposable.clear();
        if (forceUpdate) {
            repository.refreshNotes();
        }
        if (showLoading) {
            viewModel.showProgressOverlay();
        }
        Disposable disposable = repository.getNotes()
                .subscribeOn(schedulerProvider.computation())
                .filter(note -> {
                    String searchText = viewModel.getSearchText();
                    if (!Strings.isNullOrEmpty(searchText)) {
                        searchText = searchText.toLowerCase();
                        String noteNote = note.getNote();
                        if (noteNote != null
                                && !noteNote.toLowerCase().contains(searchText)) {
                            return false;
                        }
                    }
                    switch (viewModel.getFilterType()) {
                        case CONFLICTED:
                            return note.isConflicted();
                        case ALL:
                        default:
                            return true;
                    }
                })
                .toList()
                .observeOn(schedulerProvider.ui())
                .doFinally(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                    if (showLoading) {
                        viewModel.hideProgressOverlay();
                    }
                })
                .subscribe(view::showNotes, throwable -> {
                    // NullPointerException
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    Log.e(TAG, throwable.toString());
                    viewModel.showDatabaseErrorSnackbar();
                });
        this.disposable.add(disposable);
    }

    @Override
    public void onNoteClick(String noteId, boolean isConflicted) {
        // TODO: normal mode selection must highlight current note filter
        if (viewModel.isActionMode()) {
            onNoteSelected(noteId);
        } else if (isConflicted) {
            view.showConflictResolution(noteId);
        }
    }

    @Override
    public boolean onNoteLongClick(String noteId) {
        view.enableActionMode();
        onNoteSelected(noteId);
        return true;
    }

    @Override
    public void onCheckboxClick(String noteId) {
        onNoteSelected(noteId);
    }

    private void onNoteSelected(String noteId) {
        int position = getPosition(noteId);
        viewModel.toggleSelection(position);
        view.selectionChanged(position);
    }

    @Override
    public void onEditClick(@NonNull String noteId) {
        view.showEditNote(noteId);
    }

    @Override
    public void onToLinksClick(@NonNull String noteId) {
    }

    @Override
    public void onToggleClick(@NonNull String noteId) {
        int position = getPosition(noteId);
        viewModel.toggleNoteVisibility(position);
        view.noteVisibilityChanged(position);
    }

    @Override
    public void onDeleteClick() {
        int[] selectedIds = viewModel.getSelectedIds();
        view.confirmNotesRemoval(selectedIds);
    }

    @Override
    public void onSelectAllClick() {
        viewModel.toggleSelection();
    }

    @Override
    public void deleteNotes(int[] selectedIds) {
        for (int selectedId : selectedIds) {
            viewModel.removeSelection(selectedId);
            String noteId = view.removeNote(selectedId);
            try {
                repository.deleteNote(noteId);
            } catch (NullPointerException e) {
                viewModel.showDatabaseErrorSnackbar();
            }
        }
    }

    @Override
    public int getPosition(String noteId) {
        return view.getPosition(noteId);
    }

    @Override
    public boolean isConflicted() {
        return repository.isConflictedNotes().blockingGet();
    }

    @Override
    public void setFilterType(@NonNull FilterType filterType) {
        viewModel.setFilterType(filterType);
        laanoUiManager.updateTitle(LaanoFragmentPagerAdapter.NOTES_TAB);
    }

    @Override
    public void updateTabNormalState() {
        laanoUiManager.setTabNormalState(LaanoFragmentPagerAdapter.NOTES_TAB, isConflicted());
    }

    @Override
    public boolean isExpandNotes() {
        return settings.isExpandNotes();
    }
}
