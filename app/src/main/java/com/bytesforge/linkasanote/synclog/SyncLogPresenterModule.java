package com.bytesforge.linkasanote.synclog;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class SyncLogPresenterModule {

    private final Context context;
    private SyncLogContract.View view;

    public SyncLogPresenterModule(Context context, SyncLogContract.View view) {
        this.context = context;
        this.view = view;
    }

    @Provides
    public SyncLogContract.View provideSyncLogContractView() {
        return view;
    }

    @Provides
    public SyncLogContract.ViewModel provideSyncLogContractViewModel() {
        return new SyncLogViewModel(context);
    }
}
