package com.bytesforge.linkasanote.utils;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.manageaccounts.AccountItem;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;

import java.io.IOException;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

public final class CloudUtils {

    private static final String TAG = CloudUtils.class.getSimpleName();

    private static final String SETTINGS_CLOUD = "SETTINGS_CLOUD";
    private static final String SETTING_LAST_SYNC_TIME = "LAST_SYNC_TIME";
    private static final String SETTING_LAST_SYNC_STATUS = "LAST_SYNC_STATUS";

    private CloudUtils() {
    }

    /*public static String getAccountType() {
        // Resources.getSystem().getString(); // system resources only
        return LaanoApplication.getContext().getString(R.string.authenticator_account_type);
    }*/

    public static String getAccountType(Context context) {
        return context.getString(R.string.authenticator_account_type);
    }

    public static String getAccountName(@NonNull Account account) {
        return checkNotNull(account).name;
    }

    @Nullable
    public static Account[] getAccountsWithPermissionCheck(
            @NonNull Context context, @NonNull AccountManager accountManager) {
        checkNotNull(context);
        checkNotNull(accountManager);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Insufficient permission to access accounts in device");
            // NOTE: if permission have been revoked when the activity run (seems it's impossible)
            return null;
        }
        return accountManager.getAccountsByType(getAccountType(context));
    }

    public static boolean isAccountExists(
            @NonNull Context context, @NonNull Account account,
            @NonNull AccountManager accountManager) {
        checkNotNull(context);
        checkNotNull(account);
        checkNotNull(accountManager);

        Account[] accounts = getAccountsWithPermissionCheck(context, accountManager);
        return accounts != null && Arrays.asList(accounts).contains(account);
    }

    @Nullable
    public static Account getDefaultAccount(
            @NonNull Context context, @NonNull AccountManager accountManager) {
        checkNotNull(context);
        checkNotNull(accountManager);

        Account[] accounts = getAccountsWithPermissionCheck(context, accountManager);
        return accounts != null && accounts.length > 0 ? accounts[0] : null;
    }

    public static String getAccountUsername(@Nullable String name) {
        return name == null ? null : name.substring(0, name.lastIndexOf('@'));
    }

    public static String getApplicationId() {
        return LaanoApplication.getApplicationId();
    }

    public static boolean isApplicationConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @Nullable
    public static OwnCloudClient getOwnCloudClient(Account account, Context context) {
        OwnCloudAccount ocAccount;
        try {
            ocAccount = new OwnCloudAccount(account, context);
        } catch (AccountUtils.AccountNotFoundException e) {
            Log.e(TAG, "Account was not found on this device. ", e);
            return null;
        }
        try {
            return OwnCloudClientManagerFactory.getDefaultSingleton()
                    .getClientFor(ocAccount, context);
        } catch (AccountUtils.AccountNotFoundException
                | OperationCanceledException | AuthenticatorException | IOException e) {
            Log.e(TAG, "Cannot get client for the account. ", e);
        }
        return null;
    }

    public static SharedPreferences getCloudSharedPreferences(Context context) {
        return context.getSharedPreferences(SETTINGS_CLOUD, Context.MODE_PRIVATE);
    }

    public static String getSyncDirectory(Context context) {
        SharedPreferences sharedPreferences = getCloudSharedPreferences(context);
        Resources resources = context.getResources();
        String defaultSyncDirectory = resources.getString(R.string.default_sync_directory);
        return JsonFile.PATH_SEPARATOR + sharedPreferences.getString(
                resources.getString(R.string.pref_key_sync_directory), defaultSyncDirectory);
    }

    public static void updateUserProfile(
            Account account, OwnCloudClient ocClient, AccountManager accountManager) {
        GetRemoteUserInfoOperation operation = new GetRemoteUserInfoOperation();
        RemoteOperationResult result = operation.execute(ocClient);
        if (result.isSuccess()) {
            UserInfo userInfo = (UserInfo) result.getData().get(0);
            accountManager.setUserData(
                    account, AccountUtils.Constants.KEY_DISPLAY_NAME, userInfo.getDisplayName());
        } else {
            Log.e(TAG, "Error while retrieving user info from server [" + result.getCode().name() + "]");
        }
    }

    public static AccountItem getAccountItem(@NonNull Account account, @NonNull Context context) {
        checkNotNull(account);
        checkNotNull(context);

        AccountItem accountItem = new AccountItem(account);
        try {
            OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
            accountItem.setDisplayName(ocAccount.getDisplayName());
        } catch (AccountUtils.AccountNotFoundException e) {
            accountItem.setDisplayName(getAccountUsername(account.name));
        }
        return accountItem;
    }

    public static synchronized void updateLastSyncStatus(
            @NonNull Context context, int lastSyncStatus) {
        checkNotNull(context);

        SharedPreferences sharedPreferences = getCloudSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(SETTING_LAST_SYNC_TIME, currentTimeMillis());
        editor.putInt(SETTING_LAST_SYNC_STATUS, lastSyncStatus);
        editor.apply();
    }

    public static int getLastSyncStatus(@NonNull Context context) {
        checkNotNull(context);

        SharedPreferences sharedPreferences = CloudUtils.getCloudSharedPreferences(context);
        return sharedPreferences.getInt(SETTING_LAST_SYNC_STATUS, 0);
    }

    public static long getLastSyncTime(@NonNull Context context) {
        checkNotNull(context);

        SharedPreferences sharedPreferences = CloudUtils.getCloudSharedPreferences(context);
        return sharedPreferences.getLong(SETTING_LAST_SYNC_TIME, 0);
    }
}
