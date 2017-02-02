package com.bytesforge.linkasanote.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

import static com.bytesforge.linkasanote.utils.UuidUtils.generateKey;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

public final class Favorite {

    @NonNull
    private final String id;

    private final long added;

    // TODO: name & tags should not be @Nullable, or may be enough to check isEmpty()
    @Nullable
    private final String name;

    @Nullable
    private final List<Tag> tags;

    private final boolean synced;

    public Favorite(@Nullable String name, @Nullable List<Tag> tags) {
        this(generateKey(), currentTimeMillis(), name, tags, false);
    }

    @VisibleForTesting
    public Favorite(@NonNull String id, @Nullable String name, @Nullable List<Tag> tags) {
        this(id, currentTimeMillis(), name, tags, false);
    }

    public Favorite(
            @NonNull String id, long added,
            @Nullable String name, @Nullable List<Tag> tags, boolean synced) {
        this.id = checkNotNull(id);

        this.added = added;
        this.name = name;
        this.tags = tags;

        this.synced = synced;
    }

    public static Favorite from(Cursor cursor, List<Tag> tags) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID));

        long added = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_ADDED));

        String name = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_NAME));

        boolean synced = cursor.getInt(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_SYNCED)) == 1;

        return new Favorite(id, added, name, tags, synced);
    }

    public static Favorite from(ContentValues values, List<Tag> tags) {
        String id = values.getAsString(LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID);

        long added = values.getAsLong(LocalContract.FavoriteEntry.COLUMN_NAME_ADDED);

        String name = values.getAsString(LocalContract.FavoriteEntry.COLUMN_NAME_NAME);

        boolean synced = values.getAsInteger(LocalContract.FavoriteEntry.COLUMN_NAME_SYNCED) == 1;

        return new Favorite(id, added, name, tags, synced);
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put(LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID, getId());

        values.put(LocalContract.FavoriteEntry.COLUMN_NAME_ADDED, getAdded());

        values.put(LocalContract.FavoriteEntry.COLUMN_NAME_NAME, getName());

        values.put(LocalContract.FavoriteEntry.COLUMN_NAME_SYNCED, isSynced() ? 1 : 0);

        return values;
    }

    public static List<Tag> tagsFrom(Cursor cursor) {
        List<Tag> tags = new ArrayList<>();

        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            tags.add(Tag.from(cursor));
        }
        return tags;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public long getAdded() {
        return added;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public boolean isSynced() {
        return synced;
    }

    @Nullable
    public List<Tag> getTags() {
        return tags;
    }

    @NonNull
    public String getTagsAsString() {
        if (tags != null) {
            Joiner joiner = Joiner.on(", ");
            return joiner.join(tags);
        } else {
            return "";
        }
    }

    public boolean isEmpty() {
        return Strings.isNullOrEmpty(name) || tags == null || tags.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Favorite favorite = (Favorite) obj;
        return Objects.equal(id, favorite.id)
                && Objects.equal(name, favorite.name)
                && tags.equals(favorite.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name);
    }
}
