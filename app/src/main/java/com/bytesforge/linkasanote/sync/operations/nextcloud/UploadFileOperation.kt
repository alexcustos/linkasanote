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

import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource
import com.bytesforge.linkasanote.sync.files.JsonFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import org.apache.commons.httpclient.Header
import org.apache.commons.httpclient.methods.PutMethod
import java.io.File

class UploadFileOperation(private val file: JsonFile) : RemoteOperation() {
    override fun run(ocClient: OwnCloudClient): RemoteOperationResult {
        val localFile = File(file.localPath!!)
        if (!localFile.exists()) {
            return RemoteOperationResult(RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND)
        }
        var result = createRemoteParent(file.remotePath!!, ocClient)
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
        remotePath: String,
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
            return if (nextcloudHeaders.isEmpty()) null else nextcloudHeaders
        }
    }

    companion object {
        private val TAG = UploadFileOperation::class.java.simpleName
        private val NEXTCLOUD_HEADER_PREFIX = "OC-".toLowerCase()
        private val NEXTCLOUD_FILE_ID_HEADER = NEXTCLOUD_HEADER_PREFIX + "FileId".toLowerCase()
        private val NEXTCLOUD_E_TAG_HEADER = NEXTCLOUD_HEADER_PREFIX + "ETag".toLowerCase()
    }

}