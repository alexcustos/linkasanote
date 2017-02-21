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
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.source.local.DatabaseHelper;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

public class Provider extends ContentProvider {

    private static final int LINK = 100;
    private static final int LINK_ITEM = 101;
    private static final int LINK_TAG = 102;

    private static final int NOTE = 200;
    private static final int NOTE_ITEM = 201;
    private static final int NOTE_TAG = 202;

    private static final int FAVORITE = 300;
    private static final int FAVORITE_ITEM = 301;
    private static final int FAVORITE_TAG = 302;

    private static final int TAG = 400;
    private static final int TAG_ITEM = 401;
    private static final int TAG_LINK = 402;
    private static final int TAG_NOTE = 403;
    private static final int TAG_FAVORITE = 404;

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
        matcher.addURI(authority,
                LocalContract.TagEntry.TABLE_NAME + "/*/" +
                LocalContract.LinkEntry.TABLE_NAME, TAG_LINK);
        matcher.addURI(authority,
                LocalContract.TagEntry.TABLE_NAME + "/*/" +
                LocalContract.NoteEntry.TABLE_NAME, TAG_NOTE);
        matcher.addURI(authority,
                LocalContract.TagEntry.TABLE_NAME + "/*/" +
                LocalContract.FavoriteEntry.TABLE_NAME, TAG_FAVORITE);

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
            case TAG_LINK:
                return LocalContract.TagEntry.CONTENT_TYPE;
            case TAG_NOTE:
                return LocalContract.TagEntry.CONTENT_TYPE;
            case TAG_FAVORITE:
                return LocalContract.TagEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri [" + uri + "]");
        }
    }

    /*
    * Note: all queries receive ENTRY_ID (except *_TAG).
    * */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String tableName;

        switch (uriMatcher.match(uri)) {
            case LINK:
                tableName = LocalContract.LinkEntry.TABLE_NAME;
                break;
            case LINK_ITEM:
                tableName = LocalContract.LinkEntry.TABLE_NAME;
                selection = LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID + " = ?";
                selectionArgs = new String[]{LocalContract.LinkEntry.getLinkId(uri)};
                break;
            case FAVORITE:
                tableName = LocalContract.FavoriteEntry.TABLE_NAME;
                break;
            case FAVORITE_ITEM:
                tableName = LocalContract.FavoriteEntry.TABLE_NAME;
                selection = LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID + " = ?";
                selectionArgs = new String[]{LocalContract.FavoriteEntry.getFavoriteId(uri)};
                break;
            case FAVORITE_TAG:
                String favoriteTable = LocalContract.FavoriteEntry.TABLE_NAME;
                tableName = sqlJoinManyToManyWithTags(favoriteTable);
                selection = favoriteTable + LocalContract.FavoriteEntry._ID + " = ?";
                selectionArgs = new String[]{LocalContract.FavoriteEntry.getFavoriteId(uri)};
                break;
            case TAG:
                tableName = LocalContract.TagEntry.TABLE_NAME;
                break;
            case TAG_ITEM:
                tableName = LocalContract.TagEntry.TABLE_NAME;
                selection = LocalContract.TagEntry.COLUMN_NAME_NAME + " = ?";
                selectionArgs = new String[]{LocalContract.TagEntry.getTagId(uri)};
                break;
            default:
                throw new UnsupportedOperationException("Unknown query uri [" + uri + "]");

        }
        Cursor returnCursor = db.query(
                tableName, projection,
                selection, selectionArgs,
                null, null, sortOrder);
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
                    rowId = updateOrInsert(db,
                            LocalContract.LinkEntry.TABLE_NAME,
                            LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID,
                            values);
                    db.setTransactionSuccessful();
                    returnUri = LocalContract.LinkEntry.buildLinksUriWith(rowId);
                } finally {
                    db.endTransaction();
                }
                break;
            case FAVORITE:
                db.beginTransaction();
                try {
                    rowId = updateOrInsert(db,
                            LocalContract.FavoriteEntry.TABLE_NAME,
                            LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID,
                            values);
                    db.setTransactionSuccessful();
                    returnUri = LocalContract.FavoriteEntry.buildFavoritesUriWith(rowId);
                } finally {
                    db.endTransaction();
                }
                break;
            case FAVORITE_TAG:
                db.beginTransaction();
                try {
                    rowId = appendTag(db,
                            LocalContract.FavoriteEntry.TABLE_NAME,
                            LocalContract.FavoriteEntry.getFavoriteId(uri),
                            values);
                    db.setTransactionSuccessful();
                    returnUri = LocalContract.TagEntry.buildTagsUriWith(rowId);
                } finally {
                    db.endTransaction();
                }
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
            case NOTE:
                tableName = LocalContract.NoteEntry.TABLE_NAME;
                break;
            case FAVORITE:
                tableName = LocalContract.FavoriteEntry.TABLE_NAME;
                break;
            case TAG:
                tableName = LocalContract.TagEntry.TABLE_NAME;
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
            case FAVORITE_ITEM:
                db.beginTransaction();
                try {
                    String idValue = LocalContract.FavoriteEntry.getFavoriteId(uri);
                    numRows = updateEntry(db, LocalContract.FavoriteEntry.TABLE_NAME,
                            LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID, idValue,
                            values);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
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
            final @NonNull SQLiteDatabase db,
            final String leftTable, final String leftId, final ContentValues values) {
        checkNotNull(db);
        // Tag
        final String tagTable = LocalContract.TagEntry.TABLE_NAME;
        final String tagNameField = LocalContract.TagEntry.COLUMN_NAME_NAME;
        final String tagNameValue = values.getAsString(tagNameField);

        Cursor exists = db.query(
                tagTable, new String[]{BaseColumns._ID},
                tagNameField + " = ?", new String[]{tagNameValue},
                null, null, null);

        long tagId = 0;
        if (exists.moveToLast()) {
            int tagIdIndex = exists.getColumnIndexOrThrow(BaseColumns._ID);
            tagId = exists.getLong(tagIdIndex);
            exists.close();
        }
        if (tagId <= 0) {
            tagId = db.insert(tagTable, null, values);
            if (tagId <= 0) {
                throw new SQLException(String.format(
                        "Failed to insert tag [%s] bound with table [%s] for record [%s]",
                        tagNameValue, leftTable, leftId));
            }
        }
        // Reference
        final String refTable = leftTable + "_" + tagTable;
        ContentValues refValues = new ContentValues();
        refValues.put(LocalContract.MANY_TO_MANY_COMMON_NAME_ADDED, currentTimeMillis());
        refValues.put(leftTable + BaseColumns._ID, leftId);
        refValues.put(tagTable + BaseColumns._ID, tagId);

        long rowId = db.insert(refTable, null, refValues);
        if (rowId <= 0) {
            throw new SQLException(String.format(
                    "Failed to insert reference [%s] with table [%s]", leftId, leftTable));
        }
        return tagId;
    }

    private long updateOrInsert(
            @NonNull final SQLiteDatabase db, final String tableName,
            final String idField, final ContentValues values) {
        checkNotNull(db);

        String idValue = values.getAsString(idField);
        // NOTE: update does not produce rowId
        Cursor exists = db.query(
                tableName, new String[]{BaseColumns._ID},
                idField + " = ?", new String[]{idValue},
                null, null, null);
        long rowId = 0;
        if (exists.moveToLast()) {
            int rowIdIndex = exists.getColumnIndexOrThrow(BaseColumns._ID);
            rowId = exists.getLong(rowIdIndex);
            exists.close();
        }
        if (rowId > 0) {
            int numRows = db.update(
                    tableName, values,
                    BaseColumns._ID + " = ?", new String[]{Long.toString(rowId)});
            if (numRows <= 0) {
                throw new SQLException(String.format(
                        "Failed to update the row [%s] in table [%s]", idValue, tableName));
            }
        } else {
            rowId = db.insertOrThrow(tableName, null, values); // SQLiteConstraintException
            if (rowId <= 0) {
                throw new SQLException(String.format(
                        "Failed to insert row [%s] in table [%s]", idValue, tableName));
            }
        }
        return rowId;
    }

    private int updateEntry(
            @NonNull final SQLiteDatabase db, final String tableName,
            final String idField, final String idValue, final ContentValues values) {
        checkNotNull(db);
        if (Strings.isNullOrEmpty(idValue)) return 0;

        Cursor exists = db.query(
                tableName, new String[]{BaseColumns._ID},
                idField + " = ?", new String[]{idValue},
                null, null, null);
        boolean isExists = exists.moveToLast();
        exists.close();
        if (!isExists) return 0;

        int numRows = db.update(tableName, values, idField + " = ?", new String[]{idValue});
        if (numRows <= 0) {
            throw new SQLiteConstraintException(String.format(
                    "Failed to update row [%s] in table [%s]", idValue, tableName));
        }
        return numRows;
    }

    private static String sqlJoinManyToManyWithTags(final String leftTable) {
        final String tagTable = LocalContract.TagEntry.TABLE_NAME;
        final String TAG_ID = tagTable + BaseColumns._ID;
        final String refTable = leftTable + "_" + tagTable;

        return refTable + " LEFT OUTER JOIN " + tagTable +
                " ON " + refTable + "." + TAG_ID + "=" + tagTable + "." + BaseColumns._ID;
    }
}
