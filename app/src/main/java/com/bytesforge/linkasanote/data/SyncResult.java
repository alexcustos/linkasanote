package com.bytesforge.linkasanote.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.source.local.LocalContract;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

public final class SyncResult {

    private final long rowId;

    private final long created;
    private final long started;

    @NonNull
    private final String entry;

    @NonNull
    private final String entryId;

    @NonNull
    private final LocalContract.SyncResultEntry.Result result;

    private final boolean applied;

    public SyncResult(
            long started, @NonNull String entry, @NonNull String entryId,
            @NonNull LocalContract.SyncResultEntry.Result result) {
        this(-1, currentTimeMillis(), started, entry, entryId, result, false);
    }

    public SyncResult(
            long rowId, long created, long started,
            @NonNull String entry, @NonNull String entryId,
            @NonNull LocalContract.SyncResultEntry.Result result, boolean applied) {
        this.rowId = rowId;
        this.created = created;
        this.started = started;
        this.entry = checkNotNull(entry);
        this.entryId = checkNotNull(entryId);
        this.result = checkNotNull(result);
        this.applied = applied;
    }

    public static SyncResult from(Cursor cursor) {
        long rowId = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.SyncResultEntry._ID));
        long created = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.SyncResultEntry.COLUMN_NAME_CREATED));
        long started = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.SyncResultEntry.COLUMN_NAME_STARTED));
        String entry = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY));
        String entry_id = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY_ID));
        String result = cursor.getString(cursor.getColumnIndexOrThrow(
                        LocalContract.SyncResultEntry.COLUMN_NAME_RESULT));
        boolean applied = cursor.getInt(cursor.getColumnIndexOrThrow(
                LocalContract.SyncResultEntry.COLUMN_NAME_APPLIED)) == 1;
        return new SyncResult(rowId, created, started, entry, entry_id,
                LocalContract.SyncResultEntry.Result.valueOf(result), applied);
    }

    public static SyncResult from(ContentValues values) {
        long rowId = values.getAsLong(LocalContract.SyncResultEntry._ID);
        long created = values.getAsLong(LocalContract.SyncResultEntry.COLUMN_NAME_CREATED);
        long started = values.getAsLong(LocalContract.SyncResultEntry.COLUMN_NAME_STARTED);
        String entry = values.getAsString(LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY);
        String entry_id = values.getAsString(LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY_ID);
        String result = values.getAsString(LocalContract.SyncResultEntry.COLUMN_NAME_RESULT);
        boolean applied = values.getAsBoolean(LocalContract.SyncResultEntry.COLUMN_NAME_APPLIED);
        return new SyncResult(rowId, created, started, entry, entry_id,
                LocalContract.SyncResultEntry.Result.valueOf(result), applied);
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        // NOTE: rowId must not be here
        values.put(LocalContract.SyncResultEntry.COLUMN_NAME_CREATED, getCreated());
        values.put(LocalContract.SyncResultEntry.COLUMN_NAME_STARTED, getStarted());
        values.put(LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY, getEntry());
        values.put(LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY_ID, getEntryId());
        values.put(LocalContract.SyncResultEntry.COLUMN_NAME_RESULT, getResult().name());
        values.put(LocalContract.SyncResultEntry.COLUMN_NAME_APPLIED, isApplied());
        return values;
    }

    public long getRowId() {
        return rowId;
    }

    public long getCreated() {
        return created;
    }

    public long getStarted() {
        return started;
    }

    @NonNull
    public String getEntry() {
        return entry;
    }

    @NonNull
    public String getEntryId() {
        return entryId;
    }

    @NonNull
    public LocalContract.SyncResultEntry.Result getResult() {
        return result;
    }

    public boolean isApplied() {
        return applied;
    }

    @Override
    public String toString() {
        return entry + " : " + entryId + " -> " + result.name();
    }
}