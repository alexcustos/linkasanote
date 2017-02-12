package com.bytesforge.linkasanote.addeditaccount;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudFragment;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudPresenter;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudPresenterModule;
import com.bytesforge.linkasanote.databinding.ActivityAddEditAccountBinding;
import com.bytesforge.linkasanote.sync.operations.OperationsService;
import com.bytesforge.linkasanote.utils.ActivityUtils;

import javax.inject.Inject;

import static com.bytesforge.linkasanote.utils.CloudUtils.getAccountType;

public class AddEditAccountActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int REQUEST_PERMISSION_GET_ACCOUNTS = 0;
    private static final String PERMISSION_GET_ACCOUNTS = Manifest.permission.GET_ACCOUNTS;
    private static String[] PERMISSIONS_GET_ACCOUNTS = {PERMISSION_GET_ACCOUNTS};

    public static final String ARGUMENT_REQUEST_CODE = "REQUEST_CODE";
    public static final int REQUEST_ADD_NEXTCLOUD_ACCOUNT = 0;
    public static final int REQUEST_UPDATE_NEXTCLOUD_ACCOUNT = 1;

    public static final String HTTP_PROTOCOL = "http://";
    public static final String HTTPS_PROTOCOL = "https://";
    public static final String DEFAULT_PROTOCOL = HTTPS_PROTOCOL;

    private static final String TAG = AddEditAccountActivity.class.getSimpleName();

    private ActivityAddEditAccountBinding binding;
    private OperationsService operationsService;
    private Bundle currentViewModelSate;

    @Inject
    NextcloudPresenter presenter;

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
        // Fragment (View)
        NextcloudFragment nextcloudFragment = (NextcloudFragment) getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        if (nextcloudFragment == null) {
            nextcloudFragment = NextcloudFragment.newInstance();
            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), nextcloudFragment, R.id.content_frame);
        }
        // Presenter
        DaggerAddEditAccountComponent.builder()
                .applicationComponent(((LaanoApplication) getApplication()).getApplicationComponent())
                .nextcloudPresenterModule(new NextcloudPresenterModule(nextcloudFragment, account))
                .build().inject(this);
        // Service binding
        Intent intent = new Intent(this, OperationsService.class);
        bindService(intent, operationsServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkGetAccountsPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (operationsService != null) {
            unbindService(operationsServiceConnection);
        }
    }

    // Get Accounts Permission

    public void checkGetAccountsPermission() {
        if (ActivityCompat.checkSelfPermission(this, PERMISSION_GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            requestGetAccountsPermission();
        } else if (!accountCanBeAdded()) {
            disableActivity();
            showUnsupportedMultipleAccountsSnackbar();
        }
    }

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
                if (accountCanBeAdded()) enableActivity();
                else showUnsupportedMultipleAccountsSnackbar();
            } else {
                showNotEnoughPermissionsSnackbar();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressWarnings("MissingPermission")
    private boolean accountCanBeAdded() {
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType(getAccountType());

        return getResources().getBoolean(R.bool.multiaccount_support) || accounts.length <= 0;
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

    private void showNotEnoughPermissionsSnackbar() {
        Snackbar.make(binding.contentFrame,
                R.string.snackbar_no_permission, Snackbar.LENGTH_LONG)
                .addCallback(new Snackbar.Callback() {

                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        cancelActivity();
                    }
                }).show();
    }

    private void showUnsupportedMultipleAccountsSnackbar() {
        Snackbar.make(binding.contentFrame,
                R.string.add_edit_accounts_unsupported_multiple_accounts, Snackbar.LENGTH_LONG)
                .addCallback(new Snackbar.Callback() {

                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        cancelActivity();
                    }
                }).show();
    }

    // Service

    private ServiceConnection operationsServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Service connected [" + className.getShortClassName() + "]");
            OperationsService.OperationsBinder binder =
                    (OperationsService.OperationsBinder) service;
            operationsService = binder.getService();
            presenter.setOperationsService(operationsService);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Service disconnected [" + className.getShortClassName() + "]");
            operationsService = null;
        }
    };

    private void cancelActivity() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
