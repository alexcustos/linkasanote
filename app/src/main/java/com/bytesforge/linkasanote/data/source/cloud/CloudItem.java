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
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

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

    private boolean isOnline() {
        // NOTE: settings.isOnline is set by receiver, this class must not depend on it
        return CloudUtils.isApplicationConnected(context);
    }

    public Single<RemoteOperationResult> upload(@NonNull final T item) {
        return upload(item, null);
    }

    public Single<RemoteOperationResult> upload(
            @NonNull final T item, final OwnCloudClient ocClient) {
        checkNotNull(item);

        return Single.fromCallable(() -> {
            if (!isOnline()) return null;

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
                Files.asCharSink(localFile, Charsets.UTF_8).write(itemJson.toString());
            } catch (IOException e) {
                Log.e(TAG, "Cannot create temporary file [" + localPath + "]");
                return null;
            }
            final String remotePath = getRemotePath(item.getId());
            final JsonFile jsonFile = new JsonFile(localPath, remotePath);
            UploadFileOperation operation = new UploadFileOperation(jsonFile);
            RemoteOperationResult result =
                    CloudDataSource.executeRemoteOperation(operation, currentOcClient)
                            .blockingGet();
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
            if (!isOnline()) return null;

            OwnCloudClient currentOcClient = ocClient;
            if (currentOcClient == null) {
                currentOcClient = getDefaultOwnCloudClient();
            }
            if (currentOcClient == null) return null;

            final String remotePath = getRemotePath(itemId);
            final String localDirectory = CommonUtils.getTempDir(context);
            final String localPath = localDirectory + remotePath;

            DownloadFileRemoteOperation operation =
                    new DownloadFileRemoteOperation(remotePath, localDirectory);
            RemoteOperationResult result =
                    CloudDataSource.executeRemoteOperation(operation, currentOcClient)
                            .blockingGet();
            if (result.isSuccess()) {
                File localFile = new File(localPath);
                String jsonString = null;
                try {
                    jsonString = Files.asCharSource(localFile, Charsets.UTF_8).read();
                } catch (IOException e) {
                    Log.e(TAG, "Cannot read the file downloaded [" + localPath + "]");
                }
                if (!localFile.delete()) {
                    Log.e(TAG, "Item file was not deleted [" + localFile.getName() + "]");
                }
                if (jsonString == null) return null;

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
            if (!isOnline()) return null;

            OwnCloudClient currentOcClient = ocClient;
            if (currentOcClient == null) {
                currentOcClient = getDefaultOwnCloudClient();
            }
            if (currentOcClient == null) return null;

            final String remotePath = getRemotePath(itemId);
            RemoveFileRemoteOperation operation = new RemoveFileRemoteOperation(remotePath);
            RemoteOperationResult result =
                    CloudDataSource.executeRemoteOperation(operation, currentOcClient)
                            .blockingGet();
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
            if (!isOnline()) return null;

            OwnCloudClient currentOcClient = ocClient;
            if (currentOcClient == null) {
                currentOcClient = getDefaultOwnCloudClient();
            }
            if (currentOcClient == null) return null;

            final String remotePath = getRemotePath(itemId);
            final ReadFileRemoteOperation operation = new ReadFileRemoteOperation(remotePath);
            final RemoteOperationResult result =
                    CloudDataSource.executeRemoteOperation(operation, currentOcClient)
                            .blockingGet();
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
