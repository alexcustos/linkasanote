package com.bytesforge.linkasanote.data.source.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.data.Favorite;
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
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class CloudFavorites {

    private static final String TAG = CloudFavorites.class.getSimpleName();

    private static final String CLOUD_DIRECTORY = JsonFile.PATH_SEPARATOR + Favorite.CLOUD_DIRECTORY;
    private static final String SETTING_LAST_SYNCED_ETAG = "FAVORITES_LAST_SYNCED_ETAG";

    private final Context context;

    public CloudFavorites(@NonNull Context context) {
        this.context = checkNotNull(context);
    }

    @Nullable
    public RemoteOperationResult uploadFavorite(
            @NonNull final Favorite favorite, @NonNull final OwnCloudClient ocClient) {
        checkNotNull(favorite);
        checkNotNull(ocClient);

        final JSONObject favoriteJson = favorite.getJsonObject();
        if (favoriteJson == null) {
            Log.e(TAG, "Favorite cannot be saved, it's probably empty");
            return null;
        }
        final String localPath = CommonUtils.getTempDir(context) + File.separator +
                JsonFile.getTempFileName(favorite.getId());
        final File localFile = new File(localPath);
        try {
            Files.write(favoriteJson.toString(), localFile, Charsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Cannot create temporary file [" + localPath + "]");
            return null;
        }
        final String remotePath = getRemotePath(favorite.getId());
        final JsonFile jsonFile = new JsonFile(localPath, remotePath);
        UploadFileOperation operation = new UploadFileOperation(jsonFile);
        RemoteOperationResult result = operation.execute(ocClient);
        if (!localFile.delete()) {
            Log.e(TAG, "Temporary file was not deleted [" + localFile.getName() + "]");
        }
        return result;
    }

    @Nullable
    public Favorite downloadFavorite(
            @NonNull final String favoriteId, @NonNull final OwnCloudClient ocClient) {
        checkNotNull(favoriteId);
        checkNotNull(ocClient);

        final String remotePath = getRemotePath(favoriteId);
        final String localDirectory = CommonUtils.getTempDir(context);
        final String localPath = localDirectory + remotePath;

        DownloadRemoteFileOperation operation =
                new DownloadRemoteFileOperation(remotePath, localDirectory);
        RemoteOperationResult result = operation.execute(ocClient);
        if (result.isSuccess()) {
            File localFile = new File(localPath);
            List<String> jsonStringList = null;
            try {
                jsonStringList = Files.readLines(localFile, Charsets.UTF_8);
            } catch (IOException e) {
                Log.e(TAG, "Cannot read the file downloaded [" + localPath + "]");
            }
            if (!localFile.delete()) {
                Log.e(TAG, "Favorite file was not deleted [" + localFile.getName() + "]");
            }
            if (jsonStringList == null) return null;

            String jsonString = jsonStringList.stream().map(Object::toString)
                    .collect(Collectors.joining());
            SyncState state = new SyncState(operation.getEtag(), SyncState.State.SYNCED);
            Favorite favorite = Favorite.from(jsonString, state);
            if (favorite == null || favorite.isEmpty() || !favoriteId.equals(favorite.getId())) {
                Log.e(TAG, "The favorite downloaded cannot be validated [" + favoriteId + "]");
                return null;
            }
            return favorite;
        }
        return null;
    }

    @NonNull
    public RemoteOperationResult deleteFavorite(
            @NonNull final String favoriteId, @NonNull final OwnCloudClient ocClient) {
        checkNotNull(favoriteId);
        checkNotNull(ocClient);

        final String remotePath = getRemotePath(favoriteId);
        RemoveRemoteFileOperation operation = new RemoveRemoteFileOperation(remotePath);
        RemoteOperationResult result = operation.execute(ocClient);
        if (result.isSuccess()
                || result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
        }
        return result;
    }

    public String getRemoteFileName(@NonNull final String favoriteId) {
        return JsonFile.getFileName(checkNotNull(favoriteId));
    }

    public String getDataSourceDirectory() {
        return CloudUtils.getSyncDirectory(context) + CLOUD_DIRECTORY;
    }

    public String getRemotePath(@NonNull final String favoriteId) {
        checkNotNull(favoriteId);

        return getDataSourceDirectory() + JsonFile.PATH_SEPARATOR + getRemoteFileName(favoriteId);
    }

    public boolean isCloudDataSourceChanged(@NonNull final String eTag) {
        checkNotNull(eTag);

        String lastSyncedETag = getLastSyncedETag();
        return lastSyncedETag == null || !lastSyncedETag.equals(eTag);
    }

    @Nullable
    public String getLastSyncedETag() {
        SharedPreferences sharedPreferences = CloudUtils.getCloudSharedPreferences(context);
        return sharedPreferences.getString(SETTING_LAST_SYNCED_ETAG, null);
    }

    public synchronized void updateLastSyncedETag(@NonNull final String eTag) {
        checkNotNull(eTag);

        SharedPreferences sharedPreferences = CloudUtils.getCloudSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SETTING_LAST_SYNCED_ETAG, eTag);
        editor.apply();
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
}
