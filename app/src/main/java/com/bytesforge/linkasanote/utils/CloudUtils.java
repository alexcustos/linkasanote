package com.bytesforge.linkasanote.utils;

import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;

public class CloudUtils {

    public static String getAccountType() {
        // Resources.getSystem().getString(); // system resources only
        return LaanoApplication.getContext().getString(R.string.authenticator_account_type);
    }

    public static String getAccountUsername(@Nullable String name) {
        return name == null ? null : name.substring(0, name.lastIndexOf('@'));
    }
}
