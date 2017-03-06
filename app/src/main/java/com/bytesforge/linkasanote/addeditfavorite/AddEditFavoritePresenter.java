package com.bytesforge.linkasanote.addeditfavorite;

import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.tokenautocomplete.TokenCompleteTextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class AddEditFavoritePresenter implements
        AddEditFavoriteContract.Presenter, TokenCompleteTextView.TokenListener<Tag> {

    private final Repository repository;
    private final AddEditFavoriteContract.View view;
    private final AddEditFavoriteContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;

    private String favoriteId; // NOTE: can be reset to null if NoSuchElementException

    @NonNull
    private final CompositeDisposable disposable;

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

        disposable = new CompositeDisposable();
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
        disposable.clear();
    }

    @Override
    public void loadTags() {
        EspressoIdlingResource.increment();
        //disposable.clear(); // NOTE: stop all other subscriptions

        Disposable disposable = repository.getTags()
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doOnError(throwable -> view.swapTagsCompletionViewItems(new ArrayList<>()))
                .doFinally(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                })
                .subscribe((tags, throwable) -> {
                    if (tags != null) view.swapTagsCompletionViewItems(tags);
                });
        this.disposable.add(disposable);
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

        Disposable disposable = repository.getFavorite(favoriteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doOnError(throwable -> {
                    favoriteId = null;
                    viewModel.showFavoriteNotFoundSnackbar();
                })
                .doFinally(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                })
                .subscribe((favorite, throwable) -> {
                    if (favorite != null) {
                        view.setupFavoriteState(favorite);
                        viewModel.checkAddButton();
                    }
                });
        this.disposable.add(disposable);
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
        saveFavorite(favorite);
    }

    private void updateFavorite(String name, List<Tag> tags) {
        if (favoriteId == null) {
            throw new RuntimeException("updateFavorite() was called but favoriteId is null");
        }
        Favorite favorite = new Favorite(favoriteId, name, tags);
        saveFavorite(favorite);
    }

    private void saveFavorite(@NonNull final Favorite favorite) {
        checkNotNull(favorite);

        if (favorite.isEmpty()) {
            viewModel.showEmptyFavoriteSnackbar();
            return;
        }
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
