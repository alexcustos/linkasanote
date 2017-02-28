package com.bytesforge.linkasanote.laano.favorites;

import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import java.util.List;

import javax.inject.Inject;

import rx.Observer;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public final class FavoritesPresenter implements FavoritesContract.Presenter {

    private static final String TAG = FavoritesPresenter.class.getSimpleName();

    private final Repository repository;
    private final FavoritesContract.View view;
    private final FavoritesContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;

    @NonNull
    private final CompositeSubscription subscription;

    private boolean firstLoad = true;

    @Inject
    FavoritesPresenter(
            Repository repository, FavoritesContract.View view,
            FavoritesContract.ViewModel viewModel,
            BaseSchedulerProvider schedulerProvider) {
        this.repository = repository;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;

        subscription = new CompositeSubscription();
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
        viewModel.setPresenter(this);
    }

    @Override
    public void subscribe() {
        loadFavorites(false);
    }

    @Override
    public void unsubscribe() {
        subscription.clear();
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
        // TODO: implement SwipeRefreshLayout and cache invalidation
        EspressoIdlingResource.increment();
        subscription.clear();

        Subscription subscription = repository.getFavorites()
                // TODO: implement filter
                //.flatMap(Observable::from)
                //.filter(favorite -> {...})
                //.toList()
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doOnTerminate(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                })
                .subscribe(new Observer<List<Favorite>>() {

                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(List<Favorite> favorites) {
                        view.showFavorites(favorites);
                    }
                });
        this.subscription.add(subscription);
    }

    @Override
    public void onFavoriteClick(int position) {
        if (viewModel.isActionMode()) {
            onFavoriteSelected(position);
        }
    }

    @Override
    public boolean onFavoriteLongClick(int position) {
        view.enableActionMode();
        onFavoriteSelected(position);
        return true;
    }

    @Override
    public void onCheckboxClick(int position) {
        onFavoriteSelected(position);
    }

    private void onFavoriteSelected(int position) {
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
        SparseBooleanArray selectedIds = viewModel.getSelectedIds();
        int size = selectedIds.size();
        for (int i = size - 1; i >= 0; i--) {
            int key = selectedIds.keyAt(i);
            viewModel.removeSelection(key);
            String favoriteId = view.removeFavorite(key).getId();
            repository.deleteFavorite(favoriteId);
        }
    } // onDeleteClick
}
