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

import com.bytesforge.linkasanote.settings.Settings.isSyncUploadToEmpty
import com.bytesforge.linkasanote.settings.Settings.isSyncProtectLocal
import com.bytesforge.linkasanote.settings.Settings.updateLastFavoritesSyncTime
import com.bytesforge.linkasanote.settings.Settings.updateLastLinksSyncTime
import com.bytesforge.linkasanote.settings.Settings.updateLastNotesSyncTime
import com.bytesforge.linkasanote.settings.Settings.syncStatus
import com.bytesforge.linkasanote.sync.files.JsonFile
import com.bytesforge.linkasanote.utils.CloudUtils
import com.bytesforge.linkasanote.utils.UuidUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.bytesforge.linkasanote.sync.operations.nextcloud.UploadFileOperation.EnhancedUploadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.common.network.WebdavUtils
import com.bytesforge.linkasanote.sync.operations.nextcloud.UploadFileOperation
import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation.ServerInfo
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.lib.common.OwnCloudCredentials
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.bytesforge.linkasanote.sync.operations.nextcloud.CheckCredentialsOperation
import com.owncloud.android.lib.common.network.RedirectionPath
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation
import com.bytesforge.linkasanote.sync.operations.OperationsService.OperationsBinder
import com.bytesforge.linkasanote.sync.operations.OperationsService.OperationsHandler
import com.bytesforge.linkasanote.sync.operations.OperationsService.OperationItem
import android.accounts.Account
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.bytesforge.linkasanote.sync.operations.OperationsService
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import android.accounts.AccountsException
import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation
import com.bytesforge.linkasanote.data.source.local.LocalItems
import com.bytesforge.linkasanote.data.source.cloud.CloudItem
import com.bytesforge.linkasanote.sync.SyncNotifications
import com.bytesforge.linkasanote.sync.SyncItemResult
import com.bytesforge.linkasanote.sync.SyncItem
import com.bytesforge.linkasanote.utils.CommonUtils
import com.bytesforge.linkasanote.data.source.local.LocalContract
import android.database.sqlite.SQLiteConstraintException
import com.bytesforge.linkasanote.data.source.local.LocalContract.SyncResultEntry
import android.accounts.AccountManager
import com.bytesforge.linkasanote.data.source.local.LocalSyncResults
import com.bytesforge.linkasanote.data.source.local.LocalLinks
import com.bytesforge.linkasanote.data.source.local.LocalFavorites
import com.bytesforge.linkasanote.data.Favorite
import com.bytesforge.linkasanote.data.source.local.LocalNotes
import com.bytesforge.linkasanote.sync.SyncAdapter
import com.bytesforge.linkasanote.R
import io.reactivex.SingleSource
import android.widget.Toast
import androidx.annotation.StringRes
import javax.inject.Inject
import com.bytesforge.linkasanote.LaanoApplication
import com.bytesforge.linkasanote.sync.SyncService
import androidx.core.app.NotificationManagerCompat
import kotlin.jvm.JvmOverloads
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.*
import android.content.res.Resources
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable
import android.os.*
import android.util.Log
import com.bytesforge.linkasanote.data.Link
import com.bytesforge.linkasanote.data.Note
import com.bytesforge.linkasanote.settings.Settings
import com.google.common.base.Joiner
import io.reactivex.Single
import java.util.ArrayList

