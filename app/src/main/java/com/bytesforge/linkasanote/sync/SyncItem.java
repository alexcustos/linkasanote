package com.bytesforge.linkasanote.sync;

import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.bytesforge.linkasanote.data.Item;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalItem;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncItem<T extends Item> {

    private static final String TAG = SyncItem.class.getSimpleName();

    private final LocalItem<T> localItem;
    private final CloudItem<T> cloudItem;
    private final SyncNotifications syncNotifications;
    private final OwnCloudClient ocClient;
    private final String notificationAction;
    private final boolean uploadToEmpty;
    private final boolean protectLocal;

    private SyncItemResult syncResult;

    public SyncItem(
            @NonNull OwnCloudClient ocClient,
            @NonNull LocalItem<T> localItem, @NonNull CloudItem<T> cloudItem,
            @NonNull SyncNotifications syncNotifications, @NonNull String notificationAction,
            boolean uploadToEmpty, boolean protectLocal) {
        this.ocClient = checkNotNull(ocClient);
        this.localItem = checkNotNull(localItem);
        this.cloudItem = checkNotNull(cloudItem);
        this.syncNotifications = checkNotNull(syncNotifications);
        this.notificationAction = checkNotNull(notificationAction);
        this.uploadToEmpty = uploadToEmpty;
        this.protectLocal = protectLocal;
        syncResult = new SyncItemResult(SyncItemResult.Status.FAILS_COUNT);
    }

    @NonNull
    public SyncItemResult sync() {
        final String dataStorageETag = cloudItem.getDataSourceETag(ocClient);
        if (dataStorageETag == null) {
            return new SyncItemResult(SyncItemResult.Status.SOURCE_NOT_READY);
        }
        boolean isCloudChanged = cloudItem.isCloudDataSourceChanged(dataStorageETag);
        syncItems(isCloudChanged);

        if (syncResult.isSuccess()) {
            cloudItem.updateLastSyncedETag(dataStorageETag);
        }
        return syncResult;
    }

    private void syncItems(boolean isCloudChanged) {
        if (!isCloudChanged) {
            localItem.getUnsynced()
                    .subscribe(
                            item -> syncItem(item, item.getETag()),
                            throwable -> setDbAccessError());
            return;
        }
        final Map<String, String> cloudDataSourceMap = cloudItem.getDataSourceMap(ocClient);
        if (cloudDataSourceMap.isEmpty() && uploadToEmpty) {
            int numRows = localItem.resetSyncState().blockingGet();
            if (numRows > 0) {
                Log.d(TAG, "Cloud storage loss is detected, starting to upload [" + numRows + "]");
            }
        }
        // Sync Local
        localItem.getAll()
                .subscribe(item -> {
                    String cloudETag = cloudDataSourceMap.get(item.getId());
                    syncItem(item, cloudETag);
                }, throwable -> {
                    CommonUtils.logStackTrace(TAG, throwable);
                    setDbAccessError();
                });
        if (syncResult.isDbAccessError()) return;

        // OPTIMIZATION: Map can be taken from previous iteration
        final Set<String> localIds = new HashSet<>();
        localItem.getIds()
                .subscribe(
                        localIds::add,
                        throwable -> setDbAccessError());
        if (syncResult.isDbAccessError()) return;

        // New cloud records
        for (String cloudId : cloudDataSourceMap.keySet()) {
            if (localIds.contains(cloudId)) continue;

            T cloudItem = download(cloudId);
            if (cloudItem == null) {
                syncResult.incFailsCount();
                continue;
            }
            syncNotifications.sendSyncBroadcast(notificationAction,
                    SyncNotifications.STATUS_DOWNLOADED, cloudId);
            boolean notifyChanged = save(cloudItem);
            if (notifyChanged) {
                syncNotifications.sendSyncBroadcast(notificationAction,
                        SyncNotifications.STATUS_CREATED, cloudId);
            }
        } // for
    }

    private void syncItem(final T item, final String cloudETag) {
        final String itemId = item.getId();
        final String itemETag = item.getETag();
        boolean notifyChanged = false;
        int statusChanged = SyncNotifications.STATUS_UPDATED;
        if (itemETag == null) { // New
            // duplicated && conflicted can be ignored
            if (item.isDeleted()) {
                // DELETE local
                Log.e(TAG, "The records never synced must be deleted immediately [" + itemId + "]");
                notifyChanged = deleteLocal(itemId);
                statusChanged = SyncNotifications.STATUS_DELETED;
            } else { // synced is ignored (!synced)
                // UPLOAD
                // NOTE: local unsynced will replace cloud with the same ID (acceptable behaviour)
                notifyChanged = upload(item);
            }
        } else if (itemETag.equals(cloudETag)) { // Green light
            // conflicted can be ignored
            if (!item.isSynced()) {
                if (item.isDeleted()) {
                    // DELETE cloud
                    notifyChanged = deleteCloud(itemId);
                    statusChanged = SyncNotifications.STATUS_DELETED;
                } else if (!item.isDuplicated()) {
                    // UPLOAD
                    notifyChanged = upload(item);
                }
            }
        } else if (cloudETag == null) { // Was deleted on cloud
            if (item.isSynced()) {
                if (protectLocal) {
                    // SET conflicted (as if it has been changed)
                    SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                    notifyChanged = update(itemId, state);
                } else {
                    // DELETE local
                    notifyChanged = deleteLocal(itemId);
                    statusChanged = SyncNotifications.STATUS_DELETED;
                }
            } else {
                if (item.isDeleted()) {
                    // DELETE local
                    notifyChanged = deleteLocal(itemId);
                    statusChanged = SyncNotifications.STATUS_DELETED;
                } else {
                    // SET conflicted
                    SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                    notifyChanged = update(itemId, state);
                }
            }
        } else { // Was changed on cloud
            // duplicated && conflicted can be ignored
            // DOWNLOAD (with synced state by default)
            T cloudItem = download(itemId);
            if (cloudItem != null) {
                syncNotifications.sendSyncBroadcast(notificationAction,
                        SyncNotifications.STATUS_DOWNLOADED, itemId);
                if (item.isSynced() && !item.isDeleted()) {
                    // SAVE local
                    notifyChanged = save(cloudItem);
                } else { // !synced || deleted
                    if (item.equals(cloudItem)) {
                        if (item.isDeleted()) {
                            // DELETE cloud
                            notifyChanged = deleteCloud(itemId);
                            statusChanged = SyncNotifications.STATUS_DELETED;
                        } else {
                            // UPDATE state
                            assert cloudItem.getETag() != null;
                            SyncState state = new SyncState(
                                    cloudItem.getETag(), SyncState.State.SYNCED);
                            // NOTE: record may be in conflicted state
                            notifyChanged = update(itemId, state);
                        }
                    } else {
                        // SET (or confirm) conflicted
                        SyncState state;
                        if (item.isDeleted()) {
                            state = new SyncState(SyncState.State.CONFLICTED_DELETE);
                        } else {
                            state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                        }
                        notifyChanged = update(itemId, state);
                    }
                }
            } else {
                syncResult.incFailsCount();
            }
        }
        if (notifyChanged) {
            syncNotifications.sendSyncBroadcast(notificationAction, statusChanged, itemId);
        }
    }

    // NOTE: any cloud item can violate the DB constraints
    private boolean save(@NonNull final T item) {
        checkNotNull(item);

        // Primary record
        try {
            long rowId = localItem.save(item).blockingGet();
            return rowId > 0;
        } catch (NullPointerException e) {
            syncResult.incFailsCount();
            return false;
        } catch (SQLiteConstraintException e) {
            // NOTE: will try to resolve it further
        }
        // Duplicated record
        try {
            long rowId = localItem.saveDuplicated(item).blockingGet();
            return rowId > 0;
        } catch (NullPointerException | SQLiteConstraintException e) {
            syncResult.incFailsCount();
        }
        return false;
    }

    private boolean deleteLocal(@NonNull String itemId) {
        checkNotNull(itemId);
        Log.i(TAG, itemId + ": DELETE local");

        int numRows = localItem.delete(itemId).blockingGet();
        return numRows == 1;
    }

    private boolean deleteCloud(@NonNull String itemId) {
        checkNotNull(itemId);
        Log.i(TAG, itemId + ": DELETE cloud");

        RemoteOperationResult result = cloudItem.delete(itemId, ocClient).blockingGet();
        return result.isSuccess() && deleteLocal(itemId);
    }

    private boolean update(@NonNull String itemId, @NonNull SyncState state) {
        checkNotNull(itemId);
        checkNotNull(state);
        Log.i(TAG, itemId + ": UPDATE");

        int numRows = localItem.update(itemId, state).blockingGet();
        return numRows == 1;
    }

    private boolean upload(@NonNull final T item) {
        checkNotNull(item);
        Log.i(TAG, item.getId() + ": UPLOAD");

        boolean notifyChanged = false;
        RemoteOperationResult result = cloudItem.upload(item, ocClient).blockingGet();
        if (result == null) {
            syncResult.incFailsCount();
            return false;
        }
        String itemId = item.getId();
        if (result.isSuccess()) {
            syncNotifications.sendSyncBroadcast(notificationAction,
                    SyncNotifications.STATUS_UPLOADED, itemId);
            JsonFile jsonFile = (JsonFile) result.getData().get(0);
            SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
            localItem.update(itemId, state).blockingGet();
        } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
            SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
            int numRows = localItem.update(itemId, state).blockingGet();
            notifyChanged = (numRows == 1);
        }
        return notifyChanged;
    }

    private T download(@NonNull String itemId) {
        checkNotNull(itemId);
        Log.i(TAG, itemId + ": DOWNLOAD");

        try {
            return cloudItem.download(itemId, ocClient).blockingGet();
        } catch (NullPointerException | NoSuchElementException e) {
            return null; // NOTE: an unexpected error, file have to be in place here
        }
    }

    private void setDbAccessError() {
        syncResult = new SyncItemResult(SyncItemResult.Status.DB_ACCESS_ERROR);
    }

    @VisibleForTesting
    public int getFailsCount() {
        return syncResult.getFailsCount();
    }
}
