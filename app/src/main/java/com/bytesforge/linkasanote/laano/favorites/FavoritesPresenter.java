package com.bytesforge.linkasanote.laano.favorites;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.BaseItemPresenter;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.laano.favorites.conflictresolution.FavoritesConflictResolutionDialog;
import com.bytesforge.linkasanote.laano.links.LinksPresenter;
import com.bytesforge.linkasanote.laano.notes.NotesPresenter;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncAdapter;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Strings;

import java.util.ArrayList;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FavoritesPresenter extends BaseItemPresenter implements
        FavoritesContract.Presenter, DataSource.Callback {

    private static final String TAG = FavoritesPresenter.class.getSimpleName();
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

    private FilterType filterType;
    private boolean firstLoad = true;

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
        loadFavorites(forceUpdate || firstLoad, true);
        firstLoad = false;
    }

    private void loadFavorites(boolean forceUpdate, final boolean showLoading) {
        EspressoIdlingResource.increment();
        compositeDisposable.clear();
        if (forceUpdate) {
            repository.refreshFavorites();
        }
        if (showLoading) {
            viewModel.showProgressOverlay();
        }
        updateFilter();
        Disposable disposable = repository.getFavorites()
                .subscribeOn(schedulerProvider.computation())
                .filter(favorite -> {
                    String searchText = viewModel.getSearchText();
                    if (!Strings.isNullOrEmpty(searchText)) {
                        searchText = searchText.toLowerCase();
                        String favoriteName = favorite.getName();
                        if (favoriteName != null
                                && !favoriteName.toLowerCase().contains(searchText)) {
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
                .toList()
                .observeOn(schedulerProvider.ui())
                .doFinally(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                    if (showLoading) {
                        viewModel.hideProgressOverlay();
                    }
                    laanoUiManager.updateTitle(TAB);
                })
                .subscribe(favorites -> {
                    view.showFavorites(favorites);
                    selectFavoriteFilter();
                }, throwable -> {
                    // NullPointerException
                    CommonUtils.logStackTrace(TAG, throwable);
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
            boolean selected = viewModel.toggleSingleSelection(favoriteId);
            // NOTE: filterType will be updated accordingly on the tab
            if (selected) {
                settings.setFavoriteFilter(favoriteId);
            } else {
                settings.setFavoriteFilter(null);
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

        String favoriteFilter = settings.getFavoriteFilter();
        if (favoriteFilter != null) {
            int position = getPosition(favoriteFilter);
            if (position >= 0) { // NOTE: check if there is the filter in the list
                viewModel.setSingleSelection(favoriteFilter, true);
                //view.scrollToPosition(position);
            }
        }
    }

    @Override
    public void onEditClick(@NonNull String favoriteId) {
        view.showEditFavorite(favoriteId);
    }

    @Override
    public void onToLinksClick(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        viewModel.setSingleSelection(favoriteId, true);
        settings.setFilterType(LinksPresenter.SETTING_LINKS_FILTER_TYPE, FilterType.FAVORITE);
        settings.setFavoriteFilter(favoriteId);
        laanoUiManager.setCurrentTab(LaanoFragmentPagerAdapter.LINKS_TAB);
    }

    @Override
    public void onToNotesClick(@NonNull String favoriteId) {
        viewModel.setSingleSelection(favoriteId, true);
        settings.setFilterType(NotesPresenter.SETTING_NOTES_FILTER_TYPE, FilterType.FAVORITE);
        settings.setFavoriteFilter(favoriteId);
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
        repository.syncSavedFavorite(favoriteId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doFinally(this::updateSyncStatus)
                .subscribe(itemState -> {
                    Log.d(TAG, "syncSavedFavorite() -> subscribe(): [" + itemState.name() + "]");
                    switch (itemState) {
                        case CONFLICTED:
                            laanoUiManager.showLongToast(R.string.toast_sync_conflict);
                            break;
                        case ERROR_CLOUD:
                            settings.setSyncStatus(SyncAdapter.SYNC_STATUS_ERROR);
                            laanoUiManager.showLongToast(R.string.toast_sync_error);
                            break;
                        case SAVED:
                            laanoUiManager.showShortToast(R.string.toast_sync_success);
                            break;
                    }
                }, throwable -> CommonUtils.logStackTrace(TAG, throwable));
    }

    @Override
    public void deleteFavorites(ArrayList<String> selectedIds) {
        Observable.fromIterable(selectedIds)
                .flatMap(favoriteId -> {
                    Log.d(TAG, "deleteFavorites(): [" + favoriteId + "]");
                    return repository.deleteFavorite(favoriteId, settings.isSyncable())
                            .subscribeOn(schedulerProvider.io())
                            .observeOn(schedulerProvider.ui())
                            .doOnNext(itemState -> {
                                Log.d(TAG, "deleteFavorites() -> doOnNext(): [" + itemState.name() + "]");
                                if (itemState == DataSource.ItemState.DELETED
                                        || itemState == DataSource.ItemState.DEFERRED) {
                                    // NOTE: can be called twice
                                    view.removeFavorite(favoriteId);
                                    settings.resetFavoriteFilter(favoriteId);
                                }
                            });
                })
                .filter(itemState -> itemState == DataSource.ItemState.CONFLICTED
                        || itemState == DataSource.ItemState.ERROR_LOCAL
                        || itemState == DataSource.ItemState.ERROR_CLOUD)
                .toList()
                .doFinally(this::updateSyncStatus)
                .subscribe(itemStates -> {
                    Log.d(TAG, "deleteFavorites(): Completed [" + itemStates.toString() + "]");
                    if (itemStates.isEmpty()) {
                        // DELETED or DEFERRED if sync is disabled
                        //viewModel.showDeleteSuccessSnackbar();
                        if (settings.isSyncable()) {
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
                }, throwable -> CommonUtils.logStackTrace(TAG, throwable));
    }

    @Override
    public void updateSyncStatus() {
        if (settings.getSyncStatus() == SyncAdapter.SYNC_STATUS_ERROR) {
            // NOTE: only SyncAdapter can reset this status
            return;
        }
        repository.getSyncStatus()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(syncStatus -> {
                    settings.setSyncStatus(syncStatus);
                    laanoUiManager.updateSyncStatus();
                }, throwable -> {
                    CommonUtils.logStackTrace(TAG, throwable);
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
        settings.setFilterType(SETTING_FAVORITES_FILTER_TYPE, filterType);
        if (this.filterType != filterType) {
            loadFavorites(false);
        }
    }

    /**
     * @return Return null if there is no additional data is required
     */
    @Nullable
    private FilterType updateFilter() {
        FilterType filterType = settings.getFilterType(SETTING_FAVORITES_FILTER_TYPE);
        switch (filterType) {
            case ALL:
            case CONFLICTED:
                if (this.filterType == filterType) {
                    return null;
                }
                this.filterType = filterType;
                laanoUiManager.setFilterType(TAB, filterType, null);
                break;
            default:
                setDefaultNotesFilterType();
        }
        return null;
    }

    private void setDefaultNotesFilterType() {
        filterType = Settings.DEFAULT_FILTER_TYPE;
        laanoUiManager.setFilterType(TAB, filterType, null);
        settings.setFilterType(SETTING_FAVORITES_FILTER_TYPE, filterType);
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
}
