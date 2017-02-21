package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.addeditaccount.AddEditAccountActivity;
import com.bytesforge.linkasanote.sync.operations.OperationsService;
import com.bytesforge.linkasanote.sync.operations.nextcloud.CheckCredentialsOperation;
import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.bytesforge.linkasanote.utils.CommonUtils.convertIdn;
import static com.google.common.base.Preconditions.checkNotNull;

public final class NextcloudPresenter
        implements NextcloudContract.Presenter, OnRemoteOperationListener {

    private static final String TAG = NextcloudPresenter.class.getSimpleName();

    private static final String NEXTCLOUD_INDEX = "/index.php";

    private final Handler handler = new Handler();
    private static final Object operationsLock = new Object();
    private OperationsService operationsService = null;
    private List<Intent> operationsQueue = new ArrayList<>();

    @Nullable
    private Account account;

    private final NextcloudContract.View view;
    private NextcloudContract.ViewModel viewModel;
    private GetServerInfoOperation.ServerInfo serverInfo;

    @Inject
    NextcloudPresenter(
            NextcloudContract.View view, @Nullable @NextcloudAccount Account account) {
        this.view = view;
        this.account = account;
        serverInfo = null;
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
    }

    @Override
    public void setOperationsService(OperationsService service) {
        // TODO: presenter should know nothing about android stuff, move it to the View
        synchronized (operationsLock) {
            operationsService = service;
            for (Intent intent : operationsQueue) {
                operationsService.queueOperation(intent, this, handler);
            }
            operationsQueue.clear();
        }
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void populateAccount() {
        if (account == null) {
            throw new RuntimeException("populateAccount() was called but account is null.");
        }
        Bundle state = view.getAccountState(account);
        viewModel.applyInstanceState(state);
        viewModel.validateServer();
        view.requestFocusOnAccountPassword();
    }

    @Override
    public void unsubscribe() {
    }

    @Override
    public void setViewModel(@NonNull NextcloudContract.ViewModel viewModel) {
        this.viewModel = checkNotNull(viewModel);
    }

    @Override
    public void checkUrl(@NonNull final String url) {
        viewModel.showTestingConnectionStatus();
        viewModel.hideRefreshButton();

        Intent getServerInfoIntent = new Intent();
        getServerInfoIntent.setAction(OperationsService.ACTION_GET_SERVER_INFO);
        getServerInfoIntent.putExtra(OperationsService.EXTRA_SERVER_URL, convertIdn(url, true));
        synchronized (operationsLock) {
            if (operationsService == null) {
                operationsQueue.add(getServerInfoIntent);
                viewModel.showCheckUrlWaitingForServiceStatus();
            } else {
                operationsService.queueOperation(getServerInfoIntent, this, handler);
            }
        } // synchronized
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
        if (!normalizedUrl.startsWith(AddEditAccountActivity.HTTP_PROTOCOL) &&
                !normalizedUrl.startsWith(AddEditAccountActivity.HTTPS_PROTOCOL)) {
            normalizedUrl = AddEditAccountActivity.DEFAULT_PROTOCOL + normalizedUrl;
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
        checkCredentialsIntent.putExtra(
                OperationsService.EXTRA_SERVER_VERSION, serverInfo.version.getVersion());
        checkCredentialsIntent.putExtra(OperationsService.EXTRA_CREDENTIALS, credentials);

        synchronized (operationsLock) {
            if (operationsService == null) {
                operationsQueue.add(checkCredentialsIntent);
                viewModel.showCheckAuthWaitingForServiceStatus();
            } else {
                operationsService.queueOperation(checkCredentialsIntent, this, handler);
            }
        } // synchronized
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
                if (isNewAccount()) {
                    view.addAccount(result, serverInfo);
                } else {
                    view.updateAccount(result, account, serverInfo);
                }
            } else if (result.isServerFail()) {
                setServerInfo(null);
                viewModel.showConnectionResultStatus(result.getCode());
                viewModel.showRefreshButton();
                viewModel.disableLoginButton();
            } else { // NOTE: it's wrong credentials or result.isException()
                viewModel.showAuthResultStatus(result.getCode());
                viewModel.enableLoginButton();
            }
        }
    }

    @Override
    public boolean isServerUrlValid() {
        return serverInfo != null && serverInfo.isSet();
    }

    @Override
    public void setServerInfo(GetServerInfoOperation.ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Override
    public Bundle getInstanceState() {
        Bundle state = new Bundle();
        viewModel.loadInstanceState(state);

        return state;
    }

    @Override
    public void applyInstanceState(Bundle state) {
        if (state == null) return;
        viewModel.applyInstanceState(state);
    }

    @Override
    public boolean isNewAccount() {
        return account == null;
    }
}
