package com.bytesforge.linkasanote.addeditaccount;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudFragment;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudPresenter;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudPresenterModule;
import com.bytesforge.linkasanote.databinding.ActivityAddEditAccountBinding;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.ActivityUtils;
import com.bytesforge.linkasanote.utils.CloudUtils;

import javax.inject.Inject;

public class AddEditAccountActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = AddEditAccountActivity.class.getSimpleName();

    private static final int REQUEST_PERMISSION_GET_ACCOUNTS = 0;
    private static final String PERMISSION_GET_ACCOUNTS = Manifest.permission.GET_ACCOUNTS;
    private static String[] PERMISSIONS_GET_ACCOUNTS = {PERMISSION_GET_ACCOUNTS};

    public static final String ARGUMENT_REQUEST_CODE = "REQUEST_CODE";
    public static final int REQUEST_ADD_NEXTCLOUD_ACCOUNT = 0;
    public static final int REQUEST_UPDATE_NEXTCLOUD_ACCOUNT = 1;

    public static final String HTTP_PROTOCOL = "http://";
    public static final String HTTPS_PROTOCOL = "https://";
    public static final String DEFAULT_PROTOCOL = HTTPS_PROTOCOL;

    private AccountAuthenticatorResponse accountAuthenticatorResponse = null;
    private ActivityAddEditAccountBinding binding;
    private Bundle currentViewModelSate;

    @Inject
    NextcloudPresenter presenter;

    @Inject
    AccountManager accountManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_edit_account);

        Intent startIntent = getIntent();
        Account account = null;
        int requestCode = startIntent.getIntExtra(ARGUMENT_REQUEST_CODE, -1);
        if (REQUEST_UPDATE_NEXTCLOUD_ACCOUNT == requestCode) {
            account = startIntent.getParcelableExtra(NextcloudFragment.ARGUMENT_EDIT_ACCOUNT_ACCOUNT);
        }
        accountAuthenticatorResponse = startIntent.getParcelableExtra(
                AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (accountAuthenticatorResponse != null) {
            accountAuthenticatorResponse.onRequestContinued();
        }
        // Fragment (View)
        NextcloudFragment nextcloudFragment = (NextcloudFragment) getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        if (nextcloudFragment == null) {
            nextcloudFragment = NextcloudFragment.newInstance();
            nextcloudFragment.setAccountAuthenticatorResponse(accountAuthenticatorResponse);
            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), nextcloudFragment, R.id.content_frame);
        }
        // Presenter
        LaanoApplication application = (LaanoApplication) getApplication();
        application.getApplicationComponent()
                .getAddEditAccountComponent(
                        new NextcloudPresenterModule(nextcloudFragment, account))
                .inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkGetAccountsPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // Get Accounts Permission

    public void checkGetAccountsPermission() {
        if (ActivityCompat.checkSelfPermission(this, PERMISSION_GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestGetAccountsPermission();
            } else {
                disableActivity();
                exitWithNotEnoughPermissionsError();
            }
        } else if (!accountCanBeProcessed()) {
            disableActivity();
            exitWithUnsupportedMultipleAccountsError();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestGetAccountsPermission() {
        disableActivity();
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_GET_ACCOUNTS)) {
            Snackbar.make(binding.contentFrame,
                    R.string.add_edit_account_permission_get_accounts,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_button_ok, view ->
                            requestPermissions(PERMISSIONS_GET_ACCOUNTS, REQUEST_PERMISSION_GET_ACCOUNTS))
                    .show();
        } else {
            requestPermissions(PERMISSIONS_GET_ACCOUNTS, REQUEST_PERMISSION_GET_ACCOUNTS);
        }
    }

    // NOTE: It must be there because activity can be launched from system settings
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_GET_ACCOUNTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (accountCanBeProcessed()) enableActivity();
                else exitWithUnsupportedMultipleAccountsError();
            } else {
                exitWithNotEnoughPermissionsError();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean accountCanBeProcessed() {
        Account[] accounts = CloudUtils.getAccountsWithPermissionCheck(this, accountManager);
        return !presenter.isNewAccount() // edit
                || (accounts != null && accounts.length <= 0) // first
                || Settings.GLOBAL_MULTIACCOUNT_SUPPORT;
    }

    private void disableActivity() {
        if (currentViewModelSate == null) {
            currentViewModelSate = presenter.getInstanceState();
        }
        ActivityUtils.disableViewGroupControls(binding.contentFrame);
    }

    private void enableActivity() {
        ActivityUtils.enableViewGroupControls(binding.contentFrame);
        if (currentViewModelSate != null) {
            presenter.applyInstanceState(currentViewModelSate); // notifyChange here
            currentViewModelSate = null;
        }
    }

    private void exitWithNotEnoughPermissionsError() {
        if (accountAuthenticatorResponse != null) {
            accountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION,
                    getResources().getString(R.string.snackbar_no_permission));
        }
        cancelActivity();
    }

    private void exitWithUnsupportedMultipleAccountsError() {
        if (accountAuthenticatorResponse != null) {
            accountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION,
                    getResources().getString(R.string.add_edit_accounts_unsupported_multiple_accounts));
        }
        cancelActivity();
    }

    private void cancelActivity() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
