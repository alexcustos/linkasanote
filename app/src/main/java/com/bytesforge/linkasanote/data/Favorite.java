package com.bytesforge.linkasanote.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.bytesforge.linkasanote.data.source.local.PersistenceContract;
import com.google.common.base.Objects;

import static com.bytesforge.linkasanote.utils.UuidUtils.generateKey;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

public final class Favorite {

    @NonNull
    private final String id;

    private final long added;

    @NonNull
    private final String name;

    private final boolean synced;

    public Favorite(@NonNull String name) {
        this(generateKey(), currentTimeMillis(), name, false);
    }

    @VisibleForTesting
    public Favorite(@NonNull String id, @NonNull String name) {
        this(id, currentTimeMillis(), name, false);
    }

    public Favorite(@NonNull String id, long added, @NonNull String name, boolean synced) {
        checkNotNull(id);
        checkNotNull(name);
        this.id = id;

        this.added = added;
        this.name = name;

        this.synced = synced;
    }

    public static Favorite from(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(
                PersistenceContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID));

        long added = cursor.getLong(cursor.getColumnIndexOrThrow(
                PersistenceContract.FavoriteEntry.COLUMN_NAME_ADDED));

        String name = cursor.getString(cursor.getColumnIndexOrThrow(
                PersistenceContract.FavoriteEntry.COLUMN_NAME_NAME));

        boolean synced = cursor.getInt(cursor.getColumnIndexOrThrow(
                PersistenceContract.FavoriteEntry.COLUMN_NAME_SYNCED)) == 1;

        return new Favorite(id, added, name, synced);
    }

    public static Favorite from(ContentValues values) {
        String id = values.getAsString(PersistenceContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID);

        long added = values.getAsLong(PersistenceContract.FavoriteEntry.COLUMN_NAME_ADDED);

        String name = values.getAsString(PersistenceContract.FavoriteEntry.COLUMN_NAME_NAME);

        boolean synced = values.getAsInteger(PersistenceContract.FavoriteEntry.COLUMN_NAME_SYNCED) == 1;

        return new Favorite(id, added, name, synced);
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put(PersistenceContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID, getId());

        values.put(PersistenceContract.FavoriteEntry.COLUMN_NAME_ADDED, getAdded());

        values.put(PersistenceContract.FavoriteEntry.COLUMN_NAME_NAME, getName());

        values.put(PersistenceContract.FavoriteEntry.COLUMN_NAME_SYNCED, isSynced() ? 1 : 0);

        return values;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public long getAdded() {
        return added;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public boolean isSynced() {
        return synced;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Favorite favorite = (Favorite) obj;
        return Objects.equal(id, favorite.id) &&
                Objects.equal(name, favorite.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name);
    }
}
