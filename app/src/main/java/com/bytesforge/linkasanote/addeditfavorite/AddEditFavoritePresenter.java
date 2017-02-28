package com.bytesforge.linkasanote.addeditfavorite;

import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.tokenautocomplete.TokenCompleteTextView;

import java.util.List;

import javax.inject.Inject;

import rx.Observer;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public final class AddEditFavoritePresenter implements
        AddEditFavoriteContract.Presenter, TokenCompleteTextView.TokenListener<Tag> {

    private final Repository repository;
    private final AddEditFavoriteContract.View view;
    private final AddEditFavoriteContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final String favoriteId;

    @NonNull
    private final CompositeSubscription subscription;

    @Inject
    AddEditFavoritePresenter(
            Repository repository, AddEditFavoriteContract.View view,
            AddEditFavoriteContract.ViewModel viewModel,
            BaseSchedulerProvider schedulerProvider,
            @Nullable @FavoriteId String favoriteId) {
        this.repository = repository;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.favoriteId = favoriteId;

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
        loadTags();
    }

    @Override
    public void unsubscribe() {
        subscription.clear();
    }

    private void loadTags() {
        EspressoIdlingResource.increment();
        //subscription.clear(); // NOTE: stop all other subscriptions

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
    public boolean isNewFavorite() {
        return favoriteId == null;
    }

    @Override
    public void populateFavorite() {
        if (favoriteId == null) {
            throw new RuntimeException("populateFavorite() was called but favoriteId is null");
        }
        EspressoIdlingResource.increment();

        Subscription subscription = repository.getFavorite(favoriteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doOnTerminate(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                })
                .subscribe(new Observer<Favorite>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Favorite favorite) {
                        view.setupFavoriteState(favorite);
                        viewModel.checkAddButton();
                    }
                });
        this.subscription.add(subscription);
    }

    @Override
    public void saveFavorite(String name, List<Tag> tags) {
        if (isNewFavorite()) {
            createFavorite(name, tags);
        } else {
            updateFavorite(name, tags);
        }
    }

    private void createFavorite(String name, List<Tag> tags) {
        Favorite favorite = new Favorite(name, tags);
        if (favorite.isEmpty()) {
            viewModel.showEmptyFavoriteSnackbar();
            return;
        }
        favorite.setSyncState(SyncState.State.UNSYNCED);
        try {
            repository.saveFavorite(favorite);
            view.finishActivity();
        } catch (SQLiteConstraintException e) {
            viewModel.showDuplicateKeyError();
        }
    }

    private void updateFavorite(String name, List<Tag> tags) {
        if (favoriteId == null) {
            throw new RuntimeException("updateFavorite() was called but favoriteId is null");
        }
        Favorite favorite = new Favorite(favoriteId, name, tags);
        if (favorite.isEmpty()) {
            viewModel.showEmptyFavoriteSnackbar();
            return;
        }
        favorite.setSyncState(SyncState.State.UNSYNCED);
        try {
            repository.saveFavorite(favorite);
            view.finishActivity();
        } catch (SQLiteConstraintException e) {
            viewModel.showDuplicateKeyError();
        }
    }

    // ViewModel

    @Override
    public void onTokenAdded(Tag tag) {
        viewModel.afterTagsChanged();
    }

    @Override
    public void onTokenRemoved(Tag tag) {
        viewModel.afterTagsChanged();
    }
}
