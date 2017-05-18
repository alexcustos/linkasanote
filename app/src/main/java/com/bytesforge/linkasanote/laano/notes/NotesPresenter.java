package com.bytesforge.linkasanote.laano.notes;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.BaseItemPresenter;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.laano.links.LinksPresenter;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncAdapter;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class NotesPresenter extends BaseItemPresenter implements
        NotesContract.Presenter, DataSource.Callback {

    private static final String TAG = NotesPresenter.class.getSimpleName();
    private static final int TAB = LaanoFragmentPagerAdapter.NOTES_TAB;

    public static final String SETTING_NOTES_FILTER_TYPE = "NOTES_FILTER_TYPE";

    private final Repository repository;
    private final NotesContract.View view;
    private final NotesContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final LaanoUiManager laanoUiManager;
    private final Settings settings;

    @NonNull
    private final CompositeDisposable compositeDisposable;

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
        compositeDisposable = new CompositeDisposable();
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
    }

    @Override
    public void subscribe() {
        repository.addNotesCallback(this);
    }

    @Override
    public void unsubscribe() {
        compositeDisposable.clear();
        repository.removeNotesCallback(this);
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
    public void showAddNote() {
        view.startAddNoteActivity(filterType == FilterType.LINK ? linkFilter : null);
    }

    @Override
    public void loadNotes(final boolean forceUpdate) {
        loadNotes(forceUpdate || firstLoad, true);
        firstLoad = false;
    }

    private void loadNotes(boolean forceUpdate, final boolean showLoading) {
        compositeDisposable.clear();
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
                        laanoUiManager.setFilterType(TAB, filterType, favorite.getName());
                        return repository.getNotes();
                    }).doOnError(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            setDefaultNotesFilterType();
                            favoriteFilter = null;
                            settings.setFavoriteFilter(null);
                        } else {
                            CommonUtils.logStackTrace(TAG, throwable);
                        }
                    }).onErrorResumeNext(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return repository.getNotes();
                        } else {
                            return Observable.empty();
                        }
                    });
        } else if (extendedFilter == FilterType.LINK) {
            loadNotes = repository.getLink(linkFilter)
                    .toObservable()
                    .flatMap(link -> {
                        filterType = FilterType.LINK;
                        linkFilter = link.getId();
                        String linkName = link.getName() == null ? link.getLink() : link.getName();
                        laanoUiManager.setFilterType(TAB, filterType, linkName);
                        return repository.getNotes();
                    }).doOnError(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            setDefaultNotesFilterType();
                            linkFilter = null;
                            settings.setLinkFilter(null);
                        } else {
                            CommonUtils.logStackTrace(TAG, throwable);
                        }
                    }).onErrorResumeNext(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return repository.getNotes();
                        } else {
                            return Observable.empty();
                        }
                    });
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
                        case UNBOUND:
                            return note.getLinkId() == null;
                        case ALL:
                        default:
                            return true;
                    }
                })
                .toList()
                .observeOn(schedulerProvider.ui())
                .doFinally(() -> {
                    if (showLoading) {
                        viewModel.hideProgressOverlay();
                    }
                    laanoUiManager.updateTitle(TAB);
                })
                .subscribe(notes -> {
                    view.showNotes(notes);
                    selectNoteFilter();
                }, throwable -> {
                    // NullPointerException
                    CommonUtils.logStackTrace(TAG, throwable);
                    viewModel.showDatabaseErrorSnackbar();
                });
        compositeDisposable.add(disposable);
    }

    @Override
    public void onNoteClick(String noteId, boolean isConflicted) {
        if (viewModel.isActionMode()) {
            onNoteSelected(noteId);
        } else if (isConflicted) {
            // NOTE: Note doesn't have AUTO conflict resolution option
            view.showConflictResolution(noteId);
        } else if (Settings.GLOBAL_ITEM_CLICK_SELECT_FILTER) {
            boolean selected = viewModel.toggleFilterId(noteId);
            // NOTE: filterType will be updated accordingly on the tab
            if (selected) {
                settings.setNoteFilter(noteId);
            } else {
                settings.setNoteFilter(null);
            }
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
        viewModel.toggleSelection(noteId);
        view.selectionChanged(noteId);
    }

    @Override
    public void selectNoteFilter() {
        if (viewModel.isActionMode()) return;

        String noteFilter = settings.getNoteFilter();
        if (noteFilter != null) {
            int position = getPosition(noteFilter);
            if (position >= 0) { // NOTE: check if there is the filter in the list
                viewModel.setFilterId(noteFilter);
            }
        }
    }

    @Override
    public void onEditClick(@NonNull String noteId) {
        checkNotNull(noteId);
        view.showEditNote(noteId);
    }

    @Override
    public void onToLinksClick(@NonNull String noteId) {
        checkNotNull(noteId);
        viewModel.setFilterId(noteId);
        settings.setFilterType(LinksPresenter.SETTING_LINKS_FILTER_TYPE, FilterType.NOTE);
        settings.setNoteFilter(noteId);
        laanoUiManager.setCurrentTab(LaanoFragmentPagerAdapter.LINKS_TAB);
    }

    @Override
    public void onToggleClick(@NonNull String noteId) {
        checkNotNull(noteId);
        viewModel.toggleVisibility(noteId);
        view.visibilityChanged(noteId);
    }

    @Override
    public void onDeleteClick() {
        ArrayList<String> selectedIds = viewModel.getSelectedIds();
        view.confirmNotesRemoval(selectedIds);
    }

    @Override
    public void onSelectAllClick() {
        String[] ids = view.getIds();
        int listSize = ids.length;
        if (listSize <= 0) return;

        int selectedCount = viewModel.getSelectedCount();
        if (selectedCount > listSize / 2) {
            viewModel.setSelection(null);
        } else {
            viewModel.setSelection(ids);
        }
    }

    @Override
    public void syncSavedNote(final String linkId, @NonNull final String noteId) {
        checkNotNull(noteId);
        if (linkId != null) {
            // NOTE: repository do not control other Item's cache
            repository.refreshLink(linkId); // saved
        }
        boolean sync = settings.isSyncable() && settings.isOnline();
        if (!sync) {
            if (settings.isSyncable() || settings.getLastSyncTime() > 0) {
                settings.setSyncStatus(SyncAdapter.SYNC_STATUS_UNSYNCED);
                laanoUiManager.updateSyncStatus();
            }
            return;
        }
        repository.syncSavedNote(noteId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doFinally(this::updateSyncStatus)
                .subscribe(itemState -> {
                    Log.d(TAG, "syncSavedNote() -> subscribe(): [" + itemState.name() + "]");
                    switch (itemState) {
                        case CONFLICTED:
                            updateTabNormalState();
                            if (linkId != null) {
                                repository.refreshLink(linkId); // synced
                            }
                            loadNotes(false);
                            laanoUiManager.showLongToast(R.string.toast_sync_conflict);
                            break;
                        case ERROR_CLOUD:
                            settings.setSyncStatus(SyncAdapter.SYNC_STATUS_ERROR);
                            laanoUiManager.showLongToast(R.string.toast_sync_error);
                            break;
                        case SAVED:
                            updateTabNormalState();
                            if (linkId != null) {
                                repository.refreshLink(linkId); // synced
                            }
                            loadNotes(false);
                            laanoUiManager.showShortToast(R.string.toast_sync_success);
                            break;
                    }
                }, throwable -> CommonUtils.logStackTrace(TAG, throwable));
    }

    @Override
    public void deleteNotes(ArrayList<String> selectedIds) {
        boolean sync = settings.isSyncable() && settings.isOnline();
        Observable.fromIterable(selectedIds)
                .flatMap(noteId -> {
                    Log.d(TAG, "deleteNotes(): [" + noteId + "]");
                    return repository.deleteNote(noteId, sync)
                            .subscribeOn(schedulerProvider.io())
                            .observeOn(schedulerProvider.ui())
                            .doOnNext(itemState -> {
                                Log.d(TAG, "deleteNotes() -> doOnNext(): [" + itemState.name() + "]");
                                if (itemState == DataSource.ItemState.DELETED
                                        || itemState == DataSource.ItemState.DEFERRED) {
                                    // NOTE: can be called twice
                                    view.removeNote(noteId);
                                    settings.resetNoteFilter(noteId);
                                }
                            });
                })
                .filter(itemState -> itemState == DataSource.ItemState.CONFLICTED
                        || itemState == DataSource.ItemState.ERROR_LOCAL
                        || itemState == DataSource.ItemState.ERROR_CLOUD)
                .toList()
                .doFinally(() -> {
                    if (sync) {
                        this.updateSyncStatus();
                    } else if (settings.isSyncable()) {
                        settings.setSyncStatus(SyncAdapter.SYNC_STATUS_UNSYNCED);
                        laanoUiManager.updateSyncStatus();
                    }
                })
                .subscribe(itemStates -> {
                    Log.d(TAG, "deleteNotes(): Completed [" + itemStates.toString() + "]");
                    if (itemStates.isEmpty()) {
                        // DELETED or DEFERRED if sync is disabled
                        //viewModel.showDeleteSuccessSnackbar();
                        if (sync) {
                            laanoUiManager.showShortToast(R.string.toast_sync_success);
                        }
                    } else if (itemStates.contains(DataSource.ItemState.CONFLICTED)) {
                        laanoUiManager.showLongToast(R.string.toast_sync_conflict);
                    } else if (itemStates.contains(DataSource.ItemState.ERROR_LOCAL)) {
                        viewModel.showDatabaseErrorSnackbar();
                    } else if (itemStates.contains(DataSource.ItemState.ERROR_CLOUD)) {
                        settings.setSyncStatus(SyncAdapter.SYNC_STATUS_ERROR);
                        laanoUiManager.showLongToast(R.string.toast_sync_error);
                    }
                    loadNotes(false);
                    updateTabNormalState();
                }, throwable -> CommonUtils.logStackTrace(TAG, throwable));
    }

    @Override
    public void updateSyncStatus() {
        int status = settings.getSyncStatus();
        if (status == SyncAdapter.SYNC_STATUS_ERROR
                || status == SyncAdapter.SYNC_STATUS_UNSYNCED) {
            // NOTE: only SyncAdapter can reset these statuses
            laanoUiManager.updateSyncStatus();
            return;
        }
        repository.getSyncStatus()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(syncStatus -> {
                    settings.setSyncStatus(syncStatus);
                    laanoUiManager.updateSyncStatus();
                }, throwable -> viewModel.showDatabaseErrorSnackbar());
    }

    @Override
    public int getPosition(String noteId) {
        return view.getPosition(noteId);
    }

    @Override
    public void setFilterType(@NonNull FilterType filterType) {
        checkNotNull(filterType);
        settings.setFilterType(SETTING_NOTES_FILTER_TYPE, filterType);
        if (this.filterType != filterType) {
            loadNotes(false);
        }
    }

    @Override
    @NonNull
    public FilterType getFilterType() {
        return settings.getFilterType(SETTING_NOTES_FILTER_TYPE);
    }

    /**
     * @return Return null if there is no additional data is required
     */
    @Nullable
    private FilterType updateFilter() {
        FilterType filterType = getFilterType();
        String prevLinkFilter = this.linkFilter;
        this.linkFilter = settings.getLinkFilter();
        // NOTE: there may be some concurrency who actually will reset the filter, but it OK
        String prevFavoriteFilter = this.favoriteFilter;
        this.favoriteFilter = settings.getFavoriteFilter();
        switch (filterType) {
            case ALL:
            case CONFLICTED:
            case NO_TAGS:
            case UNBOUND:
                if (this.filterType == filterType) {
                    return null;
                }
                this.filterType = filterType;
                laanoUiManager.setFilterType(TAB, filterType, null);
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
        filterType = Settings.DEFAULT_FILTER_TYPE;
        laanoUiManager.setFilterType(TAB, filterType, null);
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
        repository.isConflictedNotes()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        conflicted -> laanoUiManager.setTabNormalState(TAB, conflicted),
                        throwable -> viewModel.showDatabaseErrorSnackbar());
    }

    @Override
    public boolean isNotesLayoutModeReading() {
        return settings.isNotesLayoutModeReading();
    }

    /**
     * @return Return true if reading mode is set to Enabled
     */
    @Override
    public boolean toggleNotesLayoutModeReading() {
        boolean readingMode = !settings.isNotesLayoutModeReading();
        settings.setNotesLayoutModeReading(readingMode);
        return readingMode;
    }

    @Override
    public void onRepositoryDelete(@NonNull String id, @NonNull DataSource.ItemState itemState) {
        if (itemState == DataSource.ItemState.SAVED) {
            throw new IllegalStateException("SAVED state is not allowed to Delete operation");
        }
    }

    @Override
    public void onRepositorySave(@NonNull String id, @NonNull DataSource.ItemState itemState) {
        if (itemState == DataSource.ItemState.DELETED) {
            throw new IllegalStateException("DELETED state is not allowed to Save operation");
        }
    }
}
