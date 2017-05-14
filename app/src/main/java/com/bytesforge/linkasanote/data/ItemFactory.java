package com.bytesforge.linkasanote.data;

import android.database.Cursor;

import com.bytesforge.linkasanote.sync.SyncState;

public interface ItemFactory<T> {

    T from(Cursor cursor);
    T from(String jsonFavoriteString, SyncState state);
}
