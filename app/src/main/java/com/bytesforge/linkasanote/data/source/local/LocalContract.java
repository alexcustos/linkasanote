package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import com.bytesforge.linkasanote.BuildConfig;

public final class LocalContract {

    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";
    public static final String CONTENT_SCHEME = "content://";
    public static final Uri BASE_CONTENT_URI = Uri.parse(CONTENT_SCHEME + CONTENT_AUTHORITY);
    public static final String MANY_TO_MANY_COLUMN_NAME_ADDED = "added";

    private LocalContract() {
    }

    public static abstract class LinkEntry implements BaseColumns {

        public static final String TABLE_NAME = "link";

        public static final String COLUMN_NAME_ENTRY_ID = "entry_id";
        public static final String COLUMN_NAME_CREATED = "created";
        public static final String COLUMN_NAME_UPDATED = "updated";
        public static final String COLUMN_NAME_VALUE = "value";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_DISABLED = "disabled";
        public static final String COLUMN_NAME_DELETED = "deleted";
        public static final String COLUMN_NAME_SYNCED = "synced";

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + LinkEntry.TABLE_NAME;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + LinkEntry.TABLE_NAME;

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();

        public static String[] LINK_COLUMNS = new String[]{
                LinkEntry._ID,
                LinkEntry.COLUMN_NAME_ENTRY_ID,
                LinkEntry.COLUMN_NAME_CREATED,
                LinkEntry.COLUMN_NAME_UPDATED,
                LinkEntry.COLUMN_NAME_VALUE,
                LinkEntry.COLUMN_NAME_TITLE,
                LinkEntry.COLUMN_NAME_DISABLED,
                LinkEntry.COLUMN_NAME_DELETED,
                LinkEntry.COLUMN_NAME_SYNCED};

        public static Uri buildLinksUri() {
            return CONTENT_URI.buildUpon().build();
        }

        public static Uri buildLinksUriWith(long rowId) {
            return ContentUris.withAppendedId(CONTENT_URI, rowId);
        }

        public static Uri buildLinksUriWith(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }

        public static String getLinkId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static abstract class NoteEntry implements BaseColumns {

        public static final String TABLE_NAME = "note";

        public static final String COLUMN_NAME_ENTRY_ID = "entry_id";
        public static final String COLUMN_NAME_CREATED = "created";
        public static final String COLUMN_NAME_UPDATED = "updated";
        public static final String COLUMN_NAME_EXCERPT = "excerpt";
        public static final String COLUMN_NAME_DELETED = "deleted";
        public static final String COLUMN_NAME_SYNCED = "synced";

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + NoteEntry.TABLE_NAME;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + NoteEntry.TABLE_NAME;

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendEncodedPath(TABLE_NAME).build();

        public static String[] NOTE_COLUMNS = new String[]{
                NoteEntry._ID,
                NoteEntry.COLUMN_NAME_ENTRY_ID,
                NoteEntry.COLUMN_NAME_CREATED,
                NoteEntry.COLUMN_NAME_UPDATED,
                NoteEntry.COLUMN_NAME_EXCERPT,
                NoteEntry.COLUMN_NAME_DELETED,
                NoteEntry.COLUMN_NAME_SYNCED};

        public static Uri buildNotesUri() {
            return CONTENT_URI.buildUpon().build();
        }

        public static Uri buildNotesUriWith(long rowId) {
            return ContentUris.withAppendedId(CONTENT_URI, rowId);
        }

        public static Uri buildNotesUriWith(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }

        public static String getNoteId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static abstract class FavoriteEntry implements BaseColumns {

        public static final String TABLE_NAME = "favorite";

        public static final String COLUMN_NAME_ENTRY_ID = "entry_id";
        public static final String COLUMN_NAME_ADDED = "added";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_SYNCED = "synced";

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + FavoriteEntry.TABLE_NAME;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + FavoriteEntry.TABLE_NAME;

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendEncodedPath(TABLE_NAME).build();

        public static String[] FAVORITE_COLUMNS = new String[]{
                FavoriteEntry._ID,
                FavoriteEntry.COLUMN_NAME_ENTRY_ID,
                FavoriteEntry.COLUMN_NAME_ADDED,
                FavoriteEntry.COLUMN_NAME_NAME,
                FavoriteEntry.COLUMN_NAME_SYNCED};

        public static Uri buildFavoritesUri() {
            return CONTENT_URI.buildUpon().build();
        }

        public static Uri buildFavoritesUriWith(long rowId) {
            return ContentUris.withAppendedId(CONTENT_URI, rowId);
        }

        public static Uri buildFavoritesUriWith(String id) {
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

        public static String getFavoriteId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static abstract class TagEntry implements BaseColumns {

        public static final String TABLE_NAME = "tag";

        public static final String COLUMN_NAME_ADDED = "added";
        public static final String COLUMN_NAME_NAME = "name";

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + TagEntry.TABLE_NAME;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + TagEntry.TABLE_NAME;

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendEncodedPath(TABLE_NAME).build();

        public static String[] TAG_COLUMNS = new String[]{
                TABLE_NAME + "." + TagEntry._ID,
                TABLE_NAME + "." + TagEntry.COLUMN_NAME_ADDED,
                TABLE_NAME + "." + TagEntry.COLUMN_NAME_NAME};

        public static Uri buildTagsUri() {
            return CONTENT_URI.buildUpon().build();
        }

        public static Uri buildTagsUriWith(long rowId) {
            return ContentUris.withAppendedId(CONTENT_URI, rowId);
        }

        // TODO: filter user input but not here
        public static Uri buildTagsUriWith(String name) {
            return CONTENT_URI.buildUpon().appendPath(name).build();
        }

        public static String getTagId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }
}
