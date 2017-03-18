package com.bytesforge.linkasanote.data.source.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.sync.operations.nextcloud.UploadFileOperation;
import com.bytesforge.linkasanote.utils.CloudUtils;
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
import java.util.stream.Collectors;

import io.reactivex.annotations.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

public final class CloudFavorites {

    private static final String TAG = CloudFavorites.class.getSimpleName();

    private static final String CLOUD_DIRECTORY = JsonFile.PATH_SEPARATOR + Favorite.CLOUD_DIRECTORY;
    private static final String SETTING_LAST_SYNCED_ETAG = "FAVORITES_LAST_SYNCED_ETAG";

    private CloudFavorites() {
    }

    @Nullable
    public static RemoteOperationResult uploadFavorite(
            @NonNull final Favorite favorite, @NonNull final OwnCloudClient ocClient,
            @NonNull final Context context) {
        checkNotNull(favorite);
        checkNotNull(ocClient);
        checkNotNull(context);

        final JSONObject favoriteJson = favorite.getJsonObject();
        if (favoriteJson == null) {
            Log.e(TAG, "Favorite cannot be saved, it's probably empty");
            return null;
        }
        final String localPath = context.getCacheDir().getAbsolutePath() + File.separator +
                JsonFile.getTempFileName(favorite.getId());
        final File localFile = new File(localPath);
        try {
            Files.write(favoriteJson.toString(), localFile, Charsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Cannot create temporary file [" + localPath + "]");
            return null;
        }
        final String remotePath = CloudFavorites.getRemotePath(context, favorite.getId());
        final JsonFile jsonFile = new JsonFile(localPath, remotePath);
        UploadFileOperation operation = new UploadFileOperation(jsonFile);
        RemoteOperationResult result = operation.execute(ocClient);
        if (!localFile.delete()) {
            Log.e(TAG, "Temporary file was not deleted [" + localFile.getName() + "]");
        }
        return result;
    }

    @Nullable
    public static Favorite downloadFavorite(
            @NonNull final String favoriteId, @NonNull final OwnCloudClient ocClient,
            @NonNull final Context context) {
        checkNotNull(favoriteId);
        checkNotNull(ocClient);
        checkNotNull(context);

        final String remotePath = getRemotePath(context, favoriteId);
        final String localDirectory = context.getCacheDir().getAbsolutePath();
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
    public static RemoteOperationResult deleteFavorite(
            @NonNull final String favoriteId, @NonNull final OwnCloudClient ocClient,
            @NonNull final Context context) {
        checkNotNull(favoriteId);
        checkNotNull(ocClient);
        checkNotNull(context);

        final String remotePath = CloudFavorites.getRemotePath(context, favoriteId);
        RemoveRemoteFileOperation operation = new RemoveRemoteFileOperation(remotePath);
        RemoteOperationResult result = operation.execute(ocClient);
        if (result.isSuccess()
                || result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
        }
        return result;
    }

    public static String getRemoteFileName(@NonNull final String favoriteId) {
        checkNotNull(favoriteId);
        return JsonFile.getFileName(favoriteId);
    }

    public static String getDataSourceDirectory(@NonNull final Context context) {
        checkNotNull(context);
        return CloudUtils.getSyncDirectory(context) + CLOUD_DIRECTORY;
    }

    public static String getRemotePath(
            @NonNull final Context context, @NonNull final String favoriteId) {
        checkNotNull(context);
        checkNotNull(favoriteId);

        return getDataSourceDirectory(context) + JsonFile.PATH_SEPARATOR +
                getRemoteFileName(favoriteId);
    }

    public synchronized static boolean isCloudDataSourceChanged(
            @NonNull final Context context, @NonNull final String eTag) {
        checkNotNull(context);
        checkNotNull(eTag);

        String lastSyncedETag = getLastSyncedETag(context);
        return lastSyncedETag == null || !lastSyncedETag.equals(eTag);
    }

    public static String getLastSyncedETag(@NonNull final Context context) {
        checkNotNull(context);

        SharedPreferences sharedPreferences = CloudUtils.getCloudSharedPreferences(context);
        return sharedPreferences.getString(SETTING_LAST_SYNCED_ETAG, null);
    }

    public static void updateLastSyncedETag(
            @NonNull final Context context, @NonNull final String eTag) {
        checkNotNull(context);
        checkNotNull(eTag);

        SharedPreferences sharedPreferences = CloudUtils.getCloudSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SETTING_LAST_SYNCED_ETAG, eTag);
        editor.apply();
    }
}
