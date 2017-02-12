package com.bytesforge.linkasanote.manageaccounts;

import android.accounts.Account;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class AccountItem {
    public static final int TYPE_ACCOUNT = 0;
    public static final int TYPE_ACTION_ADD = 1;

    @Nullable
    private Account account;

    @Nullable
    private String displayName;

    private int type;

    public AccountItem(@NonNull Account account) {
        this.account = checkNotNull(account);
        type = TYPE_ACCOUNT;
    }

    public AccountItem() {
        type = TYPE_ACTION_ADD;
    }

    @Nullable
    public Account getAccount() {
        return account;
    }

    public int getType() {
        return type;
    }

    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
    }

    @Nullable
    public String getAccountName() {
        return account == null ? null : account.name;
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }
}
