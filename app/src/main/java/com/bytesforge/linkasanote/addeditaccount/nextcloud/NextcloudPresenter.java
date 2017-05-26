package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Patterns;

import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.google.common.base.Strings;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;

public final class NextcloudPresenter implements NextcloudContract.Presenter {

    private static final String TAG = NextcloudPresenter.class.getSimpleName();

    private static final String NEXTCLOUD_INDEX = "/index.php";

    @Nullable
    private Account account;

    @Nullable
    private AccountAuthenticatorResponse accountAuthenticatorResponse;

    private GetServerInfoOperation.ServerInfo serverInfo;

    private final NextcloudContract.View view;
    private final NextcloudContract.ViewModel viewModel;
    private final AccountManager accountManager;

    @Inject
    NextcloudPresenter(
            NextcloudContract.View view, NextcloudContract.ViewModel viewModel,
            AccountManager accountManager, @Nullable @NextcloudAccount Account account,
            @Nullable AccountAuthenticatorResponse accountAuthenticatorResponse) {
        this.view = view;
        this.viewModel = viewModel;
        this.accountManager = accountManager;
        this.account = account;
        this.accountAuthenticatorResponse = accountAuthenticatorResponse;
        serverInfo = null;
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
        view.setAccountManager(accountManager);
        view.setAccountAuthenticatorResponse(accountAuthenticatorResponse);
        viewModel.setPresenter(this);
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void unsubscribe() {
    }

    @Override
    public void populateAccount() {
        if (account == null) {
            throw new RuntimeException("populateAccount() was called but account is null");
        }
        view.setupAccountState(account);
    }

    @Override
    public void checkUrl(@NonNull final String url) {
        if (view.sendGetServerInfoOperation(url)) {
            viewModel.hideRefreshButton();
            viewModel.showTestingConnectionStatus();
        } else {
            viewModel.showRefreshButton();
            viewModel.showCheckUrlWaitingForServiceStatus();
        }
    }

    @Override
    public String normalizeUrl(final String serverUrl) {
        String normalizedUrl = "";
        // Empty
        if (Strings.isNullOrEmpty(serverUrl)) {
            viewModel.showEmptyUrlWarning();
            return null;
        }
        // Protocol
        normalizedUrl = serverUrl.trim().toLowerCase().replaceAll("\\s+", "");
        // Parsing
        if (!Patterns.WEB_URL.matcher(normalizedUrl).matches()) {
            viewModel.showMalformedUrlWarning();
            return null;
        }
        normalizedUrl = CommonUtils.normalizeUrlProtocol(normalizedUrl);
        URL url;
        try {
            url = new URL(normalizedUrl);
        } catch (MalformedURLException e) {
            viewModel.showMalformedUrlWarning();
            return null;
        }
        // Rebuild URL
        String path = url.getPath();
        if (path.contains(NEXTCLOUD_INDEX)) {
            path = path.substring(0, path.indexOf(NEXTCLOUD_INDEX));
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        Uri.Builder uriBuilder = new Uri.Builder()
                .scheme(url.getProtocol())
                .encodedAuthority(url.getAuthority())
                .encodedPath(path);
        return uriBuilder.build().toString();
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
    public void enableLayout() {
        viewModel.enableLayout();
    }

    @Override
    public void disableLayout() {
        viewModel.disableLayout();
    }

    @Override
    public boolean isNewAccount() {
        return account == null;
    }

    @Override
    public void onAboutNextcloudClick() {
        view.openAboutNextcloudLink();
    }

    @Override
    public void validateServerUrlText(final String serverUrlText) {
        String normalizedUrl = normalizeUrl(serverUrlText);
        if (normalizedUrl == null) return; // NOTE: a warning has already been shown

        if (!normalizedUrl.equals(serverUrlText)) {
            viewModel.replaceServerUrlText(normalizedUrl);
            viewModel.showNormalizedUrlSnackbar();
        }
        if (!normalizedUrl.isEmpty()
                && (serverInfo == null || !serverInfo.isSet())) {
            checkUrl(normalizedUrl);
        }
    }
}
