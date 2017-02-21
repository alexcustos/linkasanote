package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.FragmentAddEditAccountNextcloudBinding;
import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import java.util.Arrays;

import static com.bytesforge.linkasanote.utils.CloudUtils.getAccountType;
import static com.bytesforge.linkasanote.utils.CloudUtils.getAccountUsername;
import static com.google.common.base.Preconditions.checkNotNull;

public class NextcloudFragment extends Fragment implements NextcloudContract.View {

    private static final String TAG = NextcloudFragment.class.getSimpleName();
    private static final int ACCOUNT_VERSION = 1;
    public static final String ARGUMENT_EDIT_ACCOUNT_ACCOUNT = "EDIT_ACCOUNT_ACCOUNT";

    private AccountAuthenticatorResponse accountAuthenticatorResponse = null;
    private NextcloudContract.Presenter presenter;
    private NextcloudContract.ViewModel viewModel;
    private FragmentAddEditAccountNextcloudBinding binding;
    private AccountManager accountManager;

    public static NextcloudFragment newInstance() {
        return new NextcloudFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.unsubscribe();
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void setPresenter(@NonNull NextcloudContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_add_edit_account_nextcloud, container, false);
        accountManager = AccountManager.get(getContext());
        viewModel = new NextcloudViewModel(getContext(), savedInstanceState);
        viewModel.setPresenter(presenter);
        presenter.setViewModel(viewModel);
        binding.setViewModel((NextcloudViewModel) viewModel);
        if (savedInstanceState == null && !presenter.isNewAccount()) {
            // NOTE: here because must be intact on orientation change
            presenter.populateAccount();
        }

        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.loadInstanceState(outState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        accountAuthenticatorResponse = getActivity().getIntent()
                .getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (accountAuthenticatorResponse != null) {
            accountAuthenticatorResponse.onRequestContinued();
        }
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

        // TODO: replace with CloudUtils.isAccountExists if permission warning is technically impossible
        Account[] accounts = CloudUtils.getAccountsWithPermissionCheck(getContext(), accountManager);
        if (accounts == null) {
            viewModel.showGetAccountsPermissionDeniedWarning();
            return;
        } else if (Arrays.asList(accounts).contains(newAccount)) {
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
    public Bundle getAccountState(@NonNull Account account) {
        checkNotNull(account);

        Bundle state = new Bundle();
        state.putBoolean(NextcloudViewModel.STATE_SERVER_URL, false);
        state.putString(NextcloudViewModel.STATE_SERVER_URL_TEXT,
                accountManager.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL));
        state.putBoolean(NextcloudViewModel.STATE_ACCOUNT_USERNAME, false);
        state.putString(NextcloudViewModel.STATE_ACCOUNT_USERNAME_TEXT,
                getAccountUsername(account.name));
        // NOTE: security hole, non-authorized user can view the password
        /*state.putString(NextcloudViewModel.STATE_ACCOUNT_PASSWORD_TEXT,
                accountManager.getPassword(account));*/

        return state;
    }

    @Override
    public void requestFocusOnAccountPassword() {
        binding.accountPassword.requestFocus();
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

    @VisibleForTesting
    public void setAccountManager(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    @VisibleForTesting
    NextcloudContract.Presenter getPresenter() {
        return presenter;
    }
}
