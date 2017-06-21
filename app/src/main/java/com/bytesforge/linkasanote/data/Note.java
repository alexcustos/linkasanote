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

public final class Note implements Comparable<Note>, Item {

    private static final String TAG = Note.class.getSimpleName();

    public static final String CLOUD_DIRECTORY_NAME = "notes";
    public static final String SETTING_LAST_SYNCED_ETAG = "NOTES_LAST_SYNCED_ETAG";

    private static final int JSON_VERSION = 1;
    private static final String JSON_CONTAINER_VERSION = "version";
    private static final String JSON_CONTAINER_NOTE = "note";

    private static final String JSON_PROPERTY_ID = "id";
    private static final String JSON_PROPERTY_CREATED = "created";
    private static final String JSON_PROPERTY_UPDATED = "updated";
    private static final String JSON_PROPERTY_NOTE = "note";
    private static final String JSON_PROPERTY_LINK_ID = "link_id";
    private static final String JSON_PROPERTY_TAGS = "tags";

    @NonNull
    private final String id;

    private final long created;
    private final long updated;

    @Nullable
    private final String note;

    @Nullable
    private final String linkId; // NOTE: may be null if it is unbound Note

    @Nullable
    private final List<Tag> tags;

    @NonNull
    private final SyncState state;

    public Note(String note, String linkId, List<Tag> tags) {
        this(generateKey(), currentTimeMillis(), currentTimeMillis(),
                note, linkId, tags, new SyncState());
    }

    public Note(String id, String note, String linkId, List<Tag> tags) {
        // NOTE: updating syncState must not change update entry
        this(id, 0, currentTimeMillis(), note, linkId, tags, new SyncState());
    }

    public Note(String id, String note, String linkId, List<Tag> tags, SyncState state) {
        this(id, 0, currentTimeMillis(), note, linkId, tags, state);
    }

    public Note(Note note, List<Tag> tags) {
        this(note.getId(), note.getCreated(), note.getUpdated(),
                note.getNote(), note.getLinkId(), tags, note.getState());
    }

    public Note(Note note, @NonNull SyncState state) {
        this(note.getId(), note.getCreated(), note.getUpdated(), note.getNote(),
                note.getLinkId(), note.getTags(), state);
    }

    public Note(
            @NonNull String id, long created, long updated, @Nullable String note,
            @Nullable String linkId, @Nullable List<Tag> tags, @NonNull SyncState state) {
        this.id = checkNotNull(id);
        this.created = created;
        this.updated = updated;
        this.note = note;
        this.linkId = Strings.isNullOrEmpty(linkId) ? null : linkId;
        this.tags = (tags == null || tags.isEmpty() ? null : tags);
        this.state = checkNotNull(state);
    }

