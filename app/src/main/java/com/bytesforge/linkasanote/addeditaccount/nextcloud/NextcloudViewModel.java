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

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.google.common.base.Strings;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import static com.google.common.base.Preconditions.checkNotNull;

public class NextcloudViewModel extends BaseObservable implements NextcloudContract.ViewModel {

    private static final String TAG = NextcloudViewModel.class.getSimpleName();

    private static final String STATE_LAYOUT_ENABLED = "LAYOUT_ENABLED";
    private static final String STATE_SERVER_URL = "SERVER_URL";
    private static final String STATE_SERVER_URL_TEXT = "SERVER_URL_TEXT";
    private static final String STATE_ACCOUNT_USERNAME = "ACCOUNT_USERNAME";
    private static final String STATE_ACCOUNT_USERNAME_TEXT = "ACCOUNT_USERNAME_TEXT";
    private static final String STATE_ACCOUNT_PASSWORD_TEXT = "ACCOUNT_PASSWORD_TEXT";
    private static final String STATE_REFRESH_BUTTON = "REFRESH_BUTTON";
    private static final String STATE_LOGIN_BUTTON = "LOGIN_BUTTON";
    private static final String STATE_SERVER_STATUS_ICON = "SERVER_STATUS_ICON";
    private static final String STATE_AUTH_STATUS_ICON = "AUTH_STATUS_ICON";
    private static final String STATE_SERVER_STATUS_TEXT = "SERVER_STATUS_TEXT";
    private static final String STATE_AUTH_STATUS_TEXT = "AUTH_STATUS_TEXT";

    public final ObservableBoolean layoutEnabled = new ObservableBoolean();
    public final ObservableField<String> serverUrlText = new ObservableField<>();
    public final ObservableField<String> accountUsernameText = new ObservableField<>();
    public final ObservableField<String> accountPasswordText = new ObservableField<>();
    public final ObservableBoolean serverUrl = new ObservableBoolean();
    public final ObservableBoolean accountUsername = new ObservableBoolean();
    public final ObservableBoolean refreshButton = new ObservableBoolean();
    public final ObservableBoolean loginButton = new ObservableBoolean();

    private final Context context;
    private NextcloudContract.Presenter presenter;
    private boolean instantiated;

    public enum SnackbarId {
        NORMALIZED_URL, SOMETHING_WRONG}

    @Bindable
    public int serverStatusIcon = 0;

    @Bindable
    public int authStatusIcon = 0;

    @Bindable
    public SnackbarId snackbarId;

    private int serverStatusText = 0;
    private int authStatusText = 0;

