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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.BuildConfig;
import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.DialogAboutLicenseTermsBinding;
import com.bytesforge.linkasanote.databinding.FragmentAboutBinding;
import com.bytesforge.linkasanote.utils.ActivityUtils;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Charsets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class AboutFragment extends Fragment implements AboutContract.View {

    private static final String TAG = AboutFragment.class.getSimpleName();
    private static final String TAG_E = AboutFragment.class.getCanonicalName();

    private static final String GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending";

    private AboutContract.Presenter presenter;
    private AboutContract.ViewModel viewModel;

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.unsubscribe();
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void setPresenter(@NonNull AboutContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setViewModel(@NonNull AboutContract.ViewModel viewModel) {
        this.viewModel = checkNotNull(viewModel);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentAboutBinding binding = FragmentAboutBinding.inflate(inflater, container, false);
        viewModel.setInstanceState(savedInstanceState);
        binding.setPresenter(presenter);
        binding.setViewModel((AboutViewModel) viewModel);
        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.saveInstanceState(outState);
    }

    @Override
    public void showGooglePlay() {
        Resources resources = getResources();
        final Uri uri = Uri.parse(
                resources.getString(R.string.google_market) + BuildConfig.APPLICATION_ID);
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        final List<ResolveInfo> apps =
                getContext().getPackageManager().queryIntentActivities(intent, 0);
        boolean found = false;
        for (ResolveInfo app : apps) {
            if (app.activityInfo.applicationInfo.packageName.equals(GOOGLE_PLAY_PACKAGE_NAME)) {
                ActivityInfo activityInfo = app.activityInfo;
                ComponentName componentName = new ComponentName(
                        activityInfo.applicationInfo.packageName, activityInfo.name);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setComponent(componentName);
                startActivity(intent);
                found = true;
                break;
            }
        }
        if (!found) {
            Uri webUri = Uri.parse(
                    resources.getString(R.string.google_play) + BuildConfig.APPLICATION_ID);
            Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
            try {
                startActivity(webIntent);
            } catch (ActivityNotFoundException e) {
                viewModel.showLaunchGooglePlayErrorSnackbar();
            }
        }
    }

    @Override
    public void showLicenseTermsAlertDialog(@NonNull String licenseAsset) {
        checkNotNull(licenseAsset);
        LicenseTermsDialog dialog = LicenseTermsDialog.newInstance(licenseAsset);
        dialog.show(getFragmentManager(), LicenseTermsDialog.DIALOG_TAG);
    }

    public static class LicenseTermsDialog extends DialogFragment {

        private static final String ARGUMENT_LICENSE_ASSET = "LICENSE_ASSET";

        public static final String DIALOG_TAG = "LICENSE_TERMS";

        private Context context;
        private Resources resources;
        DialogAboutLicenseTermsBinding binding;

        private String licenseAsset;

        @Inject
        BaseSchedulerProvider schedulerProvider;

        public static LicenseTermsDialog newInstance(String licenseAsset) {
            Bundle args = new Bundle();
            args.putString(ARGUMENT_LICENSE_ASSET, licenseAsset);
            LicenseTermsDialog dialog = new LicenseTermsDialog();
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void onStart() {
            super.onStart();
            binding.licenseTerms.setMovementMethod(LinkMovementMethod.getInstance());
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            licenseAsset = getArguments().getString(ARGUMENT_LICENSE_ASSET);
            context = getContext();
            resources = context.getResources();
            LaanoApplication application = (LaanoApplication) getActivity().getApplication();
            application.getApplicationComponent().inject(this);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = LayoutInflater.from(context);
            binding = DialogAboutLicenseTermsBinding.inflate(inflater, null, false);
            Single.fromCallable(() -> getLicenseText(licenseAsset))
                    .subscribeOn(schedulerProvider.computation())
                    .map(ActivityUtils::fromHtmlCompat)
                    .observeOn(schedulerProvider.ui())
                    .subscribe(
                            binding.licenseTerms::setText,
                            throwable -> CommonUtils.logStackTrace(TAG_E, throwable));
            return new AlertDialog.Builder(context)
                    .setView(binding.getRoot())
                    .setPositiveButton(resources.getString(R.string.dialog_button_ok), null)
                    .create();
        }

        private String getLicenseText(@NonNull String assetName) {
            checkNotNull(assetName);
            Resources resources = context.getResources();
            String licenseText;
            try {
                String line;
                StringBuilder builder = new StringBuilder();
                InputStream stream = resources.getAssets().open(assetName);
                BufferedReader in = new BufferedReader(new InputStreamReader(stream, Charsets.UTF_8));
                while ((line = in.readLine()) != null) {
                    builder.append(line);
                    builder.append('\n');
                }
                in.close();
                licenseText = builder.toString();
            } catch (IOException e) {
                licenseText = resources.getString(R.string.about_fragment_error_license, assetName);
            }
            return licenseText;
        }
    }
}
