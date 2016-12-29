package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.Manifest;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.Snackbar;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.FragmentAddEditAccountNextcloudBinding;

import static com.google.common.base.Preconditions.checkNotNull;

public class NextcloudFragment extends Fragment
        implements NextcloudContract.View, FragmentCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = NextcloudFragment.class.getSimpleName();

    private static final int REQUEST_GET_ACCOUNTS = 0;
    private static final String PERMISSION_GET_ACCOUNTS = Manifest.permission.GET_ACCOUNTS;
    private static String[] PERMISSIONS_GET_ACCOUNTS = {PERMISSION_GET_ACCOUNTS};

    private AccountAuthenticatorResponse accountAuthenticatorResponse = null;
    private FragmentAddEditAccountNextcloudBinding binding;
    private NextcloudContract.Presenter presenter;

    public static NextcloudFragment newInstance() {
        return new NextcloudFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.start();
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void setPresenter(@NonNull NextcloudContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @VisibleForTesting
    NextcloudContract.Presenter getPresenter() {
        return presenter;
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_add_edit_account_nextcloud, container, false);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        binding.setViewModel((NextcloudViewModel) presenter.getViewModel());

        accountAuthenticatorResponse = getActivity().getIntent()
                .getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (accountAuthenticatorResponse != null) {
            accountAuthenticatorResponse.onRequestContinued();
        }
    }

    @Override
    public void finishActivity(@NonNull Intent result) {
        checkNotNull(result);
        FragmentActivity activity = getActivity();

        if (accountAuthenticatorResponse != null) {
            accountAuthenticatorResponse.onResult(result.getExtras());
            accountAuthenticatorResponse = null;
        }
        activity.setResult(Activity.RESULT_OK, result);
        activity.finish();
    }

    private void finishActivity() {
        FragmentActivity activity = getActivity();

        if (accountAuthenticatorResponse != null) {
            accountAuthenticatorResponse.onError(
                    AccountManager.ERROR_CODE_CANCELED,
                    getString(R.string.add_edit_account_nextcloud_canceled));
            accountAuthenticatorResponse = null;
        }
        activity.setResult(Activity.RESULT_CANCELED);
        activity.finish();
    }

    // Get Accounts Permission

    @Override
    public void checkGetAccountsPermission() {
        if (ActivityCompat.checkSelfPermission(getContext(), PERMISSION_GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            requestGetAccountsPermission();
        } else {
            AccountManager accountManager = AccountManager.get(getContext());
            presenter.onGetAccountsPermissionGranted(accountManager);
        }
    }

    private void requestGetAccountsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                getActivity(), PERMISSION_GET_ACCOUNTS)) {
            Snackbar.make(binding.snackbarLayout,
                    R.string.add_edit_account_nextcloud_permission_get_accounts,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.add_edit_account_nextcloud_ok, (view) -> {
                        requestPermissions(PERMISSIONS_GET_ACCOUNTS, REQUEST_GET_ACCOUNTS);
                    }).show();
        } else {
            requestPermissions(PERMISSIONS_GET_ACCOUNTS, REQUEST_GET_ACCOUNTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_GET_ACCOUNTS) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AccountManager accountManager = AccountManager.get(getContext());
                presenter.onGetAccountsPermissionGranted(accountManager);
            } else {
                presenter.onGetAccountsPermissionDenied();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
