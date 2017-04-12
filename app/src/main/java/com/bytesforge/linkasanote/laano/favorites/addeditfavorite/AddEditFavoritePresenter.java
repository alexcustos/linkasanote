package com.bytesforge.linkasanote.laano.favorites.addeditfavorite;

import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.favorites.FavoriteId;
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

    private static final String TAG = AddEditFavoritePresenter.class.getSimpleName();

    private final Repository repository;
    private final AddEditFavoriteContract.View view;
    private final AddEditFavoriteContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;

    private String favoriteId; // NOTE: can be reset to null if NoSuchElementException

    @NonNull
    private final CompositeDisposable tagsDisposable;

    @NonNull
    private final CompositeDisposable favoriteDisposable;

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
        tagsDisposable = new CompositeDisposable();
        favoriteDisposable = new CompositeDisposable();
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
        tagsDisposable.clear();
        favoriteDisposable.clear();
    }

    @Override
    public void loadTags() {
        EspressoIdlingResource.increment();
        tagsDisposable.clear(); // stop previous requests

        Disposable disposable = repository.getTags()
                .toList()
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
        tagsDisposable.add(disposable);
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
        favoriteDisposable.clear();

        Disposable disposable = repository.getFavorite(favoriteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doFinally(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                })
                .subscribe(viewModel::populateFavorite, throwable -> {
                    favoriteId = null;
                    viewModel.showFavoriteNotFoundSnackbar();
                });
        favoriteDisposable.add(disposable);
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
        saveFavorite(new Favorite(name, tags));
    }

    private void updateFavorite(String name, List<Tag> tags) {
        if (favoriteId == null) {
            throw new RuntimeException("updateFavorite() was called but favoriteId is null");
        }
        // NOTE: state eTag will NOT be overwritten if null
        saveFavorite(new Favorite(favoriteId, name, tags)); // UNSYNCED
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
