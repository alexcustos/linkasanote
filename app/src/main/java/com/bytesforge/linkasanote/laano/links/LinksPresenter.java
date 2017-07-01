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

package com.bytesforge.linkasanote.laano.links;

import android.net.Uri;
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
import com.bytesforge.linkasanote.laano.links.conflictresolution.LinksConflictResolutionDialog;
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

public final class LinksPresenter extends BaseItemPresenter implements
        LinksContract.Presenter, DataSource.Callback {

    private static final String TAG = LinksPresenter.class.getSimpleName();
    private static final String TAG_E = LinksPresenter.class.getCanonicalName();

    private static final int TAB = LaanoFragmentPagerAdapter.LINKS_TAB;

    public static final String SETTING_LINKS_FILTER_TYPE = "LINKS_FILTER_TYPE";

    private final Repository repository;
    private final LinksContract.View view;
    private final LinksContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final LaanoUiManager laanoUiManager;
    private final Settings settings;

    @NonNull
    private final CompositeDisposable compositeDisposable;

    private String favoriteFilterId;
    private int favoriteHashCode;
    private String noteFilterId;
    private int noteHashCode;
    private int linkCacheSize = -1;
    private FilterType filterType;
    private boolean filterIsChanged = true;
    private boolean loadIsCompleted = true;
    private boolean loadIsDeferred = false;
    private long lastSyncTime;

    @Inject
    LinksPresenter(
            Repository repository, LinksContract.View view,
            LinksContract.ViewModel viewModel, BaseSchedulerProvider schedulerProvider,
            LaanoUiManager laanoUiManager, Settings settings) {
        this.repository = repository;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.laanoUiManager = laanoUiManager;
        this.settings = settings;
        compositeDisposable = new CompositeDisposable();
        lastSyncTime = settings.getLastLinksSyncTime();
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
    }

    @Override
    public void subscribe() {
        repository.addLinksCallback(this);
    }

    @Override
    public void unsubscribe() {
        compositeDisposable.clear();
        repository.removeLinksCallback(this);
        if (!loadIsCompleted) {
            loadIsCompleted = true;
            loadIsDeferred = true;
        }
    }

    @Override
    public void onTabSelected() {
        loadLinks(false);
    }

    @Override
    public void onTabDeselected() {
        view.finishActionMode();
    }

    @Override
    public void showAddLink() {
        view.startAddLinkActivity();
    }

    @Override
    public void loadLinks(final boolean forceUpdate) {
        loadLinks(forceUpdate, false);
    }

    private void loadLinks(final boolean forceUpdate, final boolean forceShowLoading) {
        final long syncTime = settings.getLastLinksSyncTime();
        Log.d(TAG, "loadLinks() [synced=" + (lastSyncTime == syncTime) +
                ", loadIsComplete=" + loadIsCompleted + ", loadIsDeferred=" + loadIsDeferred + "]");
        if (!view.isActive()) {
            Log.d(TAG, "loadLinks(): View is not active");
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
        if (!repository.isLinkCacheNeedRefresh()
                && !filterIsChanged
                && linkCacheSize == repository.getLinkCacheSize()
                && lastSyncTime == syncTime
                && !loadIsDeferred) {
            return;
        }
        compositeDisposable.clear();
        loadIsCompleted = false;
        loadIsDeferred = false;
        if (lastSyncTime != syncTime) {
            lastSyncTime = syncTime;
            repository.checkLinksSyncLog();
            updateTabNormalState();
        }
        Observable<Link> loadLinks = null;
        if (extendedFilter == FilterType.FAVORITE) {
            loadLinks = repository.getFavorite(favoriteFilterId)
                    .toObservable()
                    .flatMap(favorite -> {
                        // NOTE: just to be sure we are still in sync
                        filterType = FilterType.FAVORITE;
                        favoriteFilterId = favorite.getId();
                        favoriteHashCode = favorite.hashCode();
                        settings.setFavoriteFilter(favorite);
                        laanoUiManager.setFilterType(TAB, filterType);
                        return repository.getLinks();
                    }).doOnError(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            setDefaultLinksFilterType();
                            favoriteFilterId = null;
                            settings.setFavoriteFilterId(null);
                            settings.setFavoriteFilter(null);
                        } else {
                            CommonUtils.logStackTrace(TAG_E, throwable);
                        }
                    }).onErrorResumeNext(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return repository.getLinks();
                        } else {
                            return Observable.empty();
                        }
                    });
        } else if (extendedFilter == FilterType.NOTE) {
            loadLinks = repository.getNote(noteFilterId)
                    .toObservable()
                    .flatMap(note -> {
                        filterType = FilterType.NOTE;
                        noteFilterId = note.getId();
                        noteHashCode = note.hashCode();
                        settings.setNoteFilter(note);
                        laanoUiManager.setFilterType(TAB, filterType);
                        return repository.getLinks();
                    }).doOnError(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            setDefaultLinksFilterType();
                            noteFilterId = null;
                            settings.setNoteFilterId(null);
                            settings.setNoteFilter(null);
                        } else {
                            CommonUtils.logStackTrace(TAG_E, throwable);
                        }
                    }).onErrorResumeNext(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return repository.getLinks();
                        } else {
                            return Observable.empty();
                        }
                    });
        }
        if (loadLinks == null) {
            loadLinks = repository.getLinks();
        }
        final String searchText;
        String text = viewModel.getSearchText();
        if (!Strings.isNullOrEmpty(text)) {
            searchText = text.toLowerCase();
        } else {
            searchText = null;
        }
        boolean filterIsActive = (searchText != null || filterType != FilterType.ALL);
        boolean loadByChunk = repository.isLinkCacheDirty();
        // TODO: replace progress layout with SwipeRefreshLayout and remove this condition
        boolean showProgress = ((!loadByChunk || filterIsActive)
                && repository.getLinkCacheSize() == 0) || forceShowLoading;
        final AtomicBoolean firstChunk = new AtomicBoolean(true);
        if (showProgress) viewModel.showProgressOverlay();
        Log.d(TAG, "loadLinks(): getLinks() [showProgress=" + showProgress + ", loadByChunk=" + loadByChunk + "]");
        Disposable disposable = loadLinks
                .subscribeOn(schedulerProvider.computation())
                .retryWhen(throwableObservable -> throwableObservable.flatMap(throwable -> {
                    if (throwable instanceof IllegalStateException) {
                        Log.d(TAG, "loadLinks(): retry [" + repository.getLinkCacheSize() + "]");
                        // NOTE: it seems the system is too busy to update UI properly while Sync in progress,
                        //       so it needles to switch loading to by chunk mode
                        return Observable.just(new Object()).compose(
                                upstream -> showProgress
                                        ? upstream.delay(25, TimeUnit.MILLISECONDS)
                                        : upstream);
                    }
                    return Observable.error(throwable);
                }))
                .filter(link -> {
                    if (searchText != null) {
                        boolean found = false;
                        String linkName = link.getName();
                        if (linkName != null && linkName.toLowerCase().contains(searchText)) {
                            found = true;
                        }
                        String linkLink = link.getLink();
                        if (linkLink != null && linkLink.toLowerCase().contains(searchText)) {
                            found = true;
                        }
                        if (!found) return false;
                    }
                    // OPTIMIZATION: query the filtered data set may be more efficient
                    switch (filterType) {
                        case CONFLICTED:
                            return link.isConflicted();
                        case FAVORITE:
                            List<Tag> linkTags = link.getTags();
                            if (favoriteFilterId == null) {
                                return true; // No filter
                            } else if (linkTags == null) {
                                return false; // No tags
                            }
                            Favorite favoriteFilter = settings.getFavoriteFilter();
                            if (favoriteFilter == null
                                    || !favoriteFilter.getId().equals(favoriteFilterId)
                                    || favoriteFilter.getTags() == null) {
                                Log.e(TAG, "Invalid Favorite filter on Links tab");
                                return false;
                            }
                            if (favoriteFilter.isAndGate()) {
                                return linkTags.containsAll(favoriteFilter.getTags());
                            } else {
                                return !Collections.disjoint(favoriteFilter.getTags(), linkTags);
                            }
                        case NOTE:
                            List<Note> notes = link.getNotes();
                            if (notes == null) return false;
                            for (Note note : notes) {
                                if (note.getId().equals(noteFilterId)) return true;
                            }
                            return false;
                        case NO_TAGS:
                            return link.getTags() == null;
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
                    loadIsCompleted = true; // NOTE: must be set before loadLinks()
                    filterIsChanged = false;
                    linkCacheSize = repository.getLinkCacheSize();
                    if (view.isActive()) {
                        view.updateView();
                    }
                    if (loadIsDeferred) {
                        new Handler().postDelayed(() -> loadLinks(false, forceShowLoading),
                                Settings.GLOBAL_DEFER_RELOAD_DELAY_MILLIS);
                    }
                })
                .doFinally(() -> {
                    laanoUiManager.updateTitle(TAB);
                    viewModel.hideProgressOverlay();
                })
                .subscribe(links -> {
                    Log.d(TAG, "loadLinks(): subscribe() [" + links.size() + "]");
                    // NOTE: just to be logical
                    if (loadByChunk && !firstChunk.getAndSet(false)) {
                        view.addLinks(links);
                    } else {
                        view.showLinks(links);
                    }
                    selectLinkFilter();
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
    public void onLinkClick(String linkId, boolean isConflicted, int numNotes) {
        if (viewModel.isActionMode()) {
            onLinkSelected(linkId);
        } else if (isConflicted) {
            if (!settings.isSyncable()) {
                laanoUiManager.showApplicationNotSyncableSnackbar();
                return;
            } else if (!settings.isOnline()) {
                laanoUiManager.showApplicationOfflineSnackbar();
                return;
            }
            repository.autoResolveLinkConflict(linkId)
                    .subscribeOn(schedulerProvider.computation())
                    .observeOn(schedulerProvider.ui())
                    .subscribe(success -> {
                        if (success) {
                            view.onActivityResult(
                                    LinksFragment.REQUEST_LINK_CONFLICT_RESOLUTION,
                                    LinksConflictResolutionDialog.RESULT_OK, null);
                        } else {
                            if (settings.isShowConflictResolutionWarning()) {
                                view.showConflictResolutionWarning(linkId);
                            } else {
                                view.showConflictResolution(linkId);
                            }
                        }
                    }, throwable -> {
                        if (throwable instanceof NullPointerException) {
                            viewModel.showDatabaseErrorSnackbar();
                        } else {
                            // NOTE: if there is any problem show it in the dialog
                            view.showConflictResolution(linkId);
                        }
                    });
        } else {
            if (Settings.GLOBAL_ITEM_CLICK_SELECT_FILTER) {
                boolean selected = viewModel.toggleFilterId(linkId);
                // NOTE: filterType will be updated accordingly on the tab
                if (selected) {
                    settings.setLinkFilterId(linkId);
                } else {
                    settings.setLinkFilterId(null);
                    settings.setLinkFilter(null);
                }
            } else if (numNotes > 0){
                onToggleClick(linkId);
            }
        }
    }

    @Override
    public boolean onLinkLongClick(String linkId) {
        view.enableActionMode();
        onLinkSelected(linkId);
        return true;
    }

    @Override
    public void onCheckboxClick(String linkId) {
        onLinkSelected(linkId);
    }

    private void onLinkSelected(String linkId) {
        viewModel.toggleSelection(linkId);
        view.selectionChanged(linkId);
    }

    @Override
    public void selectLinkFilter() {
        if (viewModel.isActionMode()) return;

        String linkFilter = settings.getLinkFilterId();
        if (linkFilter != null) {
            int position = getPosition(linkFilter);
            if (position >= 0) {
                viewModel.setFilterId(linkFilter);
            }
        }
    }

    @Override
    public void onEditClick(@NonNull String linkId) {
        checkNotNull(linkId);
        view.showEditLink(linkId);
    }

    @Override
    public void onLinkOpenClick(@NonNull String linkId) {
        checkNotNull(linkId);
        repository.getLink(checkNotNull(linkId))
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(link -> {
                    Uri uri = Uri.parse(link.getLink());
                    if (view.isActive()) {
                        view.openLink(uri);
                    }
                }, throwable -> {
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    viewModel.showOpenLinkErrorSnackbar();
                });
    }

    @Override
    public void onToNotesClick(@NonNull String linkId) {
        checkNotNull(linkId);
        viewModel.setFilterId(linkId);
        settings.setNotesFilterType(FilterType.LINK);
        settings.setLinkFilterId(linkId);
        laanoUiManager.setCurrentTab(LaanoFragmentPagerAdapter.NOTES_TAB);
    }

    @Override
    public void onAddNoteClick(@NonNull String linkId) {
        view.showAddNote(linkId);
    }

    @Override
    public void onToggleClick(@NonNull String linkId) {
        checkNotNull(linkId);
        viewModel.toggleVisibility(linkId);
        view.visibilityChanged(linkId);
    }

    @Override
    public void onDeleteClick() {
        ArrayList<String> selectedIds = viewModel.getSelectedIds();
        view.confirmLinksRemoval(selectedIds);
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
    public void syncSavedLink(@NonNull final String linkId) {
        checkNotNull(linkId);
        boolean sync = settings.isSyncable() && settings.isOnline();
        if (!sync) {
            if (settings.isSyncable() || settings.getLastSyncTime() > 0) {
                settings.setSyncStatus(SyncAdapter.SYNC_STATUS_UNSYNCED);
                laanoUiManager.updateSyncStatus();
            }
            return;
        }
        repository.syncSavedLink(linkId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doFinally(this::updateSyncStatus)
                .subscribe(itemState -> {
                    Log.d(TAG, "syncSavedLink() -> subscribe(): [" + itemState.name() + "]");
                    switch (itemState) {
                        case CONFLICTED:
                            updateTabNormalState();
                            loadLinks(false);
                            laanoUiManager.showLongToast(R.string.toast_sync_conflict);
                            break;
                        case ERROR_CLOUD:
                            settings.setSyncStatus(SyncAdapter.SYNC_STATUS_ERROR);
                            laanoUiManager.showLongToast(R.string.toast_sync_error);
                            break;
                        case SAVED:
                            updateTabNormalState();
                            loadLinks(false);
                            laanoUiManager.showShortToast(R.string.toast_sync_success);
                            break;
                    }
                }, throwable -> CommonUtils.logStackTrace(TAG_E, throwable));
    }

    @Override
    public void syncSavedNote(@NonNull final String linkId, @NonNull final String noteId) {
        checkNotNull(linkId);
        checkNotNull(noteId);
        boolean sync = settings.isSyncable() && settings.isOnline();
        if (!sync) {
            if (settings.isSyncable()) {
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
                            updateNotesTabNormalState();
                            repository.refreshLink(linkId); // synced
                            // NOTE: it is needless to force loadNotes, because conflicted state is practically impossible (UUID constraint)
                            loadLinks(false);
                            laanoUiManager.showLongToast(R.string.toast_sync_conflict);
                            break;
                        case ERROR_CLOUD:
                            settings.setSyncStatus(SyncAdapter.SYNC_STATUS_ERROR);
                            laanoUiManager.showLongToast(R.string.toast_sync_error);
                            break;
                        case SAVED:
                            updateNotesTabNormalState();
                            repository.refreshLink(linkId); // synced
                            loadLinks(false);
                            laanoUiManager.showShortToast(R.string.toast_sync_success);
                            break;
                    }
                }, throwable -> CommonUtils.logStackTrace(TAG_E, throwable));
    }

    @Override
    public void deleteLinks(
            @NonNull final ArrayList<String> selectedIds, final boolean deleteNotes) {
        checkNotNull(selectedIds);
        boolean sync = settings.isSyncable() && settings.isOnline();
        if (sync) {
            laanoUiManager.setSyncDrawerMenu();
        }
        long started = currentTimeMillis();
        Observable.fromIterable(selectedIds)
                .flatMap(linkId -> {
                    return repository.deleteLink(linkId, sync, started, deleteNotes)
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
                                        view.removeLink(linkId);
                                    }
                                    settings.resetLinkFilterId(linkId);
                                }
                            });
                })
                .filter(itemState -> itemState == DataSource.ItemState.CONFLICTED
                        || itemState == DataSource.ItemState.ERROR_LOCAL
                        || itemState == DataSource.ItemState.ERROR_CLOUD
                        || itemState == DataSource.ItemState.ERROR_EXTRA)
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
                    } else if (itemStates.contains(DataSource.ItemState.ERROR_EXTRA)) {
                        laanoUiManager.showLongToast(R.string.toast_error);
                    }
                    loadLinks(false);
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
                }, throwable -> {
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    viewModel.showDatabaseErrorSnackbar();
                });
    }

    @Override
    public int getPosition(String linkId) {
        return view.getPosition(linkId);
    }

    @Override
    public void setFilterType(@NonNull FilterType filterType) {
        checkNotNull(filterType);
        settings.setLinksFilterType(filterType);
        if (this.filterType != filterType) {
            loadLinks(false);
        }
    }

    @Override
    @NonNull
    public FilterType getFilterType() {
        return settings.getLinksFilterType();
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

        String prevNoteFilterId = this.noteFilterId;
        this.noteFilterId = settings.getNoteFilterId();
        Note noteFilter = settings.getNoteFilter();
        int prevNoteHashCode = this.noteHashCode;
        this.noteHashCode = noteFilter == null ? 0 : noteFilter.hashCode();

        switch (filterType) {
            case ALL:
            case CONFLICTED:
            case NO_TAGS:
                if (this.filterType == filterType) {
                    return null;
                }
                filterIsChanged = true;
                this.filterType = filterType;
                laanoUiManager.setFilterType(TAB, filterType);
                break;
            case FAVORITE:
                if (this.filterType == filterType
                        && this.favoriteFilterId != null
                        && this.favoriteFilterId.equals(prevFavoriteFilterId)
                        && this.favoriteHashCode == prevFavoriteHashCode) {
                    return null;
                }
                filterIsChanged = true;
                if (this.favoriteFilterId == null) {
                    setDefaultLinksFilterType();
                    return null;
                }
                this.filterType = filterType;
                return filterType;
            case NOTE:
                if (this.filterType == filterType
                        && this.noteFilterId != null
                        && this.noteFilterId.equals(prevNoteFilterId)
                        && this.noteHashCode == prevNoteHashCode) {
                    return null;
                }
                filterIsChanged = true;
                if (this.noteFilterId == null) {
                    setDefaultLinksFilterType();
                    return null;
                }
                this.filterType = filterType;
                return filterType;
            default:
                filterIsChanged = true;
                setDefaultLinksFilterType();
        }
        return null;
    }

    private void setDefaultLinksFilterType() {
        filterType = Settings.DEFAULT_FILTER_TYPE;
        laanoUiManager.setFilterType(TAB, filterType);
        settings.setLinksFilterType(filterType);
    }

    @Override
    public boolean isFavoriteFilter() {
        return favoriteFilterId != null;
    }

    @Override
    public boolean isNoteFilter() {
        return noteFilterId != null;
    }

    @Override
    public boolean isExpandLinks() {
        return settings.isExpandLinks();
    }

    @Override
    public void updateTabNormalState() {
        repository.isConflictedLinks()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        conflicted -> laanoUiManager.setTabNormalState(TAB, conflicted),
                        throwable -> {
                            CommonUtils.logStackTrace(TAG_E, throwable);
                            viewModel.showDatabaseErrorSnackbar();
                        });
    }

    private void updateNotesTabNormalState() {
        repository.isConflictedNotes()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        conflicted -> laanoUiManager.setTabNormalState(
                                LaanoFragmentPagerAdapter.NOTES_TAB, conflicted),
                        throwable -> {
                            CommonUtils.logStackTrace(TAG_E, throwable);
                            viewModel.showDatabaseErrorSnackbar();
                        });
    }

    @Override
    public void setShowConflictResolutionWarning(boolean show) {
        settings.setShowConflictResolutionWarning(show);
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
