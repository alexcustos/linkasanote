package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.accounts.Account;
import android.support.annotation.Nullable;

import dagger.Module;
import dagger.Provides;

@Module
public class NextcloudPresenterModule {

    private final NextcloudContract.View view;

    @Nullable
    private final Account account;

    public NextcloudPresenterModule(NextcloudContract.View view, @Nullable Account account) {
        this.view = view;
        this.account = account;
    }

    @Provides
    NextcloudContract.View provideNextcloudContractView() {
        return view;
    }

    @Provides
    @Nullable
    @NextcloudAccount
    Account provideAccount() {
        return account;
    }
}
