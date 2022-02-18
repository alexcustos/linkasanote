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
package com.bytesforge.linkasanote.sync.files

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
import android.net.Uri
import com.google.common.base.Objects
import com.google.common.base.Preconditions
import java.io.File

class JsonFile : Parcelable, Comparable<JsonFile> {
    private var length: Long
    var localPath: String?

    // Getters & Setters
    var remotePath: String?
        private set
    var eTag: String?

    constructor(remotePath: String) {
        Preconditions.checkNotNull(remotePath)
        require(remotePath.startsWith(PATH_SEPARATOR)) { "Remote path must be absolute [$remotePath]" }
        length = 0
        localPath = null
        this.remotePath = remotePath
        eTag = null
    }

    constructor(localPath: String, remotePath: String) : this(remotePath) {
        Preconditions.checkNotNull(localPath)
        this.localPath = localPath
        val localFile = File(localPath)
        length = localFile.length()
    }

    protected constructor(`in`: Parcel) {
        length = `in`.readLong()
        localPath = `in`.readString()
        remotePath = `in`.readString()
        eTag = `in`.readString()
    }

    override fun describeContents(): Int {
        return super.hashCode()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(length)
        dest.writeString(localPath)
        dest.writeString(remotePath)
        dest.writeString(eTag)
    }

    override fun compareTo(obj: JsonFile): Int {
        return remotePath!!.compareTo(obj.remotePath!!, ignoreCase = true)
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null || javaClass != obj.javaClass) return false
        val file = obj as JsonFile
        return (Objects.equal(remotePath, file.remotePath)
                && Objects.equal(length, file.length))
    }

    override fun hashCode(): Int {
        return Objects.hashCode(length, localPath, remotePath, eTag)
    }

    fun setLength(length: Long) {
        this.length = length
    }

    companion object {
        val mimeType = "application/json"
            get() = Companion.field
        private const val FILE_EXTENSION = ".json"
        const val PATH_SEPARATOR = "/"
        val CREATOR: Parcelable.Creator<JsonFile> = object : Parcelable.Creator<JsonFile?> {
            override fun createFromParcel(`in`: Parcel): JsonFile? {
                return JsonFile(`in`)
            }

            override fun newArray(size: Int): Array<JsonFile?> {
                return arrayOfNulls(size)
            }
        }

        @JvmStatic
        fun getFileName(id: String): String {
            return Preconditions.checkNotNull(id).toString() + FILE_EXTENSION
        }

        @JvmStatic
        fun getTempFileName(id: String): String {
            return Preconditions.checkNotNull(id).toString() + "." + CloudUtils.getApplicationId()
        }

        @JvmStatic
        fun getId(mimeType: String?, filePath: String?): String? {
            if (mimeType == null || filePath == null) return null
            if (mimeType != Companion.mimeType) return null
            var id = Uri.parse(filePath).lastPathSegment
            id = if (id != null && id.endsWith(FILE_EXTENSION)) {
                id.substring(0, id.length - FILE_EXTENSION.length)
            } else {
                return null
            }
            return if (UuidUtils.isKeyValidUuid(id)) {
                id
            } else null
        }
    }
}