    public static Note from(Cursor cursor) {
        SyncState state = SyncState.from(cursor);

        String id = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID));
        long created = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.NoteEntry.COLUMN_NAME_CREATED));
        long updated = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.NoteEntry.COLUMN_NAME_UPDATED));
        String note = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.NoteEntry.COLUMN_NAME_NOTE));
        String linkId = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.NoteEntry.COLUMN_NAME_LINK_ID));

        return new Note(id, created, updated, note, linkId, null, state);
    }

    public static Note from(ContentValues values) {
        SyncState state = SyncState.from(values);

        String id = values.getAsString(LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID);
        long created = values.getAsLong(LocalContract.NoteEntry.COLUMN_NAME_CREATED);
        long updated = values.getAsLong(LocalContract.NoteEntry.COLUMN_NAME_UPDATED);
        String note = values.getAsString(LocalContract.NoteEntry.COLUMN_NAME_NOTE);
        String linkId = values.getAsString(LocalContract.NoteEntry.COLUMN_NAME_LINK_ID);

        return new Note(id, created, updated, note, linkId, null, state);
    }

    @Nullable
    public static Note from(String jsonNoteString, SyncState state) {
        try {
            JSONObject jsonNote = new JSONObject(jsonNoteString);
            return from(jsonNote, state);
        } catch (JSONException e) {
            Log.v(TAG, "Exception while processing Note JSON string");
            return null;
        }
    }

    @Nullable
    public static Note from(JSONObject jsonContainer, SyncState state) {
        int version;
        try {
            version = jsonContainer.getInt(JSON_CONTAINER_VERSION);
        } catch (JSONException e) {
            Log.v(TAG, "Exception while checking Note JSON object version");
            return null;
        }
        if (version != JSON_VERSION) {
            Log.v(TAG, "An unsupported version of JSON object was detected [" + version + "]");
            return null;
        }
        try {
            JSONObject jsonNote = jsonContainer.getJSONObject(JSON_CONTAINER_NOTE);
            String id = jsonNote.getString(JSON_PROPERTY_ID);
            if (UuidUtils.isKeyValidUuid(id)) {
                long created = jsonNote.getLong(JSON_PROPERTY_CREATED);
                long updated = jsonNote.getLong(JSON_PROPERTY_UPDATED);
                String note = jsonNote.getString(JSON_PROPERTY_NOTE);
                String linkId = jsonNote.getString(JSON_PROPERTY_LINK_ID);
                JSONArray jsonTags = jsonNote.getJSONArray(JSON_PROPERTY_TAGS);
                List<Tag> tags = new ArrayList<>();
                for (int i = 0; i < jsonTags.length(); i++) {
                    tags.add(Tag.from(jsonTags.getJSONObject(i)));
                }
                return new Note(id, created, updated, note, linkId, tags, state);
            } else {
                return null;
            }
        } catch (JSONException e) {
            Log.w(TAG, "Exception while processing Note JSON object [" + e.getMessage() + "]");
            return null;
        }
    }

    public ContentValues getContentValues() {
        ContentValues values = state.getContentValues();

        values.put(LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID, getId());
        long created = getCreated();
        if (created > 0) {
            // NOTE: CURRENT_TIMESTAMP will add another dateTime format to maintain
            values.put(LocalContract.NoteEntry.COLUMN_NAME_CREATED, created);
        }
        values.put(LocalContract.NoteEntry.COLUMN_NAME_UPDATED, getUpdated());
        values.put(LocalContract.NoteEntry.COLUMN_NAME_NOTE, getNote());
        values.put(LocalContract.NoteEntry.COLUMN_NAME_LINK_ID, getLinkId());

        return values;
    }

    @NonNull
    public SyncState getState() {
        return state;
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

    @Override
    public boolean isConflicted() {
        return state.isConflicted();
    }

    public boolean isDeleted() {
        return state.isDeleted();
    }

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
    public String getNote() {
        return note;
    }

    @Nullable
    public String getLinkId() {
        return linkId;
    }

    @Nullable
    public List<Tag> getTags() {
        return tags;
    }

    @Override
    public String getDuplicatedKey() {
        throw new RuntimeException("Note does not have unique constraint for this option");
    }

    @Override
    public String getRelatedId() {
        return getLinkId();
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

        JSONObject jsonContainer = new JSONObject();
        try {
            JSONObject jsonNote = new JSONObject();
            jsonNote.put(JSON_PROPERTY_ID, id);
            jsonNote.put(JSON_PROPERTY_CREATED, created);
            jsonNote.put(JSON_PROPERTY_UPDATED, updated);
            jsonNote.put(JSON_PROPERTY_NOTE, note);
            jsonNote.put(JSON_PROPERTY_LINK_ID, linkId == null ? "" : linkId);
            JSONArray jsonTags = new JSONArray();
            if (tags != null) {
                for (Tag tag : tags) {
                    jsonTags.put(tag.getJsonObject());
                }
            }
            jsonNote.put(JSON_PROPERTY_TAGS, jsonTags);
            // Container
            jsonContainer.put(JSON_CONTAINER_VERSION, JSON_VERSION);
            jsonContainer.put(JSON_CONTAINER_NOTE, jsonNote);
        } catch (JSONException e) {
            return null;
        }
        return jsonContainer;
    }

    @Override
    public boolean isEmpty() {
        return Strings.isNullOrEmpty(note);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Note note = (Note) obj;
        return Objects.equal(id, note.id)
                && Objects.equal(this.note, note.note);
                //&& (tags == note.tags || (tags != null && tags.equals(note.tags)));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, note);
    }

    @Override
    public int compareTo(@NonNull Note obj) {
        checkNotNull(obj);

        String objNote = obj.getNote();
        if (this == obj) return 0;
        if (note == null && objNote == null) return 0;
        if (note == null ^ objNote == null) {
            return note == null ? -1 : 1;
        }
        return note.compareTo(objNote);
    }

    @Override
    public String toString() {
        return getId();
    }

    public static NoteFactory<Note> getFactory() {
        return new NoteFactory<Note>() {

            @Override
            public Note build(Note note, List<Tag> tags) {
                return new Note(note, tags);
            }

            @Override
            public Note buildOrphaned(Note note) {
                SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                return new Note(
                        note.getId(), note.getCreated(), note.getUpdated(),
                        note.getNote(), null, note.getTags(), state);
            }

            @Override
            public Note from(Cursor cursor) {
                return Note.from(cursor);
            }

            @Override
            public Note from(String jsonFavoriteString, SyncState state) {
                return Note.from(jsonFavoriteString, state);
            }
        };
    }
}
