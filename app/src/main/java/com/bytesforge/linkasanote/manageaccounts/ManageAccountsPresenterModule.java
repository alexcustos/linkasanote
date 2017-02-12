package com.bytesforge.linkasanote.manageaccounts;

import dagger.Module;
import dagger.Provides;

@Module
public class ManageAccountsPresenterModule {

    private final ManageAccountsContract.View view;

    public ManageAccountsPresenterModule(ManageAccountsContract.View view) {
        this.view = view;
    }

    @Provides
    ManageAccountsContract.View provideManageAccountsContractView() {
        return view;
    }
}
