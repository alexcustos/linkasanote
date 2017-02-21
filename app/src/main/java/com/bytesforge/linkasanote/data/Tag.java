package com.bytesforge.linkasanote.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import static java.lang.System.currentTimeMillis;

public final class Tag implements Serializable {

    private static final String TAG = Tag.class.getSimpleName();
    private static final String JSON_PROPERTY_NAME = "name";

    private final long added;

    @Nullable
    private final String name;

    public Tag(@Nullable String name) {
        this(currentTimeMillis(), name);
    }

    public Tag(long added, @Nullable String name) {
        this.added = added;
        this.name = name;
    }

    public static Tag from(Cursor cursor) {
        long added = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.TagEntry.COLUMN_NAME_ADDED));

        String name = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.TagEntry.COLUMN_NAME_NAME));

        return new Tag(added, name);
    }

    public static Tag from(ContentValues values) {
        long added = values.getAsLong(LocalContract.TagEntry.COLUMN_NAME_ADDED);

        String name = values.getAsString(LocalContract.TagEntry.COLUMN_NAME_NAME);

        return new Tag(added, name);
    }

    public static Tag from(JSONObject jsonTag) {
        try {
            String name = jsonTag.getString(JSON_PROPERTY_NAME);
            return new Tag(name);
        } catch (JSONException e) {
            Log.v(TAG, "Exception while processing Tag JSON object");
            return new Tag(null);
        }
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put(LocalContract.TagEntry.COLUMN_NAME_ADDED, getAdded());

        values.put(LocalContract.TagEntry.COLUMN_NAME_NAME, getName());

        return values;
    }

    public long getAdded() {
        return added;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public JSONObject getJsonObject() {
        if (isEmpty()) return null;

        JSONObject jsonTag = new JSONObject();
        try {
            jsonTag.put(JSON_PROPERTY_NAME, name);
        } catch (JSONException e) {
            return null;
        }
        return jsonTag;
    }

    public boolean isEmpty() {
        return Strings.isNullOrEmpty(name);
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

    @Override
    public String toString() {
        return name;
    }
}
