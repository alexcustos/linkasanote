package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.annotation.Nullable;

import dagger.Module;
import dagger.Provides;

@Module
public class NextcloudPresenterModule {

    private final Context context;
    private final NextcloudContract.View view;

    @Nullable
    private final Account account;

    public NextcloudPresenterModule(
            Context context, NextcloudContract.View view, @Nullable Account account) {
        this.context = context;
        this.view = view;
        this.account = account;
    }

    @Provides
    public NextcloudContract.View provideNextcloudContractView() {
        return view;
    }

    @Provides
    public NextcloudContract.ViewModel provideNextcloudContractViewModel() {
        return new NextcloudViewModel(context);
    }

    @Provides
    public AccountManager provideAccountManager() {
        return AccountManager.get(context);
    }

    @Provides
    @Nullable
    @NextcloudAccount
    public Account provideAccount() {
        return account;
    }
}
