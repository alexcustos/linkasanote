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

package com.bytesforge.linkasanote.laano.favorites;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.BaseItemPresenter;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.laano.favorites.conflictresolution.FavoritesConflictResolutionDialog;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncAdapter;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

public final class FavoritesPresenter extends BaseItemPresenter implements
        FavoritesContract.Presenter, DataSource.Callback {

    private static final String TAG = FavoritesPresenter.class.getSimpleName();
    private static final String TAG_E = FavoritesPresenter.class.getCanonicalName();

    private static final int TAB = LaanoFragmentPagerAdapter.FAVORITES_TAB;

    public static final String SETTING_FAVORITES_FILTER_TYPE = "FAVORITES_FILTER_TYPE";

    private final Repository repository;
    private final FavoritesContract.View view;
    private final FavoritesContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final LaanoUiManager laanoUiManager;
    private final Settings settings;

    @NonNull
    private final CompositeDisposable compositeDisposable;

    private int favoriteCacheSize = -1;
    private FilterType filterType;
    private boolean filterIsChanged = true;
    private boolean loadIsCompleted = true;
    private boolean loadIsDeferred = false;
    private long lastSyncTime;

    @Inject
    FavoritesPresenter(
            Repository repository, FavoritesContract.View view,
            FavoritesContract.ViewModel viewModel, BaseSchedulerProvider schedulerProvider,
            LaanoUiManager laanoUiManager, Settings settings) {
        this.repository = repository;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.laanoUiManager = laanoUiManager;
        this.settings = settings;
        lastSyncTime = settings.getLastFavoritesSyncTime();
        compositeDisposable = new CompositeDisposable();
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
    }

    @Override
    public void subscribe() {
        repository.addFavoritesCallback(this);
    }

    @Override
    public void unsubscribe() {
        compositeDisposable.clear();
        repository.removeFavoritesCallback(this);
        if (!loadIsCompleted) {
            loadIsCompleted = true;
            loadIsDeferred = true;
        }
    }

    @Override
    public void onTabSelected() {
        loadFavorites(false);
    }

    @Override
    public void onTabDeselected() {
        view.finishActionMode();
    }

    @Override
    public void showAddFavorite() {
        view.startAddFavoriteActivity();
    }

    @Override
    public void loadFavorites(final boolean forceUpdate) {
        loadFavorites(forceUpdate, false);
    }

    private void loadFavorites(final boolean forceUpdate, final boolean forceShowLoading) {
        final long syncTime = settings.getLastFavoritesSyncTime();
        Log.d(TAG, "loadFavorites() [synced=" + (lastSyncTime == syncTime) +
                ", loadIsComplete=" + loadIsCompleted + ", loadIsDeferred=" + loadIsDeferred + "]");
        if (!view.isActive()) {
            Log.d(TAG, "loadFavorites(): View is not active");
            return;
        }
        if (forceUpdate) { // NOTE: for testing and for the future option to reload by swipe
            repository.refreshLinks();
        }
        if (!loadIsCompleted) {
            loadIsDeferred = true;
            return;
        }
        updateFilter();
        if (!repository.isFavoriteCacheNeedRefresh()
                && !filterIsChanged
                && favoriteCacheSize == repository.getFavoriteCacheSize()
                && lastSyncTime == syncTime
                && !loadIsDeferred) {
            return;
        }
        compositeDisposable.clear();
        loadIsCompleted = false;
        loadIsDeferred = false;
        if (lastSyncTime != syncTime) {
            lastSyncTime = syncTime;
            repository.checkFavoritesSyncLog();
            updateTabNormalState();
        }
        final String searchText;
        String text = viewModel.getSearchText();
        if (!Strings.isNullOrEmpty(text)) {
            searchText = text.toLowerCase();
        } else {
            searchText = null;
        }
        boolean filterIsActive = (searchText != null || filterType != FilterType.ALL);
        boolean loadByChunk = repository.isFavoriteCacheDirty();
        boolean showProgress = ((!loadByChunk || filterIsActive)
                && repository.getFavoriteCacheSize() == 0) || forceShowLoading;
        final AtomicBoolean firstChunk = new AtomicBoolean(true);
        if (showProgress) viewModel.showProgressOverlay();
        Log.d(TAG, "loadFavorites(): getFavorites() [showProgress=" + showProgress + ", loadByChunk=" + loadByChunk + "]");
        Disposable disposable = repository.getFavorites()
                .subscribeOn(schedulerProvider.computation())
                .retryWhen(throwableObservable -> throwableObservable.flatMap(throwable -> {
                    if (throwable instanceof IllegalStateException) {
                        Log.d(TAG, "loadFavorites(): retry [" + repository.getFavoriteCacheSize() + "]");
                        // NOTE: it seems the system is too busy to update UI properly while Sync in progress,
                        //       so it needles to switch loading to by chunk mode
                        return Observable.just(new Object()).compose(
                                upstream -> showProgress
                                        ? upstream.delay(25, TimeUnit.MILLISECONDS)
                                        : upstream);
                    }
                    return Observable.error(throwable);
                }))
                .filter(favorite -> {
                    if (searchText != null) {
                        String favoriteName = favorite.getName();
                        if (favoriteName == null
                                || !favoriteName.toLowerCase().contains(searchText)) {
                            return false;
                        }
                    }
                    switch (filterType) {
                        case CONFLICTED:
                            return favorite.isConflicted();
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
                    loadIsCompleted = true; // NOTE: must be set before loadFavorites()
                    filterIsChanged = false;
                    favoriteCacheSize = repository.getFavoriteCacheSize();
                    if (view.isActive()) {
                        view.updateView();
                    }
                    if (loadIsDeferred) {
                        new Handler().postDelayed(() -> loadFavorites(false, forceShowLoading),
                                Settings.GLOBAL_DEFER_RELOAD_DELAY_MILLIS);
                    }
                })
                .doFinally(() -> {
                    laanoUiManager.updateTitle(TAB);
                    viewModel.hideProgressOverlay();
                })
                .subscribe(favorites -> {
                    Log.d(TAG, "loadFavorites(): subscribe() [" + favorites.size() + "]");
                    // NOTE: just to be logical
                    if (loadByChunk && !firstChunk.getAndSet(false)) {
                        view.addFavorites(favorites);
                    } else {
                        view.showFavorites(favorites);
                    }
                    selectFavoriteFilter();
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
    public void onFavoriteClick(String favoriteId, boolean isConflicted) {
        // NOTE: only click on chevrons will select the Favorite
        if (viewModel.isActionMode()) {
            onFavoriteSelected(favoriteId);
        } else if (isConflicted) {
            if (!settings.isSyncable()) {
                laanoUiManager.showApplicationNotSyncableSnackbar();
                return;
            } else if (!settings.isOnline()) {
                laanoUiManager.showApplicationOfflineSnackbar();
                return;
            }
            repository.autoResolveFavoriteConflict(favoriteId)
                    .subscribeOn(schedulerProvider.computation())
                    .observeOn(schedulerProvider.ui())
                    .subscribe(success -> {
                        if (success) {
                            view.onActivityResult(
                                    FavoritesFragment.REQUEST_FAVORITE_CONFLICT_RESOLUTION,
                                    FavoritesConflictResolutionDialog.RESULT_OK, null);
                        } else {
                            view.showConflictResolution(favoriteId);
                        }
                    }, throwable -> {
                        if (throwable instanceof NullPointerException) {
                            viewModel.showDatabaseErrorSnackbar();
                        } else {
                            // NOTE: if there is any problem show it in the dialog
                            view.showConflictResolution(favoriteId);
                        }
                    });
        } else if (Settings.GLOBAL_ITEM_CLICK_SELECT_FILTER) {
            boolean selected = viewModel.toggleFilterId(favoriteId);
            // NOTE: filterType will be updated accordingly on the tab
            if (selected) {
                settings.setFavoriteFilterId(favoriteId);
            } else {
                settings.setFavoriteFilterId(null);
            }
        }
    }

    @Override
    public boolean onFavoriteLongClick(String favoriteId) {
        view.enableActionMode();
        onFavoriteSelected(favoriteId);
        return true;
    }

    @Override
    public void onCheckboxClick(String favoriteId) {
        onFavoriteSelected(favoriteId);
    }

    private void onFavoriteSelected(String favoriteId) {
        viewModel.toggleSelection(favoriteId);
        view.selectionChanged(favoriteId);
    }

    @Override
    public void selectFavoriteFilter() {
        if (viewModel.isActionMode()) return;

        String favoriteFilter = settings.getFavoriteFilterId();
        if (favoriteFilter != null) {
            int position = getPosition(favoriteFilter);
            if (position >= 0) { // NOTE: check if there is the filter in the list
                viewModel.setFilterId(favoriteFilter);
            }
        }
    }

    @Override
    public void onEditClick(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        view.showEditFavorite(favoriteId);
    }

    @Override
    public void onToLinksClick(@NonNull Favorite favoriteFilter) {
        checkNotNull(favoriteFilter);
        String filterId = favoriteFilter.getId();
        viewModel.setFilterId(filterId);
        settings.setLinksFilterType(FilterType.FAVORITE);
        settings.setFavoriteFilterId(filterId);
        settings.setFavoriteFilter(favoriteFilter);
        laanoUiManager.setCurrentTab(LaanoFragmentPagerAdapter.LINKS_TAB);
    }

    @Override
    public void onToNotesClick(@NonNull Favorite favoriteFilter) {
        checkNotNull(favoriteFilter);
        String filterId = favoriteFilter.getId();
        viewModel.setFilterId(filterId);
        settings.setNotesFilterType(FilterType.FAVORITE);
        settings.setFavoriteFilterId(filterId);
        settings.setFavoriteFilter(favoriteFilter);
        laanoUiManager.setCurrentTab(LaanoFragmentPagerAdapter.NOTES_TAB);
    }

    @Override
    public void onDeleteClick() {
        ArrayList<String> selectedIds = viewModel.getSelectedIds();
        view.confirmFavoritesRemoval(selectedIds);
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
    public void syncSavedFavorite(@NonNull final String favoriteId) {
        checkNotNull(favoriteId);
        boolean sync = settings.isSyncable() && settings.isOnline();
        if (!sync) {
            if (settings.isSyncable() || settings.getLastSyncTime() > 0) {
                settings.setSyncStatus(SyncAdapter.SYNC_STATUS_UNSYNCED);
                laanoUiManager.updateSyncStatus();
            }
            return;
        }
        repository.syncSavedFavorite(favoriteId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doFinally(this::updateSyncStatus)
                .subscribe(itemState -> {
                    Log.d(TAG, "syncSavedFavorite() -> subscribe(): [" + itemState.name() + "]");
                    switch (itemState) {
                        case CONFLICTED:
                            updateTabNormalState();
                            loadFavorites(false);
                            laanoUiManager.showLongToast(R.string.toast_sync_conflict);
                            break;
                        case ERROR_CLOUD:
                            settings.setSyncStatus(SyncAdapter.SYNC_STATUS_ERROR);
                            laanoUiManager.showLongToast(R.string.toast_sync_error);
                            break;
                        case SAVED:
                            updateTabNormalState();
                            loadFavorites(false);
                            laanoUiManager.showShortToast(R.string.toast_sync_success);
                            break;
                    }
                }, throwable -> CommonUtils.logStackTrace(TAG_E, throwable));
    }

    @Override
    public void deleteFavorites(@NonNull ArrayList<String> selectedIds) {
        checkNotNull(selectedIds);
        boolean sync = settings.isSyncable() && settings.isOnline();
        if (sync) {
            laanoUiManager.setSyncDrawerMenu();
        }
        long started = currentTimeMillis();
        Observable.fromIterable(selectedIds)
                .flatMap(favoriteId -> {
                    return repository.deleteFavorite(favoriteId, sync, started)
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
                                        view.removeFavorite(favoriteId);
                                    }
                                    settings.resetFavoriteFilterId(favoriteId);
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
                    loadFavorites(false);
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
    public int getPosition(String favoriteId) {
        return view.getPosition(favoriteId);
    }

    @Override
    public void setFilterType(@NonNull FilterType filterType) {
        checkNotNull(filterType);
        settings.setFavoritesFilterType(filterType);
        if (this.filterType != filterType) {
            loadFavorites(false);
        }
    }

    @Override
    @NonNull
    public FilterType getFilterType() {
        return settings.getFavoritesFilterType();
    }

    /**
     * @return Return null if there is no additional data is required
     */
    @Nullable
    private FilterType updateFilter() {
        FilterType filterType = getFilterType();
        switch (filterType) {
            case ALL:
            case CONFLICTED:
                if (this.filterType == filterType) {
                    return null;
                }
                filterIsChanged = true;
                this.filterType = filterType;
                laanoUiManager.setFilterType(TAB, filterType);
                break;
            default:
                filterIsChanged = true;
                setDefaultFavoriteFilterType();
        }
        return null;
    }

    private void setDefaultFavoriteFilterType() {
        filterType = Settings.DEFAULT_FILTER_TYPE;
        laanoUiManager.setFilterType(TAB, filterType);
        settings.setFavoritesFilterType(filterType);
    }

    @Override
    public void updateTabNormalState() {
        repository.isConflictedFavorites()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        conflicted -> laanoUiManager.setTabNormalState(TAB, conflicted),
                        throwable -> viewModel.showDatabaseErrorSnackbar());
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
