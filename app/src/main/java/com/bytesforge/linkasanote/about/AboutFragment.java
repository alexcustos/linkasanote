package com.bytesforge.linkasanote.about;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.FragmentAboutBinding;
import com.google.common.base.Charsets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.google.common.base.Preconditions.checkNotNull;

public class AboutFragment extends Fragment implements AboutContract.View {

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
    public void showGplV3TermsAlertDialog() {
        LicenseTermsDialog dialog = LicenseTermsDialog.newInstance("gpl-3.0.en.html");
        dialog.show(getFragmentManager(), LicenseTermsDialog.DIALOG_TAG);
    }

    @Override
    public void showApacheV2TermsAlertDialog() {
        LicenseTermsDialog dialog = LicenseTermsDialog.newInstance("LICENSE-2.0.html");
        dialog.show(getFragmentManager(), LicenseTermsDialog.DIALOG_TAG);
    }

    public static class LicenseTermsDialog extends DialogFragment {

        private static final String ARGUMENT_LICENSE_NAME = "LICENSE_NAME";

        public static final String DIALOG_TAG = "LICENSE_TERMS";

        private String licenseName;

        public static LicenseTermsDialog newInstance(String licenseName) {
            Bundle args = new Bundle();
            args.putString(ARGUMENT_LICENSE_NAME, licenseName);
            LicenseTermsDialog dialog = new LicenseTermsDialog();
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void onStart() {
            super.onStart();
            ((TextView) getDialog().findViewById(android.R.id.message))
                    .setMovementMethod(LinkMovementMethod.getInstance());
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            licenseName = getArguments().getString(ARGUMENT_LICENSE_NAME);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String licenseText;
            Resources resources = getContext().getResources();
            try {
                String line;
                StringBuilder builder = new StringBuilder();
                InputStream stream = getContext().getResources().getAssets().open(licenseName);
                BufferedReader in = new BufferedReader(new InputStreamReader(stream, Charsets.UTF_8));
                while ((line = in.readLine()) != null) {
                    builder.append(line);
                    builder.append('\n');
                }
                in.close();
                licenseText = builder.toString();
            } catch (IOException e) {
                licenseText = resources.getString(
                        R.string.about_fragment_error_license, licenseName);
            }
            return new AlertDialog.Builder(getContext())
                    .setMessage(Html.fromHtml(licenseText, Html.FROM_HTML_MODE_LEGACY))
                    .setPositiveButton(resources.getString(R.string.dialog_button_ok), null)
                    .create();
        }
    }
}
