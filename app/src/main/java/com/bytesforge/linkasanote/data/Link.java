package com.bytesforge.linkasanote.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.bytesforge.linkasanote.data.source.local.PersistenceContract;
import com.google.common.base.Objects;

import static com.bytesforge.linkasanote.utils.UuidUtils.generateKey;
import static java.lang.System.currentTimeMillis;

public final class Link {

    @NonNull
    private final String id;

    private final long created;
    private final long updated;

    @NonNull
    private final String value;

    @Nullable
    private final String title;

    private final boolean disabled;
    private final boolean deleted;
    private final boolean synced;

    public Link(@NonNull String value, @Nullable String title) {
        this(generateKey(), currentTimeMillis(), currentTimeMillis(),
                value, title, false, false, false);
    }

    @VisibleForTesting
    public Link(@NonNull String id, @NonNull String value, @Nullable String title) {
        this(id, currentTimeMillis(), currentTimeMillis(), value, title, false, false, false);
    }

    public Link(
            @NonNull String id,
            long created, long updated,
            @NonNull String value, @Nullable String title,
            boolean disabled, boolean deleted, boolean synced) {
        this.id = id;

        this.created = created;
        this.updated = updated;

        this.value = value;
        this.title = title;

        this.disabled = disabled;
        this.deleted = deleted;
        this.synced = synced;
    }

    public static Link from(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(
                PersistenceContract.LinkEntry.COLUMN_NAME_ENTRY_ID));

        long created = cursor.getLong(cursor.getColumnIndexOrThrow(
                PersistenceContract.LinkEntry.COLUMN_NAME_CREATED));
        long updated = cursor.getLong(cursor.getColumnIndexOrThrow(
                PersistenceContract.LinkEntry.COLUMN_NAME_UPDATED));

        String value = cursor.getString(cursor.getColumnIndexOrThrow(
                PersistenceContract.LinkEntry.COLUMN_NAME_VALUE));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(
                PersistenceContract.LinkEntry.COLUMN_NAME_TITLE));

        boolean disabled = cursor.getInt(cursor.getColumnIndexOrThrow(
                PersistenceContract.LinkEntry.COLUMN_NAME_DISABLED)) == 1;
        boolean deleted = cursor.getInt(cursor.getColumnIndexOrThrow(
                PersistenceContract.LinkEntry.COLUMN_NAME_DELETED)) == 1;
        boolean synced = cursor.getInt(cursor.getColumnIndexOrThrow(
                PersistenceContract.LinkEntry.COLUMN_NAME_SYNCED)) == 1;

        return new Link(id, created, updated, value, title, disabled, deleted, synced);
    }

    public static Link from(ContentValues values) {
        String id = values.getAsString(PersistenceContract.LinkEntry.COLUMN_NAME_ENTRY_ID);

        long created = values.getAsLong(PersistenceContract.LinkEntry.COLUMN_NAME_CREATED);
        long updated = values.getAsLong(PersistenceContract.LinkEntry.COLUMN_NAME_UPDATED);

        String value = values.getAsString(PersistenceContract.LinkEntry.COLUMN_NAME_VALUE);
        String title = values.getAsString(PersistenceContract.LinkEntry.COLUMN_NAME_TITLE);

        boolean disabled = values.getAsInteger(PersistenceContract.LinkEntry.COLUMN_NAME_DISABLED) == 1;
        boolean deleted = values.getAsInteger(PersistenceContract.LinkEntry.COLUMN_NAME_DELETED) == 1;
        boolean synced = values.getAsInteger(PersistenceContract.LinkEntry.COLUMN_NAME_SYNCED) == 1;

        return new Link(id, created, updated, value, title, disabled, deleted, synced);
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put(PersistenceContract.LinkEntry.COLUMN_NAME_ENTRY_ID, getId());
        values.put(PersistenceContract.LinkEntry.COLUMN_NAME_CREATED, getCreated());
        values.put(PersistenceContract.LinkEntry.COLUMN_NAME_UPDATED, getUpdated());

        values.put(PersistenceContract.LinkEntry.COLUMN_NAME_VALUE, getValue());
        values.put(PersistenceContract.LinkEntry.COLUMN_NAME_TITLE, getTitle());

        values.put(PersistenceContract.LinkEntry.COLUMN_NAME_DISABLED, isDisabled() ? 1 : 0);
        values.put(PersistenceContract.LinkEntry.COLUMN_NAME_DELETED, isDeleted() ? 1 : 0);
        values.put(PersistenceContract.LinkEntry.COLUMN_NAME_SYNCED, isSynced() ? 1 : 0);

        return values;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public long getCreated() {
        return created;
    }

    public long getUpdated() {
        return updated;
    }

    @NonNull
    public String getValue() {
        return value;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isSynced() {
        return synced;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Link link = (Link) obj;
        return Objects.equal(id, link.id) &&
                Objects.equal(value, link.value) &&
                Objects.equal(title, link.title);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, value, title);
    }
}
