package com.bytesforge.linkasanote.data;

import android.database.Cursor;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;

public interface ItemFactory<T> {

    T build(T item, List<Tag> tags, List<Note> notes);
    T build(T item, List<Tag> tags);
    T build(T item, @NonNull SyncState state);
    T from(Cursor cursor);
    T from(String jsonFavoriteString, SyncState state);
}
