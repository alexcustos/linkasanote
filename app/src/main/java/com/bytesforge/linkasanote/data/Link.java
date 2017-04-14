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

public final class Link implements Comparable<Link> {

    private static final String TAG = Link.class.getSimpleName();

    public static final String CLOUD_DIRECTORY_NAME = "links";
    public static final String SETTING_LAST_SYNCED_ETAG = "LINKS_LAST_SYNCED_ETAG";

    private static final int JSON_VERSION = 1;
    private static final String JSON_CONTAINER_VERSION = "version";
    private static final String JSON_CONTAINER_LINK = "link";

    private static final String JSON_PROPERTY_ID = "id";
    private static final String JSON_PROPERTY_CREATED = "created";
    private static final String JSON_PROPERTY_UPDATED = "updated";
    private static final String JSON_PROPERTY_LINK = "link";
    private static final String JSON_PROPERTY_NAME = "name";
    private static final String JSON_PROPERTY_DISABLED = "disabled";
    private static final String JSON_PROPERTY_TAGS = "tags";

    @NonNull
    private final String id;

    private final long created;
    private final long updated;

    @Nullable
    private final String link;

    @Nullable
    private final String name;

    private final boolean disabled;

    @Nullable
    private final List<Tag> tags;

    /*@Nullable
    private final List<Note> notes;*/

    @NonNull
    private final SyncState state;

    public Link(String link, String name, boolean disabled, List<Tag> tags) {
        this(generateKey(), currentTimeMillis(), currentTimeMillis(),
                link, name, disabled, tags, new SyncState());
    }

    public Link(String id, String link, String name, boolean disabled, List<Tag> tags) {
        // NOTE: updating syncState must not change update entry
        this(id, 0, currentTimeMillis(), link, name, disabled, tags, new SyncState());
    }

    @VisibleForTesting
    public Link(
            String id, String link, String name, boolean disabled,
            List<Tag> tags, SyncState state) {
        this(id, 0, currentTimeMillis(), link, name, disabled, tags, state);
    }

    public Link(
            @NonNull String id, long created, long updated, @Nullable String link,
            @Nullable String name, boolean disabled, @Nullable List<Tag> tags,
            @NonNull SyncState state) {
        this.id = checkNotNull(id);
        this.created = created;
        this.updated = updated;
        this.link = link;
        this.name = name;
        this.disabled = disabled;
        this.tags = (tags == null || tags.isEmpty() ? null : tags);
        this.state = checkNotNull(state);
    }

    public Link(Link link, @NonNull SyncState state) {
        this(link.getId(), link.getCreated(), link.getUpdated(), link.getLink(),
                link.getName(), link.isDisabled(), link.getTags(), state);
    }

