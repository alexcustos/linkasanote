package com.bytesforge.linkasanote.data;

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.util.List;

public interface Item {

    ContentValues getContentValues();
    String getDuplicatedKey();

    long getRowId();
    @Nullable String getETag();
    boolean isDuplicated();
    boolean isConflicted();
    boolean isDeleted();
    boolean isSynced();
    @NonNull String getId();
    List<Tag> getTags();
    @Nullable JSONObject getJsonObject();
    boolean isEmpty();
}
