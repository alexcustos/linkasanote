package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.accounts.AccountManager;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.sync.operations.OperationsService;
import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

public interface NextcloudContract {

    interface View extends BaseView<Presenter> {

        void checkGetAccountsPermission();
        void finishActivity(@NonNull Intent result);
        boolean isActive();
    }

    interface ViewModel extends BaseView<Presenter> {

        void showRefreshButton();
        void hideRefreshButton();
        void enableLoginButton();
        void disableLoginButton();
        void checkLoginButton();

        void showEmptyUrlWarning();
        void showMalformedUrlWarning();
        void showTestingConnectionStatus();
        void showTestingAuthStatus();
        void showCheckUrlServiceNotReadyWarning();
        void showCheckAuthServiceNotReadyWarning();
        void showConnectionResultStatus(RemoteOperationResult.ResultCode result);
        void showAuthResultStatus(RemoteOperationResult.ResultCode result);
        void showGetAccountsPermissionDeniedWarning();
    }

    interface Presenter extends BasePresenter {

        NextcloudContract.ViewModel getViewModel();
        void setOperationsService(OperationsService service);

        String normalizeUrl(String url);
        void checkUrl(String url);
        void checkAuth(String username, String password);
        boolean isServerUrlValid();
        void setServerInfo(GetServerInfoOperation.ServerInfo serverInfo);

        void onGetAccountsPermissionGranted(@NonNull final AccountManager accountManager);
        void onGetAccountsPermissionDenied();
    }
}
