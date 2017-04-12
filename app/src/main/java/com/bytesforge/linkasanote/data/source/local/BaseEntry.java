package com.bytesforge.linkasanote.data.source.local;

import android.provider.BaseColumns;

public interface BaseEntry extends BaseColumns {

    // NOTE: entry_id must not be part of SyncState
    String COLUMN_NAME_ENTRY_ID = "entry_id";
    String COLUMN_NAME_CREATED = "created";
    String COLUMN_NAME_UPDATED = "updated";
    // SyncState
    String COLUMN_NAME_ETAG = "etag";
    String COLUMN_NAME_DUPLICATED = "duplicated";
    String COLUMN_NAME_CONFLICTED = "conflicted";
    String COLUMN_NAME_DELETED = "deleted";
    String COLUMN_NAME_SYNCED = "synced";
}

