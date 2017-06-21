/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
                                repository.refreshFavorite(favoriteId);
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
        }
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
            replaceFavorite(viewModel.getLocalId(), viewModel.getCloudId());
        } else {
            deleteFavorite(viewModel.getLocalId());
        }
    }

    private void replaceFavorite(
            @NonNull final String localFavoriteId, @NonNull final String duplicatedFavoriteId) {
        checkNotNull(localFavoriteId);
        checkNotNull(duplicatedFavoriteId);
        viewModel.showProgressOverlay();
        deleteFavoriteSingle(localFavoriteId)
                .subscribeOn(schedulerProvider.io())
                .map(success -> {
                    if (success) {
                        SyncState state = new SyncState(SyncState.State.SYNCED);
                        success = localFavorites.update(duplicatedFavoriteId, state).blockingGet();
                        repository.refreshFavorite(duplicatedFavoriteId);
                    }
                    return success;
                })
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
                        settings.resetFavoriteFilterId(favoriteId);
                    }
                });
    }

    @Override
    public void onCloudDeleteClick() {
        viewModel.deactivateButtons();
        deleteFavorite(viewModel.getCloudId());
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
        String favoriteId = viewModel.getLocalId();
        localFavorites.get(favoriteId)
                .subscribeOn(schedulerProvider.io())
                .flatMap(cloudFavorites::upload)
                .map(result -> {
                    boolean success = false;
                    if (result.isSuccess()) {
                        JsonFile jsonFile = (JsonFile) result.getData().get(0);
                        SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                        success = localFavorites.update(favoriteId, state).blockingGet();
                    }
                    return success;
                })
                .observeOn(schedulerProvider.ui())
                .doFinally(viewModel::hideProgressOverlay)
                .subscribe(success -> {
                    if (success) {
                        repository.refreshFavorite(favoriteId);
                        refreshFavoriteFilter(favoriteId);
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
        String favoriteId = viewModel.getCloudId();
        cloudFavorites.download(favoriteId)
                .subscribeOn(schedulerProvider.io())
                .flatMap(localFavorites::save)
                .observeOn(schedulerProvider.ui())
                .doFinally(viewModel::hideProgressOverlay)
                .subscribe(success -> {
                    if (success) {
                        // NOTE: most likely the same Favorite was updated and position remain unchanged
                        repository.refreshFavorite(favoriteId);
                        refreshFavoriteFilter(favoriteId);
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    private void refreshFavoriteFilter(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        String favoriteFilterId = settings.getFavoriteFilterId();
        if (favoriteId.equals(favoriteFilterId)) {
            settings.setFavoriteFilter(null); // filter is set to be refreshed
        }
    }
}
