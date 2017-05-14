package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

public interface NextcloudContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull NextcloudContract.ViewModel viewModel);
        void setAccountManager(@NonNull AccountManager accountManager);
        void setAccountAuthenticatorResponse(
                AccountAuthenticatorResponse accountAuthenticatorResponse);
        boolean isActive();

        void addAccount(
                @NonNull RemoteOperationResult result,
                @NonNull GetServerInfoOperation.ServerInfo serverInfo);
        void updateAccount(
                @NonNull RemoteOperationResult result, @Nullable Account account,
                @NonNull GetServerInfoOperation.ServerInfo serverInfo);
        void finishActivity(@NonNull Account account, @NonNull String password, @NonNull Bundle data);
        void cancelActivity();
        void setupAccountState(@NonNull Account account);
        void requestFocusOnAccountPassword();
        boolean sendGetServerInfoOperation(String url);
        boolean sendCheckCredentialsOperation(
                String username, String password,
                @Nullable GetServerInfoOperation.ServerInfo serverInfo);
        void openAboutNextcloudLink();
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(@NonNull Bundle outState);
        void applyInstanceState(@NonNull Bundle state);
        Bundle getDefaultInstanceState();

        void validateServer();

        void showRefreshButton();
        void hideRefreshButton();
        void enableLoginButton();
        void disableLoginButton();
        void checkLoginButton();

        void showEmptyUrlWarning();
        void showMalformedUrlWarning();
        void showTestingConnectionStatus();
        void showTestingAuthStatus();
        void showCheckUrlWaitingForServiceStatus();
        void showCheckAuthWaitingForServiceStatus();
        void showConnectionResultStatus(RemoteOperationResult.ResultCode result);
        void showAuthResultStatus(RemoteOperationResult.ResultCode result);
        void showGetAccountsPermissionDeniedWarning();
        void showSomethingWrongSnackbar();
    }

    interface Presenter extends BasePresenter {

        boolean isNewAccount();
        void populateAccount();

        Bundle getInstanceState();
        void applyInstanceState(@Nullable Bundle state);

        String normalizeUrl(String url);
        void checkUrl(String url);
        void checkAuth(String username, String password);
        boolean isServerUrlValid();
        void setServerInfo(@Nullable GetServerInfoOperation.ServerInfo serverInfo);
        @Nullable GetServerInfoOperation.ServerInfo getServerInfo();
        @Nullable Account getAccount();
        void onAboutNextcloudClick();
    }
}
