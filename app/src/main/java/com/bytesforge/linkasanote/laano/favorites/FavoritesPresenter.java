package com.bytesforge.linkasanote.laano.favorites;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.laano.favorites.conflictresolution.FavoritesConflictResolutionDialog;
import com.bytesforge.linkasanote.laano.links.LinksPresenter;
import com.bytesforge.linkasanote.laano.notes.NotesPresenter;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Strings;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public final class FavoritesPresenter implements FavoritesContract.Presenter {

    private static final String TAG = FavoritesPresenter.class.getSimpleName();

    private final Repository repository;
    private final FavoritesContract.View view;
    private final FavoritesContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final LaanoUiManager laanoUiManager;
    private final Settings settings;

    @NonNull
    private final CompositeDisposable compositeDisposable;

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
        viewModel.setPresenter(this);
        viewModel.setLaanoUiManager(laanoUiManager);
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void unsubscribe() {
        compositeDisposable.clear();
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
    public void addFavorite() {
        view.showAddFavorite();
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
                    switch (viewModel.getFilterType()) {
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
            int position = getPosition(favoriteId);
            boolean selected = viewModel.toggleSingleSelection(position);
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
        int position = view.getPosition(favoriteId);
        if (viewModel.isActionMode()) {
            viewModel.toggleSelection(position);
            view.selectionChanged(position);
        } else {
            viewModel.toggleSingleSelection(position);
        }
    }

    @Override
    public void selectFavoriteFilter() {
        if (viewModel.isActionMode()) return;

        String favoriteFilter = settings.getFavoriteFilter();
        if (favoriteFilter != null) {
            int position = getPosition(favoriteFilter);
            if (position >= 0) {
                viewModel.setSingleSelection(position, true);
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
        int position = getPosition(favoriteId);
        viewModel.setSingleSelection(position, true);
        settings.setFilterType(LinksPresenter.SETTING_LINKS_FILTER_TYPE, FilterType.FAVORITE);
        settings.setFavoriteFilter(favoriteId);
        laanoUiManager.setCurrentTab(LaanoFragmentPagerAdapter.LINKS_TAB);
    }

    @Override
    public void onToNotesClick(@NonNull String favoriteId) {
        int position = getPosition(favoriteId);
        viewModel.setSingleSelection(position, true);
        settings.setFilterType(NotesPresenter.SETTING_NOTES_FILTER_TYPE, FilterType.FAVORITE);
        settings.setFavoriteFilter(favoriteId);
        laanoUiManager.setCurrentTab(LaanoFragmentPagerAdapter.NOTES_TAB);
    }

    @Override
    public void onDeleteClick() {
        int[] selectedIds = viewModel.getSelectedIds();
        view.confirmFavoritesRemoval(selectedIds);
    }

    @Override
    public void onSelectAllClick() {
        viewModel.toggleSelection();
    }

    @Override
    public void deleteFavorites(int[] selectedIds) {
        for (int selectedId : selectedIds) {
            viewModel.removeSelection(selectedId);
            String favoriteId = view.removeFavorite(selectedId);
            try {
                repository.deleteFavorite(favoriteId);
                settings.resetFavoriteFilter(favoriteId);
            } catch (NullPointerException e) {
                viewModel.showDatabaseErrorSnackbar();
            }
        }
    }

    @Override
    public int getPosition(String favoriteId) {
        return view.getPosition(favoriteId);
    }

    @Override
    public boolean isConflicted() {
        return repository.isConflictedFavorites().blockingGet();
    }

    @Override
    public void setFilterType(@NonNull FilterType filterType) {
        viewModel.setFilterType(filterType);
        laanoUiManager.updateTitle(LaanoFragmentPagerAdapter.FAVORITES_TAB);
    }

    @Override
    public void updateTabNormalState() {
        laanoUiManager.setTabNormalState(LaanoFragmentPagerAdapter.FAVORITES_TAB, isConflicted());
    }
}
