package com.bytesforge.linkasanote.sync;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.data.source.cloud.CloudFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.settings.Settings;

import javax.inject.Inject;

public class SyncService extends Service {

    // TODO: check out best practice about this warning
    private static SyncAdapter syncAdapter = null;
    private static final Object syncAdapterLock = new Object();

    @Inject
    Context context;

    @Inject
    Settings settings;

    @Inject
    AccountManager accountManager;

    @Inject
    LocalFavorites localFavorites;

    @Inject
    CloudFavorites cloudFavorites;

    @Override
    public void onCreate() {
        super.onCreate();
        LaanoApplication application = (LaanoApplication) getApplication();
        application.getApplicationComponent().inject(this);

        synchronized (syncAdapterLock) {
            if (syncAdapter == null) {
                SyncNotifications syncNotifications = new SyncNotifications(context);
                syncAdapter = new SyncAdapter(context, settings, true,
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
