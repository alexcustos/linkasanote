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
import android.os.Parcelable
import com.bytesforge.linkasanote.sync.files.JsonFile
import android.os.Parcel
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
import android.os.Bundle
import com.owncloud.android.lib.common.OwnCloudCredentials
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.bytesforge.linkasanote.sync.operations.nextcloud.CheckCredentialsOperation
import com.owncloud.android.lib.common.network.RedirectionPath
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation
import android.os.IBinder
import com.bytesforge.linkasanote.sync.operations.OperationsService.OperationsBinder
import com.bytesforge.linkasanote.sync.operations.OperationsService.OperationsHandler
import com.bytesforge.linkasanote.sync.operations.OperationsService.OperationItem
import android.accounts.Account
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import android.os.HandlerThread
import android.content.Intent
import com.bytesforge.linkasanote.sync.operations.OperationsService
import android.os.Looper
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
import android.content.ContentValues
import android.accounts.AccountManager
import com.bytesforge.linkasanote.data.source.local.LocalSyncResults
import com.bytesforge.linkasanote.data.source.local.LocalLinks
import com.bytesforge.linkasanote.data.source.local.LocalFavorites
import com.bytesforge.linkasanote.data.Favorite
import com.bytesforge.linkasanote.data.source.local.LocalNotes
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
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
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import com.bytesforge.linkasanote.data.Item
import com.google.common.base.Preconditions
import java.lang.NullPointerException
import java.util.HashSet
import java.util.NoSuchElementException

