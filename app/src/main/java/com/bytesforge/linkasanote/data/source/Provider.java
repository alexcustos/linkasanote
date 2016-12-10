package com.bytesforge.linkasanote.data.source;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.source.local.DatabaseHelper;
import com.bytesforge.linkasanote.data.source.local.PersistenceContract;

import static com.google.common.base.Preconditions.checkNotNull;

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
        final String authority = PersistenceContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, PersistenceContract.LinkEntry.TABLE_NAME, LINK);
        matcher.addURI(authority, PersistenceContract.LinkEntry.TABLE_NAME + "/*", LINK_ITEM);
        matcher.addURI(authority,
                PersistenceContract.LinkEntry.TABLE_NAME + "/*/" +
                PersistenceContract.TagEntry.TABLE_NAME, LINK_TAG);

        matcher.addURI(authority, PersistenceContract.NoteEntry.TABLE_NAME, NOTE);
        matcher.addURI(authority, PersistenceContract.NoteEntry.TABLE_NAME + "/*", NOTE_ITEM);
        matcher.addURI(authority,
                PersistenceContract.NoteEntry.TABLE_NAME + "/*/" +
                PersistenceContract.TagEntry.TABLE_NAME, NOTE_TAG);

        matcher.addURI(authority, PersistenceContract.FavoriteEntry.TABLE_NAME, FAVORITE);
        matcher.addURI(authority, PersistenceContract.FavoriteEntry.TABLE_NAME + "/*", FAVORITE_ITEM);
        matcher.addURI(authority,
                PersistenceContract.FavoriteEntry.TABLE_NAME + "/*/" +
                PersistenceContract.TagEntry.TABLE_NAME, FAVORITE_TAG);

        matcher.addURI(authority, PersistenceContract.TagEntry.TABLE_NAME, TAG);
        matcher.addURI(authority, PersistenceContract.TagEntry.TABLE_NAME + "/*", TAG_ITEM);
        matcher.addURI(authority,
                PersistenceContract.TagEntry.TABLE_NAME + "/*/" +
                PersistenceContract.LinkEntry.TABLE_NAME, TAG_LINK);
        matcher.addURI(authority,
                PersistenceContract.TagEntry.TABLE_NAME + "/*/" +
                PersistenceContract.NoteEntry.TABLE_NAME, TAG_NOTE);
        matcher.addURI(authority,
                PersistenceContract.TagEntry.TABLE_NAME + "/*/" +
                PersistenceContract.FavoriteEntry.TABLE_NAME, TAG_FAVORITE);

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
                return PersistenceContract.LinkEntry.CONTENT_TYPE;
            case LINK_ITEM:
                return PersistenceContract.LinkEntry.CONTENT_ITEM_TYPE;
            case LINK_TAG:
                return PersistenceContract.LinkEntry.CONTENT_TYPE;
            case NOTE:
                return PersistenceContract.NoteEntry.CONTENT_TYPE;
            case NOTE_ITEM:
                return PersistenceContract.NoteEntry.CONTENT_ITEM_TYPE;
            case NOTE_TAG:
                return PersistenceContract.NoteEntry.CONTENT_TYPE;
            case FAVORITE:
                return PersistenceContract.FavoriteEntry.CONTENT_TYPE;
            case FAVORITE_ITEM:
                return PersistenceContract.FavoriteEntry.CONTENT_ITEM_TYPE;
            case FAVORITE_TAG:
                return PersistenceContract.FavoriteEntry.CONTENT_TYPE;
            case TAG:
                return PersistenceContract.TagEntry.CONTENT_TYPE;
            case TAG_ITEM:
                return PersistenceContract.TagEntry.CONTENT_ITEM_TYPE;
            case TAG_LINK:
                return PersistenceContract.TagEntry.CONTENT_TYPE;
            case TAG_NOTE:
                return PersistenceContract.TagEntry.CONTENT_TYPE;
            case TAG_FAVORITE:
                return PersistenceContract.TagEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor returnCursor;

        switch (uriMatcher.match(uri)) {
            case LINK:
                returnCursor = db.query(
                        PersistenceContract.LinkEntry.TABLE_NAME, projection,
                        selection, selectionArgs,
                        null, null, sortOrder);
                break;

            case LINK_ITEM:
                String[] where = {uri.getLastPathSegment()};
                returnCursor = db.query(
                        PersistenceContract.LinkEntry.TABLE_NAME, projection,
                        PersistenceContract.LinkEntry.COLUMN_NAME_ENTRY_ID + " = ?", where,
                        null, null, sortOrder);
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);

        }
        returnCursor.setNotificationUri(contentResolver, uri);

        return returnCursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();
        Uri returnUri = null;

        switch (uriMatcher.match(uri)) {
            case LINK:
                long _id = updateOrInsert(db,
                        PersistenceContract.LinkEntry.TABLE_NAME,
                        PersistenceContract.LinkEntry.COLUMN_NAME_ENTRY_ID,
                        values);
                if (_id >= 0) {
                    returnUri = PersistenceContract.LinkEntry.buildLinksUriWith(
                            values.getAsString(PersistenceContract.LinkEntry.COLUMN_NAME_ENTRY_ID));
                }
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        contentResolver.notifyChange(uri, null);

        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values,
            String selection, String[] selectionArgs) {
        return 0;
    }

    private long updateOrInsert(
            @NonNull SQLiteDatabase db,
            String tableName, String idFieldName, ContentValues values) {
        checkNotNull(db);

        long returnId;
        String idFieldValue = values.getAsString(idFieldName);

        Cursor exists = db.query(
                tableName, new String[] {idFieldName},
                idFieldName + " = ?", new String[] {idFieldValue},
                null, null, null);

        if (exists.moveToLast()) {
            returnId = db.update(
                    tableName, values,
                    idFieldName + " = ?", new String[] {idFieldValue});
            if (returnId < 0) {
                throw new SQLException(String.format(
                        "Failed to update row '%s' in table '%s'", idFieldValue, tableName));
            }
        } else {
            returnId = db.insert(tableName, null, values);
            if (returnId < 0) {
                throw new SQLException(String.format(
                        "Failed to insert row '%s' in table '%s'", idFieldValue, tableName));
            }
        }
        exists.close();

        return returnId;
    }
}
