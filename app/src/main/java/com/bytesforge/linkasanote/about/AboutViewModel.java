package com.bytesforge.linkasanote.about;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;

import com.bytesforge.linkasanote.BuildConfig;
import com.bytesforge.linkasanote.R;

import static com.google.common.base.Preconditions.checkNotNull;

public class AboutViewModel extends BaseObservable implements AboutContract.ViewModel {

    public static final String STATE_APP_VERSION_TEXT = "APP_VERSION_TEXT";
    public static final String STATE_APP_COPYRIGHT_TEXT = "APP_COPYRIGHT_TEXT";

    public final ObservableField<String> appVersionText = new ObservableField<>();
    public final ObservableField<String> appCopyrightText = new ObservableField<>();

    private Context context;
    private Resources resources;
    private AboutContract.Presenter presenter;


    public AboutViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
        resources = context.getResources();
    }

    @Override
    public void setPresenter(@NonNull AboutContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
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

        defaultState.putString(STATE_APP_VERSION_TEXT,
                resources.getString(R.string.about_app_version, BuildConfig.VERSION_NAME));
        defaultState.putString(STATE_APP_COPYRIGHT_TEXT, resources.getString(
                R.string.about_app_copyright, buildYear, resources.getString(R.string.app_author)));

        return defaultState;
    }
}
