package com.bytesforge.linkasanote.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
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

public final class Favorite implements Comparable<Favorite> {

    private static final String TAG = Favorite.class.getSimpleName();

    public static final String CLOUD_DIRECTORY_NAME = "favorites";
    public static final String SETTING_LAST_SYNCED_ETAG = "FAVORITES_LAST_SYNCED_ETAG";

    private static final int JSON_VERSION = 1;
    private static final String JSON_CONTAINER_VERSION = "version";
    private static final String JSON_CONTAINER_FAVORITE = "favorite";

    private static final String JSON_PROPERTY_ID = "id";
    private static final String JSON_PROPERTY_CREATED = "created";
    private static final String JSON_PROPERTY_UPDATED = "updated";
    private static final String JSON_PROPERTY_NAME = "name";
    private static final String JSON_PROPERTY_TAGS = "tags";

    @NonNull
    private final String id;

    private final long created;
    private final long updated;

    @Nullable
    private final String name;

    @Nullable
    private final List<Tag> tags;

    @NonNull
    private final SyncState state;

    public Favorite(String name, List<Tag> tags) {
        this(generateKey(), currentTimeMillis(), currentTimeMillis(), name, tags, new SyncState());
    }

    public Favorite(String id, String name, List<Tag> tags) {
        // NOTE: updating syncState must not change update entry
        this(id, 0, currentTimeMillis(), name, tags, new SyncState());
    }

    @VisibleForTesting
    public Favorite(String id, String name, List<Tag> tags, SyncState state) {
        this(id, 0, currentTimeMillis(), name, tags, state);
    }

    public Favorite(
            @NonNull String id, long created, long updated, @Nullable String name,
            @Nullable List<Tag> tags, @NonNull SyncState state) {
        this.id = checkNotNull(id);
        this.created = created;
        this.updated = updated;
        this.name = name;
        this.tags = (tags == null || tags.isEmpty() ? null : tags);
        this.state = checkNotNull(state);
    }

    public Favorite(Favorite favorite, @NonNull SyncState state) {
        this(favorite.getId(), favorite.getCreated(), favorite.getUpdated(), favorite.getName(),
                favorite.getTags(), state);
    }

    public static Favorite from(Cursor cursor, List<Tag> tags) {
        SyncState state = SyncState.from(cursor);

        String id = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID));
        long created = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_CREATED));
        long updated = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_UPDATED));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_NAME));

        return new Favorite(id, created, updated, name, tags, state);
    }

    public static Favorite from(ContentValues values, List<Tag> tags) {
        SyncState state = SyncState.from(values);

        String id = values.getAsString(LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID);
        long created = values.getAsLong(LocalContract.FavoriteEntry.COLUMN_NAME_CREATED);
        long updated = values.getAsLong(LocalContract.FavoriteEntry.COLUMN_NAME_UPDATED);
        String name = values.getAsString(LocalContract.FavoriteEntry.COLUMN_NAME_NAME);

        return new Favorite(id, created, updated, name, tags, state);
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
    public static Favorite from(JSONObject jsonContainer, SyncState state) {
        int version;
        try {
            version = jsonContainer.getInt(JSON_CONTAINER_VERSION);
        } catch (JSONException e) {
            Log.v(TAG, "Exception while checking Favorite JSON object version");
            return null;
        }
        if (version != JSON_VERSION) {
            Log.v(TAG, "An unsupported version of JSON object was detected [" + version + "]");
            return null;
        }
        try {
            JSONObject jsonFavorite = jsonContainer.getJSONObject(JSON_CONTAINER_FAVORITE);
            String id = jsonFavorite.getString(JSON_PROPERTY_ID);
            long created = jsonFavorite.getLong(JSON_PROPERTY_CREATED);
            long updated = jsonFavorite.getLong(JSON_PROPERTY_UPDATED);
            String name = jsonFavorite.getString(JSON_PROPERTY_NAME);
            JSONArray jsonTags = jsonFavorite.getJSONArray(JSON_PROPERTY_TAGS);
            List<Tag> tags = new ArrayList<>();
            for (int i = 0; i < jsonTags.length(); i++) {
                tags.add(Tag.from(jsonTags.getJSONObject(i)));
            }
            return new Favorite(id, created, updated, name, tags, state);
        } catch (JSONException e) {
            Log.v(TAG, "Exception while processing Favorite JSON object");
            return null;
        }
    }

    public ContentValues getContentValues() {
        ContentValues values = state.getContentValues();

        values.put(LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID, getId());
        long created = getCreated();
        if (created > 0) {
            // NOTE: CURRENT_TIMESTAMP will add another dateTime format to maintain
            values.put(LocalContract.FavoriteEntry.COLUMN_NAME_CREATED, created);
        }
        values.put(LocalContract.FavoriteEntry.COLUMN_NAME_UPDATED, getUpdated());
        values.put(LocalContract.FavoriteEntry.COLUMN_NAME_NAME, getName());

        return values;
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

    public long getCreated() {
        return created;
    }

    public long getUpdated() {
        return updated;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public List<Tag> getTags() {
        return tags;
    }

    @Nullable
    public String getTagsAsString() {
        if (tags != null) {
            Joiner joiner = Joiner.on(", ");
            return joiner.join(tags);
        }
        return null;
    }

    @Nullable
    public JSONObject getJsonObject() {
        if (isEmpty()) return null;
        assert tags != null;

        JSONObject jsonContainer = new JSONObject();
        try {
            JSONObject jsonFavorite = new JSONObject();
            jsonFavorite.put(JSON_PROPERTY_ID, id);
            jsonFavorite.put(JSON_PROPERTY_CREATED, created);
            jsonFavorite.put(JSON_PROPERTY_UPDATED, updated);
            jsonFavorite.put(JSON_PROPERTY_NAME, name);
            JSONArray jsonTags = new JSONArray();
            for (Tag tag : tags) {
                jsonTags.put(tag.getJsonObject());
            }
            jsonFavorite.put(JSON_PROPERTY_TAGS, jsonTags);
            // Container
            jsonContainer.put(JSON_CONTAINER_VERSION, JSON_VERSION);
            jsonContainer.put(JSON_CONTAINER_FAVORITE, jsonFavorite);
        } catch (JSONException e) {
            return null;
        }
        return jsonContainer;
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

    @Override
    public int compareTo(@NonNull Favorite obj) {
        checkNotNull(obj);

        String objName = obj.getName();
        if (this == obj) return 0;
        if (name == null && objName == null) return 0;
        if (name == null ^ objName == null) {
            return name == null ? -1 : 1;
        }
        return name.compareTo(objName);
    }
}
