package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.content.Context;
import android.support.annotation.Nullable;

import dagger.Module;
import dagger.Provides;

@Module
public class NextcloudPresenterModule {

    private final NextcloudContract.View view;

    @Nullable
    private final Account account;

    @Nullable
    private final AccountAuthenticatorResponse accountAuthenticatorResponse;

    public NextcloudPresenterModule(
            NextcloudContract.View view, @Nullable Account account,
            @Nullable AccountAuthenticatorResponse accountAuthenticatorResponse) {
        this.view = view;
        this.account = account;
        this.accountAuthenticatorResponse = accountAuthenticatorResponse;
    }

    @Provides
    public NextcloudContract.View provideNextcloudContractView() {
        return view;
    }

    @Provides
    public NextcloudContract.ViewModel provideNextcloudContractViewModel(Context context) {
        return new NextcloudViewModel(context);
    }

    @Provides
    @Nullable
    @NextcloudAccount
    public Account provideAccount() {
        return account;
    }

    @Provides
    @Nullable
    public AccountAuthenticatorResponse provideAccountAuthenticatorResponse() {
        return accountAuthenticatorResponse;
    }
}
