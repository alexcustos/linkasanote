package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.addeditaccount.AddEditAccountActivity;
import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;

public final class NextcloudPresenter implements NextcloudContract.Presenter {

    private static final String TAG = NextcloudPresenter.class.getSimpleName();

    private static final String NEXTCLOUD_INDEX = "/index.php";

    @Nullable
    private Account account;

    private GetServerInfoOperation.ServerInfo serverInfo;

    private final NextcloudContract.View view;
    private final NextcloudContract.ViewModel viewModel;
    private final AccountManager accountManager;

    @Inject
    NextcloudPresenter(
            NextcloudContract.View view, NextcloudContract.ViewModel viewModel,
            AccountManager accountManager, @Nullable @NextcloudAccount Account account) {
        this.view = view;
        this.viewModel = viewModel;
        this.accountManager = accountManager;
        this.account = account;
        serverInfo = null;
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
        view.setAccountManager(accountManager);
        viewModel.setPresenter(this);
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void populateAccount() {
        if (account == null) {
            throw new RuntimeException("populateAccount() was called but account is null");
        }
        view.setupAccountState(account);
        viewModel.validateServer();
    }

    @Override
    public void unsubscribe() {
    }

    @Override
    public void checkUrl(@NonNull final String url) {
        viewModel.hideRefreshButton();
        if (view.sendGetServerInfoOperation(url)) {
            viewModel.showTestingConnectionStatus();
        } else {
            viewModel.showCheckUrlWaitingForServiceStatus();
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
        viewModel.disableLoginButton();
        if (view.sendCheckCredentialsOperation(username, password, serverInfo)) {
            viewModel.showTestingAuthStatus();
        } else {
            viewModel.showCheckAuthWaitingForServiceStatus();
        }
    }

    @Override
    public boolean isServerUrlValid() {
        return serverInfo != null && serverInfo.isSet();
    }

    @Nullable
    @Override
    public GetServerInfoOperation.ServerInfo getServerInfo() {
        return serverInfo;
    }

    @Override
    public void setServerInfo(@Nullable GetServerInfoOperation.ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Nullable
    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public Bundle getInstanceState() {
        Bundle state = new Bundle();
        viewModel.saveInstanceState(state);

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

    @Override
    public void onAboutNextcloudClick() {
        view.openAboutNextcloudLink();
    }
}
