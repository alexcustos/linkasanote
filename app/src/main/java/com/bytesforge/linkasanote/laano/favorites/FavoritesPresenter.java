package com.bytesforge.linkasanote.laano.favorites;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import java.util.ArrayList;

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

    @NonNull
    private final CompositeDisposable disposable;

    private boolean firstLoad = true;

    @Inject
    FavoritesPresenter(
            Repository repository, FavoritesContract.View view,
            FavoritesContract.ViewModel viewModel, BaseSchedulerProvider schedulerProvider,
            LaanoUiManager laanoUiManager) {
        this.repository = repository;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.laanoUiManager = laanoUiManager;
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
        loadFavorites(false);
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
        disposable.clear();
        if (forceUpdate) {
            repository.refreshFavorites();
        }

        Disposable disposable = repository.getFavorites()
                .filter(favorite -> {
                    switch (viewModel.getFilterType()) {
                        case FAVORITES_CONFLICTED:
                            return favorite.isConflicted();
                        case FAVORITES_ALL:
                        default:
                            return true;
                    }
                })
                .toList()
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                // NoSuchElementException, NullPointerException
                .doOnError(throwable -> view.showFavorites(new ArrayList<>()))
                .doFinally(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                })
                // NOTE: BiConsumer must be here or OnErrorNotImplementedException
                .subscribe((favorites, throwable) -> {
                    if (favorites != null) view.showFavorites(favorites);
                });
        this.disposable.add(disposable);
    }

    @Override
    public void onFavoriteClick(String favoriteId, boolean isConflicted) {
        // TODO: normal mode selection must highlight current favorite filter
        if (viewModel.isActionMode()) {
            onFavoriteSelected(favoriteId);
        } else if (isConflicted){
            view.showConflictResolution(favoriteId);
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
        viewModel.toggleSelection(position);
        view.selectionChanged(position);
    }

    @Override
    public void onEditClick(@NonNull String favoriteId) {
        view.showEditFavorite(favoriteId);
    }

    @Override
    public void onToLinksClick(@NonNull String favoriteId) {
    }

    @Override
    public void onToNotesClick(@NonNull String favoriteId) {
    }

    @Override
    public void onDeleteClick() {
        int[] selectedIds = viewModel.getSelectedIds();
        view.confirmFavoritesRemoval(selectedIds);
    } // onDeleteClick

    @Override
    public void deleteFavorites(int[] selectedIds) {
        for (int selectedId : selectedIds) {
            viewModel.removeSelection(selectedId);
            String favoriteId = view.removeFavorite(selectedId);
            try {
                repository.deleteFavorite(favoriteId);
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
    public void setFilterType(@NonNull FavoritesFilterType filterType) {
        viewModel.setFilterType(filterType);
        laanoUiManager.updateTitle(LaanoFragmentPagerAdapter.FAVORITES_TAB);
    }

    @Override
    public void updateTabNormalState() {
        laanoUiManager.setTabNormalState(
                LaanoFragmentPagerAdapter.FAVORITES_TAB, isConflicted());
    }
}
