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

package com.bytesforge.linkasanote.sync.operations.nextcloud;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.PutMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

public class UploadFileOperation extends RemoteOperation {

    private static final String TAG = UploadFileOperation.class.getSimpleName();

    private static final String NEXTCLOUD_HEADER_PREFIX = "OC-".toLowerCase();
    private static final String NEXTCLOUD_FILE_ID_HEADER = NEXTCLOUD_HEADER_PREFIX + "FileId".toLowerCase();
    private static final String NEXTCLOUD_E_TAG_HEADER = NEXTCLOUD_HEADER_PREFIX + "ETag".toLowerCase();

    private JsonFile file;

    public UploadFileOperation(@NonNull JsonFile file) {
        this.file = checkNotNull(file);
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient ocClient) {
        File localFile = new File(file.getLocalPath());
        if (!localFile.exists()) {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND);
        }
        RemoteOperationResult result = createRemoteParent(file.getRemotePath(), ocClient);
        if (!result.isSuccess()) return result;

        EnhancedUploadFileRemoteOperation uploadOperation = new EnhancedUploadFileRemoteOperation(
                file.getLocalPath(), file.getRemotePath(), file.getMimeType());
        result = uploadOperation.execute(ocClient);
        ArrayList<Object> data = new ArrayList<>();
        data.add(file);
        result.setData(data);
        if (result.isSuccess()) {
            String eTag = uploadOperation.getETag();
            file.setETag(eTag);
        } else {
            file.setETag(null);
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.SYNC_CONFLICT);
        }
        return result;
    }

    private RemoteOperationResult createRemoteParent(String remotePath, OwnCloudClient ocClient) {
        String remoteParent = new File(remotePath).getParent();
        ExistenceCheckRemoteOperation existenceOperation =
                new ExistenceCheckRemoteOperation(remoteParent, false);
        RemoteOperationResult result = existenceOperation.execute(ocClient);
        if (result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
            CreateFolderRemoteOperation createOperation =
                    new CreateFolderRemoteOperation(remoteParent, true);
            result = createOperation.execute(ocClient);
        }
        return result;
    }

    private class EnhancedUploadFileRemoteOperation extends UploadFileRemoteOperation {

        private OwnCloudClient ocClient;
        private String fileId;
        private String eTag;

        public EnhancedUploadFileRemoteOperation(
                String localPath, String remotePath, String mimeType) {
            super(localPath, remotePath, mimeType, Long.toString(currentTimeMillis() / 1000));
        }

        @Nullable
        public String getETag() {
            if (eTag != null) return eTag;

            requestNextcloudFileAttributes();
            return eTag;
        }

        @Nullable
        public String getFileId() {
            if (fileId != null) return fileId;

            requestNextcloudFileAttributes();
            return fileId;
        }

        private void requestNextcloudFileAttributes() {
            ReadFileRemoteOperation operation = new ReadFileRemoteOperation(this.remotePath);
            RemoteOperationResult result =
                    CloudDataSource.executeRemoteOperation(operation, ocClient)
                            .blockingGet();
            if (result.isSuccess()) {
                RemoteFile file = (RemoteFile) result.getData().get(0);
                fileId = file.getRemoteId();
                eTag = file.getEtag();
            }
        }

        @Override
        public RemoteOperationResult execute(@NonNull OwnCloudClient ocClient) {
            checkNotNull(ocClient);

            this.ocClient = ocClient;
            RemoteOperationResult result = super.execute(ocClient);
            // TODO: make sure this replacement of this.putMethod is working well
            PutMethod putMethod = new PutMethod(
                    ocClient.getWebdavUri() + WebdavUtils.encodePath(this.remotePath));
            Map<String, String> nextcloudHeaders =
                    extractNextcloudResponseHeaders(putMethod.getResponseHeaders());
            if (nextcloudHeaders != null) {
                fileId = nextcloudHeaders.get(NEXTCLOUD_FILE_ID_HEADER);
                eTag = nextcloudHeaders.get(NEXTCLOUD_E_TAG_HEADER);
            }
            return result;
        }

        @Nullable
        private Map<String, String> extractNextcloudResponseHeaders(@Nullable Header[] headers) {
            if (headers == null) return null;

            Map<String, String> nextcloudHeaders = new HashMap<>();
            for (Header header : headers) {
                String name = header.getName().toLowerCase();
                if (name.startsWith(NEXTCLOUD_HEADER_PREFIX)) {
                    String value = header.getValue().replaceAll("^\"|\"$", "");
                    nextcloudHeaders.put(name, value);
                }
            }
            return nextcloudHeaders.size() <= 0 ? null : nextcloudHeaders;
        }
    }
}
