package com.bytesforge.linkasanote.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.google.common.base.Objects;

import java.io.Serializable;

import static java.lang.System.currentTimeMillis;

public final class Tag implements Serializable {

    private final long added;

    @NonNull
    private final String name;

    public Tag(@NonNull String name) {
        this(currentTimeMillis(), name);
    }

    public Tag(long added, @NonNull String name) {
        this.added = added;
        this.name = name;
    }

    public static Tag from(Cursor cursor) {
        long added = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.TagEntry.COLUMN_NAME_ADDED));

        String name = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.TagEntry.COLUMN_NAME_NAME));

        return new Tag(added, name);
    }

    public static Tag from(ContentValues values) {
        long added = values.getAsLong(LocalContract.TagEntry.COLUMN_NAME_ADDED);

        String name = values.getAsString(LocalContract.TagEntry.COLUMN_NAME_NAME);

        return new Tag(added, name);
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put(LocalContract.TagEntry.COLUMN_NAME_ADDED, getAdded());

        values.put(LocalContract.TagEntry.COLUMN_NAME_NAME, getName());

        return values;
    }

    public long getAdded() {
        return added;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Tag tag = (Tag) obj;
        return Objects.equal(name, tag.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
