package com.bytesforge.linkasanote.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.cloud.CloudFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = SyncAdapter.class.getSimpleName();
    public static final int LAST_SYNC_STATUS_UNKNOWN = 0;
    public static final int LAST_SYNC_STATUS_SUCCESS = 1;
    public static final int LAST_SYNC_STATUS_ERROR = 2;
    public static final int LAST_SYNC_STATUS_CONFLICT = 3;

    private final Context context;
    private final Settings settings;
    private final SyncNotifications syncNotifications;
    private final LocalFavorites localFavorites;
    private final CloudFavorites cloudFavorites;
    private final AccountManager accountManager;
    private final Resources resources;

    private OwnCloudClient ocClient;
    private boolean dbAccessError;
    private boolean favoriteEmptySource;
    private int favoriteFailsCount; // NOTE: may be failed at different points, so global

    // NOTE: Note should contain linkId to notify related Link, Link contain noteIds just for integrity check
    public SyncAdapter(
            Context context, Settings settings, boolean autoInitialize,
            AccountManager accountManager, SyncNotifications syncNotifications,
            LocalFavorites localFavorites, CloudFavorites cloudFavorites) {
        super(context, autoInitialize);
        this.context = context;
        this.settings = settings;
        this.accountManager = accountManager;
        this.syncNotifications = syncNotifications;
        this.localFavorites = localFavorites;
        this.cloudFavorites = cloudFavorites;
        resources = context.getResources();
    }

    @Override
    public void onPerformSync(
            Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        dbAccessError = false;
        syncNotifications.setAccountName(CloudUtils.getAccountName(account));

        ocClient = CloudUtils.getOwnCloudClient(account, context);
        if (ocClient == null) {
            syncNotifications.notifyFailedSynchronization(
                    resources.getString(R.string.sync_adapter_title_failed_login),
                    resources.getString(R.string.sync_adapter_text_failed_login));
            return;
        }
        //Start
        syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC, SyncNotifications.STATUS_SYNC_START);
        syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC_FAVORITES, SyncNotifications.STATUS_SYNC_START);
        CloudUtils.updateUserProfile(account, ocClient, accountManager);
        // Favorites
        favoriteEmptySource = false;
        final String favoritesDataStorageETag = cloudFavorites.getDataSourceETag(ocClient);
        if (favoritesDataStorageETag == null) {
            syncNotifications.notifyFailedSynchronization(
                    resources.getString(R.string.sync_adapter_title_failed_cloud),
                    resources.getString(R.string.sync_adapter_text_failed_cloud));
            return;
        }
        favoriteFailsCount = 0;
        boolean isFavoriteCloudChanged =
                cloudFavorites.isCloudDataSourceChanged(favoritesDataStorageETag);
        syncFavorites(isFavoriteCloudChanged);
        syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC_FAVORITES, SyncNotifications.STATUS_SYNC_STOP);
        if (!dbAccessError && !favoriteEmptySource && favoriteFailsCount == 0) {
            cloudFavorites.updateLastSyncedETag(favoritesDataStorageETag);
        }
        // Notes
        syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC_NOTES, SyncNotifications.STATUS_SYNC_START);
        syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC_NOTES, SyncNotifications.STATUS_SYNC_STOP);
        // Links
        syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC_LINKS, SyncNotifications.STATUS_SYNC_START);
        syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC_LINKS, SyncNotifications.STATUS_SYNC_STOP);
        // Stop
        saveLastSyncStatus();
        syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC, SyncNotifications.STATUS_SYNC_STOP);
        // Error notifications
        if (dbAccessError) {
            syncNotifications.notifyFailedSynchronization(
                    resources.getString(R.string.sync_adapter_title_failed_database),
                    resources.getString(R.string.sync_adapter_text_failed_database));
        }
        if (favoriteEmptySource) {
            syncNotifications.notifyFailedSynchronization(
                    resources.getString(R.string.sync_adapter_title_empty_storage),
                    resources.getString(R.string.sync_adapter_text_empty_storage));
        }
        if (favoriteFailsCount > 0) {
            syncNotifications.notifyFailedSynchronization(resources.getQuantityString(
                    R.plurals.sync_adapter_text_failed_favorites,
                    favoriteFailsCount, favoriteFailsCount));
        }
    }

    private void saveLastSyncStatus() {
        int lastSyncStatus;
        if (dbAccessError || favoriteEmptySource) {
            lastSyncStatus = LAST_SYNC_STATUS_ERROR;
        } else {
            boolean isConflictedFavorites = localFavorites.isConflictedFavorites().blockingGet();
            if (isConflictedFavorites) {
                lastSyncStatus = LAST_SYNC_STATUS_CONFLICT;
            } else {
                lastSyncStatus = LAST_SYNC_STATUS_SUCCESS;
            }
        }
        settings.updateLastSyncTime();
        settings.setLastSyncStatus(lastSyncStatus);
    }

    private void syncFavorites(boolean isCloudChanged) {
        if (!isCloudChanged) {
            final String selection = LocalContract.FavoriteEntry.COLUMN_NAME_SYNCED + " = ?";
            final String[] selectionArgs = {"0"};
            localFavorites.getFavorites(selection, selectionArgs, null).subscribe(
                    favorite -> syncFavorite(favorite, favorite.getETag()),
                    throwable -> setDbAccessError(true));
            return;
        }
        final Map<String, String> cloudDataSourceMap = cloudFavorites.getDataSourceMap(ocClient);
        // Protection of the local storage
        if (cloudDataSourceMap.isEmpty()) {
            int numRows = localFavorites.resetFavoritesSyncState().blockingGet();
            if (numRows > 0) {
                favoriteEmptySource = true;
                return;
            }
        }
        // Sync Local
        localFavorites.getFavorites().subscribe(favorite -> {
            String cloudETag = cloudDataSourceMap.get(favorite.getId());
            syncFavorite(favorite, cloudETag);
        }, throwable -> setDbAccessError(true));
        if (dbAccessError) return;

        // OPTIMIZATION: Map can be taken from previous iteration
        final Set<String> localFavoriteIds = new HashSet<>();
        localFavorites.getFavoriteIds().subscribe(
                localFavoriteIds::add, throwable -> setDbAccessError(true));
        if (dbAccessError) return;

        // Load new records from cloud
        for (String cloudFavoriteId : cloudDataSourceMap.keySet()) {
            if (localFavoriteIds.contains(cloudFavoriteId)) continue;

            Favorite cloudFavorite = downloadFavorite(cloudFavoriteId);
            if (cloudFavorite == null) {
                favoriteFailsCount++;
                continue;
            }
            syncNotifications.sendSyncBroadcast(SyncNotifications.ACTION_SYNC_FAVORITES,
                    SyncNotifications.STATUS_DOWNLOADED, cloudFavoriteId);
            boolean notifyChanged = saveFavorite(cloudFavorite);
            if (notifyChanged) {
                syncNotifications.sendSyncBroadcast(SyncNotifications.ACTION_SYNC_FAVORITES,
                        SyncNotifications.STATUS_CREATED, cloudFavoriteId);
            }
        } // for
    }

    private void syncFavorite(Favorite favorite, String cloudETag) {
        String favoriteId = favorite.getId();
        String favoriteETag = favorite.getETag();
        boolean notifyChanged = false;
        int statusChanged = SyncNotifications.STATUS_UPDATED;
        if (favoriteETag == null) { // New
            // duplicated && conflicted can be ignored
            if (favorite.isDeleted()) {
                // DELETE local
                Log.e(TAG, "The records never synced must be deleted immediately [" + favoriteId + "]");
                notifyChanged = deleteLocalFavorite(favoriteId);
                statusChanged = SyncNotifications.STATUS_DELETED;
            } else { // synced is ignored (!synced)
                // UPLOAD
                // NOTE: local unsynced will replace cloud with the same ID (acceptable behaviour)
                notifyChanged = uploadFavorite(favorite);
            }
        } else if (favoriteETag.equals(cloudETag)) { // Green light
            // conflicted can be ignored
            if (!favorite.isSynced()) {
                if (favorite.isDeleted()) {
                    // DELETE cloud
                    notifyChanged = deleteCloudFavorite(favoriteId);
                    statusChanged = SyncNotifications.STATUS_DELETED;
                } else if (!favorite.isDuplicated()) {
                    // UPLOAD
                    notifyChanged = uploadFavorite(favorite);
                }
            }
        } else if (cloudETag == null) { // Was deleted on cloud
            if (favorite.isSynced()) {
                // DELETE local
                notifyChanged = deleteLocalFavorite(favoriteId);
                statusChanged = SyncNotifications.STATUS_DELETED;
            } else {
                // SET conflicted
                SyncState state;
                if (favorite.isDeleted()) {
                    state = new SyncState(SyncState.State.CONFLICTED_DELETE);
                } else {
                    state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                }
                notifyChanged = updateFavorite(favoriteId, state);
            }
        } else { // Was changed on cloud
            // duplicated && conflicted can be ignored
            // DOWNLOAD (with synced state by default)
            Favorite cloudFavorite = downloadFavorite(favoriteId);
            if (cloudFavorite != null) {
                syncNotifications.sendSyncBroadcast(SyncNotifications.ACTION_SYNC_FAVORITES,
                        SyncNotifications.STATUS_DOWNLOADED, favoriteId);
                if (favorite.isSynced() && !favorite.isDeleted()) {
                    // SAVE local
                    notifyChanged = saveFavorite(cloudFavorite);
                } else { // !synced || deleted
                    if (favorite.equals(cloudFavorite)) {
                        if (favorite.isDeleted()) {
                            // DELETE cloud
                            notifyChanged = deleteCloudFavorite(favoriteId);
                            statusChanged = SyncNotifications.STATUS_DELETED;
                        } else {
                            // UPDATE state
                            assert cloudFavorite.getETag() != null;
                            SyncState state = new SyncState(
                                    cloudFavorite.getETag(), SyncState.State.SYNCED);
                            // NOTE: record may be in conflicted state
                            notifyChanged = updateFavorite(favoriteId, state);
                        }
                    } else {
                        // SET (or confirm) conflicted
                        SyncState state;
                        if (favorite.isDeleted()) {
                            state = new SyncState(SyncState.State.CONFLICTED_DELETE);
                        } else {
                            state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                        }
                        notifyChanged = updateFavorite(favoriteId, state);
                    }
                }
            } else {
                favoriteFailsCount++;
            }
        }
        if (notifyChanged) {
            syncNotifications.sendSyncBroadcast(
                    SyncNotifications.ACTION_SYNC_FAVORITES, statusChanged, favoriteId);
        }
    }

    // NOTE: any cloud item can violate the DB constraints
    private boolean saveFavorite(@NonNull final Favorite favorite) {
        checkNotNull(favorite);

        // Primary record
        try {
            long rowId = localFavorites.saveFavorite(favorite).blockingGet();
            return rowId > 0;
        } catch (NullPointerException e) {
            favoriteFailsCount++;
            return false;
        } catch (SQLiteConstraintException e) {
            // NOTE: will try to resolve it further
        }
        // Duplicated record
        int duplicated = localFavorites.getNextDuplicated(favorite.getName()).blockingGet();
        SyncState state = new SyncState(favorite.getETag(), duplicated);
        Favorite duplicatedFavorite = new Favorite(favorite, state);
        try {
            long rowId = localFavorites.saveFavorite(duplicatedFavorite).blockingGet();
            return rowId > 0;
        } catch (NullPointerException | SQLiteConstraintException e) {
            favoriteFailsCount++;
        }
        return false;
    }

    private boolean deleteLocalFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        Log.i(TAG, favoriteId + ": DELETE local");

        int numRows = localFavorites.deleteFavorite(favoriteId).blockingGet();
        return numRows == 1;
    }

    private boolean deleteCloudFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        Log.i(TAG, favoriteId + ": DELETE cloud");

        RemoteOperationResult result = cloudFavorites.deleteFavorite(favoriteId, ocClient)
                .blockingGet();
        return result.isSuccess() && deleteLocalFavorite(favoriteId);
    }

    private boolean updateFavorite(@NonNull String favoriteId, @NonNull SyncState state) {
        checkNotNull(favoriteId);
        checkNotNull(state);
        Log.i(TAG, favoriteId + ": UPDATE");

        int numRows = localFavorites.updateFavorite(favoriteId, state).blockingGet();
        return numRows == 1;
    }

    private boolean uploadFavorite(@NonNull final Favorite favorite) {
        checkNotNull(favorite);
        Log.i(TAG, favorite.getId() + ": UPLOAD");

        boolean notifyChanged = false;
        RemoteOperationResult result = cloudFavorites.uploadFavorite(favorite, ocClient)
                .blockingGet();
        if (result == null) {
            favoriteFailsCount++;
            return false;
        }
        String favoriteId = favorite.getId();
        if (result.isSuccess()) {
            syncNotifications.sendSyncBroadcast(SyncNotifications.ACTION_SYNC_FAVORITES,
                    SyncNotifications.STATUS_UPLOADED, favoriteId);
            JsonFile jsonFile = (JsonFile) result.getData().get(0);
            SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
            localFavorites.updateFavorite(favoriteId, state).blockingGet();
        } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
            SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
            int numRows = localFavorites.updateFavorite(favoriteId, state).blockingGet();
            notifyChanged = (numRows == 1);
        }
        return notifyChanged;
    }

    private Favorite downloadFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        Log.i(TAG, favoriteId + ": DOWNLOAD");

        try {
            return cloudFavorites.downloadFavorite(favoriteId, ocClient).blockingGet();
        } catch (NoSuchElementException e) {
            return null; // NOTE: an unexpected error, file have to be in place here
        }
    }

    private void setDbAccessError(boolean dbAccessError) {
        this.dbAccessError = dbAccessError;
    }

    @VisibleForTesting
    public int getFavoriteFailsCount() {
        return favoriteFailsCount;
    }
}
