package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.sync.operations.OperationsService;
import com.bytesforge.linkasanote.sync.operations.nextcloud.CheckCredentialsOperation;
import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import javax.inject.Inject;

import static com.bytesforge.linkasanote.utils.CommonUtils.convertIdn;
import static com.bytesforge.linkasanote.utils.CommonUtils.getAccountType;
import static com.google.common.base.Preconditions.checkNotNull;

public class NextcloudPresenter
        implements NextcloudContract.Presenter, OnRemoteOperationListener {

    private static final String TAG = NextcloudPresenter.class.getSimpleName();

    private static final int ACCOUNT_VERSION = 1;
    public static final String HTTP_PROTOCOL = "http://";
    private static final String HTTPS_PROTOCOL = "https://";
    private static final String DEFAULT_PROTOCOL = HTTPS_PROTOCOL;
    private static final String NEXTCLOUD_INDEX = "/index.php";

    private final Handler handler = new Handler();
    private OperationsService operationsService = null;

    private NextcloudContract.View view;
    private NextcloudContract.ViewModel viewModel;

    private GetServerInfoOperation.ServerInfo serverInfo;
    private RemoteOperationResult checkCredentialsResult; // for the callback

    @Inject
    NextcloudPresenter(
            NextcloudContract.View view, NextcloudContract.ViewModel viewModel) {
        this.view = view;
        this.viewModel = viewModel;
        serverInfo = null;
    }

    @Inject
    void setupView() {
        viewModel.setPresenter(this);
        view.setPresenter(this);
    }

    @Override
    public void setOperationsService(OperationsService service) {
        operationsService = service;
    }

    @Override
    public void start() {
    }

    @Override
    public void checkUrl(@NonNull final String url) {
        viewModel.showTestingConnectionStatus();
        viewModel.hideRefreshButton();

        Intent getServerInfoIntent = new Intent();
        getServerInfoIntent.setAction(OperationsService.ACTION_GET_SERVER_INFO);
        getServerInfoIntent.putExtra(
                OperationsService.EXTRA_SERVER_URL, convertIdn(url, true));

        if (operationsService != null) {
            operationsService.queueOperation(getServerInfoIntent, this, handler);
        } else {
            viewModel.showRefreshButton();
            viewModel.showCheckUrlServiceNotReadyWarning();
        }
    }

    @Override
    public String normalizeUrl(final String serverUrl) {
        String normalizedUrl = "";

        // Empty
        if (serverUrl == null || serverUrl.length() <= 0) {
            viewModel.showEmptyUrlWarning();
            return normalizedUrl;
        }

        // Protocol
        normalizedUrl = serverUrl.toLowerCase().replaceAll("\\s+", "");
        if (!normalizedUrl.startsWith(HTTP_PROTOCOL) &&
                !normalizedUrl.startsWith(HTTPS_PROTOCOL)) {
            normalizedUrl = DEFAULT_PROTOCOL + normalizedUrl;
        }

        // Parsing
        URL url;
        try {
            url = new URL(normalizedUrl);
        } catch (MalformedURLException e) {
            viewModel.showMalformedUrlWarning();
            return normalizedUrl;
        }

        // Instance URL
        int port = url.getPort();
        String path = url.getPath();
        if (path.contains(NEXTCLOUD_INDEX)) {
            path = path.substring(0, path.indexOf(NEXTCLOUD_INDEX));
        }
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        normalizedUrl = url.getProtocol() + "://" +
                url.getHost() + ((port == -1 || port == 80) ? "" : ":" + port) + path;

        return normalizedUrl;
    }

    @Override
    public void checkAuth(String username, String password) {
        viewModel.showTestingAuthStatus();
        viewModel.disableLoginButton();

        Bundle credentials = new Bundle();
        credentials.putString(CheckCredentialsOperation.ACCOUNT_USERNAME, username);
        credentials.putString(CheckCredentialsOperation.ACCOUNT_PASSWORD, password);

        Intent checkCredentialsIntent = new Intent();
        checkCredentialsIntent.setAction(OperationsService.ACTION_CHECK_CREDENTIALS);
        checkCredentialsIntent.putExtra(OperationsService.EXTRA_SERVER_URL, serverInfo.baseUrl);
        checkCredentialsIntent.putExtra(OperationsService.EXTRA_CREDENTIALS, credentials);

        if (operationsService != null) {
            operationsService.queueOperation(checkCredentialsIntent, this, handler);
        } else {
            viewModel.enableLoginButton();
            viewModel.showCheckAuthServiceNotReadyWarning();
        }
    }

    @Override
    public NextcloudContract.ViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof GetServerInfoOperation) {
            // GetServerInfoOperation
            viewModel.showConnectionResultStatus(result.getCode());
            if (result.isSuccess()) {
                setServerInfo((GetServerInfoOperation.ServerInfo) result.getData().get(0));
                viewModel.checkLoginButton();

            } else {
                viewModel.showRefreshButton();
            }
        } else if (operation instanceof CheckCredentialsOperation) {
            // CheckCredentialsOperation
            if (result.isSuccess()) {
                checkCredentialsResult = result;
                view.checkGetAccountsPermission();

            } else if (result.isServerFail() || result.isException()) {
                setServerInfo(null);
                viewModel.showConnectionResultStatus(result.getCode());
                viewModel.showRefreshButton();
                viewModel.disableLoginButton();

            } else { // assume it's wrong credentials
                viewModel.showAuthResultStatus(result.getCode());
                viewModel.enableLoginButton();
            }
        }
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onGetAccountsPermissionGranted(@NonNull final AccountManager accountManager) {
        checkNotNull(accountManager);

        if (checkCredentialsResult == null) {
            viewModel.showAuthResultStatus(RemoteOperationResult.ResultCode.UNKNOWN_ERROR);
            viewModel.enableLoginButton();
            return;
        }
        RemoteOperationResult result = checkCredentialsResult;
        checkCredentialsResult = null;

        GetRemoteUserInfoOperation.UserInfo userInfo = null;
        OwnCloudCredentials credentials = null;

        for (Object object : result.getData()) {
            if (object instanceof GetRemoteUserInfoOperation.UserInfo) {
                userInfo = (GetRemoteUserInfoOperation.UserInfo) object;
            } else if (object instanceof OwnCloudCredentials) {
                credentials = (OwnCloudCredentials) object;
            }
        }
        checkNotNull(credentials);

        Uri uri = Uri.parse(serverInfo.baseUrl);
        String username = credentials.getUsername();
        String accountName = AccountUtils.buildAccountName(uri, username);
        Account account = new Account(accountName, getAccountType());

        Account[] accounts = accountManager.getAccountsByType(getAccountType());
        if (Arrays.asList(accounts).contains(account)) {
            viewModel.showAuthResultStatus(RemoteOperationResult.ResultCode.ACCOUNT_NOT_NEW);
            viewModel.enableLoginButton();
            return;
        }

        final Bundle userData = new Bundle();
        userData.putInt(AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION, ACCOUNT_VERSION);
        userData.putString(AccountUtils.Constants.KEY_OC_VERSION, serverInfo.version.getVersion());
        userData.putString(AccountUtils.Constants.KEY_OC_BASE_URL, serverInfo.baseUrl);
        if (userInfo != null) {
            userData.putString(AccountUtils.Constants.KEY_DISPLAY_NAME, userInfo.mDisplayName);
        }
        // TODO: check why account can't be added implicitly
        accountManager.addAccountExplicitly(account, credentials.getAuthToken(), userData);

        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
        intent.putExtra(AccountManager.KEY_PASSWORD, credentials.getAuthToken());
        intent.putExtra(AccountManager.KEY_USERDATA, userData);

        view.finishActivity(intent);
    }

    @Override
    public void onGetAccountsPermissionDenied() {
        viewModel.showGetAccountsPermissionDeniedWarning();
        viewModel.enableLoginButton();
    }

    @Override
    public boolean isServerUrlValid() {
        return (serverInfo != null && serverInfo.isSet());
    }

    @Override
    public void setServerInfo(GetServerInfoOperation.ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }
}
