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

import android.accounts.Account
import android.accounts.AccountManager
import android.content.*
import android.content.res.Resources
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.data.Favorite
import com.bytesforge.linkasanote.data.Link
import com.bytesforge.linkasanote.data.Note
import com.bytesforge.linkasanote.data.source.cloud.CloudItem
import com.bytesforge.linkasanote.data.source.local.LocalFavorites
import com.bytesforge.linkasanote.data.source.local.LocalLinks
import com.bytesforge.linkasanote.data.source.local.LocalNotes
import com.bytesforge.linkasanote.data.source.local.LocalSyncResults
import com.bytesforge.linkasanote.settings.Settings
import com.bytesforge.linkasanote.utils.CloudUtils
import com.google.common.base.Joiner
import io.reactivex.Single

class SyncAdapter(
    private val contextVal: Context?, private val settings: Settings?, autoInitialize: Boolean,
    private val accountManager: AccountManager?, private val syncNotifications: SyncNotifications,
    private val localSyncResults: LocalSyncResults?,
    localLinks: LocalLinks<Link>?, cloudLinks: CloudItem<Link>?,
    localFavorites: LocalFavorites<Favorite>?, cloudFavorites: CloudItem<Favorite>?,
    localNotes: LocalNotes<Note>?, cloudNotes: CloudItem<Note>?
) : AbstractThreadedSyncAdapter(
    contextVal, autoInitialize
) {
    private val localLinks: LocalLinks<Link> = localLinks!!
    private val cloudLinks: CloudItem<Link> = cloudLinks!!
    private val localFavorites: LocalFavorites<Favorite> = localFavorites!!
    private val cloudFavorites: CloudItem<Favorite> = cloudFavorites!!
    private val localNotes: LocalNotes<Note> = localNotes!!
    private val cloudNotes: CloudItem<Note> = cloudNotes!!
    private val resources: Resources = contextVal!!.resources

    private var manualMode = false

    override fun onPerformSync(
        account: Account, extras: Bundle, authority: String,
        provider: ContentProviderClient, syncResult: SyncResult
    ) {
        manualMode = extras.getBoolean(SYNC_MANUAL_MODE, false)
        val started = System.currentTimeMillis()
        syncNotifications.setAccountName(CloudUtils.getAccountName(account))
        val ocClient = CloudUtils.getOwnCloudClient(account, contextVal)
        if (ocClient == null) {
            syncNotifications.notifyFailedSynchronization(
                resources.getString(R.string.sync_adapter_title_failed_login),
                resources.getString(R.string.sync_adapter_text_failed_login)
            )
            return
        }

        //Start
        syncNotifications.sendSyncBroadcast(
            SyncNotifications.ACTION_SYNC, SyncNotifications.STATUS_SYNC_START
        )
        val updated = CloudUtils.updateUserProfile(account, ocClient, accountManager)
        if (!updated) {
            syncNotifications.notifyFailedSynchronization(
                resources.getString(R.string.sync_adapter_title_failed_cloud),
                resources.getString(R.string.sync_adapter_text_failed_cloud_profile)
            )
            return
        }
        val numRows = localSyncResults!!.cleanup().blockingGet()
        Log.d(TAG, "onPerformSync(): cleanupSyncResults() [$numRows]")
        var fatalError: Boolean
        val favoritesSyncResult: SyncItemResult
        var linksSyncResult: SyncItemResult? = null
        var notesSyncResult: SyncItemResult? = null

        // Favorites
        syncNotifications.sendSyncBroadcast(
            SyncNotifications.ACTION_SYNC_FAVORITES,
            SyncNotifications.STATUS_SYNC_START
        )
        val syncFavorites = SyncItem(
            ocClient, localFavorites, cloudFavorites,
            syncNotifications, SyncNotifications.ACTION_SYNC_FAVORITES,
            settings!!.isSyncUploadToEmpty, settings.isSyncProtectLocal, started
        )
        favoritesSyncResult = syncFavorites.sync()
        settings.updateLastFavoritesSyncTime()
        syncNotifications.sendSyncBroadcast(
            SyncNotifications.ACTION_SYNC_FAVORITES,
            SyncNotifications.STATUS_SYNC_STOP
        )
        fatalError = favoritesSyncResult.isFatal

        // Links
        if (!fatalError) {
            syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC_LINKS,
                SyncNotifications.STATUS_SYNC_START
            )
            val syncLinks = SyncItem(
                ocClient, localLinks, cloudLinks,
                syncNotifications, SyncNotifications.ACTION_SYNC_LINKS,
                settings.isSyncUploadToEmpty, settings.isSyncProtectLocal, started
            )
            linksSyncResult = syncLinks.sync()
            settings.updateLastLinksSyncTime()
            syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC_LINKS,
                SyncNotifications.STATUS_SYNC_STOP
            )
            fatalError = linksSyncResult.isFatal
        }

        // Notes
        if (!fatalError) {
            syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC_NOTES,
                SyncNotifications.STATUS_SYNC_START
            )
            val syncNotes = SyncItem(
                ocClient, localNotes, cloudNotes,
                syncNotifications, SyncNotifications.ACTION_SYNC_NOTES,
                settings.isSyncUploadToEmpty, settings.isSyncProtectLocal, started
            )
            notesSyncResult = syncNotes.sync()
            settings.updateLastNotesSyncTime()
            settings.updateLastLinksSyncTime() // NOTE: because there are related links
            syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC_NOTES,
                SyncNotifications.STATUS_SYNC_STOP
            )
            fatalError = notesSyncResult.isFatal
        }

        // Stop
        val success = (!fatalError && favoritesSyncResult.isSuccess
                && linksSyncResult!!.isSuccess && notesSyncResult!!.isSuccess)
        saveLastSyncStatus(success)
        syncNotifications.sendSyncBroadcast(
            SyncNotifications.ACTION_SYNC, SyncNotifications.STATUS_SYNC_STOP
        )

        // Error notifications
        if (fatalError) {
            var fatalResult = favoritesSyncResult
            if (linksSyncResult != null) fatalResult = linksSyncResult
            if (notesSyncResult != null) fatalResult = notesSyncResult
            if (fatalResult.isDbAccessError) {
                syncNotifications.notifyFailedSynchronization(
                    resources.getString(R.string.sync_adapter_title_failed_database),
                    resources.getString(R.string.sync_adapter_text_failed_database)
                )
            } else if (fatalResult.isSourceNotReady) {
                syncNotifications.notifyFailedSynchronization(
                    resources.getString(R.string.sync_adapter_title_failed_cloud),
                    resources.getString(R.string.sync_adapter_text_failed_cloud_access)
                )
            }
        } else {
            // Fail
            val failSources: MutableList<String> = ArrayList()
            val linkFailsCount = linksSyncResult!!.failsCount
            if (linkFailsCount > 0) {
                failSources.add(
                    resources.getQuantityString(
                        R.plurals.count_links,
                        linkFailsCount, linkFailsCount
                    )
                )
            }
            val favoriteFailsCount = favoritesSyncResult.failsCount
            if (favoriteFailsCount > 0) {
                failSources.add(
                    resources.getQuantityString(
                        R.plurals.count_favorites,
                        favoriteFailsCount, favoriteFailsCount
                    )
                )
            }
            val noteFailsCount = notesSyncResult!!.failsCount
            if (noteFailsCount > 0) {
                failSources.add(
                    resources.getQuantityString(
                        R.plurals.count_notes,
                        noteFailsCount, noteFailsCount
                    )
                )
            }
            if (failSources.isNotEmpty()) {
                syncNotifications.notifyFailedSynchronization(
                    resources.getString(
                        R.string.sync_adapter_text_failed, Joiner.on(", ").join(failSources)
                    )
                )
            }
        }
    }

    private fun saveLastSyncStatus(success: Boolean) {
        val syncStatus: Int
        if (success) {
            val conflictedStatus = localLinks.isConflicted
                .flatMap { conflicted: Boolean -> if (conflicted) Single.just(true) else localFavorites.isConflicted }
                .flatMap { conflicted: Boolean -> if (conflicted) Single.just(true) else localNotes.isConflicted }
                .blockingGet()
            val unsyncedStatus = localLinks.isUnsynced
                .flatMap { unsynced: Boolean -> if (unsynced) Single.just(true) else localFavorites.isUnsynced }
                .flatMap { unsynced: Boolean -> if (unsynced) Single.just(true) else localNotes.isUnsynced }
                .blockingGet()
            if (conflictedStatus) {
                syncStatus = SYNC_STATUS_CONFLICT
                if (manualMode) {
                    showToast(R.string.toast_sync_conflict, Toast.LENGTH_LONG)
                }
            } else if (unsyncedStatus) {
                // NOTE: normally it should not be happened, but the chance is not zero
                syncStatus = SYNC_STATUS_UNSYNCED
                if (manualMode) {
                    showToast(R.string.toast_sync_unsynced, Toast.LENGTH_LONG)
                }
            } else {
                syncStatus = SYNC_STATUS_SYNCED
                if (manualMode) {
                    showToast(R.string.toast_sync_success, Toast.LENGTH_SHORT)
                }
            }
        } else {
            syncStatus = SYNC_STATUS_ERROR
            if (manualMode) {
                showToast(R.string.toast_sync_error, Toast.LENGTH_LONG)
            }
        }
        settings!!.syncStatus = syncStatus
    }

    private fun showToast(@StringRes toastId: Int, duration: Int) {
        val handler = Handler(Looper.getMainLooper())
        handler.post { Toast.makeText(contextVal, toastId, duration).show() }
    }

    companion object {
        private val TAG = SyncAdapter::class.java.simpleName
        const val SYNC_STATUS_UNKNOWN = 0
        const val SYNC_STATUS_SYNCED = 1
        const val SYNC_STATUS_UNSYNCED = 2
        const val SYNC_STATUS_ERROR = 3
        const val SYNC_STATUS_CONFLICT = 4
        const val SYNC_MANUAL_MODE = "MANUAL_MODE"
    }

}