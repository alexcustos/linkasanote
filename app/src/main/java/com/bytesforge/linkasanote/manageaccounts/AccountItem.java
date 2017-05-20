package com.bytesforge.linkasanote.manageaccounts;

import android.accounts.Account;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Objects;

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AccountItem accountItem = (AccountItem) obj;
        if (account == null ^ accountItem.account == null) return false;

        // NOTE: (account == null && accountItem.account == null)
        return (account == null || Objects.equal(account.name, accountItem.account.name))
                && Objects.equal(displayName, accountItem.displayName);
    }
}
