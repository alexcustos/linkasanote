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

package com.bytesforge.linkasanote.sync;

import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.bytesforge.linkasanote.data.Item;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.data.source.local.LocalItems;
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
    private static final String TAG_E = SyncItem.class.getCanonicalName();

    private final LocalItems<T> localItems;
    private final CloudItem<T> cloudItem;
    private final SyncNotifications syncNotifications;
    private final OwnCloudClient ocClient;
    private final String notificationAction;
    private final boolean uploadToEmpty;
    private final boolean protectLocal;
    private final long started;

    private int uploaded;
    private int downloaded;

    private SyncItemResult syncResult;

    public SyncItem(
            @NonNull OwnCloudClient ocClient,
            @NonNull LocalItems<T> localItems, @NonNull CloudItem<T> cloudItem,
            @NonNull SyncNotifications syncNotifications, @NonNull String notificationAction,
            boolean uploadToEmpty, boolean protectLocal, long started) {
        this.ocClient = checkNotNull(ocClient);
        this.localItems = checkNotNull(localItems);
        this.cloudItem = checkNotNull(cloudItem);
        this.syncNotifications = checkNotNull(syncNotifications);
        this.notificationAction = checkNotNull(notificationAction);
        this.uploadToEmpty = uploadToEmpty;
        this.protectLocal = protectLocal;
        this.started = started;
        syncResult = new SyncItemResult(SyncItemResult.Status.FAILS_COUNT);
        uploaded = 0;
        downloaded = 0;
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
            localItems.getUnsynced()
                    .subscribe(
                            item -> syncItem(item, item.getETag()),
                            throwable -> setDbAccessError());
            return;
        }
        final Map<String, String> cloudDataSourceMap = cloudItem.getDataSourceMap(ocClient);
        if (cloudDataSourceMap.isEmpty() && uploadToEmpty) {
            int numRows = localItems.resetSyncState().blockingGet();
            if (numRows > 0) {
                Log.d(TAG, "Cloud storage loss is detected, starting to upload [" + numRows + "]");
            }
        }
        // Sync Local
        localItems.getAll()
                .subscribe(item -> {
                    String cloudETag = cloudDataSourceMap.get(item.getId());
                    syncItem(item, cloudETag);
                }, throwable -> {
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    setDbAccessError();
                });
        if (syncResult.isDbAccessError()) return;

        // OPTIMIZATION: Local Map can be taken from previous step
        final Set<String> localIds = new HashSet<>();
        localItems.getIds()
                .subscribe(
                        localIds::add,
                        throwable -> setDbAccessError());
        if (syncResult.isDbAccessError()) return;

        // New cloud records
        // OPTIMIZATION: Cloud Map can be updated in the previous step
        final Set<String> cloudIds = cloudItem.getDataSourceMap(ocClient).keySet();
        for (String cloudId : cloudIds) {
            if (localIds.contains(cloudId)) {
                continue;
            }
            T cloudItem = download(cloudId);
            if (cloudItem == null) {
                continue;
            }
            syncNotifications.sendSyncBroadcast(notificationAction,
                    SyncNotifications.STATUS_DOWNLOADED, cloudId, ++downloaded);
            boolean notifyChanged = save(cloudItem);
            if (notifyChanged) {
                syncNotifications.sendSyncBroadcast(notificationAction,
                        SyncNotifications.STATUS_CREATED, cloudId);
            }
        }
    }

    private void syncItem(final T item, final String cloudETag) {
        // NOTE: some updates on the conflicted state may cause constraint violation, so let it be resolved first
        if (item.isConflicted()) return;

        final String itemId = item.getId();
        final String itemETag = item.getETag();
        boolean notifyChanged = false;
        int statusChanged = SyncNotifications.STATUS_UPDATED;
        if (itemETag == null) { // New
            // duplicated && conflicted can be ignored
            if (item.isDeleted()) {
                // DELETE local
                Log.e(TAG, "The records never synced must be deleted immediately [" + itemId + "]");
                notifyChanged = deleteLocal(item);
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
                    notifyChanged = deleteCloud(item);
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
                    notifyChanged = update(item, state);
                } else {
                    // DELETE local
                    notifyChanged = deleteLocal(item);
                    statusChanged = SyncNotifications.STATUS_DELETED;
                }
            } else {
                if (item.isDeleted()) {
                    // DELETE local
                    notifyChanged = deleteLocal(item);
                    statusChanged = SyncNotifications.STATUS_DELETED;
                } else {
                    // SET conflicted
                    SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                    notifyChanged = update(item, state);
                }
            }
        } else { // Was changed on cloud
            // duplicated && conflicted can be ignored
            // DOWNLOAD (with synced state by default)
            T cloudItem = download(itemId);
            if (cloudItem != null) {
                syncNotifications.sendSyncBroadcast(notificationAction,
                        SyncNotifications.STATUS_DOWNLOADED, itemId, ++downloaded);
                if (item.isSynced() && !item.isDeleted()) {
                    // SAVE local
                    notifyChanged = save(cloudItem);
                } else { // !synced || deleted
                    if (item.equals(cloudItem)) {
                        if (item.isDeleted()) {
                            // DELETE cloud
                            notifyChanged = deleteCloud(item);
                            statusChanged = SyncNotifications.STATUS_DELETED;
                        } else {
                            // UPDATE state
                            assert cloudItem.getETag() != null;
                            SyncState state = new SyncState(
                                    cloudItem.getETag(), SyncState.State.SYNCED);
                            // NOTE: record may be in conflicted state
                            notifyChanged = update(item, state);
                        }
                    } else {
                        // SET (or confirm) conflicted
                        SyncState state;
                        if (item.isDeleted()) {
                            state = new SyncState(SyncState.State.CONFLICTED_DELETE);
                        } else {
                            state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                        }
                        notifyChanged = update(item, state);
                    }
                }
            }
        }
        if (notifyChanged) {
            syncNotifications.sendSyncBroadcast(notificationAction, statusChanged, itemId);
        }
    }

    // NOTE: any cloud item can violate the DB constraints
    private boolean save(@NonNull final T item) { // downloaded
        checkNotNull(item);
        String itemId = item.getId();
        // Primary record
        try {
            boolean success = localItems.save(item).blockingGet();
            if (success) {
                localItems.logSyncResult(started, itemId,
                        LocalContract.SyncResultEntry.Result.DOWNLOADED).blockingGet();
                String relatedId = item.getRelatedId();
                if (relatedId != null) {
                    localItems.logSyncResult(started, relatedId,
                            LocalContract.SyncResultEntry.Result.RELATED).blockingGet();
                }
            }
            return success;
        } catch (NullPointerException e) {
            syncResult.incFailsCount();
            localItems.logSyncResult(started, itemId,
                    LocalContract.SyncResultEntry.Result.ERROR).blockingGet();
            return false;
        } catch (SQLiteConstraintException e) {
            // NOTE: will try to resolve it further
        }
        // Duplicated record
        try {
            boolean success = localItems.saveDuplicated(item).blockingGet();
            if (success) {
                localItems.logSyncResult(started, itemId,
                        LocalContract.SyncResultEntry.Result.DOWNLOADED).blockingGet();
                String relatedId = item.getRelatedId();
                if (relatedId != null) {
                    localItems.logSyncResult(started, relatedId,
                            LocalContract.SyncResultEntry.Result.RELATED).blockingGet();
                }
            }
            return success;
        } catch (NullPointerException | SQLiteConstraintException e) {
            syncResult.incFailsCount();
            localItems.logSyncResult(started, itemId,
                    LocalContract.SyncResultEntry.Result.ERROR).blockingGet();
        }
        return false;
    }

    private boolean deleteLocal(@NonNull final T item) {
        checkNotNull(item);
        final String itemId = item.getId();
        final String relatedId = item.getRelatedId();
        Log.d(TAG, itemId + ": DELETE local");

        boolean success = localItems.delete(itemId).blockingGet();
        if (success) {
            localItems.logSyncResult(started, itemId,
                    LocalContract.SyncResultEntry.Result.DELETED).blockingGet();
            if (relatedId != null) {
                localItems.logSyncResult(started, relatedId,
                        LocalContract.SyncResultEntry.Result.RELATED).blockingGet();
            }
        }
        return success;
    }

    private boolean deleteCloud(@NonNull final T item) {
        checkNotNull(item);
        final String itemId = item.getId();
        Log.d(TAG, itemId + ": DELETE cloud");

        RemoteOperationResult result = cloudItem.delete(itemId, ocClient).blockingGet();
        return result.isSuccess() && deleteLocal(item);
    }

    private boolean update(@NonNull final T item, @NonNull final SyncState state) {
        checkNotNull(item);
        checkNotNull(state);
        final String itemId = item.getId();
        final String relatedId = item.getRelatedId();
        Log.d(TAG, itemId + ": UPDATE");

        boolean success = localItems.update(itemId, state).blockingGet();
        if (success) {
            LocalContract.SyncResultEntry.Result result;
            if (state.isConflicted()) {
                result = LocalContract.SyncResultEntry.Result.CONFLICT;
            } else {
                result = LocalContract.SyncResultEntry.Result.SYNCED;
            }
            localItems.logSyncResult(started, itemId, result).blockingGet();
            if (relatedId != null) {
                localItems.logSyncResult(started, relatedId,
                        LocalContract.SyncResultEntry.Result.RELATED).blockingGet();
            }
        }
        return success;
    }

    private boolean upload(@NonNull final T item) {
        checkNotNull(item);
        final String itemId = item.getId();
        final String relatedId = item.getRelatedId();
        Log.d(TAG, item.getId() + ": UPLOAD");

        boolean success = false;
        RemoteOperationResult result = cloudItem.upload(item, ocClient).blockingGet();
        if (result == null) {
            syncResult.incFailsCount();
            localItems.logSyncResult(started, itemId,
                    LocalContract.SyncResultEntry.Result.ERROR).blockingGet();
            return false;
        }
        if (result.isSuccess()) {
            syncNotifications.sendSyncBroadcast(notificationAction,
                    SyncNotifications.STATUS_UPLOADED, itemId, ++uploaded);
            JsonFile jsonFile = (JsonFile) result.getData().get(0);
            SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
            success = localItems.update(itemId, state).blockingGet();
            if (success) {
                localItems.logSyncResult(started, itemId,
                        LocalContract.SyncResultEntry.Result.UPLOADED).blockingGet();
                if (relatedId != null) {
                    localItems.logSyncResult(started, relatedId,
                            LocalContract.SyncResultEntry.Result.RELATED).blockingGet();
                }
            }
        } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
            SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
            success = update(item, state);
        }
        return success;
    }

    private T download(@NonNull String itemId) {
        checkNotNull(itemId);
        Log.d(TAG, itemId + ": DOWNLOAD");

        try {
            // Note: will be logged in save() or update()
            return cloudItem.download(itemId, ocClient).blockingGet();
        } catch (NullPointerException | NoSuchElementException e) {
            syncResult.incFailsCount();
            localItems.logSyncResult(started, itemId,
                    LocalContract.SyncResultEntry.Result.ERROR).blockingGet();
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
