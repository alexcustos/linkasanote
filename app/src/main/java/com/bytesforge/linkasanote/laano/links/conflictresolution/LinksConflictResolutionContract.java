package com.bytesforge.linkasanote.laano.links.conflictresolution;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Link;

import io.reactivex.Single;

public interface LinksConflictResolutionContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull LinksConflictResolutionContract.ViewModel viewModel);
        boolean isActive();
        void finishActivity();
        void cancelActivity();
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(@NonNull Bundle outState);
        void applyInstanceState(@NonNull Bundle state);

        void populateLocalLink(@NonNull Link link);
        boolean isLocalPopulated();
        void populateCloudLink(@NonNull Link link);
        boolean isCloudPopulated();
        void showCloudNotFound();
        void showCloudDownloadError();
        void showDatabaseError();
        void showCloudLoading();
        boolean isStateDuplicated();
        String getLocalName();
        void activateButtons();
        void deactivateButtons();
    }

    interface Presenter extends BasePresenter {

        void onLocalDeleteClick();
        void onCloudDeleteClick();
        void onCloudRetryClick();
        void onLocalUploadClick();
        void onCloudDownloadClick();
        Single<Boolean> autoResolve();
    }
}
