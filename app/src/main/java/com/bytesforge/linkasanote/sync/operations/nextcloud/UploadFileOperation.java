package com.bytesforge.linkasanote.sync.operations.nextcloud;

import android.accounts.Account;
import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

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
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class UploadFileOperation extends RemoteOperation {

    private static final String NEXTCLOUD_HEADER_PREFIX = "OC-".toLowerCase();
    private static final String NEXTCLOUD_ETAG_HEADER = NEXTCLOUD_HEADER_PREFIX + "ETag".toLowerCase();
    private static final String TAG = UploadFileOperation.class.getSimpleName();

    private Account account;
    private JsonFile file;
    private ContentResolver contentResolver;

    @NonNull
    public static JsonFile createJsonFile(
            @NonNull Uri uri, @NonNull String localPath, @NonNull String remotePath) {
        checkNotNull(localPath);
        checkNotNull(remotePath);

        JsonFile file = new JsonFile(remotePath);
        file.setUri(uri);
        file.setLocalPath(localPath);

        File localFile = new File(localPath);
        file.setLength(localFile.length());
        return file;
    }

    public UploadFileOperation(
            @NonNull Account account, @NonNull JsonFile file,
            @NonNull ContentResolver contentResolver) {
        this.account = checkNotNull(account);
        this.file = checkNotNull(file);
        this.contentResolver = checkNotNull(contentResolver);
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        File localFile = new File(file.getLocalPath());
        if (!localFile.exists()) {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND);
        }
        RemoteOperationResult result = createRemoteParent(file.getRemotePath(), client);
        if (!result.isSuccess()) return result;

        EnhancedUploadRemoteFileOperation uploadOperation = new EnhancedUploadRemoteFileOperation(
                file.getLocalPath(), file.getRemotePath(), file.getMimeType());
        result = uploadOperation.execute(client);
        if (result.isSuccess()) {
            file.setETag(uploadOperation.getETag());
            file.setSyncedState();
            int rowsUpdated = contentResolver.update(
                    file.getUri(), file.getUpdateValues(), null, null);
            if (rowsUpdated != 1) {
                Log.d(TAG, "Unexpected number of rows were updated [" + rowsUpdated + "]");
            }
            boolean isDeleted = localFile.delete();
            if (!isDeleted) {
                Log.e(TAG, "Temporary file was not deleted [" + localFile.getName() + "]");
            }
        } else {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.SYNC_CONFLICT);
        }
        return result;
    }

    private RemoteOperationResult createRemoteParent(String remotePath, OwnCloudClient client) {
        String remoteParent = new File(remotePath).getParent();
        ExistenceCheckRemoteOperation existenceOperation =
                new ExistenceCheckRemoteOperation(remoteParent, false);
        RemoteOperationResult result = existenceOperation.execute(client);

        if (result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
            CreateRemoteFolderOperation createOperation =
                    new CreateRemoteFolderOperation(remoteParent, true);
            result = createOperation.execute(client);
        }
        return result;
    }

    public Account getAccount() {
        return account;
    }

    private class EnhancedUploadRemoteFileOperation extends UploadRemoteFileOperation {

        private Map<String, String> nextcloudHeaders = new HashMap<>();
        private OwnCloudClient client;
        private String eTag;

        public EnhancedUploadRemoteFileOperation(String localPath, String remotePath, String mimeType) {
            super(localPath, remotePath, mimeType);
        }

        @Nullable
        public String getETag() {
            if (eTag != null) return eTag;

            if (nextcloudHeaders != null) {
                eTag = nextcloudHeaders.get(NEXTCLOUD_ETAG_HEADER);
            }
            if (eTag == null) {
                ReadRemoteFileOperation operation = new ReadRemoteFileOperation(this.mRemotePath);
                RemoteOperationResult result = operation.execute(client);
                if (result.isSuccess()) {
                    RemoteFile file = (RemoteFile) result.getData().get(0);
                    eTag = file.getEtag();
                }
            }
            return eTag;
        }

        @Override
        public RemoteOperationResult execute(OwnCloudClient client) {
            this.client = client;
            RemoteOperationResult result = super.execute(client);
            nextcloudHeaders = extractNextcloudResponseHeaders(this.mPutMethod.getResponseHeaders());
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
    } // class EnhancedUploadRemoteFileOperation
}
