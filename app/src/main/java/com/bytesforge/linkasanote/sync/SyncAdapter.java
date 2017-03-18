package com.bytesforge.linkasanote.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.bytesforge.linkasanote.BuildConfig;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.cloud.CloudFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = SyncAdapter.class.getSimpleName();

    public static final String ACTION_SYNC_START = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_START";
    public static final String ACTION_SYNC_END = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_END";
    public static final String ACTION_SYNC_LINK = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_LINK";
    public static final String ACTION_SYNC_FAVORITE = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_FAVORITE";
    public static final String ACTION_SYNC_NOTE = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_NOTE";

    public static final String EXTRA_ACCOUNT_NAME = "ACCOUNT_NAME";
    public static final String EXTRA_ID = "ID";
    public static final String EXTRA_STATUS = "STATUS";

    public static final int STATUS_CREATED = 0;
    public static final int STATUS_UPDATED = 1;
    public static final int STATUS_DELETED = 2;

    private static final int NOTIFICATION_SYNC = 0;

    private final Context context;
    private final ContentResolver contentResolver;
    private final AccountManager accountManager;
    private final NotificationManagerCompat notificationManager;
    private final Resources resources;

    private Account account;
    private OwnCloudClient ocClient;
    private boolean dbAccessError;
    private int favoriteFailsCount; // NOTE: may be failed at different points, so global

    // NOTE: Note should contain linkId to notify related Link, Link contain noteIds just for integrity check
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.context = context;
        contentResolver = context.getContentResolver();
        accountManager = AccountManager.get(context);
        notificationManager = NotificationManagerCompat.from(context);
        resources = context.getResources();
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        this.context = context;
        contentResolver = context.getContentResolver();
        accountManager = AccountManager.get(context);
        notificationManager = NotificationManagerCompat.from(context);
        resources = context.getResources();
    }

    @Override
    public void onPerformSync(
            Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        this.account = account;
        dbAccessError = false;

        ocClient = CloudUtils.getOwnCloudClient(account, context);
        if (ocClient == null) {
            notifyFailedSynchronization(
                    resources.getString(R.string.sync_adapter_title_failed_login),
                    resources.getString(R.string.sync_adapter_text_failed_client));
            return;
        }

        sendSyncBroadcast(ACTION_SYNC_START);
        updateUserProfile(account, ocClient, accountManager);
        // Favorites
        final String favoriteDataSourceDirectory = CloudFavorites.getDataSourceDirectory(context);
        final String eTag = getDataSourceETag(ocClient, favoriteDataSourceDirectory, true);
        if (CloudFavorites.isCloudDataSourceChanged(context, eTag)) {
            Log.i(TAG, "Favorite DataSource has been changed [" + eTag + "]");
            favoriteFailsCount = 0;
            syncFavorites(ocClient, favoriteDataSourceDirectory);
        }
        // End
        if (!dbAccessError && favoriteFailsCount == 0) {
            CloudFavorites.updateLastSyncedETag(context, eTag);
        }
        sendSyncBroadcast(ACTION_SYNC_END);
        // Notifications
        if (dbAccessError) {
            notifyFailedSynchronization(
                    resources.getString(R.string.sync_adapter_title_failed_database),
                    resources.getString(R.string.sync_adapter_text_failed_db));
        }
        if (favoriteFailsCount > 0) {
            notifyFailedSynchronization(resources.getQuantityString(
                    R.plurals.sync_adapter_text_failed_favorites, favoriteFailsCount,
                    favoriteFailsCount));
        }
    }

    private void syncFavorites(OwnCloudClient ocClient, String dataSourceDirectory) {
        final Map<String, String> cloudDataSourceMap = getDataSourceMap(ocClient, dataSourceDirectory);
        if (cloudDataSourceMap == null) return;

        // Sync Local
        LocalFavorites.getFavorites(contentResolver, null, null, null).subscribe(favorite -> {
            String cloudETag = cloudDataSourceMap.get(favorite.getId());
            syncFavorite(favorite, cloudETag);
        }, throwable -> setDbAccessError(true));
        if (dbAccessError) return;

        // OPTIMIZATION: Map can be taken from previous iteration.
        final Set<String> localDataSourceSet = new HashSet<>();
        LocalFavorites.getFavoriteIds(contentResolver).subscribe(
                localDataSourceSet::add, throwable -> setDbAccessError(true));
        if (dbAccessError) return;

        // Load new records from cloud
        for (String cloudFavoriteId : cloudDataSourceMap.keySet()) {
            if (localDataSourceSet.contains(cloudFavoriteId)) continue;

            Favorite cloudFavorite =
                    CloudFavorites.downloadFavorite(cloudFavoriteId, ocClient, context);
            if (cloudFavorite == null) {
                favoriteFailsCount++;
                continue;
            }
            boolean notifyChanged = saveFavorite(cloudFavorite);
            if (notifyChanged) {
                sendSyncBroadcast(ACTION_SYNC_FAVORITE, cloudFavoriteId, STATUS_CREATED);
            }
        } // for
    }

    private void syncFavorite(Favorite favorite, String cloudETag) {
        String favoriteId = favorite.getId();
        String favoriteETag = favorite.getETag();
        boolean notifyChanged = false;
        int statusChanged = STATUS_UPDATED;
        if (favoriteETag == null) { // New
            // duplicated && conflicted can be ignored
            if (favorite.isDeleted()) {
                // DELETE local
                Log.e(TAG, "The records never synced must be deleted immediately [" + favoriteId + "]");
                notifyChanged = deleteLocalFavorite(favoriteId);
                statusChanged = STATUS_DELETED;
            } else { // synced is ignored (!synced)
                // UPLOAD
                notifyChanged = uploadFavorite(favorite);
            }
        } else if (favoriteETag.equals(cloudETag)) { // Green light
            // conflicted can be ignored
            if (!favorite.isSynced()) {
                if (favorite.isDeleted()) {
                    // DELETE cloud
                    notifyChanged = deleteCloudFavorite(favoriteId);
                    statusChanged = STATUS_DELETED;
                } else if (!favorite.isDuplicated()) {
                    // UPLOAD
                    notifyChanged = uploadFavorite(favorite);
                }
            }
        } else if (cloudETag == null) { // Was deleted on cloud
            if (favorite.isSynced()) {
                // DELETE local
                notifyChanged = deleteLocalFavorite(favoriteId);
                statusChanged = STATUS_DELETED;
            } else {
                // SET conflicted
                SyncState state = new SyncState(SyncState.State.CONFLICTED_DELETE);
                notifyChanged = updateFavorite(favoriteId, state);
            }
        } else { // Was changed on cloud
            // duplicated && conflicted can be ignored
            // DOWNLOAD (with synced state by default)
            Favorite cloudFavorite = CloudFavorites.downloadFavorite(
                    favoriteId, ocClient, context);
            if (cloudFavorite != null) {
                if (favorite.isSynced() && !favorite.isDeleted()) {
                    // SAVE local
                    notifyChanged = saveFavorite(cloudFavorite);
                } else { // !synced || deleted
                    if (favorite.equals(cloudFavorite)) {
                        if (favorite.isDeleted()) {
                            // DELETE cloud
                            notifyChanged = deleteCloudFavorite(favoriteId);
                            statusChanged = STATUS_DELETED;
                        } else {
                            // UPDATE state
                            assert cloudFavorite.getETag() != null;
                            SyncState state = new SyncState(
                                    cloudFavorite.getETag(), SyncState.State.SYNCED);
                            notifyChanged = updateFavorite(favoriteId, state);
                        }
                    } else {
                        // SET (or confirm) conflicted
                        SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                        notifyChanged = updateFavorite(favoriteId, state);
                    }
                }
            } else {
                favoriteFailsCount++;
            }
        }
        if (notifyChanged) {
            sendSyncBroadcast(ACTION_SYNC_FAVORITE, favoriteId, statusChanged);
        }
    }

    // NOTE: any cloud item can violate the DB constraints
    private boolean saveFavorite(final @NonNull Favorite favorite) {
        checkNotNull(favorite);

        // Primary record
        try {
            long rowId = LocalFavorites
                    .saveFavorite(contentResolver, favorite)
                    .blockingGet();
            return rowId > 0;
        } catch (NullPointerException e) {
            favoriteFailsCount++;
            return false;
        } catch (SQLiteConstraintException e) {
            // NOTE: will try to resolve it further
        }
        // Duplicated record
        int duplicated = LocalFavorites
                .getNextDuplicated(context, favorite.getName())
                .blockingGet();
        SyncState state = new SyncState(favorite.getETag(), duplicated);
        Favorite duplicatedFavorite = new Favorite(favorite, state);
        try {
            long rowId = LocalFavorites
                    .saveFavorite(contentResolver, duplicatedFavorite)
                    .blockingGet();
            return rowId > 0;
        } catch (NullPointerException | SQLiteConstraintException e) {
            favoriteFailsCount++;
        }
        return false;
    }

    private boolean deleteLocalFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        Log.i(TAG, favoriteId + ": DELETE local");

        int numRows = LocalFavorites
                .deleteFavorite(contentResolver, favoriteId)
                .blockingGet();
        return numRows == 1;
    }

    private boolean deleteCloudFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        Log.i(TAG, favoriteId + ": DELETE cloud");

        RemoteOperationResult result = CloudFavorites.deleteFavorite(favoriteId, ocClient, context);
        return result.isSuccess() && deleteLocalFavorite(favoriteId);
    }

    private boolean updateFavorite(@NonNull String favoriteId, @NonNull SyncState state) {
        checkNotNull(favoriteId);
        checkNotNull(state);
        Log.i(TAG, favoriteId + ": UPDATE");

        int numRows = LocalFavorites
                .updateFavorite(contentResolver, favoriteId, state)
                .blockingGet();
        return numRows == 1;
    }

    private boolean uploadFavorite(@NonNull Favorite favorite) {
        checkNotNull(favorite);
        Log.i(TAG, favorite.getId() + ": UPLOAD");

        boolean notifyChanged = false;
        RemoteOperationResult result = CloudFavorites.uploadFavorite(favorite, ocClient, context);
        if (result == null) {
            favoriteFailsCount++;
            return false;
        }
        if (result.isSuccess()) {
            JsonFile jsonFile = (JsonFile) result.getData().get(0);
            SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
            LocalFavorites
                    .updateFavorite(contentResolver, favorite.getId(), state)
                    .blockingGet();
        } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
            SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
            int numRows = LocalFavorites
                    .updateFavorite(contentResolver, favorite.getId(), state)
                    .blockingGet();
            notifyChanged = (numRows == 1);
        }
        return notifyChanged;
    }

    private static Map<String, String> getDataSourceMap(
            @NonNull OwnCloudClient ocClient, @NonNull String remotePath) {
        checkNotNull(ocClient);
        checkNotNull(remotePath);

        final Map<String, String> dataSourceMap = new HashMap<>();
        CloudDataSource.getRemoteFiles(ocClient, remotePath).subscribe(file -> {
            String fileMimeType = file.getMimeType();
            String fileRemotePath = file.getRemotePath();
            String id = JsonFile.getId(fileMimeType, fileRemotePath);
            // TODO: check file size and reject above reasonable limit
            if (id != null) {
                dataSourceMap.put(id, file.getEtag());
            } else {
                Log.w(TAG, "A problem was found in cloud dataSource "
                        + "[" + fileRemotePath + ", mimeType=" + fileMimeType + "]");
            }
        }, throwable -> {});
        return dataSourceMap;
    }

    private static void updateUserProfile(
            Account account, OwnCloudClient ocClient, AccountManager accountManager) {
        GetRemoteUserInfoOperation operation = new GetRemoteUserInfoOperation();
        RemoteOperationResult result = operation.execute(ocClient);
        if (result.isSuccess()) {
            UserInfo userInfo = (UserInfo) result.getData().get(0);
            accountManager.setUserData(
                    account, AccountUtils.Constants.KEY_DISPLAY_NAME, userInfo.getDisplayName());
        } else {
            Log.e(TAG, "Error while retrieving user info from server [" + result.getCode().name() + "]");
        }
    }

    private void sendSyncBroadcast(String action) {
        sendSyncBroadcast(action, null, -1);
    }

    private void sendSyncBroadcast(String action, String id, int status) {
        Intent intent = new Intent(action);

        intent.putExtra(EXTRA_ACCOUNT_NAME, account.name);
        if (id != null) intent.putExtra(EXTRA_ID, id);
        if (status >= 0) intent.putExtra(EXTRA_STATUS, status);

        context.sendBroadcast(intent);
    }

    private void notifyFailedSynchronization(@NonNull String text) {
        checkNotNull(text);
        notifyFailedSynchronization(null, text);
    }

    private void notifyFailedSynchronization(String title, @NonNull String text) {
        checkNotNull(text);

        //notificationManager.cancel(NOTIFICATION_SYNC);
        String defaultTitle = context.getString(R.string.sync_adapter_title_failed_default);
        String notificationTitle = title == null ? defaultTitle : defaultTitle + ": " + title;

        Notification notification = new NotificationCompat.Builder(context)
                // TODO: change to simplified application icon
                .setSmallIcon(R.drawable.ic_sync_white)
                .setLargeIcon(getLauncherBitmap(context))
                .setColor(context.getResources().getColor(R.color.color_primary, context.getTheme()))
                .setTicker(notificationTitle)
                .setContentTitle(notificationTitle)
                .setContentText(text)
                .build();
        notificationManager.notify(NOTIFICATION_SYNC, notification);
    }

    @Nullable
    private static String getDataSourceETag(
            OwnCloudClient ocClient, String remotePath, boolean createDataSource) {
        if (remotePath == null) return null;

        final ReadRemoteFileOperation readOperation = new ReadRemoteFileOperation(remotePath);
        RemoteOperationResult result = readOperation.execute(ocClient);
        if (result.isSuccess()) {
            RemoteFile file = (RemoteFile) result.getData().get(0);
            return file.getEtag();
        } else if (result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND
                && createDataSource) {
            CreateRemoteFolderOperation writeOperation =
                    new CreateRemoteFolderOperation(remotePath, true);
            result = writeOperation.execute(ocClient);
            if (result.isSuccess()) {
                Log.i(TAG, "New folder has been created");
                return getDataSourceETag(ocClient, remotePath, false);
            }
        }
        return null;
    }

    private static Bitmap getLauncherBitmap(@NonNull Context context) {
        Drawable logo = context.getDrawable(R.mipmap.ic_launcher);
        if (logo instanceof BitmapDrawable) {
            return ((BitmapDrawable) logo).getBitmap();
        }
        return null;
    }

    public void setDbAccessError(boolean dbAccessError) {
        this.dbAccessError = dbAccessError;
    }
}
