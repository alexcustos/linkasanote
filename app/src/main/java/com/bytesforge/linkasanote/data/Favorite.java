/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bytesforge.linkasanote.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.utils.UuidUtils;
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

public final class Favorite implements Comparable<Favorite>, Item {

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
    private static final String JSON_PROPERTY_AND_GATE = "and_gate";
    private static final String JSON_PROPERTY_TAGS = "tags";

    @NonNull
    private final String id;

    private final long created;
    private final long updated;

    @Nullable
    private final String name;

    private final boolean andGate;

    @Nullable
    private final List<Tag> tags;

    @NonNull
    private final SyncState state;

    public Favorite(String name, boolean andGate, List<Tag> tags) {
        this(generateKey(), currentTimeMillis(), currentTimeMillis(),
                name, andGate, tags, new SyncState());
    }

    public Favorite(String id, String name, boolean andGate, List<Tag> tags) {
        // NOTE: updating syncState must not change update entry
        this(id, 0, currentTimeMillis(), name, andGate, tags, new SyncState());
    }

    public Favorite(String id, String name, boolean andGate, List<Tag> tags, SyncState state) {
        this(id, 0, currentTimeMillis(), name, andGate, tags, state);
    }

    public Favorite(Favorite favorite, List<Tag> tags) {
        this(favorite.getId(), favorite.getCreated(), favorite.getUpdated(),
                favorite.getName(), favorite.isAndGate(), tags, favorite.getState());
    }

    public Favorite(Favorite favorite, @NonNull SyncState state) {
        this(favorite.getId(), favorite.getCreated(), favorite.getUpdated(),
                favorite.getName(), favorite.isAndGate(), favorite.getTags(), state);
    }

    public Favorite(
            @NonNull String id, long created, long updated, @Nullable String name,
            boolean andGate, @Nullable List<Tag> tags, @NonNull SyncState state) {
        this.id = checkNotNull(id);
        this.created = created;
        this.updated = updated;
        this.name = name;
        this.andGate = andGate;
        this.tags = (tags == null || tags.isEmpty() ? null : tags);
        this.state = checkNotNull(state);
    }

    public static Favorite from(Cursor cursor) {
        SyncState state = SyncState.from(cursor);

        String id = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID));
        long created = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_CREATED));
        long updated = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_UPDATED));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_NAME));
        boolean andGate = cursor.getInt(cursor.getColumnIndexOrThrow(
                LocalContract.FavoriteEntry.COLUMN_NAME_AND_GATE)) == 1;

        return new Favorite(id, created, updated, name, andGate, null, state);
    }

    public static Favorite from(ContentValues values) {
        SyncState state = SyncState.from(values);

        String id = values.getAsString(LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID);
        long created = values.getAsLong(LocalContract.FavoriteEntry.COLUMN_NAME_CREATED);
        long updated = values.getAsLong(LocalContract.FavoriteEntry.COLUMN_NAME_UPDATED);
        String name = values.getAsString(LocalContract.FavoriteEntry.COLUMN_NAME_NAME);
        boolean andGate = values.getAsBoolean(LocalContract.FavoriteEntry.COLUMN_NAME_AND_GATE);

        return new Favorite(id, created, updated, name, andGate, null, state);
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
            if (UuidUtils.isKeyValidUuid(id)) {
                long created = jsonFavorite.getLong(JSON_PROPERTY_CREATED);
                long updated = jsonFavorite.getLong(JSON_PROPERTY_UPDATED);
                String name = jsonFavorite.getString(JSON_PROPERTY_NAME);
                boolean andGate = jsonFavorite.getBoolean(JSON_PROPERTY_AND_GATE);
                JSONArray jsonTags = jsonFavorite.getJSONArray(JSON_PROPERTY_TAGS);
                List<Tag> tags = new ArrayList<>();
                for (int i = 0; i < jsonTags.length(); i++) {
                    tags.add(Tag.from(jsonTags.getJSONObject(i)));
                }
                return new Favorite(id, created, updated, name, andGate, tags, state);
            } else {
                return null;
            }
        } catch (JSONException e) {
            Log.w(TAG, "Exception while processing Favorite JSON object [" + e.getMessage() + "]");
            return null;
        }
    }

    @Override
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
        values.put(LocalContract.FavoriteEntry.COLUMN_NAME_AND_GATE, isAndGate());

        return values;
    }

    @NonNull
    public SyncState getState() {
        return state;
    }

    @Override
    public long getRowId() {
        return state.getRowId();
    }

    @Override
    @Nullable
    public String getETag() {
        return state.getETag();
    }

    public int getDuplicated() {
        return state.getDuplicated();
    }

    @Override
    public boolean isDuplicated() {
        return state.isDuplicated();
    }

    @Override
    public boolean isConflicted() {
        return state.isConflicted();
    }

    @Override
    public boolean isDeleted() {
        return state.isDeleted();
    }

    @Override
    public boolean isSynced() {
        return state.isSynced();
    }

    @Override
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

    public boolean isAndGate() {
        return andGate;
    }

    @Override
    public String getDuplicatedKey() {
        return getName();
    }

    @Override
    public String getRelatedId() {
        return null; // NOTE: there is no related object
    }

    @Override
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

    @Override
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
            jsonFavorite.put(JSON_PROPERTY_AND_GATE, andGate);
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

    @Override
    public boolean isEmpty() {
        // NOTE: favorite must contain at least one tag
        return Strings.isNullOrEmpty(name) || tags == null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Favorite favorite = (Favorite) obj;
        return Objects.equal(id, favorite.id)
                && Objects.equal(name, favorite.name)
                && Objects.equal(andGate, favorite.andGate)
                && (tags == favorite.tags || (tags != null && tags.equals(favorite.tags)));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name, andGate, tags);
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

    @Override
    public String toString() {
        return getId();
    }

    public static FavoriteFactory<Favorite> getFactory() {
        return new FavoriteFactory<Favorite>() {

            @Override
            public Favorite build(Favorite item, List<Tag> tags) {
                return new Favorite(item, tags);
            }

            @Override
            public Favorite build(Favorite item, @NonNull SyncState state) {
                return new Favorite(item, state);
            }

            @Override
            public Favorite from(Cursor cursor) {
                return Favorite.from(cursor);
            }

            @Override
            public Favorite from(String jsonFavoriteString, SyncState state) {
                return Favorite.from(jsonFavoriteString, state);
            }
        };
    }
}
