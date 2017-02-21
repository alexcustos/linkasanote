package com.bytesforge.linkasanote.addeditaccount;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudFragment;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.owncloud.android.lib.common.accounts.AccountTypeUtils;

public class Authenticator extends AbstractAccountAuthenticator {

    private Context context;

    public Authenticator(Context context) {
        super(context);

        this.context = context;
    }

    @Override
    public Bundle editProperties(
            AccountAuthenticatorResponse accountAuthenticatorResponse, String s) {
        return null;
    }

    @Override
    public Bundle addAccount(
            AccountAuthenticatorResponse response,
            String accountType, String authTokenType,
            String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        try {
            validateAccountType(accountType);
        } catch (UnsupportedAccountTypeException e) {
            return e.getBundle();
        }
        final Bundle bundle = new Bundle();

        final Intent intent = new Intent(context, AddEditAccountActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        setIntentFlags(intent);
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    private void setIntentFlags(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_FROM_BACKGROUND);
    }

    @Override
    public Bundle confirmCredentials(
            AccountAuthenticatorResponse response,
            Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(
            AccountAuthenticatorResponse response,
            Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        try {
            validateAccountType(account.type);
            validateAuthTokenType(authTokenType);
        } catch (AuthenticatorException e) {
            return e.getBundle();
        }
        final AccountManager accountManager = AccountManager.get(context);
        final Bundle result = new Bundle();
        String authToken = accountManager.getPassword(account);

        if (authToken != null) {
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }
        // TODO: find where this intent can be useful
        Intent updateAccountIntent = new Intent(context, AddEditAccountActivity.class);
        updateAccountIntent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        updateAccountIntent.putExtra(NextcloudFragment.ARGUMENT_EDIT_ACCOUNT_ACCOUNT, account);
        updateAccountIntent.putExtra(AddEditAccountActivity.ARGUMENT_REQUEST_CODE,
                AddEditAccountActivity.REQUEST_UPDATE_NEXTCLOUD_ACCOUNT);
        result.putParcelable(AccountManager.KEY_INTENT, updateAccountIntent);

        return result;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(
            AccountAuthenticatorResponse response,
            Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(
            AccountAuthenticatorResponse response,
            Account account, String[] features) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account)
            throws NetworkErrorException {
        // TODO: check response to update account list
        return super.getAccountRemovalAllowed(response, account);
    }

    // Validators

    private void validateAccountType(String type) throws UnsupportedAccountTypeException {
        if (!type.equals(CloudUtils.getAccountType(context))) {
            throw new UnsupportedAccountTypeException();
        }
    }

    private void validateAuthTokenType(String authTokenType) throws
            UnsupportedAuthTokenTypeException {
        String accountType = CloudUtils.getAccountType(context);
        if (!authTokenType.equals(AccountTypeUtils.getAuthTokenTypePass(accountType))) {
            throw new UnsupportedAuthTokenTypeException();
        }
    }

    // Exceptions

    private static class AuthenticatorException extends Exception {

        private static final long serialVersionUID = 0L;
        private Bundle bundle;

        public AuthenticatorException(int code, String msg) {
            bundle = new Bundle();
            bundle.putInt(AccountManager.KEY_ERROR_CODE, code);
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, msg);
        }

        public Bundle getBundle() {
            return bundle;
        }
    }

    private static class UnsupportedAccountTypeException extends AuthenticatorException {

        private static final long serialVersionUID = 0L;

        public UnsupportedAccountTypeException() {
            super(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION, "Unsupported account type");
        }
    }

    private static class UnsupportedAuthTokenTypeException extends AuthenticatorException {

        private static final long serialVersionUID = 0L;

        public UnsupportedAuthTokenTypeException() {
            super(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION, "Unsupported auth token type");
        }
    }
}
