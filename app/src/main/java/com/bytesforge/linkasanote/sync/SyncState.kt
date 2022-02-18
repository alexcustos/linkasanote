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
import com.bytesforge.linkasanote.data.source.cloud.CloudItem
import com.bytesforge.linkasanote.sync.SyncNotifications
import com.bytesforge.linkasanote.sync.SyncItemResult
import com.bytesforge.linkasanote.sync.SyncItem
import com.bytesforge.linkasanote.utils.CommonUtils
import android.database.sqlite.SQLiteConstraintException
import com.bytesforge.linkasanote.data.source.local.LocalContract.SyncResultEntry
import android.content.ContentValues
import android.accounts.AccountManager
import com.bytesforge.linkasanote.data.Favorite
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
import android.database.Cursor
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable
import com.bytesforge.linkasanote.data.source.local.*
import com.google.common.base.Objects
import com.google.common.base.Preconditions
import java.lang.IllegalArgumentException

class SyncState : Parcelable {
    val rowId: Long
    val duplicated: Int
    val isConflicted: Boolean
    val isDeleted: Boolean
    val isSynced: Boolean
    var eTag: String?
        private set

    override fun describeContents(): Int {
        return super.hashCode()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(rowId)
        dest.writeString(eTag)
        dest.writeInt(duplicated)
        dest.writeInt(if (isConflicted) 1 else 0)
        dest.writeInt(if (isDeleted) 1 else 0)
        dest.writeInt(if (isSynced) 1 else 0)
    }

    enum class State {
        UNSYNCED, SYNCED, DELETED, CONFLICTED_UPDATE, CONFLICTED_DELETE
    }

    constructor() : this(-1, null, 0, false, false, false) {
        // UNSYNCED
    }

    private constructor(
        rowId: Long, eTag: String?,
        duplicated: Int, conflicted: Boolean, deleted: Boolean, synced: Boolean
    ) {
        this.rowId = rowId
        this.eTag = eTag
        this.duplicated = duplicated
        isConflicted = conflicted
        isDeleted = deleted
        isSynced = synced
    }

    protected constructor(source: Parcel) {
        rowId = source.readLong()
        eTag = source.readString()
        duplicated = source.readInt()
        isConflicted = source.readInt() != 0
        isDeleted = source.readInt() != 0
        isSynced = source.readInt() != 0
    }

    constructor(syncState: SyncState?, state: State) {
        var syncState = syncState
        Preconditions.checkNotNull(state)
        if (syncState == null) {
            syncState = SyncState()
        }
        rowId = syncState.rowId
        eTag = syncState.eTag
        duplicated = syncState.duplicated
        when (state) {
            State.UNSYNCED -> {
                isConflicted = syncState.isConflicted
                isDeleted = syncState.isDeleted
                isSynced = false
            }
            State.SYNCED -> {
                isConflicted = syncState.isConflicted
                isDeleted = false
                isSynced = true
            }
            State.DELETED -> {
                isConflicted = syncState.isConflicted
                isDeleted = true
                isSynced = false
            }
            State.CONFLICTED_UPDATE -> {
                // NOTE: Local record was updated and Cloud one was modified or deleted
                isConflicted = true
                isDeleted = false
                isSynced = false
            }
            State.CONFLICTED_DELETE -> {
                // NOTE: Local record was deleted and Cloud one was modified
                isConflicted = true
                isDeleted = true
                isSynced = false
            }
            else -> throw IllegalArgumentException("Unexpected state was provided [" + state.name + "]")
        }
    }

    constructor(state: State) : this(null as SyncState?, state) {}
    constructor(eTag: String?, duplicated: Int) {
        requireNotNull(eTag) { "Duplicate conflict state must be constructed with valid eTag" }
        require(duplicated > 0) { "Cannot setup duplicate conflict state for primary record" }
        rowId = -1
        this.eTag = eTag
        // CONFLICTED_DUPLICATE: CdS && duplicated > 0, duplicated = 0 resolved (otherState)
        this.duplicated = duplicated
        isConflicted = true
        isDeleted = false
        isSynced = true
    }

    constructor(eTag: String, state: State) : this(state) {
        this.eTag = Preconditions.checkNotNull(eTag)
    }// NOTE: lets DB maintain the default value

    // NOTE: rowId must not be here
    val contentValues: ContentValues
        get() {
            val values = ContentValues()

            // NOTE: rowId must not be here
            if (eTag != null) {
                // NOTE: lets DB maintain the default value
                values.put(BaseEntry.COLUMN_NAME_ETAG, eTag)
            }
            values.put(BaseEntry.COLUMN_NAME_DUPLICATED, duplicated)
            values.put(BaseEntry.COLUMN_NAME_CONFLICTED, isConflicted)
            values.put(BaseEntry.COLUMN_NAME_DELETED, isDeleted)
            values.put(BaseEntry.COLUMN_NAME_SYNCED, isSynced)
            return values
        }

    fun isDuplicated(): Boolean {
        return duplicated != 0
    }

    override fun hashCode(): Int {
        return Objects.hashCode(rowId, eTag, duplicated, isConflicted, isDeleted, isSynced)
    }

    companion object {
        val CREATOR: Parcelable.Creator<SyncState> = object : Parcelable.Creator<SyncState?> {
            override fun createFromParcel(source: Parcel): SyncState? {
                return SyncState(source)
            }

            override fun newArray(size: Int): Array<SyncState?> {
                return arrayOfNulls(size)
            }
        }

        fun from(cursor: Cursor): SyncState {
            val rowId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseEntry._ID))
            val eTag = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    BaseEntry.COLUMN_NAME_ETAG
                )
            )
            val duplicated = cursor.getInt(
                cursor.getColumnIndexOrThrow(
                    BaseEntry.COLUMN_NAME_DUPLICATED
                )
            )
            val conflicted = cursor.getInt(
                cursor.getColumnIndexOrThrow(
                    BaseEntry.COLUMN_NAME_CONFLICTED
                )
            ) == 1
            val deleted = cursor.getInt(
                cursor.getColumnIndexOrThrow(
                    BaseEntry.COLUMN_NAME_DELETED
                )
            ) == 1
            val synced = cursor.getInt(
                cursor.getColumnIndexOrThrow(
                    BaseEntry.COLUMN_NAME_SYNCED
                )
            ) == 1
            return SyncState(rowId, eTag, duplicated, conflicted, deleted, synced)
        }

        @JvmStatic
        fun from(values: ContentValues): SyncState {
            val rowId = values.getAsLong(BaseEntry._ID)
            val eTag = values.getAsString(BaseEntry.COLUMN_NAME_ETAG)
            val duplicated = values.getAsInteger(BaseEntry.COLUMN_NAME_DUPLICATED)
            val conflicted = values.getAsBoolean(BaseEntry.COLUMN_NAME_CONFLICTED)
            val deleted = values.getAsBoolean(BaseEntry.COLUMN_NAME_DELETED)
            val synced = values.getAsBoolean(BaseEntry.COLUMN_NAME_SYNCED)
            return SyncState(rowId, eTag, duplicated, conflicted, deleted, synced)
        }
    }
}