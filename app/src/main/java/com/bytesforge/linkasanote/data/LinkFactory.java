package com.bytesforge.linkasanote.data;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;

public interface LinkFactory<T> extends ItemFactory<T> {

    T build(T item, List<Tag> tags, List<Note> notes);
    T build(T item, @NonNull SyncState state);
}
