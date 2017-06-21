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

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.text.format.DateFormat;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.BuildConfig;
import com.bytesforge.linkasanote.R;

import static com.google.common.base.Preconditions.checkNotNull;

public class AboutViewModel extends BaseObservable implements AboutContract.ViewModel {

    public static final String STATE_APP_VERSION_TEXT = "APP_VERSION_TEXT";
    public static final String STATE_APP_COPYRIGHT_TEXT = "APP_COPYRIGHT_TEXT";

    public final ObservableField<String> appVersionText = new ObservableField<>();
    public final ObservableField<String> appCopyrightText = new ObservableField<>();

    private Resources resources;

    public enum SnackbarId {
        ABOUT_LAUNCH_GOOGLE_PLAY_ERROR}

    public AboutViewModel(@NonNull Context context) {
        resources = checkNotNull(context).getResources();
    }

    @Bindable
    public SnackbarId snackbarId;

    @BindingAdapter({"snackbarId"})
    public static void showSnackbar(CoordinatorLayout view, SnackbarId snackbarId) {
        if (snackbarId == null) return;

        switch (snackbarId) {
            case ABOUT_LAUNCH_GOOGLE_PLAY_ERROR:
                Snackbar.make(view,
                        R.string.about_launch_google_play_error,
                        Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public void setPresenter(@NonNull AboutContract.Presenter presenter) {
    }

    @Override
    public void setInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            applyInstanceState(getDefaultInstanceState());
        } else {
            applyInstanceState(savedInstanceState);
        }
    }

    @Override
    public void saveInstanceState(@NonNull Bundle outState) {
        checkNotNull(outState);
        outState.putString(STATE_APP_VERSION_TEXT, appVersionText.get());
        outState.putString(STATE_APP_COPYRIGHT_TEXT, appCopyrightText.get());
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        appVersionText.set(state.getString(STATE_APP_VERSION_TEXT));
        appCopyrightText.set(state.getString(STATE_APP_COPYRIGHT_TEXT));
    }

    @Override
    public Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();
        String buildYear = DateFormat.format("yyyy", BuildConfig.BUILD_TIMESTAMP).toString();
        String createdYear = resources.getString(R.string.app_was_created_year);
        if (!buildYear.equals(createdYear)) {
            buildYear = createdYear + " - " + buildYear;
        }
        defaultState.putString(STATE_APP_VERSION_TEXT,
                resources.getString(R.string.about_app_version, BuildConfig.VERSION_NAME));
        defaultState.putString(STATE_APP_COPYRIGHT_TEXT, resources.getString(
                R.string.about_app_copyright, buildYear, resources.getString(R.string.app_author)));

        return defaultState;
    }

    // Snackbar

    @Override
    public void showLaunchGooglePlayErrorSnackbar() {
        snackbarId = SnackbarId.ABOUT_LAUNCH_GOOGLE_PLAY_ERROR;
        notifyPropertyChanged(BR.snackbarId);
    }
}
