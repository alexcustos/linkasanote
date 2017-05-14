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
import android.support.design.widget.Snackbar;
import android.text.format.DateFormat;
import android.widget.FrameLayout;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.BuildConfig;
import com.bytesforge.linkasanote.R;

import static com.google.common.base.Preconditions.checkNotNull;

public class AboutViewModel extends BaseObservable implements AboutContract.ViewModel {

    public static final String STATE_APP_VERSION_TEXT = "APP_VERSION_TEXT";
    public static final String STATE_APP_COPYRIGHT_TEXT = "APP_COPYRIGHT_TEXT";
    private static final String STATE_PROGRESS_OVERLAY = "PROGRESS_OVERLAY";

    public final ObservableField<String> appVersionText = new ObservableField<>();
    public final ObservableField<String> appCopyrightText = new ObservableField<>();

    private Resources resources;

    public enum SnackbarId {
        ABOUT_LAUNCH_GOOGLE_PLAY_ERROR}

    public AboutViewModel(@NonNull Context context) {
        resources = context.getResources();
    }

    @Bindable
    public boolean progressOverlay;

    @Bindable
    public SnackbarId snackbarId;

    @BindingAdapter({"snackbarId"})
    public static void showSnackbar(FrameLayout view, SnackbarId snackbarId) {
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
        outState.putBoolean(STATE_PROGRESS_OVERLAY, progressOverlay);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        appVersionText.set(state.getString(STATE_APP_VERSION_TEXT));
        appCopyrightText.set(state.getString(STATE_APP_COPYRIGHT_TEXT));
        progressOverlay = state.getBoolean(STATE_PROGRESS_OVERLAY);
    }

    @Override
    public Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();
        String buildYear = DateFormat.format("yyyy", BuildConfig.BUILD_TIMESTAMP).toString();

        defaultState.putString(STATE_APP_VERSION_TEXT,
                resources.getString(R.string.about_app_version, BuildConfig.VERSION_NAME));
        defaultState.putString(STATE_APP_COPYRIGHT_TEXT, resources.getString(
                R.string.about_app_copyright, buildYear, resources.getString(R.string.app_author)));
        defaultState.putBoolean(STATE_PROGRESS_OVERLAY, false);

        return defaultState;
    }

    // Progress

    @Override
    public void showProgressOverlay() {
        if (!progressOverlay) {
            progressOverlay = true;
            notifyPropertyChanged(BR.progressOverlay);
        }
    }

    @Override
    public void hideProgressOverlay() {
        if (progressOverlay) {
            progressOverlay = false;
            notifyPropertyChanged(BR.progressOverlay);
        }
    }

    // Snackbar

    @Override
    public void showLaunchGooglePlayErrorSnackbar() {
        snackbarId = SnackbarId.ABOUT_LAUNCH_GOOGLE_PLAY_ERROR;
        notifyPropertyChanged(BR.snackbarId);
    }
}
