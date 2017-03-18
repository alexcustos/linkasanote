package com.bytesforge.linkasanote.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.sync.SyncState;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.bytesforge.linkasanote.utils.UuidUtils.generateKey;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

public final class Favorite {

    private static final String TAG = Favorite.class.getSimpleName();

    public static final String CLOUD_DIRECTORY = "favorites";
    private static final String JSON_PROPERTY_ID = "id";
    private static final String JSON_PROPERTY_NAME = "name";
    private static final String JSON_PROPERTY_TAGS = "tags";

    @NonNull
    private final String id;

    private final long added;

    @Nullable
    private final String name;

    @Nullable
    private final List<Tag> tags;

    @NonNull
    private final SyncState state;

    public Favorite(String name, List<Tag> tags) {
        this(generateKey(), name, tags);
    }

    public Favorite(String id, String name, List<Tag> tags) {
        this(id, name, tags, new SyncState());
    }

    public Favorite(String id, String name, List<Tag> tags, SyncState state) {
        this(id, currentTimeMillis(), name, tags, state);
    }

    public Favorite(
            @NonNull String id, long added, @Nullable String name, @Nullable List<Tag> tags,
            @NonNull SyncState state) {
        this.id = checkNotNull(id);
        this.added = added;
        this.name = name;
        this.tags = tags;
        this.state = checkNotNull(state);
    }

    public Favorite(Favorite favorite, @NonNull SyncState state) {
        this(favorite.getId(), favorite.getAdded(), favorite.getName(), favorite.getTags(), state);
    }

    public static Favorite from(Cursor cursor, List<Tag> tags) {
        SyncState state = SyncState.from(cursor);

        String id = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID));
        long added = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_ADDED));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_NAME));

        return new Favorite(id, added, name, tags, state);
    }

    public static Favorite from(ContentValues values, List<Tag> tags) {
        SyncState state = SyncState.from(values);

        String id = values.getAsString(LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID);
        long added = values.getAsLong(LocalContract.FavoriteEntry.COLUMN_NAME_ADDED);
        String name = values.getAsString(LocalContract.FavoriteEntry.COLUMN_NAME_NAME);

        return new Favorite(id, added, name, tags, state);
    }

    @Nullable
    public static Favorite from(String jsonFavoriteString, SyncState state) {
        try {
            JSONObject jsonFavorite = new JSONObject(jsonFavoriteString);
            return from(jsonFavorite, state);
        } catch (JSONException e) {
            Log.v(TAG, "Exception while processing Favorite JSON string");
            return null;
        }
    }

    @Nullable
    public static Favorite from(JSONObject jsonFavorite, SyncState state) {
        try {
            String id = jsonFavorite.getString(JSON_PROPERTY_ID);
            String name = jsonFavorite.getString(JSON_PROPERTY_NAME);
            JSONArray jsonTags = jsonFavorite.getJSONArray(JSON_PROPERTY_TAGS);
            List<Tag> tags = new ArrayList<>();
            for (int i = 0; i < jsonTags.length(); i++) {
                tags.add(Tag.from(jsonTags.getJSONObject(i)));
            }
            return new Favorite(id, name, tags, state);
        } catch (JSONException e) {
            Log.v(TAG, "Exception while processing Favorite JSON object");
            return null;
        }
    }

    public ContentValues getContentValues() {
        ContentValues values = state.getContentValues();

        values.put(LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID, getId());
        values.put(LocalContract.FavoriteEntry.COLUMN_NAME_ADDED, getAdded());
        values.put(LocalContract.FavoriteEntry.COLUMN_NAME_NAME, getName());

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

    public long getRowId() {
        return state.getRowId();
    }

    @Nullable
    public String getETag() {
        return state.getETag();
    }

    public int getDuplicated() {
        return state.getDuplicated();
    }

    public boolean isDuplicated() {
        return state.isDuplicated();
    }

    public boolean isConflicted() {
        return state.isConflicted();
    }

    public boolean isDeleted() {
        return state.isDeleted();
    }

    public boolean isSynced() {
        return state.isSynced();
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

    @Nullable
    public JSONObject getJsonObject() {
        if (isEmpty()) return null;
        assert tags != null;

        JSONObject jsonFavorite = new JSONObject();
        try {
            jsonFavorite.put(JSON_PROPERTY_ID, id);
            jsonFavorite.put(JSON_PROPERTY_NAME, name);
            JSONArray jsonTags = new JSONArray();
            for (Tag tag : tags) {
                jsonTags.put(tag.getJsonObject());
            }
            jsonFavorite.put(JSON_PROPERTY_TAGS, jsonTags);
        } catch (JSONException e) {
            return null;
        }
        return jsonFavorite;
    }

    public boolean isEmpty() {
        // NOTE: favorite must contain at least one tag
        return Strings.isNullOrEmpty(name) || tags == null || tags.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Favorite favorite = (Favorite) obj;
        return Objects.equal(id, favorite.id)
                && Objects.equal(name, favorite.name)
                && (tags == favorite.tags || (tags != null && tags.equals(favorite.tags)));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name);
    }
}
