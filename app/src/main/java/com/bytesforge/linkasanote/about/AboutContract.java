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

package com.bytesforge.linkasanote.about;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;

public interface AboutContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull AboutContract.ViewModel viewModel);
        boolean isActive();

        void showGooglePlay();
        void showLicenseTermsAlertDialog(@NonNull String licenseText);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(Bundle outState);
        void applyInstanceState(@NonNull Bundle state);
        Bundle getDefaultInstanceState();
        void showLaunchGooglePlayErrorSnackbar();
    }

    interface Presenter extends BasePresenter {

        void onLaunchGooglePlay();
        void onLicenseTermsGplV3Click();
        void onLicenseTermsApacheV2Click();
        void onLicenseTermsMitClick();
    }
}
