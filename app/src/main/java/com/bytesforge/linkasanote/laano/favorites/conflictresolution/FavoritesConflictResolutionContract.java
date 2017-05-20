package com.bytesforge.linkasanote.laano.favorites.conflictresolution;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Favorite;

public interface FavoritesConflictResolutionContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull FavoritesConflictResolutionContract.ViewModel viewModel);
        boolean isActive();
        void finishActivity();
        void cancelActivity();
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(@NonNull Bundle outState);
        void applyInstanceState(@NonNull Bundle state);

        void populateLocalFavorite(@NonNull Favorite favorite);
        boolean isLocalPopulated();
        void populateCloudFavorite(@NonNull Favorite favorite);
        boolean isCloudPopulated();
        void showCloudNotFound();
        void showCloudDownloadError();
        void showDatabaseError();
        void showCloudLoading();
        boolean isStateDuplicated();
        String getLocalId();
        String getCloudId();
        void activateButtons();
        void deactivateButtons();
        void showProgressOverlay();
        void hideProgressOverlay();
    }

    interface Presenter extends BasePresenter {

        void onLocalDeleteClick();
        void onCloudDeleteClick();
        void onCloudRetryClick();
        void onLocalUploadClick();
        void onCloudDownloadClick();
    }
}
