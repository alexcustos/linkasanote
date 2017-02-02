package com.bytesforge.linkasanote.addeditfavorite;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import java.util.List;

import javax.inject.Inject;

import rx.Observer;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public final class AddEditFavoritePresenter implements AddEditFavoriteContract.Presenter {

    private final AddEditFavoriteContract.View view;
    private final Repository repository;
    private final String favoriteId;
    private final BaseSchedulerProvider schedulerProvider;

    private AddEditFavoriteContract.ViewModel viewModel;

    @NonNull
    private final CompositeSubscription subscription;

    @Inject
    AddEditFavoritePresenter(
            Repository repository, AddEditFavoriteContract.View view,
            BaseSchedulerProvider schedulerProvider,
            @Nullable @FavoriteId String favoriteId) {
        this.repository = repository;
        this.view = view;
        this.schedulerProvider = schedulerProvider;
        this.favoriteId = favoriteId;

        subscription = new CompositeSubscription();
    }

    @Inject
    void setupView(Context context) {
        viewModel = new AddEditFavoriteViewModel(context, isNewFavorite());
        viewModel.setPresenter(this);

        view.setPresenter(this);
        view.setViewModel(viewModel);
    }

    @Override
    public void subscribe() {
        loadTags();
    }

    private void loadTags() {
        EspressoIdlingResource.increment();
        subscription.clear();

        Subscription subscription = repository.getTags()
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doOnTerminate(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                })
                .subscribe(new Observer<List<Tag>>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(List<Tag> tags) {
                        view.swapTagsCompletionViewItems(tags);
                    }
                });
        this.subscription.add(subscription);
    }

    @Override
    public void unsubscribe() {
        subscription.clear();
    }

    @Override
    public void saveFavorite(String name, List<Tag> tags) {
        if (isNewFavorite()) {
            createFavorite(name, tags);
        } else {
            updateFavorite(name, tags);
        }
    }

    private boolean isNewFavorite() {
        return favoriteId == null;
    }

    private void createFavorite(String name, List<Tag> tags) {
        Favorite favorite = new Favorite(name, tags);
        if (favorite.isEmpty()) {
            viewModel.showEmptyFavoriteSnackbar();
        } else {
            repository.saveFavorite(favorite);
            view.finishActivity();
        }
    }

    // TODO: implement Favorite update
    private void updateFavorite(String name, List<Tag> tags) {
        if (isNewFavorite()) {
            throw new RuntimeException("updateFavorite() was called but Favorite is new.");
        }
    }
}