class SyncAdapter(
    private val context: Context?, private val settings: Settings?, autoInitialize: Boolean,
    private val accountManager: AccountManager?, private val syncNotifications: SyncNotifications,
    private val localSyncResults: LocalSyncResults?,
    localLinks: LocalLinks<Link>?, cloudLinks: CloudItem<Link>?,
    localFavorites: LocalFavorites<Favorite>?, cloudFavorites: CloudItem<Favorite>?,
    localNotes: LocalNotes<Note>?, cloudNotes: CloudItem<Note>?
) : AbstractThreadedSyncAdapter(
    context, autoInitialize
) {
    private val localLinks: LocalLinks<Link>
    private val cloudLinks: CloudItem<Link>
    private val localFavorites: LocalFavorites<Favorite>
    private val cloudFavorites: CloudItem<Favorite>
    private val localNotes: LocalNotes<Note>
    private val cloudNotes: CloudItem<Note>
    private val resources: Resources
    private var manualMode = false
    override fun onPerformSync(
        account: Account, extras: Bundle, authority: String,
        provider: ContentProviderClient, syncResult: SyncResult
    ) {
        manualMode = extras.getBoolean(SYNC_MANUAL_MODE, false)
        val started = System.currentTimeMillis()
        syncNotifications.setAccountName(CloudUtils.getAccountName(account))
        val ocClient = CloudUtils.getOwnCloudClient(account, context)
        if (ocClient == null) {
            syncNotifications.notifyFailedSynchronization(
                resources.getString(R.string.sync_adapter_title_failed_login),
                resources.getString(R.string.sync_adapter_text_failed_login)
            )
            return
        }

        //Start
        syncNotifications.sendSyncBroadcast(
            SyncNotifications.Companion.ACTION_SYNC, SyncNotifications.Companion.STATUS_SYNC_START
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
            SyncNotifications.Companion.ACTION_SYNC_FAVORITES,
            SyncNotifications.Companion.STATUS_SYNC_START
        )
        val syncFavorites = SyncItem(
            ocClient, localFavorites, cloudFavorites,
            syncNotifications, SyncNotifications.Companion.ACTION_SYNC_FAVORITES,
            settings!!.isSyncUploadToEmpty, settings.isSyncProtectLocal, started
        )
        favoritesSyncResult = syncFavorites.sync()
        settings.updateLastFavoritesSyncTime()
        syncNotifications.sendSyncBroadcast(
            SyncNotifications.Companion.ACTION_SYNC_FAVORITES,
            SyncNotifications.Companion.STATUS_SYNC_STOP
        )
        fatalError = favoritesSyncResult.isFatal

        // Links
        if (!fatalError) {
            syncNotifications.sendSyncBroadcast(
                SyncNotifications.Companion.ACTION_SYNC_LINKS,
                SyncNotifications.Companion.STATUS_SYNC_START
            )
            val syncLinks = SyncItem(
                ocClient, localLinks, cloudLinks,
                syncNotifications, SyncNotifications.Companion.ACTION_SYNC_LINKS,
                settings.isSyncUploadToEmpty, settings.isSyncProtectLocal, started
            )
            linksSyncResult = syncLinks.sync()
            settings.updateLastLinksSyncTime()
            syncNotifications.sendSyncBroadcast(
                SyncNotifications.Companion.ACTION_SYNC_LINKS,
                SyncNotifications.Companion.STATUS_SYNC_STOP
            )
            fatalError = linksSyncResult.isFatal
        }

        // Notes
        if (!fatalError) {
            syncNotifications.sendSyncBroadcast(
                SyncNotifications.Companion.ACTION_SYNC_NOTES,
                SyncNotifications.Companion.STATUS_SYNC_START
            )
            val syncNotes = SyncItem(
                ocClient, localNotes, cloudNotes,
                syncNotifications, SyncNotifications.Companion.ACTION_SYNC_NOTES,
                settings.isSyncUploadToEmpty, settings.isSyncProtectLocal, started
            )
            notesSyncResult = syncNotes.sync()
            settings.updateLastNotesSyncTime()
            settings.updateLastLinksSyncTime() // NOTE: because there are related links
            syncNotifications.sendSyncBroadcast(
                SyncNotifications.Companion.ACTION_SYNC_NOTES,
                SyncNotifications.Companion.STATUS_SYNC_STOP
            )
            fatalError = notesSyncResult.isFatal
        }

        // Stop
        val success = (!fatalError && favoritesSyncResult.isSuccess
                && linksSyncResult!!.isSuccess && notesSyncResult!!.isSuccess)
        saveLastSyncStatus(success)
        syncNotifications.sendSyncBroadcast(
            SyncNotifications.Companion.ACTION_SYNC, SyncNotifications.Companion.STATUS_SYNC_STOP
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
            val linkFailsCount = linksSyncResult.getFailsCount()
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
            val noteFailsCount = notesSyncResult.getFailsCount()
            if (noteFailsCount > 0) {
                failSources.add(
                    resources.getQuantityString(
                        R.plurals.count_notes,
                        noteFailsCount, noteFailsCount
                    )
                )
            }
            if (!failSources.isEmpty()) {
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
        handler.post { Toast.makeText(context, toastId, duration).show() }
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

    // NOTE: Note should contain linkId to notify related Link
    init {
        this.localLinks = localLinks!!
        this.cloudLinks = cloudLinks!!
        this.localFavorites = localFavorites!!
        this.cloudFavorites = cloudFavorites!!
        this.localNotes = localNotes!!
        this.cloudNotes = cloudNotes!!
        resources = context!!.resources
    }
}