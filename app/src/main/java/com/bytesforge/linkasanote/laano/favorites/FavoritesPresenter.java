package com.bytesforge.linkasanote.laano.favorites;

import android.content.Context;
import android.support.annotation.NonNull;

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

    private final Repository repository;
    private final FavoritesContract.View view;
    private final BaseSchedulerProvider schedulerProvider;

    @NonNull
    private final CompositeSubscription subscription;

    private boolean firstLoad = true;

    @Inject
    FavoritesPresenter(
            Repository repository, FavoritesContract.View view,
            BaseSchedulerProvider schedulerProvider) {
        this.repository = repository;
        this.view = view;
        this.schedulerProvider = schedulerProvider;

        subscription = new CompositeSubscription();
    }

    @Inject
    void setupView(Context context) {
        FavoritesContract.ViewModel viewModel = new FavoritesViewModel(context);
        viewModel.setPresenter(this);

        view.setPresenter(this);
        view.setViewModel(viewModel);
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
}
