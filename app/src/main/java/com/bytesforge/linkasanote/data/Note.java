package com.bytesforge.linkasanote.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.google.common.base.Objects;

import static com.bytesforge.linkasanote.utils.UuidUtils.generateKey;
import static java.lang.System.currentTimeMillis;

public final class Note {

    public static final String CLOUD_DIRECTORY = "notes";

    @NonNull
    private final String id;

    private final long created;
    private final long updated;

    @NonNull
    private final String excerpt;

    private final boolean deleted;
    private final boolean synced;

    public Note(@NonNull String excerpt) {
        this(generateKey(), currentTimeMillis(), currentTimeMillis(), excerpt, false, false);
    }

    @VisibleForTesting
    public Note(@NonNull String id, @NonNull String excerpt) {
        this(id, currentTimeMillis(), currentTimeMillis(), excerpt, false, false);
    }

    public Note(
            @NonNull String id, long created, long updated,
            @NonNull String excerpt,
            boolean deleted, boolean synced) {
        this.id = id;

        this.created = created;
        this.updated = updated;

        this.excerpt = excerpt;

        this.deleted = deleted;
        this.synced = synced;
    }

    public static Note from(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID));

        long created = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.NoteEntry.COLUMN_NAME_CREATED));
        long updated = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.NoteEntry.COLUMN_NAME_UPDATED));

        String excerpt = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.NoteEntry.COLUMN_NAME_NOTE));

        boolean deleted = cursor.getInt(cursor.getColumnIndexOrThrow(
                LocalContract.NoteEntry.COLUMN_NAME_DELETED)) == 1;
        boolean synced = cursor.getInt(cursor.getColumnIndexOrThrow(
                LocalContract.NoteEntry.COLUMN_NAME_SYNCED)) == 1;

        return new Note(id, created, updated, excerpt, deleted, synced);
    }

    public static Note from(ContentValues values) {
        String id = values.getAsString(LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID);

        long created = values.getAsLong(LocalContract.NoteEntry.COLUMN_NAME_CREATED);
        long updated = values.getAsLong(LocalContract.NoteEntry.COLUMN_NAME_UPDATED);

        String excerpt = values.getAsString(LocalContract.NoteEntry.COLUMN_NAME_NOTE);

        boolean deleted = values.getAsInteger(LocalContract.NoteEntry.COLUMN_NAME_DELETED) == 1;
        boolean synced = values.getAsInteger(LocalContract.NoteEntry.COLUMN_NAME_SYNCED) == 1;

        return new Note(id, created, updated, excerpt, deleted, synced);
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put(LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID, getId());

        values.put(LocalContract.NoteEntry.COLUMN_NAME_CREATED, getCreated());
        values.put(LocalContract.NoteEntry.COLUMN_NAME_UPDATED, getUpdated());

        values.put(LocalContract.NoteEntry.COLUMN_NAME_NOTE, getExcerpt());

        values.put(LocalContract.NoteEntry.COLUMN_NAME_DELETED, isDeleted() ? 1 : 0);
        values.put(LocalContract.NoteEntry.COLUMN_NAME_SYNCED, isSynced() ? 1 : 0);

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
    public String getExcerpt() {
        return excerpt;
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

        Note note = (Note) obj;
        return Objects.equal(id, note.id)
                && Objects.equal(excerpt, note.excerpt);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, excerpt);
    }
}
