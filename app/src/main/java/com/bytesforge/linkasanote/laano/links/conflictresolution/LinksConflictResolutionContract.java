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

package com.bytesforge.linkasanote.laano.links.conflictresolution;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Link;

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
