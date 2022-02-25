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

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.bytesforge.linkasanote.data.Item
import com.bytesforge.linkasanote.data.source.cloud.CloudItem
import com.bytesforge.linkasanote.data.source.local.LocalContract.SyncResultEntry
import com.bytesforge.linkasanote.data.source.local.LocalItems
import com.bytesforge.linkasanote.sync.files.JsonFile
import com.bytesforge.linkasanote.utils.CommonUtils
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult

class SyncItem<T : Item?>(
    private val ocClient: OwnCloudClient,
    private val localItems: LocalItems<T>, private val cloudItem: CloudItem<T>,
    private val syncNotifications: SyncNotifications, private val notificationAction: String,
    private val uploadToEmpty: Boolean, private val protectLocal: Boolean,
    private val started: Long
) {
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
                SyncNotifications.STATUS_DOWNLOADED, cloudId, ++downloaded
            )
            val notifyChanged = save(cloudItem)
            if (notifyChanged) {
                syncNotifications.sendSyncBroadcast(
                    notificationAction,
                    SyncNotifications.STATUS_CREATED, cloudId
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
        var statusChanged: Int = SyncNotifications.STATUS_UPDATED
        if (itemETag == null) { // New
            // duplicated && conflicted can be ignored
            if (item.isDeleted) {
                // DELETE local
                Log.e(TAG, "The records never synced must be deleted immediately [$itemId]")
                notifyChanged = deleteLocal(item)
                statusChanged = SyncNotifications.STATUS_DELETED
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
                    statusChanged = SyncNotifications.STATUS_DELETED
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
                    statusChanged = SyncNotifications.STATUS_DELETED
                }
            } else {
                if (item.isDeleted) {
                    // DELETE local
                    notifyChanged = deleteLocal(item)
                    statusChanged = SyncNotifications.STATUS_DELETED
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
                    SyncNotifications.STATUS_DOWNLOADED, itemId, ++downloaded
                )
                if (item.isSynced && !item.isDeleted) {
                    // SAVE local
                    notifyChanged = save(cloudItem)
                } else { // !synced || deleted
                    if (item == cloudItem) {
                        if (item.isDeleted) {
                            // DELETE cloud
                            notifyChanged = deleteCloud(item)
                            statusChanged = SyncNotifications.STATUS_DELETED
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
                        val state: SyncState = if (item.isDeleted) {
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
        checkNotNull(item)

        val itemId = item.id
        // Primary record
        try {
            val success = localItems.save(item).blockingGet()
            if (success) {
                localItems.logSyncResult(started, itemId,
                    SyncResultEntry.Result.DOWNLOADED).blockingGet()
                val relatedId = item.relatedId
                if (relatedId != null) {
                    localItems.logSyncResult(started, relatedId,
                        SyncResultEntry.Result.RELATED).blockingGet()
                }
            }
            return success
        } catch (e: NullPointerException) {
            logError(itemId)
            return false
        } catch (e: SQLiteConstraintException) {
            // NOTE: will try to resolve it further
        }
        // Duplicated record
        try {
            val success = localItems.saveDuplicated(item).blockingGet()
            if (success) {
                localItems.logSyncResult(started, itemId,
                    SyncResultEntry.Result.DOWNLOADED).blockingGet()
                val relatedId = item.relatedId
                if (relatedId != null) {
                    localItems.logSyncResult(started, relatedId,
                        SyncResultEntry.Result.RELATED).blockingGet()
                }
            }
            return success
        } catch (e: NullPointerException) {
            logError(itemId)
        } catch (e: SQLiteConstraintException) {
            logError(itemId)
        }
        return false
    }

    private fun deleteLocal(item: T): Boolean {
        checkNotNull(item)

        val itemId = item.id
        val relatedId = item.relatedId
        Log.d(TAG, "$itemId: DELETE local")
        val success = localItems.delete(itemId).blockingGet()
        if (success) {
            localItems.logSyncResult(started, itemId,
                SyncResultEntry.Result.DELETED).blockingGet()
            if (relatedId != null) {
                localItems.logSyncResult(started, relatedId,
                    SyncResultEntry.Result.RELATED).blockingGet()
            }
        }
        return success
    }

    private fun deleteCloud(item: T): Boolean {
        checkNotNull(item)

        val itemId = item.id
        Log.d(TAG, "$itemId: DELETE cloud")
        val result = cloudItem.delete(itemId, ocClient).blockingGet()
        return result.isSuccess && deleteLocal(item)
    }

    private fun update(item: T, state: SyncState): Boolean {
        checkNotNull(item)

        val itemId = item.id
        val relatedId = item.relatedId
        Log.d(TAG, "$itemId: UPDATE")
        val success = localItems.update(itemId, state).blockingGet()
        if (success) {
            val result: SyncResultEntry.Result = if (state.isConflicted) {
                SyncResultEntry.Result.CONFLICT
            } else {
                SyncResultEntry.Result.SYNCED
            }
            localItems.logSyncResult(started, itemId, result).blockingGet()
            if (relatedId != null) {
                localItems.logSyncResult(started, relatedId,
                    SyncResultEntry.Result.RELATED).blockingGet()
            }
        }
        return success
    }

    private fun upload(item: T): Boolean {
        checkNotNull(item)

        val itemId = item.id
        val relatedId = item.relatedId
        Log.d(TAG, item.id + ": UPLOAD")
        var success = false
        val result = cloudItem.upload(item, ocClient).blockingGet()
        if (result == null) {
            logError(itemId)
            return false
        }
        if (result.isSuccess) {
            syncNotifications.sendSyncBroadcast(
                notificationAction,
                SyncNotifications.STATUS_UPLOADED, itemId, ++uploaded
            )
            val jsonFile = result.data[0] as JsonFile
            val state = SyncState(jsonFile.eTag!!, SyncState.State.SYNCED)
            success = localItems.update(itemId, state).blockingGet()
            if (success) {
                localItems.logSyncResult(started, itemId,
                    SyncResultEntry.Result.UPLOADED).blockingGet()
                if (relatedId != null) {
                    localItems.logSyncResult(started, relatedId,
                        SyncResultEntry.Result.RELATED).blockingGet()
                }
            }
        } else if (result.code == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
            val state = SyncState(SyncState.State.CONFLICTED_UPDATE)
            success = update(item, state)
        }
        return success
    }

    private fun logError(itemId: String) {
        syncResult.incFailsCount()
        localItems.logSyncResult(started, itemId,
            SyncResultEntry.Result.ERROR).blockingGet()
    }

    private fun download(itemId: String): T? {
        Log.d(TAG, "$itemId: DOWNLOAD")
        return try {
            // Note: will be logged in save() or update()
            cloudItem.download(itemId, ocClient).blockingGet()
        } catch (e: NullPointerException) {
            logError(itemId)
            null
        } catch (e: NoSuchElementException) {
            // NOTE: unexpected error, file has to be in place already
            logError(itemId)
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
        syncResult = SyncItemResult(SyncItemResult.Status.FAILS_COUNT)
        uploaded = 0
        downloaded = 0
    }
}