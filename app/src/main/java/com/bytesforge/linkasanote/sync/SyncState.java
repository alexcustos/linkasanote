package com.bytesforge.linkasanote.sync;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.source.local.LocalContract;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncState {

    private final long rowId;

    private final int duplicated;
    private final boolean conflicted;
    private final boolean deleted;
    private final boolean synced;

    @Nullable
    private String eTag;

    public enum State {UNSYNCED, SYNCED, DELETED, CONFLICTED_UPDATE, CONFLICTED_DELETE}

    public SyncState() {
        // UNSYNCED
        this(-1, null, 0, false, false, false);
    }

    public SyncState(
            long rowId, @Nullable String eTag,
            int duplicated, boolean conflicted, boolean deleted, boolean synced) {
        this.rowId = rowId;
        this.eTag = eTag;
        this.duplicated = duplicated;
        this.conflicted = conflicted;
        this.deleted = deleted;
        this.synced = synced;
    }

    public SyncState(State state) {
        rowId = -1;
        eTag = null;
        duplicated = 0;
        switch (state) {
            case UNSYNCED: // cds
                conflicted = false;
                deleted = false;
                synced = false;
                break;
            case SYNCED: // cdS
                conflicted = false;
                deleted = false;
                synced = true;
                break;
            case DELETED: // cDs, cDS successfully deleted (delete record)
                conflicted = false;
                deleted = true;
                synced = false;
                break;
            case CONFLICTED_UPDATE: // Cds, CdS successfully resolved (syncedState)
                // NOTE: Local record was updated and Cloud one was modified or deleted
                // TODO: Conflicted record must preload Cloud copy and check if conflict still exists
                conflicted = true;
                deleted = false;
                synced = false;
                break;
            case CONFLICTED_DELETE: // CDs, CDS successfully resolved (syncedState)
                // NOTE: Local record was deleted and Cloud one was modified
                conflicted = true;
                deleted = true;
                synced = false;
                break;
            default:
                throw new IllegalArgumentException("Unexpected state was provided [" + state.name() + "]");
        }
    }

    public SyncState(@Nullable String eTag, int duplicated) {
        if (eTag == null) {
            throw new IllegalArgumentException("Duplicate conflict state must be constructed with valid eTag");
        }
        if (duplicated <= 0) {
            throw new IllegalArgumentException("Cannot setup duplicate conflict state for primary record");
        }
        rowId = -1;
        this.eTag = eTag;
        // CONFLICTED_DUPLICATE: CdS && duplicated > 0, duplicated = 0 resolved (otherState)
        this.duplicated = duplicated;
        conflicted = true;
        deleted = false;
        synced = true;
    }

    public SyncState(@NonNull String eTag, State state) {
        this(state);
        this.eTag = checkNotNull(eTag);
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        // NOTE: rowId must not be here
        if (eTag != null) {
            // NOTE: lets DB maintain the default value
            values.put(LocalContract.COMMON_NAME_ETAG, eTag);
        }
        values.put(LocalContract.COMMON_NAME_DUPLICATED, duplicated);
        values.put(LocalContract.COMMON_NAME_CONFLICTED, conflicted);
        values.put(LocalContract.COMMON_NAME_DELETED, deleted);
        values.put(LocalContract.COMMON_NAME_SYNCED, synced);

        return values;
    }

    public static SyncState from(Cursor cursor) {
        long rowId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));

        String eTag = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.COMMON_NAME_ETAG));
        int duplicated = cursor.getInt(cursor.getColumnIndexOrThrow(
                LocalContract.COMMON_NAME_DUPLICATED));
        boolean conflicted = cursor.getInt(cursor.getColumnIndexOrThrow(
                LocalContract.COMMON_NAME_CONFLICTED)) == 1;
        boolean deleted = cursor.getInt(cursor.getColumnIndexOrThrow(
                LocalContract.COMMON_NAME_DELETED)) == 1;
        boolean synced = cursor.getInt(cursor.getColumnIndexOrThrow(
                LocalContract.COMMON_NAME_SYNCED)) == 1;

        return new SyncState(rowId, eTag, duplicated, conflicted, deleted, synced);
    }

    public static SyncState from(ContentValues values) {
        long rowId = values.getAsLong(BaseColumns._ID);

        String eTag = values.getAsString(LocalContract.COMMON_NAME_ETAG);
        int duplicated = values.getAsInteger(LocalContract.COMMON_NAME_DUPLICATED);
        boolean conflicted = values.getAsBoolean(LocalContract.COMMON_NAME_CONFLICTED);
        boolean deleted = values.getAsBoolean(LocalContract.COMMON_NAME_DELETED);
        boolean synced = values.getAsBoolean(LocalContract.COMMON_NAME_SYNCED);

        return new SyncState(rowId, eTag, duplicated, conflicted, deleted, synced);
    }

    public long getRowId() {
        return rowId;
    }

    @Nullable
    public String getETag() {
        return eTag;
    }

    public int getDuplicated() {
        return duplicated;
    }

    public boolean isDuplicated() {
        return duplicated != 0;
    }

    public boolean isConflicted() {
        return conflicted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isSynced() {
        return synced;
    }
}
