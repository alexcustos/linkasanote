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

package com.bytesforge.linkasanote.data.source;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.source.local.BaseEntry;
import com.bytesforge.linkasanote.data.source.local.DatabaseHelper;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.google.common.base.Strings;
import com.google.common.collect.ObjectArrays;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

public class Provider extends ContentProvider {

    private static final int LINK = 100;
    private static final int LINK_ITEM = 101;
    private static final int LINK_TAG = 102;
    private static final int LINK_NOTE = 103;

    private static final int NOTE = 200;
    private static final int NOTE_ITEM = 201;
    private static final int NOTE_TAG = 202;

    private static final int FAVORITE = 300;
    private static final int FAVORITE_ITEM = 301;
    private static final int FAVORITE_TAG = 302;

    private static final int TAG = 400;
    private static final int TAG_ITEM = 401;

    private static final int SYNC_RESULT = 500;
    private static final int SYNC_RESULT_LINK = 501;
    private static final int SYNC_RESULT_NOTE = 502;
    private static final int SYNC_RESULT_FAVORITE = 503;

    private static final UriMatcher uriMatcher = buildUriMatcher();

    private DatabaseHelper databaseHelper;
    private ContentResolver contentResolver;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = LocalContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, LocalContract.LinkEntry.TABLE_NAME, LINK);
        matcher.addURI(authority, LocalContract.LinkEntry.TABLE_NAME + "/*", LINK_ITEM);
        matcher.addURI(authority,
                LocalContract.LinkEntry.TABLE_NAME + "/*/" +
                LocalContract.TagEntry.TABLE_NAME, LINK_TAG);
        matcher.addURI(authority,
                LocalContract.LinkEntry.TABLE_NAME + "/*/" +
                LocalContract.NoteEntry.TABLE_NAME, LINK_NOTE);

        matcher.addURI(authority, LocalContract.NoteEntry.TABLE_NAME, NOTE);
        matcher.addURI(authority, LocalContract.NoteEntry.TABLE_NAME + "/*", NOTE_ITEM);
        matcher.addURI(authority,
                LocalContract.NoteEntry.TABLE_NAME + "/*/" +
                LocalContract.TagEntry.TABLE_NAME, NOTE_TAG);

        matcher.addURI(authority, LocalContract.FavoriteEntry.TABLE_NAME, FAVORITE);
        matcher.addURI(authority, LocalContract.FavoriteEntry.TABLE_NAME + "/*", FAVORITE_ITEM);
        matcher.addURI(authority,
                LocalContract.FavoriteEntry.TABLE_NAME + "/*/" +
                LocalContract.TagEntry.TABLE_NAME, FAVORITE_TAG);

        matcher.addURI(authority, LocalContract.TagEntry.TABLE_NAME, TAG);
        matcher.addURI(authority, LocalContract.TagEntry.TABLE_NAME + "/*", TAG_ITEM);

        matcher.addURI(authority, LocalContract.SyncResultEntry.TABLE_NAME, SYNC_RESULT);
        matcher.addURI(authority,
                LocalContract.SyncResultEntry.TABLE_NAME + "/" +
                LocalContract.LinkEntry.TABLE_NAME, SYNC_RESULT_LINK);
        matcher.addURI(authority,
                LocalContract.SyncResultEntry.TABLE_NAME + "/" +
                LocalContract.NoteEntry.TABLE_NAME, SYNC_RESULT_NOTE);
        matcher.addURI(authority,
                LocalContract.SyncResultEntry.TABLE_NAME + "/" +
                LocalContract.FavoriteEntry.TABLE_NAME, SYNC_RESULT_FAVORITE);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        databaseHelper = new DatabaseHelper(getContext());
        Context context = getContext();
        if (context != null) {
            contentResolver = context.getContentResolver();
        }
        return true;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case LINK:
                return LocalContract.LinkEntry.CONTENT_TYPE;
            case LINK_ITEM:
                return LocalContract.LinkEntry.CONTENT_ITEM_TYPE;
            case LINK_TAG:
                return LocalContract.LinkEntry.CONTENT_TYPE;
            case LINK_NOTE:
                return LocalContract.LinkEntry.CONTENT_TYPE;
            case NOTE:
                return LocalContract.NoteEntry.CONTENT_TYPE;
            case NOTE_ITEM:
                return LocalContract.NoteEntry.CONTENT_ITEM_TYPE;
            case NOTE_TAG:
                return LocalContract.NoteEntry.CONTENT_TYPE;
            case FAVORITE:
                return LocalContract.FavoriteEntry.CONTENT_TYPE;
            case FAVORITE_ITEM:
                return LocalContract.FavoriteEntry.CONTENT_ITEM_TYPE;
            case FAVORITE_TAG:
                return LocalContract.FavoriteEntry.CONTENT_TYPE;
            case TAG:
                return LocalContract.TagEntry.CONTENT_TYPE;
            case TAG_ITEM:
                return LocalContract.TagEntry.CONTENT_ITEM_TYPE;
            case SYNC_RESULT:
                return LocalContract.SyncResultEntry.CONTENT_TYPE;
            case SYNC_RESULT_LINK:
                return LocalContract.SyncResultEntry.CONTENT_TYPE;
            case SYNC_RESULT_NOTE:
                return LocalContract.SyncResultEntry.CONTENT_TYPE;
            case SYNC_RESULT_FAVORITE:
                return LocalContract.SyncResultEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri [" + uri + "]");
        }
    }

    // NOTE: all queries take ENTRY_ID (except *_TAG).
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String tableName;
        String paramLimit = uri.getQueryParameter(LocalContract.QUERY_PARAMETER_LIMIT);
        String paramOffset = uri.getQueryParameter(LocalContract.QUERY_PARAMETER_OFFSET);
        String limit = null;
        if (paramLimit != null) {
            if (paramOffset != null) {
                limit = paramOffset + "," + paramLimit;
            } else {
                limit = paramLimit;
            }
        }
        switch (uriMatcher.match(uri)) {
            case LINK:
                tableName = LocalContract.LinkEntry.TABLE_NAME;
                break;
            case LINK_ITEM:
                tableName = LocalContract.LinkEntry.TABLE_NAME;
                selection = LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID + " = ?";
                selectionArgs = new String[]{LocalContract.LinkEntry.getIdFrom(uri)};
                break;
            case LINK_TAG:
                String linkTable = LocalContract.LinkEntry.TABLE_NAME;
                tableName = sqlJoinManyToManyWithTags(linkTable);
                selection = linkTable + LocalContract.LinkEntry._ID + " = ?";
                selectionArgs = new String[]{LocalContract.LinkEntry.getIdFrom(uri)};
                if (sortOrder == null) {
                    sortOrder = sqlDefaultTagsSortOrder(linkTable);
                }
                break;
            case LINK_NOTE:
                tableName = LocalContract.NoteEntry.TABLE_NAME;
                selection = LocalContract.NoteEntry.COLUMN_NAME_LINK_ID + " = ?";
                selectionArgs = new String[]{LocalContract.LinkEntry.getIdFrom(uri)};
                break;
            case FAVORITE:
                tableName = LocalContract.FavoriteEntry.TABLE_NAME;
                break;
            case FAVORITE_ITEM:
                tableName = LocalContract.FavoriteEntry.TABLE_NAME;
                selection = LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID + " = ?";
                selectionArgs = new String[]{LocalContract.FavoriteEntry.getIdFrom(uri)};
                break;
            case FAVORITE_TAG:
                String favoriteTable = LocalContract.FavoriteEntry.TABLE_NAME;
                tableName = sqlJoinManyToManyWithTags(favoriteTable);
                selection = favoriteTable + LocalContract.FavoriteEntry._ID + " = ?";
                selectionArgs = new String[]{LocalContract.FavoriteEntry.getIdFrom(uri)};
                if (sortOrder == null) {
                    sortOrder = sqlDefaultTagsSortOrder(favoriteTable);
                }
                break;
            case NOTE:
                tableName = LocalContract.NoteEntry.TABLE_NAME;
                break;
            case NOTE_ITEM:
                tableName = LocalContract.NoteEntry.TABLE_NAME;
                selection = LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID + " = ?";
                selectionArgs = new String[]{LocalContract.NoteEntry.getIdFrom(uri)};
                break;
            case NOTE_TAG:
                String noteTable = LocalContract.NoteEntry.TABLE_NAME;
                tableName = sqlJoinManyToManyWithTags(noteTable);
                selection = noteTable + LocalContract.NoteEntry._ID + " = ?";
                selectionArgs = new String[]{LocalContract.NoteEntry.getIdFrom(uri)};
                if (sortOrder == null) {
                    sortOrder = sqlDefaultTagsSortOrder(noteTable);
                }
                break;
            case TAG:
                tableName = LocalContract.TagEntry.TABLE_NAME;
                break;
            case TAG_ITEM:
                tableName = LocalContract.TagEntry.TABLE_NAME;
                selection = LocalContract.TagEntry.COLUMN_NAME_NAME + " = ?";
                selectionArgs = new String[]{LocalContract.TagEntry.getNameFrom(uri)};
                break;
            case SYNC_RESULT:
                tableName = LocalContract.SyncResultEntry.TABLE_NAME;
                break;
            case SYNC_RESULT_LINK:
                tableName = LocalContract.SyncResultEntry.TABLE_NAME;
                String linkSelection = LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY + " = ?";
                String[] linkSelectionArgs = new String[]{LocalContract.LinkEntry.TABLE_NAME};
                selection = (selection == null ? linkSelection
                        : "(" + selection + ") AND " + linkSelection);
                selectionArgs = (selectionArgs == null ? linkSelectionArgs
                        : ObjectArrays.concat(selectionArgs, linkSelectionArgs, String.class));
                break;
            case SYNC_RESULT_NOTE:
                tableName = LocalContract.SyncResultEntry.TABLE_NAME;
                String noteSelection = LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY + " = ?";
                String[] noteSelectionArgs = new String[]{LocalContract.NoteEntry.TABLE_NAME};
                selection = (selection == null ? noteSelection
                        : "(" + selection + ") AND " + noteSelection);
                selectionArgs = (selectionArgs == null ? noteSelectionArgs
                        : ObjectArrays.concat(selectionArgs, noteSelectionArgs, String.class));
                break;
            case SYNC_RESULT_FAVORITE:
                tableName = LocalContract.SyncResultEntry.TABLE_NAME;
                String favoriteSelection = LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY + " = ?";
                String[] favoriteSelectionArgs = new String[]{LocalContract.FavoriteEntry.TABLE_NAME};
                selection = (selection == null ? favoriteSelection
                        : "(" + selection + ") AND " + favoriteSelection);
                selectionArgs = (selectionArgs == null ? favoriteSelectionArgs
                        : ObjectArrays.concat(selectionArgs, favoriteSelectionArgs, String.class));
                break;
            default:
                throw new UnsupportedOperationException("Unknown query uri [" + uri + "]");
        }
        Cursor returnCursor = db.query(
                tableName, projection, selection, selectionArgs, null, null, sortOrder, limit);
        returnCursor.setNotificationUri(contentResolver, uri);
        return returnCursor;
    }

    /*
    * Note: all insert operations receive ENTRY_ID (except *_TAG) then return _ID.
    * ENTRY_ID can be taken from values.
    * */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();

        long rowId;
        Uri returnUri = null;
        switch (uriMatcher.match(uri)) {
            case LINK:
                db.beginTransaction();
                try {
                    rowId = updateOrInsert(db, LocalContract.LinkEntry.TABLE_NAME,
                            LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID, values);
                    db.setTransactionSuccessful();
                    returnUri = LocalContract.LinkEntry.buildUriWith(rowId);
                } finally {
                    db.endTransaction();
                }
                break;
            case LINK_TAG:
                db.beginTransaction();
                try {
                    rowId = appendTag(db, LocalContract.LinkEntry.TABLE_NAME,
                            LocalContract.LinkEntry.getIdFrom(uri), values);
                    db.setTransactionSuccessful();
                    returnUri = LocalContract.TagEntry.buildUriWith(rowId);
                } finally {
                    db.endTransaction();
                }
                break;
            case FAVORITE:
                db.beginTransaction();
                try {
                    rowId = updateOrInsert(db, LocalContract.FavoriteEntry.TABLE_NAME,
                            LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID, values);
                    db.setTransactionSuccessful();
                    returnUri = LocalContract.FavoriteEntry.buildUriWith(rowId);
                } finally {
                    db.endTransaction();
                }
                break;
            case FAVORITE_TAG:
                db.beginTransaction();
                try {
                    rowId = appendTag(db, LocalContract.FavoriteEntry.TABLE_NAME,
                            LocalContract.FavoriteEntry.getIdFrom(uri), values);
                    db.setTransactionSuccessful();
                    returnUri = LocalContract.TagEntry.buildUriWith(rowId);
                } finally {
                    db.endTransaction();
                }
                break;
            case NOTE:
                db.beginTransaction();
                try {
                    rowId = updateOrInsert(db, LocalContract.NoteEntry.TABLE_NAME,
                            LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID, values);
                    db.setTransactionSuccessful();
                    returnUri = LocalContract.NoteEntry.buildUriWith(rowId);
                } finally {
                    db.endTransaction();
                }
                break;
            case NOTE_TAG:
                db.beginTransaction();
                try {
                    rowId = appendTag(db, LocalContract.NoteEntry.TABLE_NAME,
                            LocalContract.NoteEntry.getIdFrom(uri), values);
                    db.setTransactionSuccessful();
                    returnUri = LocalContract.TagEntry.buildUriWith(rowId);
                } finally {
                    db.endTransaction();
                }
                break;
            case SYNC_RESULT:
                rowId = insertEntry(db, LocalContract.SyncResultEntry.TABLE_NAME, values);
                returnUri = LocalContract.SyncResultEntry.buildUriWith(rowId);
                break;
            default:
                throw new UnsupportedOperationException("Unknown insert uri [" + uri + "]");
        }
        contentResolver.notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();

        String tableName;
        switch (uriMatcher.match(uri)) {
            case LINK:
                tableName = LocalContract.LinkEntry.TABLE_NAME;
                break;
            case LINK_ITEM:
                tableName = LocalContract.LinkEntry.TABLE_NAME;
                selection = LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID + " = ?";
                selectionArgs = new String[]{LocalContract.LinkEntry.getIdFrom(uri)};
                break;
            case FAVORITE:
                tableName = LocalContract.FavoriteEntry.TABLE_NAME;
                break;
            case FAVORITE_ITEM:
                tableName = LocalContract.FavoriteEntry.TABLE_NAME;
                selection = LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID + " = ?";
                selectionArgs = new String[]{LocalContract.FavoriteEntry.getIdFrom(uri)};
                break;
            case NOTE:
                tableName = LocalContract.NoteEntry.TABLE_NAME;
                break;
            case NOTE_ITEM:
                tableName = LocalContract.NoteEntry.TABLE_NAME;
                selection = LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID + " = ?";
                selectionArgs = new String[]{LocalContract.NoteEntry.getIdFrom(uri)};
                break;
            case TAG:
                tableName = LocalContract.TagEntry.TABLE_NAME;
                break;
            case SYNC_RESULT:
                tableName = LocalContract.SyncResultEntry.TABLE_NAME;
                break;
            default:
                throw new UnsupportedOperationException("Unknown delete uri [" + uri + "]");
        }
        int rowsDeleted = db.delete(tableName, selection, selectionArgs);
        if (selection == null || rowsDeleted != 0) {
            contentResolver.notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values,
            String selection, String[] selectionArgs) {
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();

        int numRows;
        switch (uriMatcher.match(uri)) {
            case LINK:
                numRows = db.update(LocalContract.LinkEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            case LINK_ITEM:
                db.beginTransaction();
                try {
                    String idValue = LocalContract.LinkEntry.getIdFrom(uri);
                    long rowId = updateEntry(db, LocalContract.LinkEntry.TABLE_NAME,
                            LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID, idValue,
                            values);
                    numRows = rowId > 0 ? 1 : 0;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            case FAVORITE:
                numRows = db.update(LocalContract.FavoriteEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            case FAVORITE_ITEM:
                db.beginTransaction();
                try {
                    String idValue = LocalContract.FavoriteEntry.getIdFrom(uri);
                    long rowId = updateEntry(db, LocalContract.FavoriteEntry.TABLE_NAME,
                            LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID, idValue,
                            values);
                    numRows = rowId > 0 ? 1 : 0;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            case NOTE:
                numRows = db.update(LocalContract.NoteEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            case NOTE_ITEM:
                db.beginTransaction();
                try {
                    String idValue = LocalContract.NoteEntry.getIdFrom(uri);
                    long rowId = updateEntry(db, LocalContract.NoteEntry.TABLE_NAME,
                            LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID, idValue,
                            values);
                    numRows = rowId > 0 ? 1 : 0;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            case SYNC_RESULT_LINK:
                String linkSelection = LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY + " = ?";
                String[] linkSelectionArgs = new String[]{LocalContract.LinkEntry.TABLE_NAME};
                selection = (selection == null ? linkSelection
                        : "(" + selection + ") AND " + linkSelection);
                selectionArgs = (selectionArgs == null ? linkSelectionArgs
                        : ObjectArrays.concat(selectionArgs, linkSelectionArgs, String.class));
                numRows = db.update(LocalContract.SyncResultEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            case SYNC_RESULT_NOTE:
                String noteSelection = LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY + " = ?";
                String[] noteSelectionArgs = new String[]{LocalContract.NoteEntry.TABLE_NAME};
                selection = (selection == null ? noteSelection
                        : "(" + selection + ") AND " + noteSelection);
                selectionArgs = (selectionArgs == null ? noteSelectionArgs
                        : ObjectArrays.concat(selectionArgs, noteSelectionArgs, String.class));
                numRows = db.update(LocalContract.SyncResultEntry.TABLE_NAME,
                        values, selection, selectionArgs);
            case SYNC_RESULT_FAVORITE:
                String favoriteSelection = LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY + " = ?";
                String[] favoriteSelectionArgs = new String[]{LocalContract.FavoriteEntry.TABLE_NAME};
                selection = (selection == null ? favoriteSelection
                        : "(" + selection + ") AND " + favoriteSelection);
                selectionArgs = (selectionArgs == null ? favoriteSelectionArgs
                        : ObjectArrays.concat(selectionArgs, favoriteSelectionArgs, String.class));
                numRows = db.update(LocalContract.SyncResultEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown update uri [" + uri + "]");
        }
        if (numRows > 0) {
            contentResolver.notifyChange(uri, null);
        }
        return numRows;
    }

    private long appendTag(
            @NonNull final SQLiteDatabase db,
            final String leftTable, final String leftId, final ContentValues values) {
        checkNotNull(db);
        // Tag
        final String tagTable = LocalContract.TagEntry.TABLE_NAME;
        final String tagNameField = LocalContract.TagEntry.COLUMN_NAME_NAME;
        final String tagNameValue = values.getAsString(tagNameField);

        long tagId = queryRowId(db, tagTable, tagNameField, tagNameValue);
        if (tagId <= 0) {
            tagId = insertEntry(db, tagTable, values);
        }
        // Reference
        final String refTable = leftTable + "_" + tagTable;
        ContentValues refValues = new ContentValues();
        refValues.put(BaseEntry.COLUMN_NAME_CREATED, currentTimeMillis());
        refValues.put(leftTable + BaseEntry._ID, leftId);
        refValues.put(tagTable + BaseEntry._ID, tagId);
        insertEntry(db, refTable, refValues); // NOTE: it's refTable rowId can be ignored
        return tagId;
    }

    private long updateOrInsert(
            @NonNull final SQLiteDatabase db, final String tableName,
            final String idField, final ContentValues values) {
        checkNotNull(db);
        String idValue = values.getAsString(idField);
        long rowId = updateEntry(db, tableName, idField, idValue, values);
        if (rowId <= 0) {
            rowId = insertEntry(db, tableName, values);
        } else { // NOTE: updateEntry mainly is for the update of the state
            // NOTE: will be recreated with the new set of tags
            deleteTagReferences(db, tableName, rowId);
        }
        return rowId;
    }

    private long insertEntry(
            @NonNull final SQLiteDatabase db,
            final String tableName, final ContentValues values) {
        checkNotNull(db);
        long rowId = db.insertOrThrow(tableName, null, values); // SQLiteConstraintException
        if (rowId <= 0) {
            throw new SQLException(String.format(
                    "Failed to insert a row to the table [%s]", tableName));
        }
        return rowId;
    }

    /**
     * Method for updating Row with the specified ID (idField, idValue)
     *
     * @return RowID of the updated record or 0 if the record was not found
     */
    private long updateEntry(
            @NonNull final SQLiteDatabase db, final String tableName,
            final String idField, final String idValue, final ContentValues values) {
        checkNotNull(db);
        // NOTE: return value
        long rowId = queryRowId(db, tableName, idField, idValue);
        if (rowId <= 0) return 0;

        final String selection = BaseEntry._ID + " = ?";
        final String[] selectionArgs = new String[]{Long.toString(rowId)};
        int numRows = db.update(tableName, values, selection, selectionArgs);
        if (numRows <= 0) {
            throw new SQLiteConstraintException(String.format(
                    "Failed to update row with ID [%s, table=%s]", idValue, tableName));
        }
        return rowId;
    }

    private int deleteTagReferences(
            @NonNull final SQLiteDatabase db, final String leftTable, final long leftId) {
        checkNotNull(db);
        final String tagTable = LocalContract.TagEntry.TABLE_NAME;
        final String refTable = leftTable + "_" + tagTable;

        final String selection = leftTable + BaseEntry._ID + " = ?";
        final String[] selectionArgs = new String[]{Long.toString(leftId)};
        return db.delete(refTable, selection, selectionArgs);
    }


    private long queryRowId(
            @NonNull final SQLiteDatabase db, final String tableName,
            final String idField, final String idValue) {
        checkNotNull(db);
        if (Strings.isNullOrEmpty(idValue)) return 0;

        final String selection = idField + " = ?";
        final String[] selectionArgs = new String[]{idValue};
        try (Cursor exists = db.query(
                tableName, new String[]{BaseEntry._ID},
                selection, selectionArgs, null, null, null)) {
            long rowId = 0;
            if (exists.moveToLast()) {
                int rowIdIndex = exists.getColumnIndexOrThrow(BaseEntry._ID);
                rowId = exists.getLong(rowIdIndex);
            }
            return rowId;
        }
    }

    private static String sqlJoinManyToManyWithTags(final String leftTable) {
        final String tagTable = LocalContract.TagEntry.TABLE_NAME;
        final String TAG_ID = tagTable + BaseEntry._ID;
        final String refTable = leftTable + "_" + tagTable;
        return refTable + " LEFT OUTER JOIN " + tagTable +
                " ON " + refTable + "." + TAG_ID + "=" + tagTable + "." + BaseEntry._ID;
    }

    private static String sqlDefaultTagsSortOrder(final String leftTable) {
        final String tagTable = LocalContract.TagEntry.TABLE_NAME;
        final String refTable = leftTable + "_" + tagTable;
        return refTable + "." + BaseEntry.COLUMN_NAME_CREATED + " ASC";
    }
}
