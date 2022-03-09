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
package com.bytesforge.linkasanote.sync

import android.accounts.AccountManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.bytesforge.linkasanote.LaanoApplication
import com.bytesforge.linkasanote.data.Favorite
import com.bytesforge.linkasanote.data.Link
import com.bytesforge.linkasanote.data.Note
import com.bytesforge.linkasanote.data.source.cloud.CloudItem
import com.bytesforge.linkasanote.data.source.local.LocalFavorites
import com.bytesforge.linkasanote.data.source.local.LocalLinks
import com.bytesforge.linkasanote.data.source.local.LocalNotes
import com.bytesforge.linkasanote.data.source.local.LocalSyncResults
import com.bytesforge.linkasanote.settings.Settings
import javax.inject.Inject

class SyncService : Service() {
    @JvmField
    @Inject
    var context: Context? = null

    @JvmField
    @Inject
    var settings: Settings? = null

    @JvmField
    @Inject
    var accountManager: AccountManager? = null

    @JvmField
    @Inject
    var localSyncResults: LocalSyncResults? = null

    @JvmField
    @Inject
    var localLinks: LocalLinks<Link>? = null

    @JvmField
    @Inject
    var cloudLinks: CloudItem<Link>? = null

    @JvmField
    @Inject
    var localFavorites: LocalFavorites<Favorite>? = null

    @JvmField
    @Inject
    var cloudFavorites: CloudItem<Favorite>? = null

    @JvmField
    @Inject
    var localNotes: LocalNotes<Note>? = null

    @JvmField
    @Inject
    var cloudNotes: CloudItem<Note>? = null
    override fun onCreate() {
        super.onCreate()
        val application = application as LaanoApplication
        application.applicationComponent.inject(this)
        synchronized(syncAdapterLock) {
            if (syncAdapter == null) {
                val syncNotifications = SyncNotifications(context)
                syncAdapter = SyncAdapter(
                    context, settings, true, accountManager, syncNotifications,
                    localSyncResults,
                    localLinks, cloudLinks,
                    localFavorites, cloudFavorites,
                    localNotes, cloudNotes
                )
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return syncAdapter!!.syncAdapterBinder
    }

    companion object {
        // NOTE: the application context is used here, so no way to leak it somehow
        private var syncAdapter: SyncAdapter? = null
        private val syncAdapterLock = Any()
    }
}