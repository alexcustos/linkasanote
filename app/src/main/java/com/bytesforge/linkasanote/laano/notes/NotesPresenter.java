package com.bytesforge.linkasanote.laano.notes;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.laano.links.LinksPresenter;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Strings;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class NotesPresenter implements NotesContract.Presenter {

    private static final String TAG = NotesPresenter.class.getSimpleName();

    public static final String SETTING_NOTES_FILTER_TYPE = "NOTES_FILTER_TYPE";

    private final Repository repository;
    private final NotesContract.View view;
    private final NotesContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final LaanoUiManager laanoUiManager;
    private final Settings settings;

    @NonNull
    private final CompositeDisposable disposable;

    private String favoriteFilter;
    private String linkFilter;
    private List<Tag> favoriteFilterTags;
    private FilterType filterType;
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
    }

    @Override
    public void unsubscribe() {
        disposable.clear();
    }

    @Override
    public void onTabSelected() {
        loadNotes(false);
    }

    @Override
    public void onTabDeselected() {
        view.finishActionMode();
    }

    @Override
    public void addNote() {
        view.showAddNote(filterType == FilterType.LINK ? linkFilter : null);
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
        FilterType extendedFilter = updateFilter();
        Observable<Note> loadNotes = null;
        if (extendedFilter == FilterType.FAVORITE) {
            loadNotes = repository.getFavorite(favoriteFilter)
                    .toObservable()
                    .flatMap(favorite -> {
                        // NOTE: just to be sure we are still in sync
                        filterType = FilterType.FAVORITE;
                        favoriteFilter = favorite.getId();
                        favoriteFilterTags = favorite.getTags();
                        laanoUiManager.setFilterType(
                                LaanoFragmentPagerAdapter.NOTES_TAB, filterType, favorite.getName());
                        return repository.getNotes();
                    }).doOnError(throwable -> {
                        CommonUtils.logStackTrace(TAG, throwable);
                        if (throwable instanceof NoSuchElementException) {
                            setDefaultNotesFilterType();
                            favoriteFilter = null;
                            settings.setFavoriteFilter(null);
                        } else {
                            viewModel.showDatabaseErrorSnackbar();
                        }
                    }).onErrorResumeNext(repository.getNotes());
        } else if (extendedFilter == FilterType.LINK) {
            loadNotes = repository.getLink(linkFilter)
                    .toObservable()
                    .flatMap(link -> {
                        filterType = FilterType.LINK;
                        linkFilter = link.getId();
                        String linkName = link.getName() == null ? link.getLink() : link.getName();
                        laanoUiManager.setFilterType(
                                LaanoFragmentPagerAdapter.NOTES_TAB, filterType, linkName);
                        return repository.getNotes();
                    }).doOnError(throwable -> {
                        CommonUtils.logStackTrace(TAG, throwable);
                        if (throwable instanceof NoSuchElementException) {
                            setDefaultNotesFilterType();
                            linkFilter = null;
                            settings.setLinkFilter(null);
                        } else {
                            viewModel.showDatabaseErrorSnackbar();
                        }
                    }).onErrorResumeNext(repository.getNotes());
        }
        if (loadNotes == null) {
            loadNotes = repository.getNotes();
        }
        Disposable disposable = loadNotes
                .subscribeOn(schedulerProvider.computation())
                .filter(note -> {
                    String searchText = viewModel.getSearchText();
                    if (!Strings.isNullOrEmpty(searchText)) {
                        searchText = searchText.toLowerCase();
                        String noteNote = note.getNote();
                        if (noteNote != null && !noteNote.toLowerCase().contains(searchText)) {
                            return false;
                        }
                    }
                    switch (filterType) {
                        case CONFLICTED:
                            return note.isConflicted();
                        case LINK:
                            String linkId = note.getLinkId();
                            return linkId != null && linkId.equals(linkFilter);
                        case FAVORITE:
                            List<Tag> noteTags = note.getTags();
                            if (favoriteFilter == null) {
                                return true; // No filter
                            } else if (noteTags == null) {
                                return false; // No tags
                            }
                            return !Collections.disjoint(favoriteFilterTags, noteTags);
                        case NO_TAGS:
                            return note.getTags() == null;
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
                    laanoUiManager.updateTitle(LaanoFragmentPagerAdapter.NOTES_TAB);
                })
                .subscribe(notes -> {
                    view.showNotes(notes);
                    selectNoteFilter();
                }, throwable -> {
                    // NullPointerException
                    CommonUtils.logStackTrace(TAG, throwable);
                    viewModel.showDatabaseErrorSnackbar();
                });
        this.disposable.add(disposable);
    }

    @Override
    public void onNoteClick(String noteId, boolean isConflicted) {
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
    public void selectNoteFilter() {
        if (viewModel.isActionMode()) return;

        String noteFilter = settings.getNoteFilter();
        if (noteFilter != null) {
            int position = getPosition(noteFilter);
            if (position >= 0) {
                viewModel.setSingleSelection(position, true);
                //view.scrollToPosition(position); // TODO: move to settings
            }
        }
    }

    @Override
    public void onEditClick(@NonNull String noteId) {
        view.showEditNote(noteId);
    }

    @Override
    public void onToLinksClick(@NonNull String noteId) {
        checkNotNull(noteId);

        int position = getPosition(noteId);
        viewModel.setSingleSelection(position, true);
        settings.setFilterType(LinksPresenter.SETTING_LINKS_FILTER_TYPE, FilterType.NOTE);
        settings.setNoteFilter(noteId);
        laanoUiManager.setCurrentTab(LaanoFragmentPagerAdapter.LINKS_TAB);
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
        settings.setFilterType(SETTING_NOTES_FILTER_TYPE, filterType);
        if (this.filterType != filterType) {
            loadNotes(false);
        }
    }

    /**
     * @return Return null if there is no additional data is required
     */
    @Nullable
    private FilterType updateFilter() {
        FilterType filterType = settings.getFilterType(SETTING_NOTES_FILTER_TYPE);
        String prevLinkFilter = this.linkFilter;
        this.linkFilter = settings.getLinkFilter();
        // NOTE: there may be some concurrency who actually will reset the filter, but it OK
        if (this.linkFilter != null) {
            repository.getLink(this.linkFilter)
                    .subscribeOn(schedulerProvider.computation())
                    .subscribe(favorite -> { /* OK */ }, throwable -> {
                        this.linkFilter = null;
                        settings.setLinkFilter(null);
                    });
        }
        String prevFavoriteFilter = this.favoriteFilter;
        this.favoriteFilter = settings.getFavoriteFilter();
        if (this.favoriteFilter != null) {
            repository.getFavorite(this.favoriteFilter)
                    .subscribeOn(schedulerProvider.computation())
                    .subscribe(favorite -> { /* OK */ }, throwable -> {
                        this.favoriteFilter = null;
                        settings.setFavoriteFilter(null);
                    });
        }
        switch (filterType) {
            case ALL:
            case CONFLICTED:
            case NO_TAGS:
                if (this.filterType == filterType) {
                    return null;
                }
                this.filterType = filterType;
                laanoUiManager.setFilterType(LaanoFragmentPagerAdapter.NOTES_TAB, filterType, null);
                break;
            case LINK:
                if (this.filterType == filterType
                        && this.linkFilter != null
                        && this.linkFilter.equals(prevLinkFilter)) {
                    return null;
                }
                if (this.linkFilter == null) {
                    setDefaultNotesFilterType();
                    return null;
                }
                this.filterType = filterType;
                return filterType;
            case FAVORITE:
                if (this.filterType == filterType
                        && this.favoriteFilter != null
                        && this.favoriteFilter.equals(prevFavoriteFilter)) {
                    return null;
                }
                if (this.favoriteFilter == null) {
                    setDefaultNotesFilterType();
                    return null;
                }
                this.filterType = filterType;
                return filterType;
            default:
                setDefaultNotesFilterType();
        }
        return null;
    }

    private void setDefaultNotesFilterType() {
        this.filterType = Settings.DEFAULT_FILTER_TYPE;
        laanoUiManager.setFilterType(
                LaanoFragmentPagerAdapter.NOTES_TAB, this.filterType, null);
        settings.setFilterType(SETTING_NOTES_FILTER_TYPE, filterType);
    }

    @Override
    public boolean isFavoriteFilter() {
        return favoriteFilter != null;
    }

    @Override
    public boolean isLinkFilter() {
        return linkFilter != null;
    }

    @Override
    public boolean isExpandNotes() {
        return settings.isExpandNotes();
    }

    @Override
    public void updateTabNormalState() {
        laanoUiManager.setTabNormalState(LaanoFragmentPagerAdapter.NOTES_TAB, isConflicted());
    }
}
