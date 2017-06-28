/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bytesforge.linkasanote.laano.notes;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.BaseItemPresenter;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncAdapter;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

public final class NotesPresenter extends BaseItemPresenter implements
        NotesContract.Presenter, DataSource.Callback {

    private static final String TAG = NotesPresenter.class.getSimpleName();
    private static final String TAG_E = NotesPresenter.class.getCanonicalName();

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

    private String favoriteFilterId;
    private int favoriteHashCode;
    private String linkFilterId;
    private int linkHashCode;
    private int noteCacheSize = -1;
    private FilterType filterType;
    private boolean filterIsChanged = true;
    private boolean loadIsCompleted = true;
    private boolean loadIsDeferred = false;
    private long lastSyncTime;

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
        lastSyncTime = settings.getLastNotesSyncTime();
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
        if (!loadIsCompleted) {
            loadIsCompleted = true;
            loadIsDeferred = true;
        }
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
        view.startAddNoteActivity(filterType == FilterType.LINK ? linkFilterId : null);
    }

    @Override
    public void loadNotes(final boolean forceUpdate) {
        loadNotes(forceUpdate, false);
    }

    private void loadNotes(boolean forceUpdate, final boolean forceShowLoading) {
        final long syncTime = settings.getLastNotesSyncTime();
        Log.d(TAG, "loadNotes() [synced=" + (lastSyncTime == syncTime) +
                ", loadIsComplete=" + loadIsCompleted + ", loadIsDeferred=" + loadIsDeferred + "]");
        if (!view.isActive()) {
            Log.d(TAG, "loadNotes(): View is not active");
            return;
        }
        if (forceUpdate) { // NOTE: for testing and for the future option to reload by swipe
            repository.refreshLinks();
        }
        if (!loadIsCompleted) {
            loadIsDeferred = true;
            return;
        }
        FilterType extendedFilter = updateFilter();
        if (!repository.isNoteCacheNeedRefresh()
                && !filterIsChanged
                && noteCacheSize == repository.getNoteCacheSize()
                && lastSyncTime == syncTime
                && !loadIsDeferred) {
            return;
        }
        compositeDisposable.clear();
        loadIsCompleted = false;
        loadIsDeferred = false;
        if (lastSyncTime != syncTime) {
            lastSyncTime = syncTime;
            repository.checkNotesSyncLog();
            updateTabNormalState();
        }
        Observable<Note> loadNotes = null;
        if (extendedFilter == FilterType.FAVORITE) {
            loadNotes = repository.getFavorite(favoriteFilterId)
                    .toObservable()
                    .flatMap(favorite -> {
                        // NOTE: just to be sure we are still in sync
                        filterType = FilterType.FAVORITE;
                        favoriteFilterId = favorite.getId();
                        favoriteHashCode = favorite.hashCode();
                        settings.setFavoriteFilter(favorite);
                        laanoUiManager.setFilterType(TAB, filterType);
                        return repository.getNotes();
                    }).doOnError(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            setDefaultNotesFilterType();
                            favoriteFilterId = null;
                            settings.setFavoriteFilterId(null);
                        } else {
                            CommonUtils.logStackTrace(TAG_E, throwable);
                        }
                    }).onErrorResumeNext(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return repository.getNotes();
                        } else {
                            return Observable.empty();
                        }
                    });
        } else if (extendedFilter == FilterType.LINK) {
            loadNotes = repository.getLink(linkFilterId)
                    .toObservable()
                    .flatMap(link -> {
                        filterType = FilterType.LINK;
                        linkFilterId = link.getId();
                        linkHashCode = link.hashCode();
                        settings.setLinkFilter(link);
                        laanoUiManager.setFilterType(TAB, filterType);
                        return repository.getNotes();
                    }).doOnError(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            setDefaultNotesFilterType();
                            linkFilterId = null;
                            settings.setLinkFilterId(null);
                        } else {
                            CommonUtils.logStackTrace(TAG_E, throwable);
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
        final String searchText;
        String text = viewModel.getSearchText();
        if (!Strings.isNullOrEmpty(text)) {
            searchText = text.toLowerCase();
        } else {
            searchText = null;
        }
        boolean filterIsActive = (searchText != null || filterType != FilterType.ALL);
        boolean loadByChunk = repository.isNoteCacheDirty();
        boolean showProgress = ((!loadByChunk || filterIsActive)
                && repository.getNoteCacheSize() == 0) || forceShowLoading;
        final AtomicBoolean firstChunk = new AtomicBoolean(true);
        if (showProgress) viewModel.showProgressOverlay();
        Log.d(TAG, "loadNotes(): getNotes() [showProgress=" + showProgress + ", loadByChunk=" + loadByChunk + "]");
        Disposable disposable = loadNotes
                .subscribeOn(schedulerProvider.computation())
                .retryWhen(throwableObservable -> throwableObservable.flatMap(throwable -> {
                    if (throwable instanceof IllegalStateException) {
                        Log.d(TAG, "loadNotes(): retry [" + repository.getNoteCacheSize() + "]");
                        // NOTE: it seems the system is too busy to update UI properly while Sync in progress,
                        //       so it needles to switch loading to by chunk mode
                        return Observable.just(new Object()).compose(
                                upstream -> showProgress
                                        ? upstream.delay(25, TimeUnit.MILLISECONDS)
                                        : upstream);
                    }
                    return Observable.error(throwable);
                }))
                .filter(note -> {
                    if (searchText != null) {
                        String noteNote = note.getNote();
                        if (noteNote == null
                                || !noteNote.toLowerCase().contains(searchText)) {
                            return false;
                        }
                    }
                    switch (filterType) {
                        case CONFLICTED:
                            return note.isConflicted();
                        case LINK:
                            String linkId = note.getLinkId();
                            return linkId != null && linkId.equals(linkFilterId);
                        case FAVORITE:
                            List<Tag> noteTags = note.getTags();
                            if (favoriteFilterId == null) {
                                return true; // No filter
                            } else if (noteTags == null) {
                                return false; // No tags
                            }
                            Favorite favoriteFilter = settings.getFavoriteFilter();
                            if (favoriteFilter == null
                                    || !favoriteFilter.getId().equals(favoriteFilterId)
                                    || favoriteFilter.getTags() == null) {
                                Log.e(TAG, "Invalid Favorite filter on Notes tab");
                                return false;
                            }
                            if (favoriteFilter.isAndGate()) {
                                return noteTags.containsAll(favoriteFilter.getTags());
                            } else {
                                return !Collections.disjoint(favoriteFilter.getTags(), noteTags);
                            }
                        case NO_TAGS:
                            return note.getTags() == null;
                        case UNBOUND:
                            return note.getLinkId() == null;
                        case ALL:
                        default:
                            return true;
                    }
                })
                .compose(upstream -> loadByChunk
                        ? upstream.buffer(Settings.GLOBAL_QUERY_CHUNK_SIZE)
                        : upstream.toList().toObservable())
                .observeOn(schedulerProvider.ui())
                .doOnComplete(() -> {
                    loadIsCompleted = true; // NOTE: must be set before loadNotes()
                    filterIsChanged = false;
                    noteCacheSize = repository.getNoteCacheSize();
                    if (view.isActive()) {
                        view.updateView();
                    }
                    if (loadIsDeferred) {
                        new Handler().postDelayed(() -> loadNotes(false, forceShowLoading),
                                Settings.GLOBAL_DEFER_RELOAD_DELAY_MILLIS);
                    }
                })
                .doFinally(() -> {
                    laanoUiManager.updateTitle(TAB);
                    viewModel.hideProgressOverlay();
                })
                .subscribe(notes -> {
                    Log.d(TAG, "loadNotes(): subscribe() [" + notes.size() + "]");
                    // NOTE: just to be logical
                    if (loadByChunk && !firstChunk.getAndSet(false)) {
                        view.addNotes(notes);
                    } else {
                        view.showNotes(notes);
                    }
                    selectNoteFilter();
                    laanoUiManager.updateTitle(TAB);
                    viewModel.hideProgressOverlay();
                }, throwable -> {
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    loadIsCompleted = true;
                    viewModel.showDatabaseErrorSnackbar();
                });
        compositeDisposable.add(disposable);
    }

    @Override
    public void onNoteClick(String noteId, boolean isConflicted) {
        if (viewModel.isActionMode()) {
            onNoteSelected(noteId);
        } else if (isConflicted) {
            if (!settings.isSyncable()) {
                laanoUiManager.showApplicationNotSyncableSnackbar();
                return;
            } else if (!settings.isOnline()) {
                laanoUiManager.showApplicationOfflineSnackbar();
                return;
            }
            // NOTE: Note doesn't have AUTO conflict resolution option
            view.showConflictResolution(noteId);
        } else {
            if (Settings.GLOBAL_ITEM_CLICK_SELECT_FILTER) {
                boolean selected = viewModel.toggleFilterId(noteId);
                // NOTE: filterType will be updated accordingly on the tab
                if (selected) {
                    settings.setNoteFilterId(noteId);
                } else {
                    settings.setNoteFilterId(null);
                }
            } else {
                onToggleClick(noteId);
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

        String noteFilter = settings.getNoteFilterId();
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
        settings.setLinksFilterType(FilterType.NOTE);
        settings.setNoteFilterId(noteId);
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
                }, throwable -> CommonUtils.logStackTrace(TAG_E, throwable));
    }

    @Override
    public void deleteNotes(ArrayList<String> selectedIds) {
        boolean sync = settings.isSyncable() && settings.isOnline();
        if (sync) {
            laanoUiManager.setSyncDrawerMenu();
        }
        long started = currentTimeMillis();
        Observable.fromIterable(selectedIds)
                .flatMap(noteId -> {
                    return repository.deleteNote(noteId, sync, started)
                            // NOTE: Sync will be concatenated on .io() scheduler
                            .subscribeOn(schedulerProvider.computation())
                            .observeOn(schedulerProvider.ui())
                            .doOnNext(itemState -> {
                                if (sync) {
                                    laanoUiManager.setSyncDrawerMenu();
                                }
                                if (itemState == DataSource.ItemState.DELETED
                                        || itemState == DataSource.ItemState.DEFERRED) {
                                    // NOTE: can be called twice
                                    if (view.isActive()) {
                                        view.removeNote(noteId);
                                    }
                                    settings.resetNoteFilterId(noteId);
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
                        laanoUiManager.setNormalDrawerMenu();
                    } else if (settings.isSyncable()) {
                        settings.setSyncStatus(SyncAdapter.SYNC_STATUS_UNSYNCED);
                        laanoUiManager.updateSyncStatus();
                    }
                })
                .subscribe(itemStates -> {
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
                }, throwable -> CommonUtils.logStackTrace(TAG_E, throwable));
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
        settings.setNotesFilterType(filterType);
        if (this.filterType != filterType) {
            loadNotes(false);
        }
    }

    @Override
    @NonNull
    public FilterType getFilterType() {
        return settings.getNotesFilterType();
    }

    @Override
    @Nullable
    public Boolean isFavoriteAndGate() {
        Favorite favoriteFilter = settings.getFavoriteFilter();
        return favoriteFilter != null && favoriteFilter.isAndGate();
    }

    /**
     * @return Return null if there is no additional data is required
     */
    @Nullable
    private FilterType updateFilter() {
        FilterType filterType = getFilterType();

        String prevLinkFilterId = this.linkFilterId;
        this.linkFilterId = settings.getLinkFilterId();
        Link linkFilter = settings.getLinkFilter();
        int prevLinkHashCode = this.linkHashCode;
        this.linkHashCode = linkFilter == null ? 0 : linkFilter.hashCode();

        String prevFavoriteFilterId = this.favoriteFilterId;
        this.favoriteFilterId = settings.getFavoriteFilterId();
        Favorite favoriteFilter = settings.getFavoriteFilter();
        int prevFavoriteHashCode = this.favoriteHashCode;
        this.favoriteHashCode = favoriteFilter == null ? 0 : favoriteFilter.hashCode();
        if (filterType != FilterType.FAVORITE
                && favoriteFilter == null
                && this.favoriteFilterId != null) {
            // NOTE: preload Favorite filter
            repository.getFavorite(favoriteFilterId)
                    .subscribeOn(schedulerProvider.computation())
                    .subscribe(favorite -> {
                        favoriteFilterId = favorite.getId();
                        favoriteHashCode = favorite.hashCode();
                        settings.setFavoriteFilter(favorite);
                    }, throwable -> {
                        favoriteFilterId = null;
                        favoriteHashCode = 0;
                        settings.setFavoriteFilter(null);
                    });
        }

        switch (filterType) {
            case ALL:
            case CONFLICTED:
            case NO_TAGS:
            case UNBOUND:
                if (this.filterType == filterType) {
                    return null;
                }
                filterIsChanged = true;
                this.filterType = filterType;
                laanoUiManager.setFilterType(TAB, filterType);
                break;
            case LINK:
                if (this.filterType == filterType
                        && this.linkFilterId != null
                        && this.linkFilterId.equals(prevLinkFilterId)
                        && this.linkHashCode == prevLinkHashCode) {
                    return null;
                }
                filterIsChanged = true;
                if (this.linkFilterId == null) {
                    setDefaultNotesFilterType();
                    return null;
                }
                this.filterType = filterType;
                return filterType;
            case FAVORITE:
                if (this.filterType == filterType
                        && this.favoriteFilterId != null
                        && this.favoriteFilterId.equals(prevFavoriteFilterId)
                        && this.favoriteHashCode == prevFavoriteHashCode) {
                    return null;
                }
                filterIsChanged = true;
                if (this.favoriteFilterId == null) {
                    setDefaultNotesFilterType();
                    return null;
                }
                this.filterType = filterType;
                return filterType;
            default:
                filterIsChanged = true;
                setDefaultNotesFilterType();
        }
        return null;
    }

    private void setDefaultNotesFilterType() {
        filterType = Settings.DEFAULT_FILTER_TYPE;
        laanoUiManager.setFilterType(TAB, filterType);
        settings.setNotesFilterType(filterType);
    }

    @Override
    public boolean isFavoriteFilter() {
        return favoriteFilterId != null;
    }

    @Override
    public boolean isLinkFilter() {
        return linkFilterId != null;
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
    public boolean toggleNotesLayoutMode() {
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

    @Override
    public void setFilterIsChanged(boolean filterIsChanged) {
        this.filterIsChanged = filterIsChanged;
    }
}
