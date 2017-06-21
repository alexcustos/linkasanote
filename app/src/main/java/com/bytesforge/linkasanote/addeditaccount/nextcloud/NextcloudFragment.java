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

package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.FragmentAddEditAccountNextcloudBinding;
import com.bytesforge.linkasanote.sync.operations.OperationsService;
import com.bytesforge.linkasanote.sync.operations.nextcloud.CheckCredentialsOperation;
import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import java.util.ArrayList;
import java.util.List;

import static com.bytesforge.linkasanote.utils.CloudUtils.getAccountType;
import static com.bytesforge.linkasanote.utils.CloudUtils.getAccountUsername;
import static com.bytesforge.linkasanote.utils.CommonUtils.convertIdn;
import static com.google.common.base.Preconditions.checkNotNull;

public class NextcloudFragment extends Fragment implements
        NextcloudContract.View, OnRemoteOperationListener {

    private static final String TAG = NextcloudFragment.class.getSimpleName();

    private static final int ACCOUNT_VERSION = 1;
    public static final String ARGUMENT_EDIT_ACCOUNT_ACCOUNT = "EDIT_ACCOUNT_ACCOUNT";

    private AccountAuthenticatorResponse accountAuthenticatorResponse = null;
    private NextcloudContract.Presenter presenter;
    private NextcloudContract.ViewModel viewModel;
    private FragmentAddEditAccountNextcloudBinding binding;
    private AccountManager accountManager;

    private final Handler handler = new Handler();
    private static final Object operationsLock = new Object();
    private OperationsService operationsService = null;
    private List<Intent> operationsQueue = new ArrayList<>();

    public static NextcloudFragment newInstance() {
        return new NextcloudFragment();
    }

    private void bindOperationService() {
        Intent intent = new Intent(getContext(), OperationsService.class);
        getActivity().bindService(intent, operationsServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindOperationService() {
        if (operationsService != null) {
            getActivity().unbindService(operationsServiceConnection);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindOperationService();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe();
    }

    @Override
    public void onPause() {
        presenter.unsubscribe();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        unbindOperationService();
        super.onDestroy();
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void setPresenter(@NonNull NextcloudContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setViewModel(@NonNull NextcloudContract.ViewModel viewModel) {
        this.viewModel = checkNotNull(viewModel);
    }

    @Override
    public void setAccountManager(@NonNull AccountManager accountManager) {
        this.accountManager = checkNotNull(accountManager);
    }

    @Override
    public void setAccountAuthenticatorResponse(
            AccountAuthenticatorResponse accountAuthenticatorResponse) {
        this.accountAuthenticatorResponse = accountAuthenticatorResponse;
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_add_edit_account_nextcloud, container, false);
        binding.setViewModel((NextcloudViewModel) viewModel);
        viewModel.setInstanceState(savedInstanceState);
        if (savedInstanceState == null && !presenter.isNewAccount()) {
            // NOTE: here because populated account must stay intact on orientation change
            presenter.populateAccount();
        }
        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.saveInstanceState(outState);
    }

    private void finishActivity(@NonNull Intent result) {
        checkNotNull(result);
        FragmentActivity activity = getActivity();
        if (accountAuthenticatorResponse != null) {
            accountAuthenticatorResponse.onResult(result.getExtras());
            accountAuthenticatorResponse = null;
        }
        activity.setResult(Activity.RESULT_OK, result);
        activity.finish();
    }

    @Override
    public void finishActivity(
            @NonNull Account account, @NonNull String password, @NonNull Bundle data) {
        checkNotNull(account);
        checkNotNull(password);
        checkNotNull(data);
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
        intent.putExtra(AccountManager.KEY_PASSWORD, password);
        intent.putExtra(AccountManager.KEY_USERDATA, data);

        finishActivity(intent);
    }

    @Override
    public void cancelActivity() {
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

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof GetServerInfoOperation) {
            // GetServerInfoOperation
            viewModel.showConnectionResultStatus(result.getCode());
            if (result.isSuccess()) {
                GetServerInfoOperation.ServerInfo serverInfo =
                        (GetServerInfoOperation.ServerInfo) result.getData().get(0);
                presenter.setServerInfo(serverInfo);
                viewModel.hideRefreshButton();
                viewModel.checkLoginButton();
            } else {
                presenter.setServerInfo(null);
                viewModel.showRefreshButton();
                viewModel.hideAuthStatus();
                viewModel.disableLoginButton();
            }
        } else if (operation instanceof CheckCredentialsOperation) {
            // CheckCredentialsOperation
            if (!presenter.isServerUrlValid()) { // NOTE: checked when operation is sent
                viewModel.showConnectionResultStatus(result.getCode());
                viewModel.showRefreshButton();
                viewModel.hideAuthStatus();
                viewModel.disableLoginButton();
            } else if (result.isSuccess()) {
                GetServerInfoOperation.ServerInfo serverInfo = presenter.getServerInfo();
                assert serverInfo != null;
                if (presenter.isNewAccount()) {
                    addAccount(result, serverInfo);
                } else {
                    updateAccount(result, presenter.getAccount(), serverInfo);
                }
            } else if (result.isServerFail()) {
                presenter.setServerInfo(null);
                viewModel.showConnectionResultStatus(result.getCode());
                viewModel.showRefreshButton();
                viewModel.hideAuthStatus();
                viewModel.disableLoginButton();
            } else { // NOTE: it's wrong credentials or result.isException()
                viewModel.showAuthResultStatus(result.getCode());
                viewModel.enableLoginButton();
            }
        }
    }

    @Override
    public void addAccount(
            @NonNull RemoteOperationResult result,
            @NonNull GetServerInfoOperation.ServerInfo serverInfo) {
        checkNotNull(result);
        checkNotNull(serverInfo);
        UserInfo userInfo = null;
        OwnCloudCredentials credentials = null;
        for (Object object : result.getData()) {
            if (object instanceof UserInfo) {
                userInfo = (UserInfo) object;
            } else if (object instanceof OwnCloudCredentials) {
                credentials = (OwnCloudCredentials) object;
            }
        }
        checkNotNull(credentials);

        Uri baseUri = Uri.parse(serverInfo.baseUrl);
        String username = credentials.getUsername();
        String accountName = AccountUtils.buildAccountName(baseUri, username);
        Account newAccount = new Account(accountName, getAccountType(getContext()));

        // NOTE: permission is already granted at this point
        if (CloudUtils.isAccountExists(getContext(), newAccount, accountManager)) {
            viewModel.showAuthResultStatus(RemoteOperationResult.ResultCode.ACCOUNT_NOT_NEW);
            viewModel.enableLoginButton();
            return;
        }
        final Bundle userData = new Bundle();
        userData.putString(AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                Integer.toString(ACCOUNT_VERSION));
        userData.putString(AccountUtils.Constants.KEY_OC_VERSION, serverInfo.version.getVersion());
        userData.putString(AccountUtils.Constants.KEY_OC_BASE_URL, serverInfo.baseUrl);
        if (userInfo != null) {
            userData.putString(AccountUtils.Constants.KEY_DISPLAY_NAME, userInfo.getDisplayName());
        }

        boolean success = accountManager.addAccountExplicitly(
                newAccount, credentials.getAuthToken(), userData);
        if (success) {
            finishActivity(newAccount, credentials.getAuthToken(), userData);
            return;
        }
        viewModel.showSomethingWrongSnackbar();
    }

    @Override
    public void updateAccount(
            @NonNull RemoteOperationResult result,
            @Nullable Account account,
            @NonNull GetServerInfoOperation.ServerInfo serverInfo) {
        checkNotNull(result);
        checkNotNull(serverInfo);
        if (account == null) {
            throw new RuntimeException("updateAccount() was called but account is null.");
        }
        UserInfo userInfo = null;
        OwnCloudCredentials credentials = null;
        // Updated info from the server
        for (Object object : result.getData()) {
            if (object instanceof UserInfo) {
                userInfo = (UserInfo) object;
            } else if (object instanceof OwnCloudCredentials) {
                credentials = (OwnCloudCredentials) object;
            }
        }
        checkNotNull(credentials);

        final Bundle userData = new Bundle();
        userData.putString(AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                Integer.toString(ACCOUNT_VERSION));
        userData.putString(AccountUtils.Constants.KEY_OC_VERSION, serverInfo.version.getVersion());
        userData.putString(AccountUtils.Constants.KEY_OC_BASE_URL, serverInfo.baseUrl);
        if (userInfo != null) {
            userData.putString(AccountUtils.Constants.KEY_DISPLAY_NAME, userInfo.getDisplayName());
        }
        // Update
        for (String key : userData.keySet()) {
            Object value = userData.get(key);
            accountManager.setUserData(account, key, value == null ? null : value.toString());
        }
        String password = credentials.getAuthToken();
        accountManager.setPassword(account, password);
        try {
            // NOTE: remove managed clients for this account to enforce creation with fresh credentials
            OwnCloudAccount ownCloudAccount = new OwnCloudAccount(account, getContext());
            OwnCloudClientManagerFactory.getDefaultSingleton().removeClientFor(ownCloudAccount);

            finishActivity(account, password, userData);
        } catch (AccountUtils.AccountNotFoundException e) {
            showAccountDoesNotExistSnackbar();
        }
    }

    @Override
    public void setupAccountState(@NonNull Account account) {
        checkNotNull(account);
        String serverUrlText = accountManager.getUserData(
                account, AccountUtils.Constants.KEY_OC_BASE_URL);
        String accountUsernameText = getAccountUsername(account.name);
        viewModel.populateAccount(serverUrlText, accountUsernameText);
        binding.accountPassword.requestFocus();
        presenter.validateServerUrlText(serverUrlText);
    }

    private boolean queueOperation(@NonNull Intent operationIntent) {
        checkNotNull(operationIntent);
        synchronized (operationsLock) {
            if (operationsService == null) {
                operationsQueue.add(operationIntent);
                return false;
            } else {
                operationsService.queueOperation(operationIntent, this, handler);
                return true;
            }
        }
    }

    @VisibleForTesting
    public void setOperationsService(@NonNull OperationsService service) {
        checkNotNull(service);
        synchronized (operationsLock) {
            operationsService = service;
            for (Intent intent : operationsQueue) {
                operationsService.queueOperation(intent, this, handler);
            }
            operationsQueue.clear();
        }
    }

    @Override
    public boolean sendGetServerInfoOperation(String url) {
        Intent getServerInfoIntent = new Intent();
        getServerInfoIntent.setAction(OperationsService.ACTION_GET_SERVER_INFO);
        getServerInfoIntent.putExtra(OperationsService.EXTRA_SERVER_URL, convertIdn(url, true));
        return queueOperation(getServerInfoIntent);
    }

    @Override
    public boolean sendCheckCredentialsOperation(
            String username, String password,
            @Nullable GetServerInfoOperation.ServerInfo serverInfo) {
        if (serverInfo == null || !serverInfo.isSet()) {
            throw new IllegalStateException(
                    "CheckCredentialsOperation must not be called before serverInfo is set");
        }
        Bundle credentials = new Bundle();
        credentials.putString(CheckCredentialsOperation.ACCOUNT_USERNAME, username);
        credentials.putString(CheckCredentialsOperation.ACCOUNT_PASSWORD, password);

        Intent checkCredentialsIntent = new Intent();
        checkCredentialsIntent.setAction(OperationsService.ACTION_CHECK_CREDENTIALS);
        checkCredentialsIntent.putExtra(OperationsService.EXTRA_SERVER_URL, serverInfo.baseUrl);
        checkCredentialsIntent.putExtra(
                OperationsService.EXTRA_SERVER_VERSION, serverInfo.version.getVersion());
        checkCredentialsIntent.putExtra(OperationsService.EXTRA_CREDENTIALS, credentials);
        return queueOperation(checkCredentialsIntent);
    }

    private void showAccountDoesNotExistSnackbar() {
        Snackbar.make(binding.snackbarLayout,
                R.string.add_edit_account_nextcloud_warning_no_account, Snackbar.LENGTH_LONG)
                .addCallback(new Snackbar.Callback() {

                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        cancelActivity();
                    }
                }).show();
    }

    @Override
    public void openAboutNextcloudLink() {
        String url = getResources().getString(R.string.about_nextcloud_url);
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    // Service

    private ServiceConnection operationsServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Service connected [" + className.getShortClassName() + "]");
            OperationsService.OperationsBinder binder =
                    (OperationsService.OperationsBinder) service;
            operationsService = binder.getService();
            setOperationsService(operationsService);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Service disconnected [" + className.getShortClassName() + "]");
            operationsService = null;
        }
    };

    @VisibleForTesting
    NextcloudContract.Presenter getPresenter() {
        return presenter;
    }
}