class SyncItem<T : Item?>(
    ocClient: OwnCloudClient,
    localItems: LocalItems<T>, cloudItem: CloudItem<T>,
    syncNotifications: SyncNotifications, notificationAction: String,
    uploadToEmpty: Boolean, protectLocal: Boolean, started: Long
) {
    private val localItems: LocalItems<T>
    private val cloudItem: CloudItem<T>
    private val syncNotifications: SyncNotifications
    private val ocClient: OwnCloudClient
    private val notificationAction: String
    private val uploadToEmpty: Boolean
    private val protectLocal: Boolean
    private val started: Long
    private var uploaded: Int
    private var downloaded: Int
    private var syncResult: SyncItemResult
    fun sync(): SyncItemResult {
        val dataStorageETag = cloudItem.getDataSourceETag(ocClient)
            ?: return SyncItemResult(SyncItemResult.Status.SOURCE_NOT_READY)
        val isCloudChanged = cloudItem.isCloudDataSourceChanged(dataStorageETag)
        syncItems(isCloudChanged)
        if (syncResult.isSuccess) {
            cloudItem.updateLastSyncedETag(dataStorageETag)
        }
        return syncResult
    }

    private fun syncItems(isCloudChanged: Boolean) {
        if (!isCloudChanged) {
            localItems.unsynced
                .subscribe(
                    { item: T -> syncItem(item, item!!.eTag) }
                ) { throwable: Throwable? -> setDbAccessError() }
            return
        }
        val cloudDataSourceMap = cloudItem.getDataSourceMap(ocClient)
        if (cloudDataSourceMap.isEmpty() && uploadToEmpty) {
            val numRows = localItems.resetSyncState().blockingGet()
            if (numRows > 0) {
                Log.d(TAG, "Cloud storage loss is detected, starting to upload [$numRows]")
            }
        }
        // Sync Local
        localItems.all
            .subscribe({ item: T ->
                val cloudETag = cloudDataSourceMap[item!!.id]
                syncItem(item, cloudETag)
            }) { throwable: Throwable? ->
                CommonUtils.logStackTrace(TAG_E, throwable!!)
                setDbAccessError()
            }
        if (syncResult.isDbAccessError) return

        // OPTIMIZATION: Local Map can be taken from previous step
        val localIds: MutableSet<String> = HashSet()
        localItems.ids
            .subscribe(
                { e: String -> localIds.add(e) }
            ) { throwable: Throwable? -> setDbAccessError() }
        if (syncResult.isDbAccessError) return

        // New cloud records
        // OPTIMIZATION: Cloud Map can be updated in the previous step
        val cloudIds: Set<String> = cloudItem.getDataSourceMap(ocClient).keys
        for (cloudId in cloudIds) {
            if (localIds.contains(cloudId)) {
                continue
            }
            val cloudItem = download(cloudId) ?: continue
            syncNotifications.sendSyncBroadcast(
                notificationAction,
                SyncNotifications.Companion.STATUS_DOWNLOADED, cloudId, ++downloaded
            )
            val notifyChanged = save(cloudItem)
            if (notifyChanged) {
                syncNotifications.sendSyncBroadcast(
                    notificationAction,
                    SyncNotifications.Companion.STATUS_CREATED, cloudId
                )
            }
        }
    }

    private fun syncItem(item: T, cloudETag: String?) {
        // NOTE: some updates on the conflicted state may cause constraint violation, so let it be resolved first
        if (item!!.isConflicted) return
        val itemId = item.id
        val itemETag = item.eTag
        var notifyChanged = false
        var statusChanged: Int = SyncNotifications.Companion.STATUS_UPDATED
        if (itemETag == null) { // New
            // duplicated && conflicted can be ignored
            if (item.isDeleted) {
                // DELETE local
                Log.e(TAG, "The records never synced must be deleted immediately [$itemId]")
                notifyChanged = deleteLocal(item)
                statusChanged = SyncNotifications.Companion.STATUS_DELETED
            } else { // synced is ignored (!synced)
                // UPLOAD
                // NOTE: local unsynced will replace cloud with the same ID (acceptable behaviour)
                notifyChanged = upload(item)
            }
        } else if (itemETag == cloudETag) { // Green light
            // conflicted can be ignored
            if (!item.isSynced) {
                if (item.isDeleted) {
                    // DELETE cloud
                    notifyChanged = deleteCloud(item)
                    statusChanged = SyncNotifications.Companion.STATUS_DELETED
                } else if (!item.isDuplicated) {
                    // UPLOAD
                    notifyChanged = upload(item)
                }
            }
        } else if (cloudETag == null) { // Was deleted on cloud
            if (item.isSynced) {
                if (protectLocal) {
                    // SET conflicted (as if it has been changed)
                    val state = SyncState(SyncState.State.CONFLICTED_UPDATE)
                    notifyChanged = update(item, state)
                } else {
                    // DELETE local
                    notifyChanged = deleteLocal(item)
                    statusChanged = SyncNotifications.Companion.STATUS_DELETED
                }
            } else {
                if (item.isDeleted) {
                    // DELETE local
                    notifyChanged = deleteLocal(item)
                    statusChanged = SyncNotifications.Companion.STATUS_DELETED
                } else {
                    // SET conflicted
                    val state = SyncState(SyncState.State.CONFLICTED_UPDATE)
                    notifyChanged = update(item, state)
                }
            }
        } else { // Was changed on cloud
            // duplicated && conflicted can be ignored
            // DOWNLOAD (with synced state by default)
            val cloudItem = download(itemId)
            if (cloudItem != null) {
                syncNotifications.sendSyncBroadcast(
                    notificationAction,
                    SyncNotifications.Companion.STATUS_DOWNLOADED, itemId, ++downloaded
                )
                if (item.isSynced && !item.isDeleted) {
                    // SAVE local
                    notifyChanged = save(cloudItem)
                } else { // !synced || deleted
                    if (item == cloudItem) {
                        if (item.isDeleted) {
                            // DELETE cloud
                            notifyChanged = deleteCloud(item)
                            statusChanged = SyncNotifications.Companion.STATUS_DELETED
                        } else {
                            // UPDATE state
                            assert(cloudItem.eTag != null)
                            val state = SyncState(
                                cloudItem.eTag!!, SyncState.State.SYNCED
                            )
                            // NOTE: record may be in conflicted state
                            notifyChanged = update(item, state)
                        }
                    } else {
                        // SET (or confirm) conflicted
                        val state: SyncState
                        state = if (item.isDeleted) {
                            SyncState(SyncState.State.CONFLICTED_DELETE)
                        } else {
                            SyncState(SyncState.State.CONFLICTED_UPDATE)
                        }
                        notifyChanged = update(item, state)
                    }
                }
            }
        }
        if (notifyChanged) {
            syncNotifications.sendSyncBroadcast(notificationAction, statusChanged, itemId)
        }
    }

    // NOTE: any cloud item can violate the DB constraints
    private fun save(item: T): Boolean { // downloaded
        Preconditions.checkNotNull(item)
        val itemId = item!!.id
        // Primary record
        try {
            val success = localItems.save(item).blockingGet()
            if (success) {
                localItems.logSyncResult(
                    started, itemId,
                    SyncResultEntry.Result.DOWNLOADED
                ).blockingGet()
                val relatedId = item.relatedId
                if (relatedId != null) {
                    localItems.logSyncResult(
                        started, relatedId,
                        SyncResultEntry.Result.RELATED
                    ).blockingGet()
                }
            }
            return success
        } catch (e: NullPointerException) {
            syncResult.incFailsCount()
            localItems.logSyncResult(
                started, itemId,
                SyncResultEntry.Result.ERROR
            ).blockingGet()
            return false
        } catch (e: SQLiteConstraintException) {
            // NOTE: will try to resolve it further
        }
        // Duplicated record
        try {
            val success = localItems.saveDuplicated(item).blockingGet()
            if (success) {
                localItems.logSyncResult(
                    started, itemId,
                    SyncResultEntry.Result.DOWNLOADED
                ).blockingGet()
                val relatedId = item.relatedId
                if (relatedId != null) {
                    localItems.logSyncResult(
                        started, relatedId,
                        SyncResultEntry.Result.RELATED
                    ).blockingGet()
                }
            }
            return success
        } catch (e: NullPointerException) {
            syncResult.incFailsCount()
            localItems.logSyncResult(
                started, itemId,
                SyncResultEntry.Result.ERROR
            ).blockingGet()
        } catch (e: SQLiteConstraintException) {
            syncResult.incFailsCount()
            localItems.logSyncResult(
                started, itemId,
                SyncResultEntry.Result.ERROR
            ).blockingGet()
        }
        return false
    }

    private fun deleteLocal(item: T): Boolean {
        Preconditions.checkNotNull(item)
        val itemId = item!!.id
        val relatedId = item.relatedId
        Log.d(TAG, "$itemId: DELETE local")
        val success = localItems.delete(itemId).blockingGet()
        if (success) {
            localItems.logSyncResult(
                started, itemId,
                SyncResultEntry.Result.DELETED
            ).blockingGet()
            if (relatedId != null) {
                localItems.logSyncResult(
                    started, relatedId,
                    SyncResultEntry.Result.RELATED
                ).blockingGet()
            }
        }
        return success
    }

    private fun deleteCloud(item: T): Boolean {
        Preconditions.checkNotNull(item)
        val itemId = item!!.id
        Log.d(TAG, "$itemId: DELETE cloud")
        val result = cloudItem.delete(itemId, ocClient).blockingGet()
        return result.isSuccess && deleteLocal(item)
    }

    private fun update(item: T, state: SyncState): Boolean {
        Preconditions.checkNotNull(item)
        Preconditions.checkNotNull(state)
        val itemId = item!!.id
        val relatedId = item.relatedId
        Log.d(TAG, "$itemId: UPDATE")
        val success = localItems.update(itemId, state).blockingGet()
        if (success) {
            val result: SyncResultEntry.Result
            result = if (state.isConflicted) {
                SyncResultEntry.Result.CONFLICT
            } else {
                SyncResultEntry.Result.SYNCED
            }
            localItems.logSyncResult(started, itemId, result).blockingGet()
            if (relatedId != null) {
                localItems.logSyncResult(
                    started, relatedId,
                    SyncResultEntry.Result.RELATED
                ).blockingGet()
            }
        }
        return success
    }

    private fun upload(item: T): Boolean {
        Preconditions.checkNotNull(item)
        val itemId = item!!.id
        val relatedId = item.relatedId
        Log.d(TAG, item.id + ": UPLOAD")
        var success = false
        val result = cloudItem.upload(item, ocClient).blockingGet()
        if (result == null) {
            syncResult.incFailsCount()
            localItems.logSyncResult(
                started, itemId,
                SyncResultEntry.Result.ERROR
            ).blockingGet()
            return false
        }
        if (result.isSuccess) {
            syncNotifications.sendSyncBroadcast(
                notificationAction,
                SyncNotifications.Companion.STATUS_UPLOADED, itemId, ++uploaded
            )
            val jsonFile = result.data[0] as JsonFile
            val state = SyncState(jsonFile.eTag, SyncState.State.SYNCED)
            success = localItems.update(itemId, state).blockingGet()
            if (success) {
                localItems.logSyncResult(
                    started, itemId,
                    SyncResultEntry.Result.UPLOADED
                ).blockingGet()
                if (relatedId != null) {
                    localItems.logSyncResult(
                        started, relatedId,
                        SyncResultEntry.Result.RELATED
                    ).blockingGet()
                }
            }
        } else if (result.code == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
            val state = SyncState(SyncState.State.CONFLICTED_UPDATE)
            success = update(item, state)
        }
        return success
    }

    private fun download(itemId: String): T? {
        Preconditions.checkNotNull(itemId)
        Log.d(TAG, "$itemId: DOWNLOAD")
        return try {
            // Note: will be logged in save() or update()
            cloudItem.download(itemId, ocClient).blockingGet()
        } catch (e: NullPointerException) {
            syncResult.incFailsCount()
            localItems.logSyncResult(
                started, itemId,
                SyncResultEntry.Result.ERROR
            ).blockingGet()
            null // NOTE: an unexpected error, file have to be in place here
        } catch (e: NoSuchElementException) {
            syncResult.incFailsCount()
            localItems.logSyncResult(
                started, itemId,
                SyncResultEntry.Result.ERROR
            ).blockingGet()
            null
        }
    }

    private fun setDbAccessError() {
        syncResult = SyncItemResult(SyncItemResult.Status.DB_ACCESS_ERROR)
    }

    @get:VisibleForTesting
    val failsCount: Int
        get() = syncResult.failsCount

    companion object {
        private val TAG = SyncItem::class.java.simpleName
        private val TAG_E = SyncItem::class.java.canonicalName
    }

    init {
        this.ocClient = Preconditions.checkNotNull(ocClient)
        this.localItems = Preconditions.checkNotNull(localItems)
        this.cloudItem = Preconditions.checkNotNull(cloudItem)
        this.syncNotifications = Preconditions.checkNotNull(syncNotifications)
        this.notificationAction = Preconditions.checkNotNull(notificationAction)
        this.uploadToEmpty = uploadToEmpty
        this.protectLocal = protectLocal
        this.started = started
        syncResult = SyncItemResult(SyncItemResult.Status.FAILS_COUNT)
        uploaded = 0
        downloaded = 0
    }
}