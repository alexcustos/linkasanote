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

package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.BuildConfig;

import static com.google.common.base.Preconditions.checkNotNull;

public final class LocalContract {

    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";
    public static final String CONTENT_SCHEME = "content://";
    public static final Uri BASE_CONTENT_URI = Uri.parse(CONTENT_SCHEME + CONTENT_AUTHORITY);
    public static final String QUERY_PARAMETER_LIMIT = "limit";
    public static final String QUERY_PARAMETER_OFFSET = "offset";

    public static final String[] SYNC_STATE_COLUMNS = new String[]{
            BaseEntry._ID,
            BaseEntry.COLUMN_NAME_ETAG,
            BaseEntry.COLUMN_NAME_DUPLICATED,
            BaseEntry.COLUMN_NAME_CONFLICTED,
            BaseEntry.COLUMN_NAME_DELETED,
            BaseEntry.COLUMN_NAME_SYNCED};

    private LocalContract() {
    }

    public static String rowIdFrom(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow(BaseEntry._ID));
    }

    public static abstract class LinkEntry implements BaseEntry {

        public static final String TABLE_NAME = "link";

        public static final String COLUMN_NAME_LINK = "link";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DISABLED = "disabled";

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + LinkEntry.TABLE_NAME;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + LinkEntry.TABLE_NAME;

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();

        public static final String[] LINK_COLUMNS = new String[]{
                LinkEntry._ID,
                LinkEntry.COLUMN_NAME_ENTRY_ID,
                LinkEntry.COLUMN_NAME_CREATED,
                LinkEntry.COLUMN_NAME_UPDATED,
                LinkEntry.COLUMN_NAME_LINK,
                LinkEntry.COLUMN_NAME_NAME,
                LinkEntry.COLUMN_NAME_DISABLED,
                LinkEntry.COLUMN_NAME_ETAG,
                LinkEntry.COLUMN_NAME_DUPLICATED,
                LinkEntry.COLUMN_NAME_CONFLICTED,
                LinkEntry.COLUMN_NAME_DELETED,
                LinkEntry.COLUMN_NAME_SYNCED};

        public static Uri buildUri() {
            return CONTENT_URI.buildUpon().build();
        }

        public static Uri buildUriWith(long rowId) {
            return ContentUris.withAppendedId(CONTENT_URI, rowId);
        }

        public static Uri appendUriWith(Uri uri, int limit, long offset) {
            return uri.buildUpon()
                    .appendQueryParameter(QUERY_PARAMETER_LIMIT, String.valueOf(limit))
                    .appendQueryParameter(QUERY_PARAMETER_OFFSET, String.valueOf(offset))
                    .build();
        }

        public static Uri buildUriWith(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }

        public static Uri buildTagsDirUriWith(long rowId) {
            return ContentUris.withAppendedId(CONTENT_URI, rowId).buildUpon()
                    .appendEncodedPath(TagEntry.TABLE_NAME).build();
        }

        public static Uri buildTagsDirUriWith(String id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(id).appendEncodedPath(TagEntry.TABLE_NAME).build();
        }

        public static Uri buildNotesDirUriWith(String id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(id).appendEncodedPath(NoteEntry.TABLE_NAME).build();
        }

        public static String getIdFrom(@NonNull Uri uri) {
            return checkNotNull(uri).getPathSegments().get(1);
        }
    }

    public static abstract class NoteEntry implements BaseEntry {

        public static final String TABLE_NAME = "note";

        public static final String COLUMN_NAME_NOTE = "note";
        public static final String COLUMN_NAME_LINK_ID = "link_id";

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + NoteEntry.TABLE_NAME;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + NoteEntry.TABLE_NAME;

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendEncodedPath(TABLE_NAME).build();

        public static final String[] NOTE_COLUMNS = new String[]{
                NoteEntry._ID,
                NoteEntry.COLUMN_NAME_ENTRY_ID,
                NoteEntry.COLUMN_NAME_CREATED,
                NoteEntry.COLUMN_NAME_UPDATED,
                NoteEntry.COLUMN_NAME_NOTE,
                NoteEntry.COLUMN_NAME_LINK_ID,
                NoteEntry.COLUMN_NAME_ETAG,
                NoteEntry.COLUMN_NAME_DUPLICATED,
                NoteEntry.COLUMN_NAME_CONFLICTED,
                NoteEntry.COLUMN_NAME_DELETED,
                NoteEntry.COLUMN_NAME_SYNCED};

        public static Uri buildUri() {
            return CONTENT_URI.buildUpon().build();
        }

        public static Uri buildUriWith(long rowId) {
            return ContentUris.withAppendedId(CONTENT_URI, rowId);
        }

        public static Uri appendUriWith(Uri uri, int limit, long offset) {
            return uri.buildUpon()
                    .appendQueryParameter(QUERY_PARAMETER_LIMIT, String.valueOf(limit))
                    .appendQueryParameter(QUERY_PARAMETER_OFFSET, String.valueOf(offset))
                    .build();
        }

        public static Uri buildUriWith(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }

        public static Uri buildTagsDirUriWith(long rowId) {
            return ContentUris.withAppendedId(CONTENT_URI, rowId).buildUpon()
                    .appendEncodedPath(TagEntry.TABLE_NAME).build();
        }

        public static Uri buildTagsDirUriWith(String id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(id).appendEncodedPath(TagEntry.TABLE_NAME).build();
        }

        public static String getIdFrom(@NonNull Uri uri) {
            return checkNotNull(uri).getPathSegments().get(1);
        }
    }

    public static abstract class FavoriteEntry implements BaseEntry {

        public static final String TABLE_NAME = "favorite";

        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_AND_GATE = "and_gate";

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + FavoriteEntry.TABLE_NAME;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + FavoriteEntry.TABLE_NAME;

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendEncodedPath(TABLE_NAME).build();

        public static final String[] FAVORITE_COLUMNS = new String[]{
                FavoriteEntry._ID,
                FavoriteEntry.COLUMN_NAME_ENTRY_ID,
                FavoriteEntry.COLUMN_NAME_CREATED,
                FavoriteEntry.COLUMN_NAME_UPDATED,
                FavoriteEntry.COLUMN_NAME_NAME,
                FavoriteEntry.COLUMN_NAME_AND_GATE,
                FavoriteEntry.COLUMN_NAME_ETAG,
                FavoriteEntry.COLUMN_NAME_DUPLICATED,
                FavoriteEntry.COLUMN_NAME_CONFLICTED,
                FavoriteEntry.COLUMN_NAME_DELETED,
                FavoriteEntry.COLUMN_NAME_SYNCED};

        public static Uri buildUri() {
            return CONTENT_URI.buildUpon().build();
        }

        public static Uri buildUriWith(long rowId) {
            return ContentUris.withAppendedId(CONTENT_URI, rowId);
        }

        public static Uri buildUriWith(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }

        public static Uri appendUriWith(Uri uri, int limit, long offset) {
            return uri.buildUpon()
                    .appendQueryParameter(QUERY_PARAMETER_LIMIT, String.valueOf(limit))
                    .appendQueryParameter(QUERY_PARAMETER_OFFSET, String.valueOf(offset))
                    .build();
        }

        public static Uri buildTagsDirUriWith(long rowId) {
            return ContentUris.withAppendedId(CONTENT_URI, rowId).buildUpon()
                    .appendEncodedPath(TagEntry.TABLE_NAME).build();
        }

        public static Uri buildTagsDirUriWith(String id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(id).appendEncodedPath(TagEntry.TABLE_NAME).build();
        }

        public static String getIdFrom(@NonNull Uri uri) {
            return checkNotNull(uri).getPathSegments().get(1);
        }
    }

    public static abstract class TagEntry implements BaseColumns {

        public static final String TABLE_NAME = "tag";

        public static final String COLUMN_NAME_CREATED = BaseEntry.COLUMN_NAME_CREATED;
        public static final String COLUMN_NAME_NAME = "name";

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + TagEntry.TABLE_NAME;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + TagEntry.TABLE_NAME;

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendEncodedPath(TABLE_NAME).build();

        public static final String[] TAG_COLUMNS = new String[]{
                TABLE_NAME + "." + TagEntry._ID,
                TABLE_NAME + "." + TagEntry.COLUMN_NAME_CREATED,
                TABLE_NAME + "." + TagEntry.COLUMN_NAME_NAME};

        public static Uri buildUri() {
            return CONTENT_URI.buildUpon().build();
        }

        public static Uri buildUriWith(long rowId) {
            return ContentUris.withAppendedId(CONTENT_URI, rowId);
        }

        public static Uri buildUriWith(String name) {
            return CONTENT_URI.buildUpon().appendPath(name).build();
        }

        public static String getNameFrom(@NonNull Uri uri) {
            return checkNotNull(uri).getPathSegments().get(1);
        }
    }

    public static abstract class SyncResultEntry implements BaseColumns {

        public static final String TABLE_NAME = "sync_result";

        public static final String COLUMN_NAME_CREATED = BaseEntry.COLUMN_NAME_CREATED;
        public static final String COLUMN_NAME_STARTED = "started";
        public static final String COLUMN_NAME_ENTRY = "entry";
        public static final String COLUMN_NAME_ENTRY_ID = "entry_id";
        public static final String COLUMN_NAME_RESULT = "result";
        public static final String COLUMN_NAME_APPLIED = "applied";

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + SyncResultEntry.TABLE_NAME;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + SyncResultEntry.TABLE_NAME;

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendEncodedPath(TABLE_NAME).build();

        public enum Result {
            UPLOADED, DOWNLOADED, DELETED, SYNCED, RELATED, CONFLICT, ERROR}

        public static final String[] SYNC_RESULT_COLUMNS = new String[]{
                SyncResultEntry._ID,
                SyncResultEntry.COLUMN_NAME_CREATED,
                SyncResultEntry.COLUMN_NAME_STARTED,
                SyncResultEntry.COLUMN_NAME_ENTRY,
                SyncResultEntry.COLUMN_NAME_ENTRY_ID,
                SyncResultEntry.COLUMN_NAME_RESULT,
                SyncResultEntry.COLUMN_NAME_APPLIED};

        public static Uri buildUri() {
            return CONTENT_URI.buildUpon().build();
        }

        public static Uri buildUriWith(@NonNull String entry) {
            return CONTENT_URI.buildUpon().appendPath(checkNotNull(entry)).build();
        }

        public static Uri buildUriWith(long rowId) {
            return ContentUris.withAppendedId(CONTENT_URI, rowId);
        }

        public static String getEntryFrom(@NonNull Uri uri) {
            return checkNotNull(uri).getPathSegments().get(1);
        }

        public static long getIdFrom(@NonNull Uri uri) {
            return Long.parseLong(checkNotNull(uri).getPathSegments().get(1));
        }
    }
}
