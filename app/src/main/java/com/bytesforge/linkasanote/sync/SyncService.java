package com.bytesforge.linkasanote.sync;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.ApplicationModule;
import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.RepositoryModule;
import com.bytesforge.linkasanote.data.source.cloud.CloudFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;

import javax.inject.Inject;

public class SyncService extends Service {

    private static SyncAdapter syncAdapter = null;
    private static final Object syncAdapterLock = new Object();

    @Inject
    AccountManager accountManager;

    @Inject
    LocalFavorites localFavorites;

    @Inject
    CloudFavorites cloudFavorites;

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();
        DaggerSyncServiceComponent.builder()
                .applicationModule(new ApplicationModule(context))
                .repositoryModule(new RepositoryModule())
                .providerModule(new ProviderModule())
                .build().inject(this);

        synchronized (syncAdapterLock) {
            if (syncAdapter == null) {
                SyncNotifications syncNotifications = new SyncNotifications(context);
                syncAdapter = new SyncAdapter(context, true,
                        accountManager, syncNotifications, localFavorites, cloudFavorites);
            }
        } // synchronized
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}
