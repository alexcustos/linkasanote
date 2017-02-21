package com.bytesforge.linkasanote.utils;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

public class CloudUtils {

    /*public static String getAccountType() {
        // Resources.getSystem().getString(); // system resources only
        return LaanoApplication.getContext().getString(R.string.authenticator_account_type);
    }*/

    public static String getAccountType(Context context) {
        return context.getString(R.string.authenticator_account_type);
    }

    @Nullable
    public static Account[] getAccountsWithPermissionCheck(
            @NonNull Context context, @NonNull AccountManager accountManager) {
        checkNotNull(context);
        checkNotNull(accountManager);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            // NOTE: if permission have been revoked when the activity run (seems it's impossible)
            return null;
        }
        return accountManager.getAccountsByType(getAccountType(context));
    }

    public static boolean isAccountExists(@NonNull Context context, @NonNull Account account) {
        checkNotNull(context);
        checkNotNull(account);

        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = getAccountsWithPermissionCheck(context, accountManager);

        return accounts != null && Arrays.asList(accounts).contains(account);
    }

    public static String getAccountUsername(@Nullable String name) {
        return name == null ? null : name.substring(0, name.lastIndexOf('@'));
    }

    public static String getApplicationId() {
        return LaanoApplication.getApplicationId();
    }

    public static String getFileExtension() {
        return LaanoApplication.getContext().getString(R.string.json_extension);
    }

    public static boolean isApplicationConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
