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
package com.bytesforge.linkasanote.sync.operations.nextcloud

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
import com.google.common.base.Preconditions
import org.apache.commons.httpclient.Header
import org.apache.commons.httpclient.methods.PutMethod
import java.io.File
import java.util.ArrayList
import java.util.HashMap

class UploadFileOperation(file: JsonFile) : RemoteOperation() {
    private val file: JsonFile
    override fun run(ocClient: OwnCloudClient): RemoteOperationResult {
        val localFile = File(file.localPath)
        if (!localFile.exists()) {
            return RemoteOperationResult(RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND)
        }
        var result = createRemoteParent(file.remotePath, ocClient)
        if (!result.isSuccess) return result
        val uploadOperation = EnhancedUploadFileRemoteOperation(
            file.localPath, file.remotePath, file.mimeType
        )
        result = uploadOperation.execute(ocClient)
        val data = ArrayList<Any>()
        data.add(file)
        result.data = data
        if (result.isSuccess) {
            val eTag = uploadOperation.getETag()
            file.eTag = eTag
        } else {
            file.eTag = null
            return RemoteOperationResult(RemoteOperationResult.ResultCode.SYNC_CONFLICT)
        }
        return result
    }

    private fun createRemoteParent(
        remotePath: String?,
        ocClient: OwnCloudClient
    ): RemoteOperationResult {
        val remoteParent = File(remotePath).parent
        val existenceOperation = ExistenceCheckRemoteOperation(remoteParent, false)
        var result = existenceOperation.execute(ocClient)
        if (result.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
            val createOperation = CreateFolderRemoteOperation(remoteParent, true)
            result = createOperation.execute(ocClient)
        }
        return result
    }

    private inner class EnhancedUploadFileRemoteOperation(
        localPath: String?, remotePath: String?, mimeType: String?
    ) : UploadFileRemoteOperation(
        localPath, remotePath, mimeType, java.lang.Long.toString(
            System.currentTimeMillis() / 1000
        )
    ) {
        private var ocClient: OwnCloudClient? = null
        private var fileId: String? = null
        private var eTag: String? = null
        fun getETag(): String? {
            if (eTag != null) return eTag
            requestNextcloudFileAttributes()
            return eTag
        }

        fun getFileId(): String? {
            if (fileId != null) return fileId
            requestNextcloudFileAttributes()
            return fileId
        }

        private fun requestNextcloudFileAttributes() {
            val operation = ReadFileRemoteOperation(remotePath)
            val result = CloudDataSource.executeRemoteOperation(operation, ocClient!!)
                .blockingGet()
            if (result.isSuccess) {
                val file = result.data[0] as RemoteFile
                fileId = file.remoteId
                eTag = file.etag
            }
        }

        override fun execute(ocClient: OwnCloudClient): RemoteOperationResult {
            Preconditions.checkNotNull(ocClient)
            this.ocClient = ocClient
            val result = super.execute(ocClient)
            // TODO: make sure this replacement of this.putMethod is working well
            val putMethod = PutMethod(
                ocClient.webdavUri.toString() + WebdavUtils.encodePath(
                    remotePath
                )
            )
            val nextcloudHeaders = extractNextcloudResponseHeaders(putMethod.responseHeaders)
            if (nextcloudHeaders != null) {
                fileId = nextcloudHeaders[NEXTCLOUD_FILE_ID_HEADER]
                eTag = nextcloudHeaders[NEXTCLOUD_E_TAG_HEADER]
            }
            return result
        }

        private fun extractNextcloudResponseHeaders(headers: Array<Header>?): Map<String, String>? {
            if (headers == null) return null
            val nextcloudHeaders: MutableMap<String, String> = HashMap()
            for (header in headers) {
                val name = header.name.toLowerCase()
                if (name.startsWith(NEXTCLOUD_HEADER_PREFIX)) {
                    val value = header.value.replace("^\"|\"$".toRegex(), "")
                    nextcloudHeaders[name] = value
                }
            }
            return if (nextcloudHeaders.size <= 0) null else nextcloudHeaders
        }
    }

    companion object {
        private val TAG = UploadFileOperation::class.java.simpleName
        private val NEXTCLOUD_HEADER_PREFIX = "OC-".toLowerCase()
        private val NEXTCLOUD_FILE_ID_HEADER = NEXTCLOUD_HEADER_PREFIX + "FileId".toLowerCase()
        private val NEXTCLOUD_E_TAG_HEADER = NEXTCLOUD_HEADER_PREFIX + "ETag".toLowerCase()
    }

    init {
        this.file = Preconditions.checkNotNull(file)
    }
}