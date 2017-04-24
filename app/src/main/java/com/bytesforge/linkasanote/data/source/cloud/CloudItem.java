package com.bytesforge.linkasanote.data.source.cloud;

import android.accounts.AccountManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.data.Item;
import com.bytesforge.linkasanote.data.ItemFactory;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.sync.operations.nextcloud.UploadFileOperation;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.DownloadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class CloudItem<T extends Item> {

    private static final String TAG = CloudItem.class.getSimpleName();

    private final Context context;
    private final AccountManager accountManager;
    private final Settings settings;
    private final String cloudDirectory;
    private final String settingLastSyncedETag;
    private final ItemFactory<T> factory;

    public CloudItem(
            @NonNull Context context, @NonNull AccountManager accountManager,
            @NonNull Settings settings, @NonNull String cloudDirectory,
            @NonNull String settingLastSyncedETag, @NonNull ItemFactory<T> factory) {
        this.context = checkNotNull(context);
        this.accountManager = checkNotNull(accountManager);
        this.settings = checkNotNull(settings);
        this.cloudDirectory = checkNotNull(cloudDirectory);
        this.settingLastSyncedETag = checkNotNull(settingLastSyncedETag);
        this.factory = factory;
    }

    public Single<RemoteOperationResult> upload(@NonNull final T item) {
        return upload(item, null);
    }

    public Single<RemoteOperationResult> upload(
            @NonNull final T item, final OwnCloudClient ocClient) {
        checkNotNull(item);

        return Single.fromCallable(() -> {
            if (!CloudUtils.isApplicationConnected(context)) return null;

            OwnCloudClient currentOcClient = ocClient;
            if (currentOcClient == null) {
                currentOcClient = getDefaultOwnCloudClient();
            }
            if (currentOcClient == null) return null;

            final JSONObject itemJson = item.getJsonObject();
            if (itemJson == null) {
                Log.e(TAG, "Item cannot be saved, it's probably empty");
                return null;
            }
            final String localPath = CommonUtils.getTempDir(context) + File.separator +
                    JsonFile.getTempFileName(item.getId());
            final File localFile = new File(localPath);
            try {
                Files.write(itemJson.toString(), localFile, Charsets.UTF_8);
            } catch (IOException e) {
                Log.e(TAG, "Cannot create temporary file [" + localPath + "]");
                return null;
            }
            final String remotePath = getRemotePath(item.getId());
            final JsonFile jsonFile = new JsonFile(localPath, remotePath);
            UploadFileOperation operation = new UploadFileOperation(jsonFile);
            RemoteOperationResult result = operation.execute(currentOcClient);
            if (!localFile.delete()) {
                Log.e(TAG, "Temporary file was not deleted [" + localFile.getName() + "]");
            }
            return result;
        });
    }

    public Single<T> download(@NonNull final String itemId) {
        return download(itemId, null);
    }

    public Single<T> download(
            @NonNull final String itemId, final OwnCloudClient ocClient) {
        checkNotNull(itemId);
        return Single.fromCallable(() -> {
            if (!CloudUtils.isApplicationConnected(context)) return null;

            OwnCloudClient currentOcClient = ocClient;
            if (currentOcClient == null) {
                currentOcClient = getDefaultOwnCloudClient();
            }
            if (currentOcClient == null) return null;

            final String remotePath = getRemotePath(itemId);
            final String localDirectory = CommonUtils.getTempDir(context);
            final String localPath = localDirectory + remotePath;

            DownloadRemoteFileOperation operation =
                    new DownloadRemoteFileOperation(remotePath, localDirectory);
            RemoteOperationResult result = operation.execute(currentOcClient);
            if (result.isSuccess()) {
                File localFile = new File(localPath);
                List<String> jsonStringList = null;
                try {
                    jsonStringList = Files.readLines(localFile, Charsets.UTF_8);
                } catch (IOException e) {
                    Log.e(TAG, "Cannot read the file downloaded [" + localPath + "]");
                }
                if (!localFile.delete()) {
                    Log.e(TAG, "Item file was not deleted [" + localFile.getName() + "]");
                }
                if (jsonStringList == null) return null;

                String jsonString = jsonStringList.stream().map(Object::toString)
                        .collect(Collectors.joining());
                SyncState state = new SyncState(operation.getEtag(), SyncState.State.SYNCED);
                T item = factory.from(jsonString, state);
                if (item == null || item.isEmpty() || !itemId.equals(item.getId())) {
                    Log.e(TAG, "The Item downloaded cannot be validated [" + itemId + "]");
                    return null;
                }
                return item;
            } else if (result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
                throw new NoSuchElementException("The requested Item was not found [" + itemId + "]");
            }
            return null;
        });
    }

    public Single<RemoteOperationResult> delete(@NonNull final String itemId) {
        return delete(itemId, null);
    }

    public Single<RemoteOperationResult> delete(
            @NonNull final String itemId, final OwnCloudClient ocClient) {
        checkNotNull(itemId);
        return Single.fromCallable(() -> {
            if (!CloudUtils.isApplicationConnected(context)) return null;

            OwnCloudClient currentOcClient = ocClient;
            if (currentOcClient == null) {
                currentOcClient = getDefaultOwnCloudClient();
            }
            if (currentOcClient == null) return null;

            final String remotePath = getRemotePath(itemId);
            RemoveRemoteFileOperation operation = new RemoveRemoteFileOperation(remotePath);
            RemoteOperationResult result = operation.execute(currentOcClient);
            if (result.isSuccess()
                    || result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
            }
            return result;
        });
    }

    public Single<RemoteFile> readFile(@NonNull final String itemId) {
        checkNotNull(itemId);
        return readFile(itemId, null);
    }

    private Single<RemoteFile> readFile(
            @NonNull final String itemId, final OwnCloudClient ocClient) {
        checkNotNull(itemId);
        return Single.fromCallable(() -> {
            if (!CloudUtils.isApplicationConnected(context)) return null;

            OwnCloudClient currentOcClient = ocClient;
            if (currentOcClient == null) {
                currentOcClient = getDefaultOwnCloudClient();
            }
            if (currentOcClient == null) return null;

            final String remotePath = getRemotePath(itemId);
            final ReadRemoteFileOperation operation = new ReadRemoteFileOperation(remotePath);
            final RemoteOperationResult result = operation.execute(currentOcClient);
            if (result.isSuccess()) {
                return (RemoteFile) result.getData().get(0);
            } else if (result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
                throw new NoSuchElementException("The requested Item file was not found [" + itemId + "]");
            }
            return null;
        });
    }

    public boolean isCloudDataSourceChanged(@NonNull final String eTag) {
        checkNotNull(eTag);
        String lastSyncedETag = settings.getLastSyncedETag(settingLastSyncedETag);
        return lastSyncedETag == null || !lastSyncedETag.equals(eTag);
    }

    public void updateLastSyncedETag(@NonNull final String eTag) {
        checkNotNull(eTag);
        settings.setLastSyncedETag(settingLastSyncedETag, eTag);
    }

    @Nullable
    public String getDataSourceETag(@NonNull OwnCloudClient ocClient) {
        checkNotNull(ocClient);
        return CloudDataSource.getDataSourceETag(ocClient, getDataSourceDirectory(), true);
    }

    @NonNull
    public Map<String, String> getDataSourceMap(@NonNull OwnCloudClient ocClient) {
        checkNotNull(ocClient);
        return CloudDataSource.getDataSourceMap(ocClient, getDataSourceDirectory());
    }

    private String getRemotePath(@NonNull final String itemId) {
        checkNotNull(itemId);
        return getDataSourceDirectory() + JsonFile.PATH_SEPARATOR + getRemoteFileName(itemId);
    }

    private String getDataSourceDirectory() {
        String syncDirectory = settings.getSyncDirectory();
        return cloudDirectory.startsWith(JsonFile.PATH_SEPARATOR)
                ? syncDirectory + cloudDirectory
                : syncDirectory + JsonFile.PATH_SEPARATOR + cloudDirectory;
    }

    private String getRemoteFileName(@NonNull final String itemId) {
        checkNotNull(itemId);
        return JsonFile.getFileName(checkNotNull(itemId));
    }

    private OwnCloudClient getDefaultOwnCloudClient() {
        return CloudUtils.getDefaultOwnCloudClient(context, accountManager);
    }
}