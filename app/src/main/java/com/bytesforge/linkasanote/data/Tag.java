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
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

public final class Tag implements Serializable, Parcelable, Comparable<Tag> {

    private static final String TAG = Tag.class.getSimpleName();

    // NOTE: tag is a part of the other containers, so it itself has no version information
    private static final String JSON_PROPERTY_NAME = "name";

    private final long created;

    @Nullable
    private final String name;

    public Tag(@Nullable String name) {
        this(currentTimeMillis(), name);
    }

    public Tag(long created, @Nullable String name) {
        this.created = created;
        this.name = Strings.isNullOrEmpty(name) ? null : name.toLowerCase();
    }

    protected Tag(Parcel in) {
        created = in.readLong();
        name = in.readString();
    }

    public static final Creator<Tag> CREATOR = new Creator<Tag>() {

        @Override
        public Tag createFromParcel(Parcel in) {
            return new Tag(in);
        }

        @Override
        public Tag[] newArray(int size) {
            return new Tag[size];
        }
    };

    @Override
    public int describeContents() {
        return super.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(created);
        dest.writeString(name);
    }

    public static Tag from(Cursor cursor) {
        long created = cursor.getLong(cursor.getColumnIndexOrThrow(
                LocalContract.TagEntry.COLUMN_NAME_CREATED));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(
                LocalContract.TagEntry.COLUMN_NAME_NAME));
        return new Tag(created, name);
    }

    public static Tag from(ContentValues values) {
        long created = values.getAsLong(LocalContract.TagEntry.COLUMN_NAME_CREATED);
        String name = values.getAsString(LocalContract.TagEntry.COLUMN_NAME_NAME);
        return new Tag(created, name);
    }

    public static Tag from(JSONObject jsonTag) {
        try {
            // NOTE: created must not be here
            String name = jsonTag.getString(JSON_PROPERTY_NAME);
            return new Tag(name);
        } catch (JSONException e) {
            Log.v(TAG, "Exception while processing Tag JSON object");
            return new Tag((String) null);
        }
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put(LocalContract.TagEntry.COLUMN_NAME_CREATED, getCreated());
        values.put(LocalContract.TagEntry.COLUMN_NAME_NAME, getName());
        return values;
    }

    public long getCreated() {
        return created;
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

    @Override
    public int compareTo(@NonNull Tag obj) {
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
