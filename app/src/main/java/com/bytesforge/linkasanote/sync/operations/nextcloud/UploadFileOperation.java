package com.bytesforge.linkasanote.sync.operations.nextcloud;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;

import org.apache.commons.httpclient.Header;

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

        EnhancedUploadRemoteFileOperation uploadOperation = new EnhancedUploadRemoteFileOperation(
                file.getLocalPath(), file.getRemotePath(), file.getMimeType());
        result = CloudDataSource.executeRemoteOperation(uploadOperation, ocClient).blockingGet();
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
        RemoteOperationResult result =
                CloudDataSource.executeRemoteOperation(existenceOperation, ocClient)
                        .blockingGet();
        if (result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
            CreateRemoteFolderOperation createOperation =
                    new CreateRemoteFolderOperation(remoteParent, true);
            result = CloudDataSource.executeRemoteOperation(createOperation, ocClient).blockingGet();
        }
        return result;
    }

    private class EnhancedUploadRemoteFileOperation extends UploadRemoteFileOperation {

        private OwnCloudClient ocClient;
        private String fileId;
        private String eTag;

        public EnhancedUploadRemoteFileOperation(
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
            ReadRemoteFileOperation operation = new ReadRemoteFileOperation(this.mRemotePath);
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
        public RemoteOperationResult execute(OwnCloudClient ocClient) {
            this.ocClient = ocClient;
            RemoteOperationResult result = super.execute(ocClient);
            Map<String, String> nextcloudHeaders =
                    extractNextcloudResponseHeaders(this.mPutMethod.getResponseHeaders());
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
