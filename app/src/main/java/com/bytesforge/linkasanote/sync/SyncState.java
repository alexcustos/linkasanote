package com.bytesforge.linkasanote.sync;

import android.content.ContentValues;

import com.bytesforge.linkasanote.data.source.local.LocalContract;

abstract public class SyncState {

    private boolean conflicted;
    private boolean deleted;
    private boolean synced;

    public enum State {
        UNSYNCED, SYNCED, DELETED, CONFLICTED_UPDATE, CONFLICTED_DELETE}

    public static ContentValues getSyncStateValues(State state) {
        // NOTE: check setSyncState below
        switch (state) {
            case UNSYNCED:
                return getSyncStateValues(false, false, false);
            case SYNCED:
                return getSyncStateValues(false, false, true);
            case DELETED:
                return getSyncStateValues(false, true, false);
            case CONFLICTED_UPDATE:
                return getSyncStateValues(true, false, false);
            case CONFLICTED_DELETE:
                return getSyncStateValues(true, true, false);
            default:
                throw new IllegalArgumentException("Unexpected state was provided [" + state + "]");
        }
    }

    private static ContentValues getSyncStateValues(
            boolean conflicted, boolean deleted, boolean synced) {
        ContentValues values = new ContentValues();
        values.put(LocalContract.COMMON_NAME_CONFLICTED, conflicted);
        values.put(LocalContract.COMMON_NAME_DELETED, deleted);
        values.put(LocalContract.COMMON_NAME_SYNCED, synced);
        return values;
    }

    public void setSyncState(State state) {
        switch (state) {
            case UNSYNCED:
                setSyncState(false, false, false); // cds
                break;
            case SYNCED:
                setSyncState(false, false, true); // cdS
                break;
            case DELETED:
                setSyncState(false, true, false); // cDs, cDS successfully deleted (delete record)
                break;
            case CONFLICTED_UPDATE:
                // NOTE: Local record was updated and Cloud one was modified or deleted
                // TODO: Conflicted record must preload Cloud copy and check if conflict still exists
                // TODO: Cloud copy: empty & cDs - deleted, cds - updated
                setSyncState(true, false, false); // Cds, CdS successfully resolved (syncedState)
                break;
            case CONFLICTED_DELETE:
                // NOTE: Local record was deleted and Cloud one was modified
                setSyncState(true, true, false); // CDs, CDS successfully resolved (syncedState)
                break;
            default:
                throw new IllegalArgumentException("Unexpected state was provided [" + state + "]");
        }
    }

    protected void setSyncState(boolean conflicted, boolean deleted, boolean synced) {
        this.conflicted = conflicted;
        this.deleted = deleted;
        this.synced = synced;
    }

    protected ContentValues getSyncStateValues() {
        return getSyncStateValues(conflicted, deleted, synced);
    }
}