    public static Link from(Cursor cursor, List<Tag> tags) {
        SyncState state = SyncState.from(cursor);

        String id = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID));
        long created = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.LinkEntry.COLUMN_NAME_CREATED));
        long updated = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.LinkEntry.COLUMN_NAME_UPDATED));
        String link = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.LinkEntry.COLUMN_NAME_LINK));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.LinkEntry.COLUMN_NAME_NAME));
        boolean disabled = cursor.getInt(cursor.getColumnIndexOrThrow(
                LocalContract.LinkEntry.COLUMN_NAME_DISABLED)) == 1;

        return new Link(id, created, updated, link,
                Strings.isNullOrEmpty(name) ? null : name, disabled, tags, state);
    }

    public static Link from(ContentValues values, List<Tag> tags) {
        SyncState state = SyncState.from(values);

        String id = values.getAsString(LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID);
        long created = values.getAsLong(LocalContract.LinkEntry.COLUMN_NAME_CREATED);
        long updated = values.getAsLong(LocalContract.LinkEntry.COLUMN_NAME_UPDATED);
        String link = values.getAsString(LocalContract.LinkEntry.COLUMN_NAME_LINK);
        String name = values.getAsString(LocalContract.LinkEntry.COLUMN_NAME_NAME);
        boolean disabled = values.getAsBoolean(LocalContract.LinkEntry.COLUMN_NAME_DISABLED);

        return new Link(id, created, updated, link, name, disabled, tags, state);
    }

    @Nullable
    public static Link from(String jsonLinkString, SyncState state) {
        try {
            JSONObject jsonLink = new JSONObject(jsonLinkString);
            return from(jsonLink, state);
        } catch (JSONException e) {
            Log.v(TAG, "Exception while processing Link JSON string");
            return null;
        }
    }

    @Nullable
    public static Link from(JSONObject jsonContainer, SyncState state) {
        int version;
        try {
            version = jsonContainer.getInt(JSON_CONTAINER_VERSION);
        } catch (JSONException e) {
            Log.v(TAG, "Exception while checking Link JSON object version");
            return null;
        }
        if (version != JSON_VERSION) {
            Log.v(TAG, "An unsupported version of JSON object was detected [" + version + "]");
            return null;
        }
        try {
            JSONObject jsonLink = jsonContainer.getJSONObject(JSON_CONTAINER_LINK);
            String id = jsonLink.getString(JSON_PROPERTY_ID);
            long created = jsonLink.getLong(JSON_PROPERTY_CREATED);
            long updated = jsonLink.getLong(JSON_PROPERTY_UPDATED);
            String link = jsonLink.getString(JSON_PROPERTY_LINK);
            String name = jsonLink.getString(JSON_PROPERTY_NAME);
            boolean disabled = jsonLink.getBoolean(JSON_PROPERTY_DISABLED);
            JSONArray jsonTags = jsonLink.getJSONArray(JSON_PROPERTY_TAGS);
            List<Tag> tags = new ArrayList<>();
            for (int i = 0; i < jsonTags.length(); i++) {
                tags.add(Tag.from(jsonTags.getJSONObject(i)));
            }
            return new Link(id, created, updated, link, name, disabled, tags, state);
        } catch (JSONException e) {
            Log.v(TAG, "Exception while processing Link JSON object");
            return null;
        }
    }

    public ContentValues getContentValues() {
        ContentValues values = state.getContentValues();

        values.put(LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID, getId());
        long created = getCreated();
        if (created > 0) {
            values.put(LocalContract.LinkEntry.COLUMN_NAME_CREATED, created);
        }
        values.put(LocalContract.LinkEntry.COLUMN_NAME_UPDATED, getUpdated());
        values.put(LocalContract.LinkEntry.COLUMN_NAME_LINK, getLink());
        values.put(LocalContract.LinkEntry.COLUMN_NAME_NAME, getName());
        values.put(LocalContract.LinkEntry.COLUMN_NAME_DISABLED, isDisabled());

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
    public String getLink() {
        return link;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public boolean isDisabled() {
        return disabled;
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
            JSONObject jsonLink = new JSONObject();
            jsonLink.put(JSON_PROPERTY_ID, id);
            jsonLink.put(JSON_PROPERTY_CREATED, created);
            jsonLink.put(JSON_PROPERTY_UPDATED, updated);
            jsonLink.put(JSON_PROPERTY_LINK, link);
            jsonLink.put(JSON_PROPERTY_NAME, name);
            jsonLink.put(JSON_PROPERTY_DISABLED, disabled);
            JSONArray jsonTags = new JSONArray();
            for (Tag tag : tags) {
                jsonTags.put(tag.getJsonObject());
            }
            jsonLink.put(JSON_PROPERTY_TAGS, jsonTags);
            // Container
            jsonContainer.put(JSON_CONTAINER_VERSION, JSON_VERSION);
            jsonContainer.put(JSON_CONTAINER_LINK, jsonLink);
        } catch (JSONException e) {
            return null;
        }
        return jsonContainer;
    }

    public boolean isEmpty() {
        // NOTE: link must contain at least one tag
        return Strings.isNullOrEmpty(link);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Link link = (Link) obj;
        return Objects.equal(id, link.id)
                && Objects.equal(this.link, link.link)
                && Objects.equal(this.name, link.name)
                && (tags == link.tags || (tags != null && tags.equals(link.tags)));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, link, name);
    }

    @Override
    public int compareTo(@NonNull Link obj) {
        checkNotNull(obj);

        String objLink = obj.getLink();
        if (this == obj) return 0;
        if (link == null && objLink == null) return 0;
        if (link == null ^ objLink == null) {
            return link == null ? -1 : 1;
        }
        return link.compareTo(objLink);
    }
}
