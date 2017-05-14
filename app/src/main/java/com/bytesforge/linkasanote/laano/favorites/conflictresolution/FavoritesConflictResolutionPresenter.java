package com.bytesforge.linkasanote.laano.favorites.conflictresolution;

import android.support.annotation.NonNull;
import android.util.Log;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.laano.favorites.FavoriteId;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FavoritesConflictResolutionPresenter implements
        FavoritesConflictResolutionContract.Presenter {

    private static final String TAG = FavoritesConflictResolutionPresenter .class.getSimpleName();

    private final Repository repository; // NOTE: for cache control
    private final Settings settings;
    private final LocalFavorites<Favorite> localFavorites;
    private final CloudItem<Favorite> cloudFavorites;
    private final FavoritesConflictResolutionContract.View view;
    private final FavoritesConflictResolutionContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;

    private String favoriteId;

    @NonNull
    private final CompositeDisposable localDisposable;

    @NonNull
    private final CompositeDisposable cloudDisposable;

    @Inject
    FavoritesConflictResolutionPresenter(
            Repository repository, Settings settings,
            LocalFavorites<Favorite> localFavorites, CloudItem<Favorite> cloudFavorites,
            FavoritesConflictResolutionContract.View view,
            FavoritesConflictResolutionContract.ViewModel viewModel,
            BaseSchedulerProvider schedulerProvider, @FavoriteId String favoriteId) {
        this.repository = repository;
        this.settings = settings;
        this.localFavorites = localFavorites;
        this.cloudFavorites = cloudFavorites;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.favoriteId = favoriteId;
        localDisposable = new CompositeDisposable();
        cloudDisposable = new CompositeDisposable();
    }

    @Inject
    void setupView() {
        view.setViewModel(viewModel);
        viewModel.setPresenter(this);
    }

    @Override
    public void subscribe() {
        populate();
    }

    @Override
    public void unsubscribe() {
        localDisposable.clear();
        cloudDisposable.clear();
    }

    private void populate() {
        if (viewModel.isLocalPopulated() && viewModel.isCloudPopulated()) {
            return;
        }
        loadLocalFavorite(); // first step, then cloud one will be loaded
    }

    private void loadLocalFavorite() {
        localDisposable.clear();
        Disposable disposable = localFavorites.get(favoriteId)
                .subscribeOn(schedulerProvider.computation()) // local
                .observeOn(schedulerProvider.ui())
                .subscribe(favorite -> {
                    if (!favorite.isConflicted()) {
                        // NOTE: to make sure that there is no problem with the cache
                        repository.refreshFavorites();
                        view.finishActivity();
                    } else {
                        populateLocalFavorite(favorite);
                    }
                }, throwable -> {
                    if (throwable instanceof NoSuchElementException) {
                        repository.refreshFavorites(); // NOTE: maybe there is a problem with cache
                        view.finishActivity(); // NOTE: no item, no problem
                    } else {
                        viewModel.showDatabaseError();
                        loadCloudFavorite();
                    }
                });
        localDisposable.add(disposable);
    }

    private void populateLocalFavorite(@NonNull final Favorite favorite) {
        checkNotNull(favorite);
        if (favorite.isDuplicated()) {
            viewModel.populateCloudFavorite(favorite);
            localFavorites.getMain(favorite.getDuplicatedKey())
                    .subscribeOn(schedulerProvider.computation()) // local
                    .observeOn(schedulerProvider.ui())
                    // NOTE: recursion, but mainFavorite is not duplicated by definition
                    .subscribe(this::populateLocalFavorite, throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            // NOTE: very bad behaviour, but it's the best choice if it had happened
                            Log.e(TAG, "Fallback for the auto Favorite conflict resolution was called");
                            SyncState state = new SyncState(SyncState.State.SYNCED);
                            boolean success = localFavorites.update(favoriteId, state).blockingGet();
                            if (success) {
                                repository.refreshFavorites();
                                view.finishActivity();
                            } else {
                                view.cancelActivity();
                            }
                        } else {
                            viewModel.showDatabaseError();
                            loadCloudFavorite();
                        }
                    });
        } else {
            viewModel.populateLocalFavorite(favorite);
            if (!viewModel.isCloudPopulated()) {
                loadCloudFavorite();
            }
        } // if
    }

    private void loadCloudFavorite() {
        cloudDisposable.clear();
        Disposable disposable = cloudFavorites.download(favoriteId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(viewModel::populateCloudFavorite, throwable -> {
                    if (throwable instanceof NoSuchElementException) {
                        viewModel.showCloudNotFound();
                    } else {
                        viewModel.showCloudDownloadError();
                    }
                });
        cloudDisposable.add(disposable);
    }

    @Override
    public void onLocalDeleteClick() {
        viewModel.deactivateButtons();
        if (viewModel.isStateDuplicated()) {
            viewModel.showProgressOverlay();
            localFavorites.getMain(viewModel.getLocalName())
                    .subscribeOn(schedulerProvider.computation()) // local
                    .observeOn(schedulerProvider.ui())
                    .doFinally(viewModel::hideProgressOverlay)
                    .subscribe(
                            favorite -> replaceFavorite(favorite.getId(), favoriteId),
                            throwable -> view.cancelActivity());
        } else {
            deleteFavorite(favoriteId);
        }
    }

    private void replaceFavorite(
            @NonNull final String mainFavoriteId, @NonNull final String favoriteId) {
        checkNotNull(mainFavoriteId);
        checkNotNull(favoriteId);
        deleteFavoriteSingle(mainFavoriteId)
                .subscribeOn(schedulerProvider.io())
                .map(success -> {
                    if (success) {
                        SyncState state = new SyncState(SyncState.State.SYNCED);
                        success = localFavorites.update(favoriteId, state).blockingGet();
                    }
                    return success;
                })
                .observeOn(schedulerProvider.ui())
                .subscribe(success -> {
                    if (success) {
                        repository.refreshFavorites(); // OPTIMIZATION: reload one item
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    private void deleteFavorite(@NonNull final String favoriteId) {
        checkNotNull(favoriteId);
        viewModel.showProgressOverlay();
        deleteFavoriteSingle(favoriteId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doFinally(viewModel::hideProgressOverlay)
                .subscribe(success -> {
                    if (success) {
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    private Single<Boolean> deleteFavoriteSingle(@NonNull final String favoriteId) {
        checkNotNull(favoriteId);
        return cloudFavorites.delete(favoriteId)
                .map(result -> {
                    boolean success = false;
                    if (result.isSuccess()) {
                        success = localFavorites.delete(favoriteId).blockingGet();
                    } else {
                        Log.e(TAG, "There was an error while deleting the Favorite from the cloud storage [" + favoriteId + "]");
                    }
                    return success;
                })
                .doOnSuccess(success -> {
                    if (success) {
                        repository.removeCachedFavorite(favoriteId);
                        settings.resetFavoriteFilter(favoriteId);
                    }
                });
    }

    @Override
    public void onCloudDeleteClick() {
        viewModel.deactivateButtons();
        deleteFavorite(favoriteId);
    }

    @Override
    public void onCloudRetryClick() {
        viewModel.showCloudLoading();
        loadCloudFavorite();
    }

    @Override
    public void onLocalUploadClick() {
        viewModel.deactivateButtons();
        viewModel.showProgressOverlay();
        Favorite favorite = localFavorites.get(favoriteId).blockingGet();
        cloudFavorites.upload(favorite)
                .subscribeOn(schedulerProvider.io())
                .map(result -> {
                    boolean success = false;
                    if (result.isSuccess()) {
                        JsonFile jsonFile = (JsonFile) result.getData().get(0);
                        SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                        success = localFavorites.update(favorite.getId(), state).blockingGet();
                    }
                    return success;
                })
                .observeOn(schedulerProvider.ui())
                .doFinally(viewModel::hideProgressOverlay)
                .subscribe(success -> {
                    if (success) {
                        repository.refreshFavorites();
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    @Override
    public void onCloudDownloadClick() {
        viewModel.deactivateButtons();
        viewModel.showProgressOverlay();
        cloudFavorites.download(favoriteId)
                .subscribeOn(schedulerProvider.io())
                .map(favorite -> localFavorites.save(favorite).blockingGet())
                .observeOn(schedulerProvider.ui())
                .doFinally(viewModel::hideProgressOverlay)
                .subscribe(success -> {
                    if (success) {
                        repository.refreshFavorites();
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }
}
