package com.bytesforge.linkasanote.laano.favorites.conflictresolution;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.cloud.CloudFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.laano.favorites.FavoriteId;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FavoritesConflictResolutionPresenter implements
        FavoritesConflictResolutionContract.Presenter {

    private final Repository repository; // NOTE: for cache control
    private final LocalFavorites localFavorites;
    private final CloudFavorites cloudFavorites;
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
            Repository repository, LocalFavorites localFavorites, CloudFavorites cloudFavorites,
            FavoritesConflictResolutionContract.View view,
            FavoritesConflictResolutionContract.ViewModel viewModel,
            BaseSchedulerProvider schedulerProvider, @FavoriteId String favoriteId) {
        this.repository = repository;
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
        view.setPresenter(this);
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
        Disposable disposable = localFavorites.getFavorite(favoriteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(favorite -> {
                    if (!favorite.isConflicted()) {
                        repository.refreshFavorites(); // NOTE: maybe there is a problem with cache
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
            localFavorites.getMainFavorite(favorite.getName())
                    .subscribeOn(schedulerProvider.computation())
                    .observeOn(schedulerProvider.ui())
                    // NOTE: recursion, but mainFavorite is not duplicated by definition
                    .subscribe(this::populateLocalFavorite, throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            // NOTE: main position is empty, so the conflict can be resolved automatically
                            SyncState state = new SyncState(SyncState.State.SYNCED);
                            int numRows = localFavorites.updateFavorite(favoriteId, state).blockingGet();
                            if (numRows == 1) {
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
        Disposable disposable = cloudFavorites.downloadFavorite(favoriteId)
                .subscribeOn(schedulerProvider.computation())
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
            localFavorites.getMainFavorite(viewModel.getLocalName())
                    .subscribeOn(schedulerProvider.computation())
                    .observeOn(schedulerProvider.ui())
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

        // DB operation is blocking; Cloud is on computation
        cloudFavorites.deleteFavorite(mainFavoriteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    boolean isSuccess = false;
                    if (result.isSuccess()) {
                        int numRows = localFavorites.deleteFavorite(mainFavoriteId).blockingGet();
                        isSuccess = (numRows == 1);
                    }
                    if (isSuccess) {
                        repository.deleteCachedFavorite(mainFavoriteId);
                        SyncState state = new SyncState(SyncState.State.SYNCED);
                        int numRows = localFavorites.updateFavorite(favoriteId, state).blockingGet();
                        isSuccess = (numRows == 1);
                    }
                    if (isSuccess) {
                        repository.refreshFavorites();
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    private void deleteFavorite(@NonNull final String favoriteId) {
        checkNotNull(favoriteId);

        cloudFavorites.deleteFavorite(favoriteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    boolean isSuccess = false;
                    if (result.isSuccess()) {
                        int numRows = localFavorites.deleteFavorite(favoriteId).blockingGet();
                        isSuccess = (numRows == 1);
                    }
                    if (isSuccess) {
                        repository.deleteCachedFavorite(favoriteId);
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
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
        Favorite favorite = localFavorites.getFavorite(favoriteId).blockingGet();
        cloudFavorites.uploadFavorite(favorite)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    boolean isSuccess = false;
                    if (result.isSuccess()) {
                        JsonFile jsonFile = (JsonFile) result.getData().get(0);
                        SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                        int numRows = localFavorites.updateFavorite(favorite.getId(), state)
                                .blockingGet();
                        isSuccess = (numRows == 1);
                    }
                    if (isSuccess) {
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
        cloudFavorites.downloadFavorite(favoriteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(favorite -> {
                    long rowId = localFavorites.saveFavorite(favorite).blockingGet();
                    if (rowId > 0) {
                        repository.refreshFavorites();
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }
}
