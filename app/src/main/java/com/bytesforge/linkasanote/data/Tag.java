package com.bytesforge.linkasanote.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.source.local.PersistenceContract;
import com.google.common.base.Objects;

import static java.lang.System.currentTimeMillis;

public final class Tag {

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
                PersistenceContract.TagEntry.COLUMN_NAME_ADDED));

        String name = cursor.getString(cursor.getColumnIndexOrThrow(
                PersistenceContract.TagEntry.COLUMN_NAME_NAME));

        return new Tag(added, name);
    }

    public static Tag from(ContentValues values) {
        long added = values.getAsLong(PersistenceContract.TagEntry.COLUMN_NAME_ADDED);

        String name = values.getAsString(PersistenceContract.TagEntry.COLUMN_NAME_NAME);

        return new Tag(added, name);
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put(PersistenceContract.TagEntry.COLUMN_NAME_ADDED, getAdded());

        values.put(PersistenceContract.TagEntry.COLUMN_NAME_NAME, getName());

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
}
