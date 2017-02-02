package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import static com.google.common.base.Preconditions.checkNotNull;

public class NextcloudViewModel extends BaseObservable implements NextcloudContract.ViewModel {

    public final ObservableField<String> serverUrl = new ObservableField<>();
    public final ObservableField<String> accountUsername = new ObservableField<>();
    public final ObservableField<String> accountPassword = new ObservableField<>();

    public final ObservableBoolean refreshButton = new ObservableBoolean(false);
    public final ObservableBoolean loginButton = new ObservableBoolean(false);

    private Context context;
    private NextcloudContract.Presenter presenter;

    public enum SnackbarId {NORMALIZED_URL};

    @Bindable
    public int serverStatusIcon = 0;

    @Bindable
    public int authStatusIcon = 0;

    @Bindable
    public SnackbarId snackbarId;

    private int serverStatusText = 0;
    private int authStatusText = 0;

    private boolean isServerUrlHasFocus = false;

    public NextcloudViewModel(Context context) {
        this.context = context;
    }

    @Override
    public void setPresenter(@NonNull NextcloudContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    // Adapters

    @BindingAdapter({"android:drawableStart", "android:text"})
    public static void showStatus(TextView view, int icon, @NonNull String text) {
        view.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
        view.setText(text);
    }

    @BindingAdapter({"snackbarId"})
    public static void showSnackbar(LinearLayout view, SnackbarId snackbarId) {
        if (snackbarId == null) return;

        switch (snackbarId) {
            case NORMALIZED_URL:
                Snackbar.make(view,
                        R.string.add_edit_account_nextcloud_info_normalized_url,
                        Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    // Server

    public void onRefreshButtonClick() {
        presenter.checkUrl(serverUrl.get());
    }

    private void setServerStatus(int iconId, int textId) {
        boolean isChanged = false;

        if (serverStatusIcon != iconId) {
            serverStatusIcon = iconId;
            notifyPropertyChanged(BR.serverStatusIcon);
            isChanged = true;
        }
        if (serverStatusText != textId) {
            serverStatusText = textId;
            notifyPropertyChanged(BR.serverStatusText);
            isChanged = true;
        }
        if (isChanged) {
            notifyPropertyChanged(BR.serverStatus);
        }
    }

    private void clearServerStatus() {
        setServerStatus(0, 0);
    }

    @Bindable
    public boolean isServerStatus() {
        return serverStatusIcon != 0 || serverStatusText != 0;
    }

    @Bindable
    public String getServerStatusText() {
        return (serverStatusText == 0) ? "" : context.getResources().getString(serverStatusText);
    }

    public void afterServerUrlChanged(Editable s) {
        if (isServerUrlHasFocus) {
            if (isServerStatus()) clearServerStatus();
            if (isAuthStatus()) clearAuthStatus();
            disableLoginButton();
            presenter.setServerInfo(null);
        }
    }

    public void onServerUrlFocusChange(View view, boolean hasFocus) {
        isServerUrlHasFocus = hasFocus;
        if (hasFocus) return;

        String url = serverUrl.get();
        String normalizedUrl = presenter.normalizeUrl(url);

        if (!normalizedUrl.isEmpty() && !normalizedUrl.equals(url)) {
            serverUrl.set(normalizedUrl);
            showNormalizedUrlSnackbar();
        }

        if (!isServerStatus()) {
            presenter.checkUrl(normalizedUrl);
        }
    }

    private void showNormalizedUrlSnackbar() {
        snackbarId = SnackbarId.NORMALIZED_URL;
        notifyPropertyChanged(BR.snackbarId);
    }

    // Auth

    public void afterAccountCredentialsChanged(Editable s) {
        clearAuthStatus();
        checkLoginButton();
    }

    public void onLoginButtonClick() {
        disableLoginButton();
        presenter.checkAuth(accountUsername.get(), accountPassword.get());
    }

    private void setAuthStatus(int iconId, int textId) {
        boolean isChanged = false;

        if (authStatusIcon != iconId) {
            authStatusIcon = iconId;
            notifyPropertyChanged(BR.authStatusIcon);
            isChanged = true;
        }
        if (authStatusText != textId) {
            authStatusText = textId;
            notifyPropertyChanged(BR.authStatusText);
        }
        if (isChanged) {
            notifyPropertyChanged(BR.authStatus);
        }
    }

    private void clearAuthStatus() {
        setAuthStatus(0, 0);
    }

    @Bindable
    public boolean isAuthStatus() {
        return authStatusIcon != 0 || authStatusText != 0;
    }

    @Bindable
    public String getAuthStatusText() {
        return (authStatusText == 0) ? "" : context.getResources().getString(authStatusText);
    }

    // Interface

    @Override
    public void showRefreshButton() {
        refreshButton.set(true);
    }

    @Override
    public void hideRefreshButton() {
        refreshButton.set(false);
    }

    @Override
    public void enableLoginButton() {
        loginButton.set(true);
    }

    @Override
    public void disableLoginButton() {
        loginButton.set(false);
    }

    @Override
    public void showEmptyUrlWarning() {
        setServerStatus(R.drawable.ic_warning,
                R.string.add_edit_account_nextcloud_warning_empty_url);
    }

    @Override
    public void showMalformedUrlWarning() {
        setServerStatus(R.drawable.ic_warning,
                R.string.add_edit_account_nextcloud_warning_malformed_url);
    }

    @Override
    public void showTestingConnectionStatus() {
        setServerStatus(R.drawable.ic_sync,
                R.string.add_edit_account_nextcloud_server_status_testing);
    }

    @Override
    public void showTestingAuthStatus() {
        setAuthStatus(R.drawable.ic_sync,
                R.string.add_edit_account_nextcloud_auth_status_checking);
    }

    @Override
    public void showCheckUrlServiceNotReadyWarning() {
        setServerStatus(R.drawable.ic_warning,
                R.string.add_edit_account_nextcloud_warning_service_not_ready);
    }

    @Override
    public void showCheckAuthServiceNotReadyWarning() {
        setAuthStatus(R.drawable.ic_warning,
                R.string.add_edit_account_nextcloud_warning_service_not_ready);
    }

    @Override
    public void showGetAccountsPermissionDeniedWarning() {
        setAuthStatus(R.drawable.ic_warning,
                R.string.add_edit_account_nextcloud_warning_no_permission);
    }

    @Override
    public void showConnectionResultStatus(RemoteOperationResult.ResultCode result) {
        int icon = R.drawable.ic_warning;
        int text;

        switch (result) {
            case OK_SSL:
                icon = R.drawable.ic_lock;
                text = R.string.add_edit_account_nextcloud_connection_secure;
                break;
            case OK:
            case OK_NO_SSL:
                if (serverUrl.get().startsWith(NextcloudPresenter.HTTP_PROTOCOL)) {
                    icon = R.drawable.ic_check;
                    text = R.string.add_edit_account_nextcloud_connection_established;
                } else {
                    icon = R.drawable.ic_lock_open;
                    text = R.string.add_edit_account_nextcloud_connection_unsecure;
                }
                break;
            case NO_NETWORK_CONNECTION:
                icon = R.drawable.ic_signal_wifi_off;
                text = R.string.add_edit_account_nextcloud_connection_no_network;
                break;
            case WRONG_CONNECTION:
            case TIMEOUT:
            case HOST_NOT_AVAILABLE:
                text = R.string.add_edit_account_nextcloud_connection_problem;
                break;
            case BAD_OC_VERSION:
            case INSTANCE_NOT_CONFIGURED:
            case FILE_NOT_FOUND:
                text = R.string.add_edit_account_nextcloud_server_unrecognized;
                break;
            case SSL_ERROR:
            case SSL_RECOVERABLE_PEER_UNVERIFIED:
            case OK_REDIRECT_TO_NON_SECURE_CONNECTION:
                text = R.string.add_edit_account_nextcloud_ssl_problem;
                break;
            case INCORRECT_ADDRESS:
            case UNKNOWN_ERROR:
            default:
                text = R.string.add_edit_account_nextcloud_unknown_error;
                break;
        } // switch

        setServerStatus(icon, text);
    }

    @Override
    public void showAuthResultStatus(RemoteOperationResult.ResultCode result) {
        int icon = R.drawable.ic_warning;
        int text;

        switch (result) {
            case OK:
                icon = R.drawable.ic_check;
                text = R.string.add_edit_account_nextcloud_successful;
                break;
            case UNAUTHORIZED:
                text = R.string.add_edit_account_nextcloud_auth_status_wrong;
                break;
            case ACCOUNT_NOT_NEW:
                text = R.string.add_edit_account_nextcloud_auth_exists;
                break;
            case ACCOUNT_NOT_THE_SAME:
                text = R.string.add_edit_account_nextcloud_auth_not_match;
                break;
            case UNHANDLED_HTTP_CODE:
            case UNKNOWN_ERROR:
            default:
                text = R.string.add_edit_account_nextcloud_unknown_error;
                break;
        } // switch

        setAuthStatus(icon, text);
    }

    @Override
    public void checkLoginButton() {
        String username = accountUsername.get();
        String password = accountPassword.get();

        if (presenter.isServerUrlValid() &&
                username != null && !username.isEmpty() &&
                password != null && !password.isEmpty()) {
            enableLoginButton();
        } else {
            disableLoginButton();
        }
    }
}
