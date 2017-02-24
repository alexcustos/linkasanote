package com.bytesforge.linkasanote.manageaccounts;

import android.accounts.AccountManager;
import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class ManageAccountsPresenterModule {

    private final ManageAccountsContract.View view;
    private final Context context;

    public ManageAccountsPresenterModule(Context context, ManageAccountsContract.View view) {
        this.context = context;
        this.view = view;
    }

    @Provides
    ManageAccountsContract.View provideManageAccountsContractView() {
        return view;
    }

    @Provides
    AccountManager provideAccountManager() {
        return AccountManager.get(context);
    }
}
