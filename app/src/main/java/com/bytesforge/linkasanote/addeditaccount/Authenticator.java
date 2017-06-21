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