    public NextcloudViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
        instantiated = false;
    }

    @Override
    public void setInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            applyInstanceState(getDefaultInstanceState());
        } else {
            applyInstanceState(savedInstanceState);
            presenter.validateServerUrlText(serverUrlText.get());
        }
    }

    @Override
    public void saveInstanceState(@NonNull Bundle outState) {
        checkNotNull(outState);
        instantiated = false;
        outState.putBoolean(STATE_LAYOUT_ENABLED, layoutEnabled.get());
        outState.putBoolean(STATE_SERVER_URL, serverUrl.get());
        outState.putString(STATE_SERVER_URL_TEXT, serverUrlText.get());
        outState.putBoolean(STATE_ACCOUNT_USERNAME, accountUsername.get());
        outState.putString(STATE_ACCOUNT_USERNAME_TEXT, accountUsernameText.get());
        outState.putString(STATE_ACCOUNT_PASSWORD_TEXT, accountPasswordText.get());
        outState.putBoolean(STATE_REFRESH_BUTTON, refreshButton.get());
        outState.putBoolean(STATE_LOGIN_BUTTON, loginButton.get());
        outState.putInt(STATE_SERVER_STATUS_ICON, serverStatusIcon);
        outState.putInt(STATE_AUTH_STATUS_ICON, authStatusIcon);
        outState.putInt(STATE_SERVER_STATUS_TEXT, serverStatusText);
        outState.putInt(STATE_AUTH_STATUS_TEXT, authStatusText);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        layoutEnabled.set(state.getBoolean(STATE_LAYOUT_ENABLED));
        serverUrl.set(state.getBoolean(STATE_SERVER_URL));
        serverUrlText.set(state.getString(STATE_SERVER_URL_TEXT));
        accountUsername.set(state.getBoolean(STATE_ACCOUNT_USERNAME));
        accountUsernameText.set(state.getString(STATE_ACCOUNT_USERNAME_TEXT));
        accountPasswordText.set(state.getString(STATE_ACCOUNT_PASSWORD_TEXT));
        refreshButton.set(state.getBoolean(STATE_REFRESH_BUTTON));
        loginButton.set(state.getBoolean(STATE_LOGIN_BUTTON));
        serverStatusIcon = state.getInt(STATE_SERVER_STATUS_ICON);
        authStatusIcon = state.getInt(STATE_AUTH_STATUS_ICON);
        serverStatusText = state.getInt(STATE_SERVER_STATUS_TEXT);
        authStatusText = state.getInt(STATE_AUTH_STATUS_TEXT);
        notifyChange();
        instantiated = true;
    }

    @Override
    public Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();
        defaultState.putBoolean(STATE_LAYOUT_ENABLED, true);
        defaultState.putBoolean(STATE_SERVER_URL, true);
        defaultState.putString(STATE_SERVER_URL_TEXT, "");
        defaultState.putBoolean(STATE_ACCOUNT_USERNAME, true);
        defaultState.putString(STATE_ACCOUNT_USERNAME_TEXT, "");
        defaultState.putString(STATE_ACCOUNT_PASSWORD_TEXT, "");
        defaultState.putBoolean(STATE_REFRESH_BUTTON, false);
        defaultState.putBoolean(STATE_LOGIN_BUTTON, false);
        defaultState.putInt(STATE_SERVER_STATUS_ICON, 0);
        defaultState.putInt(STATE_AUTH_STATUS_ICON, 0);
        defaultState.putInt(STATE_SERVER_STATUS_TEXT, 0);
        defaultState.putInt(STATE_AUTH_STATUS_TEXT, 0);
        return defaultState;
    }

    @Override
    public void setPresenter(@NonNull NextcloudContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void populateAccount(String serverUrlText, String accountUsernameText) {
        serverUrl.set(false);
        this.serverUrlText.set(serverUrlText);
        accountUsername.set(false);
        this.accountUsernameText.set(accountUsernameText);
        // NOTE: do not set password here, non-authorized user can view it
        accountPasswordText.set(null);
    }

    // Adapters

    @BindingAdapter({"layoutEnabled"})
    public static void updateLayoutEnabled(ScrollView view, boolean enabled) {
        setEnabledViewGroupViews(view, enabled);
    }

    private static void setEnabledViewGroupViews(ViewGroup viewGroup, boolean enabled) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            int childId = child.getId();
            if (child instanceof TextView
                    && childId != R.id.server_url
                    && childId != R.id.account_username
                    && childId != R.id.login_button) {
                child.setEnabled(enabled);
            }
            if (child instanceof ViewGroup) {
                setEnabledViewGroupViews((ViewGroup) child, enabled);
            }
        }
    }

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
                        Snackbar.LENGTH_SHORT).show();
                break;
            case SOMETHING_WRONG:
                Snackbar.make(view,
                        R.string.snackbar_something_wrong,
                        Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    // Server

    public void onRefreshButtonClick() {
        presenter.checkUrl(serverUrlText.get());
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

    private void hideServerStatus() {
        setServerStatus(0, 0);
    }

    @Override
    @Bindable
    public boolean isServerStatus() {
        return serverStatusIcon != 0 || serverStatusText != 0;
    }

    @Bindable
    public String getServerStatusText() {
        return (serverStatusText == 0) ? "" : context.getResources().getString(serverStatusText);
    }

    public void afterServerUrlChanged(EditText view) {
        if (view.hasFocus()) {
            if (isServerStatus()) hideServerStatus();
            if (isAuthStatus()) hideAuthStatus();
            disableLoginButton();
            presenter.setServerInfo(null);
        }
    }

    public void onServerUrlFocusChange(View view, boolean hasFocus) {
        if (hasFocus || !instantiated) return;

        presenter.validateServerUrlText(serverUrlText.get());
    }

    @Override
    public void replaceServerUrlText(String serverUrlText) {
        this.serverUrlText.set(serverUrlText);
    }

    @Override
    public void showNormalizedUrlSnackbar() {
        snackbarId = SnackbarId.NORMALIZED_URL;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showSomethingWrongSnackbar() {
        snackbarId = SnackbarId.SOMETHING_WRONG;
        notifyPropertyChanged(BR.snackbarId);
    }

    // Auth

    public void afterAccountCredentialsChanged() {
        if (serverStatusText == 0) {
            presenter.validateServerUrlText(serverUrlText.get());
        }
        hideAuthStatus();
        checkLoginButton();
    }

    public void onLoginButtonClick() {
        disableLoginButton();
        presenter.checkAuth(accountUsernameText.get(), accountPasswordText.get());
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
            isChanged = true;
        }
        if (isChanged) {
            notifyPropertyChanged(BR.authStatus);
        }
    }

    @Override
    public void hideAuthStatus() {
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
    public void enableLayout() {
        layoutEnabled.set(true);
        serverUrl.set(true);
        accountUsername.set(true);
        checkLoginButton();
    }

    @Override
    public void disableLayout() {
        layoutEnabled.set(false);
        serverUrl.set(false);
        accountUsername.set(false);
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
    public void showCheckUrlWaitingForServiceStatus() {
        setServerStatus(R.drawable.ic_sync,
                R.string.add_edit_account_nextcloud_warning_waiting_for_service);
    }

    @Override
    public void showCheckAuthWaitingForServiceStatus() {
        setAuthStatus(R.drawable.ic_sync,
                R.string.add_edit_account_nextcloud_warning_waiting_for_service);
    }

    @Override
    public void showGetAccountsPermissionDeniedWarning() {
        setAuthStatus(R.drawable.ic_warning,
                R.string.add_edit_account_nextcloud_warning_no_permission);
    }

    @Override
    public void showConnectionResultStatus(RemoteOperationResult.ResultCode resultCode) {
        int icon = R.drawable.ic_warning;
        int text;
        switch (resultCode) {
            case OK_SSL:
                icon = R.drawable.ic_lock;
                text = R.string.add_edit_account_nextcloud_connection_secure;
                break;
            case OK:
            case OK_NO_SSL:
                if (serverUrlText.get().startsWith(CommonUtils.HTTP_PROTOCOL)) {
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
                text = R.string.add_edit_account_nextcloud_warning_malformed_url;
                break;
            default:
                RemoteOperationResult result = new RemoteOperationResult(resultCode);
                Log.d(TAG, "showAuthResultStatus(): Unknown error [" + resultCode.name() +  "; message=" + result.getLogMessage() + "]");
                text = R.string.add_edit_account_nextcloud_unknown_error;
                break;
        }
        setServerStatus(icon, text);
    }

    @Override
    public void showAuthResultStatus(RemoteOperationResult.ResultCode resultCode) {
        int icon = R.drawable.ic_warning;
        int text;
        switch (resultCode) {
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
            case TIMEOUT:
                text = R.string.add_edit_account_nextcloud_timeout;
                break;
            default:
                RemoteOperationResult result = new RemoteOperationResult(resultCode);
                Log.d(TAG, "showAuthResultStatus(): Unknown error [" + resultCode.name() +  "; message=" + result.getLogMessage() + "]");
                text = R.string.add_edit_account_nextcloud_unknown_error;
                break;
        }
        setAuthStatus(icon, text);
    }

    @Override
    public void checkLoginButton() {
        String username = accountUsernameText.get();
        String password = accountPasswordText.get();
        // NOTE: after orientation is changed the callback can't update this status for unknown reason,
        //       so let the user to retry manually
        hideAuthStatus();
        if (presenter.isServerUrlValid()
                && !Strings.isNullOrEmpty(username)
                && !Strings.isNullOrEmpty(password)) {
            enableLoginButton();
        } else {
            disableLoginButton();
        }
    }

    public void onAboutNextcloudClick() {
        presenter.onAboutNextcloudClick();
    }
}
