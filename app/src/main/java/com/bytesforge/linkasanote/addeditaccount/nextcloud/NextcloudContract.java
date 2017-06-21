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
        void populateAccount(String serverUrlText, String accountUsernameText);

        boolean isServerStatus();
        void replaceServerUrlText(String serverUrlText);
        void showRefreshButton();
        void hideRefreshButton();
        void enableLoginButton();
        void disableLoginButton();
        void enableLayout();
        void disableLayout();
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
        void showNormalizedUrlSnackbar();
        void showSomethingWrongSnackbar();
        void hideAuthStatus();
    }

    interface Presenter extends BasePresenter {

        boolean isNewAccount();
        void populateAccount();

        void enableLayout();
        void disableLayout();

        String normalizeUrl(String url);
        void checkUrl(String url);
        void checkAuth(String username, String password);
        boolean isServerUrlValid();
        void setServerInfo(@Nullable GetServerInfoOperation.ServerInfo serverInfo);
        @Nullable GetServerInfoOperation.ServerInfo getServerInfo();
        @Nullable Account getAccount();
        void onAboutNextcloudClick();
        void validateServerUrlText(final String serverUrlText);
    }
}
