/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bytesforge.linkasanote.sync;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalLinks;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.data.source.local.LocalSyncResults;
import com.bytesforge.linkasanote.settings.Settings;

import javax.inject.Inject;

public class SyncService extends Service {

    // NOTE: the application context is used here, so no way to leak it somehow
    private static SyncAdapter syncAdapter = null;
    private static final Object syncAdapterLock = new Object();

    @Inject
    Context context;

    @Inject
    Settings settings;

    @Inject
    AccountManager accountManager;

    @Inject
    LocalSyncResults localSyncResults;

    @Inject
    LocalLinks<Link> localLinks;

    @Inject
    CloudItem<Link> cloudLinks;

    @Inject
    LocalFavorites<Favorite> localFavorites;

    @Inject
    CloudItem<Favorite> cloudFavorites;

    @Inject
    LocalNotes<Note> localNotes;

    @Inject
    CloudItem<Note> cloudNotes;

    @Override
    public void onCreate() {
        super.onCreate();
        LaanoApplication application = (LaanoApplication) getApplication();
        application.getApplicationComponent().inject(this);

        synchronized (syncAdapterLock) {
            if (syncAdapter == null) {
                SyncNotifications syncNotifications = new SyncNotifications(context);
                syncAdapter = new SyncAdapter(
                        context, settings, true, accountManager, syncNotifications,
                        localSyncResults,
                        localLinks, cloudLinks,
                        localFavorites, cloudFavorites,
                        localNotes, cloudNotes);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}
