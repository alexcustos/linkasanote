package com.bytesforge.linkasanote;

import android.accounts.AccountManager;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule { // NOTE: final removed and public added for sake of DaggerMock

    private final Context context;

    public ApplicationModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return context;
    }

    @Provides
    @Singleton
    public AccountManager provideAccountManager() {
        return AccountManager.get(context);
    }
}
