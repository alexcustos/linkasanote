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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "laano.sqlite";

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String DATETIME_TYPE = " DATETIME";
    private static final String BOOLEAN_TYPE = " INTEGER";

    private static final String SQL_CREATE_LINK_ENTRIES =
            "CREATE TABLE " + LocalContract.LinkEntry.TABLE_NAME + " (" +
                    LocalContract.LinkEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + " NOT NULL UNIQUE," +
                    LocalContract.LinkEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_UPDATED + DATETIME_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_LINK + TEXT_TYPE + " NOT NULL," + // UNIQUE
                    LocalContract.LinkEntry.COLUMN_NAME_NAME + TEXT_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_DISABLED + BOOLEAN_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_ETAG + TEXT_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_DUPLICATED + INTEGER_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_CONFLICTED + BOOLEAN_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_DELETED + BOOLEAN_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_SYNCED + BOOLEAN_TYPE + "," +
                    "UNIQUE (" + LocalContract.LinkEntry.COLUMN_NAME_LINK + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_DUPLICATED + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_DELETED + ") ON CONFLICT ABORT" +
            ");";
    private static final String SQL_CREATE_LINK_ENTRY_ID_INDEX =
            sqlCreateEntryIdIndex(LocalContract.LinkEntry.TABLE_NAME);
    private static final String SQL_CREATE_LINK_CREATED_INDEX = sqlCreateIndex(
            LocalContract.LinkEntry.TABLE_NAME, LocalContract.LinkEntry.COLUMN_NAME_CREATED);

    private static final String SQL_CREATE_FAVORITE_ENTRIES =
            "CREATE TABLE " + LocalContract.FavoriteEntry.TABLE_NAME + " (" +
                    LocalContract.FavoriteEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + " NOT NULL UNIQUE," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_UPDATED + DATETIME_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_NAME + TEXT_TYPE + " NOT NULL," + // UNIQUE
                    LocalContract.FavoriteEntry.COLUMN_NAME_AND_GATE + BOOLEAN_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_ETAG + TEXT_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_DUPLICATED + INTEGER_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_CONFLICTED + BOOLEAN_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_DELETED + BOOLEAN_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_SYNCED + BOOLEAN_TYPE + "," +
                    "UNIQUE (" + LocalContract.FavoriteEntry.COLUMN_NAME_NAME + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_DUPLICATED + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_DELETED + ") ON CONFLICT ABORT" +
            ");";
    private static final String SQL_CREATE_FAVORITE_ENTRY_ID_INDEX =
            sqlCreateEntryIdIndex(LocalContract.FavoriteEntry.TABLE_NAME);
    private static final String SQL_CREATE_FAVORITE_NAME_INDEX = sqlCreateIndex(
            LocalContract.FavoriteEntry.TABLE_NAME, LocalContract.FavoriteEntry.COLUMN_NAME_NAME);
    private static final String SQL_CREATE_FAVORITE_CREATED_INDEX = sqlCreateIndex(
            LocalContract.FavoriteEntry.TABLE_NAME, LocalContract.FavoriteEntry.COLUMN_NAME_CREATED);

    // NOTE: note entry must _not_ be unique, but duplicate filter is needed
    private static final String SQL_CREATE_NOTE_ENTRIES =
            "CREATE TABLE " + LocalContract.NoteEntry.TABLE_NAME + " (" +
                    LocalContract.NoteEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + " NOT NULL UNIQUE," +
                    LocalContract.NoteEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_UPDATED + DATETIME_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_NOTE + TEXT_TYPE + " NOT NULL," +
                    // NOTE: reference to the entry_id because this column goes to a cloud
                    LocalContract.NoteEntry.COLUMN_NAME_LINK_ID + TEXT_TYPE + " REFERENCES " +
                    LocalContract.LinkEntry.TABLE_NAME + "(" +
                    LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID + ") ON DELETE SET NULL," +
                    LocalContract.NoteEntry.COLUMN_NAME_ETAG + TEXT_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_DUPLICATED + INTEGER_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_CONFLICTED + BOOLEAN_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_DELETED + BOOLEAN_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_SYNCED + BOOLEAN_TYPE +
            ");";
    private static final String SQL_CREATE_NOTE_ENTRY_ID_INDEX =
            sqlCreateEntryIdIndex(LocalContract.NoteEntry.TABLE_NAME);
    private static final String SQL_CREATE_NOTE_LINK_ID_INDEX = sqlCreateIndex(
            LocalContract.NoteEntry.TABLE_NAME, LocalContract.NoteEntry.COLUMN_NAME_LINK_ID);
    private static final String SQL_CREATE_NOTE_CREATED_INDEX = sqlCreateIndex(
            LocalContract.NoteEntry.TABLE_NAME, LocalContract.NoteEntry.COLUMN_NAME_CREATED);

    private static final String SQL_CREATE_TAG_ENTRIES =
            "CREATE TABLE " + LocalContract.TagEntry.TABLE_NAME + " (" +
                    LocalContract.TagEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    LocalContract.TagEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                    LocalContract.TagEntry.COLUMN_NAME_NAME + TEXT_TYPE + " UNIQUE" +
            ");";

    private static final String SQL_CREATE_SYNC_RESULT_ENTRIES =
            "CREATE TABLE " + LocalContract.SyncResultEntry.TABLE_NAME + " (" +
                    LocalContract.SyncResultEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    LocalContract.SyncResultEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                    LocalContract.SyncResultEntry.COLUMN_NAME_STARTED + DATETIME_TYPE + "," +
                    LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY + TEXT_TYPE + " NOT NULL," +
                    LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + " NOT NULL," +
                    LocalContract.SyncResultEntry.COLUMN_NAME_RESULT + TEXT_TYPE + " NOT NULL," +
                    LocalContract.SyncResultEntry.COLUMN_NAME_APPLIED + BOOLEAN_TYPE +
            ");";
    private static final String SQL_CREATE_SYNC_RESULT_CREATED_INDEX = sqlCreateIndex(
            LocalContract.SyncResultEntry.TABLE_NAME, LocalContract.SyncResultEntry.COLUMN_NAME_CREATED);
    private static final String SQL_CREATE_SYNC_RESULT_STARTED_INDEX = sqlCreateIndex(
            LocalContract.SyncResultEntry.TABLE_NAME, LocalContract.SyncResultEntry.COLUMN_NAME_STARTED);
    private static final String SQL_CREATE_SYNC_RESULT_ENTRY_INDEX = sqlCreateIndex(
            LocalContract.SyncResultEntry.TABLE_NAME, LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY);
    private static final String SQL_CREATE_SYNC_RESULT_RESULT_INDEX = sqlCreateIndex(
            LocalContract.SyncResultEntry.TABLE_NAME, LocalContract.SyncResultEntry.COLUMN_NAME_RESULT);
    private static final String SQL_CREATE_SYNC_RESULT_APPLIED_INDEX = sqlCreateIndex(
            LocalContract.SyncResultEntry.TABLE_NAME, LocalContract.SyncResultEntry.COLUMN_NAME_APPLIED);

    private static final String SQL_CREATE_LINK_TAG_ENTRIES =
            sqlCreateTableManyToManyWithTags(LocalContract.LinkEntry.TABLE_NAME);
    private static final String SQL_CREATE_LINK_TAG_LEFT_INDEX =
            sqlCreateLeftIndexWithTag(LocalContract.LinkEntry.TABLE_NAME);
    private static final String SQL_CREATE_LINK_TAG_RIGHT_INDEX =
            sqlCreateRightIndexWithTag(LocalContract.LinkEntry.TABLE_NAME);
    private static final String SQL_CREATE_LINK_TAG_CREATED_INDEX =
            sqlCreateCreatedIndexWithTag(LocalContract.LinkEntry.TABLE_NAME);

    private static final String SQL_CREATE_NOTE_TAG_ENTRIES =
            sqlCreateTableManyToManyWithTags(LocalContract.NoteEntry.TABLE_NAME);
    private static final String SQL_CREATE_NOTE_TAG_LEFT_INDEX =
            sqlCreateLeftIndexWithTag(LocalContract.NoteEntry.TABLE_NAME);
    private static final String SQL_CREATE_NOTE_TAG_RIGHT_INDEX =
            sqlCreateRightIndexWithTag(LocalContract.NoteEntry.TABLE_NAME);
    private static final String SQL_CREATE_NOTE_TAG_CREATED_INDEX =
            sqlCreateCreatedIndexWithTag(LocalContract.NoteEntry.TABLE_NAME);

    private static final String SQL_CREATE_FAVORITE_TAG_ENTRIES =
            sqlCreateTableManyToManyWithTags(LocalContract.FavoriteEntry.TABLE_NAME);
    private static final String SQL_CREATE_FAVORITE_TAG_LEFT_INDEX =
            sqlCreateLeftIndexWithTag(LocalContract.FavoriteEntry.TABLE_NAME);
    private static final String SQL_CREATE_FAVORITE_TAG_RIGHT_INDEX =
            sqlCreateRightIndexWithTag(LocalContract.FavoriteEntry.TABLE_NAME);
    private static final String SQL_CREATE_FAVORITE_TAG_CREATED_INDEX =
            sqlCreateCreatedIndexWithTag(LocalContract.FavoriteEntry.TABLE_NAME);

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    private static String sqlCreateTableManyToManyWithTags(final String leftTable) {
        final String rightTable = LocalContract.TagEntry.TABLE_NAME;
        final String LID = leftTable + BaseEntry._ID;
        final String RID = rightTable + BaseEntry._ID;

        return "CREATE TABLE " + leftTable + "_" + rightTable + " (" +
                BaseEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                BaseEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                LID + INTEGER_TYPE + " REFERENCES " + leftTable + "(" + BaseEntry._ID + ") ON DELETE CASCADE," +
                RID + INTEGER_TYPE + " REFERENCES " + rightTable + "(" + BaseEntry._ID + ") ON DELETE CASCADE," +
                "UNIQUE (" + LID + "," + RID + ") ON CONFLICT ABORT);";
    }

    private static String sqlCreateLeftIndexWithTag(final String leftTable) {
        final String rightTable = LocalContract.TagEntry.TABLE_NAME;
        final String LID = leftTable + BaseEntry._ID;
        return sqlCreateIndex(leftTable + "_" + rightTable, LID);
    }

    private static String sqlCreateRightIndexWithTag(final String leftTable) {
        final String rightTable = LocalContract.TagEntry.TABLE_NAME;
        final String RID = rightTable + BaseEntry._ID;
        return sqlCreateIndex(leftTable + "_" + rightTable, RID);
    }

    private static String sqlCreateCreatedIndexWithTag(final String leftTable) {
        final String rightTable = LocalContract.TagEntry.TABLE_NAME;
        return sqlCreateIndex(leftTable + "_" + rightTable, BaseEntry.COLUMN_NAME_CREATED);
    }

    private static String sqlCreateEntryIdIndex(final String table) {
        return sqlCreateIndex(table, BaseEntry.COLUMN_NAME_ENTRY_ID);
    }

    private static String sqlCreateIndex(final String table, final String column) {
        return "CREATE INDEX " + table + "_" + column + "_index ON " + table + "(" + column + ")";
    }

    private static String sqlDropLeftIndexWithTag(final String leftTable) {
        final String rightTable = LocalContract.TagEntry.TABLE_NAME;
        final String LID = leftTable + BaseEntry._ID;
        return sqlDropIndex(leftTable + "_" + rightTable, LID);
    }

    private static String sqlDropRightIndexWithTag(final String leftTable) {
        final String rightTable = LocalContract.TagEntry.TABLE_NAME;
        final String RID = rightTable + BaseEntry._ID;
        return sqlDropIndex(leftTable + "_" + rightTable, RID);
    }

    private static String sqlDropCreatedIndexWithTag(final String leftTable) {
        final String rightTable = LocalContract.TagEntry.TABLE_NAME;
        return sqlDropIndex(leftTable + "_" + rightTable, BaseEntry.COLUMN_NAME_CREATED);
    }

    private static String sqlDropEntryIdIndex(final String table) {
        return sqlDropIndex(table, BaseEntry.COLUMN_NAME_ENTRY_ID);
    }

    private static String sqlDropIndex(final String table, final String column) {
        return "DROP INDEX IF EXISTS " + table + "_" + column + "_index";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_LINK_ENTRIES);
        db.execSQL(SQL_CREATE_LINK_ENTRY_ID_INDEX);
        db.execSQL(SQL_CREATE_LINK_CREATED_INDEX);
        db.execSQL(SQL_CREATE_NOTE_ENTRIES);
        db.execSQL(SQL_CREATE_NOTE_ENTRY_ID_INDEX);
        db.execSQL(SQL_CREATE_NOTE_LINK_ID_INDEX);
        db.execSQL(SQL_CREATE_NOTE_CREATED_INDEX);
        db.execSQL(SQL_CREATE_FAVORITE_ENTRIES);
        db.execSQL(SQL_CREATE_FAVORITE_ENTRY_ID_INDEX);
        db.execSQL(SQL_CREATE_FAVORITE_NAME_INDEX);
        db.execSQL(SQL_CREATE_FAVORITE_CREATED_INDEX);
        db.execSQL(SQL_CREATE_TAG_ENTRIES);
        db.execSQL(SQL_CREATE_SYNC_RESULT_ENTRIES);
        db.execSQL(SQL_CREATE_SYNC_RESULT_CREATED_INDEX);
        db.execSQL(SQL_CREATE_SYNC_RESULT_STARTED_INDEX);
        db.execSQL(SQL_CREATE_SYNC_RESULT_ENTRY_INDEX);
        db.execSQL(SQL_CREATE_SYNC_RESULT_RESULT_INDEX);
        db.execSQL(SQL_CREATE_SYNC_RESULT_APPLIED_INDEX);

        db.execSQL(SQL_CREATE_LINK_TAG_ENTRIES);
        db.execSQL(SQL_CREATE_LINK_TAG_LEFT_INDEX);
        db.execSQL(SQL_CREATE_LINK_TAG_RIGHT_INDEX);
        db.execSQL(SQL_CREATE_LINK_TAG_CREATED_INDEX);

        db.execSQL(SQL_CREATE_NOTE_TAG_ENTRIES);
        db.execSQL(SQL_CREATE_NOTE_TAG_LEFT_INDEX);
        db.execSQL(SQL_CREATE_NOTE_TAG_RIGHT_INDEX);
        db.execSQL(SQL_CREATE_NOTE_TAG_CREATED_INDEX);

        db.execSQL(SQL_CREATE_FAVORITE_TAG_ENTRIES);
        db.execSQL(SQL_CREATE_FAVORITE_TAG_LEFT_INDEX);
        db.execSQL(SQL_CREATE_FAVORITE_TAG_RIGHT_INDEX);
        db.execSQL(SQL_CREATE_FAVORITE_TAG_CREATED_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LocalContract.LinkEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LocalContract.NoteEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LocalContract.FavoriteEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LocalContract.TagEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LocalContract.SyncResultEntry.TABLE_NAME);

        final String tagTable = LocalContract.TagEntry.TABLE_NAME;
        final String linkRefTable = LocalContract.LinkEntry.TABLE_NAME + "_" + tagTable;
        final String noteRefTable = LocalContract.NoteEntry.TABLE_NAME + "_" + tagTable;
        final String favoriteRefTable = LocalContract.FavoriteEntry.TABLE_NAME + "_" + tagTable;

        db.execSQL("DROP TABLE IF EXISTS " + linkRefTable);
        db.execSQL("DROP TABLE IF EXISTS " + noteRefTable);
        db.execSQL("DROP TABLE IF EXISTS " + favoriteRefTable);

        db.execSQL(sqlDropEntryIdIndex(LocalContract.LinkEntry.TABLE_NAME));
        db.execSQL(sqlDropIndex(LocalContract.LinkEntry.TABLE_NAME, LocalContract.LinkEntry.COLUMN_NAME_CREATED));
        db.execSQL(sqlDropEntryIdIndex(LocalContract.NoteEntry.TABLE_NAME));
        db.execSQL(sqlDropIndex(LocalContract.NoteEntry.TABLE_NAME, LocalContract.NoteEntry.COLUMN_NAME_LINK_ID));
        db.execSQL(sqlDropIndex(LocalContract.NoteEntry.TABLE_NAME, LocalContract.NoteEntry.COLUMN_NAME_CREATED));
        db.execSQL(sqlDropEntryIdIndex(LocalContract.FavoriteEntry.TABLE_NAME));
        db.execSQL(sqlDropIndex(LocalContract.FavoriteEntry.TABLE_NAME, LocalContract.FavoriteEntry.COLUMN_NAME_CREATED));
        db.execSQL(sqlDropIndex(LocalContract.SyncResultEntry.TABLE_NAME, LocalContract.SyncResultEntry.COLUMN_NAME_CREATED));
        db.execSQL(sqlDropIndex(LocalContract.SyncResultEntry.TABLE_NAME, LocalContract.SyncResultEntry.COLUMN_NAME_STARTED));
        db.execSQL(sqlDropIndex(LocalContract.SyncResultEntry.TABLE_NAME, LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY));
        db.execSQL(sqlDropIndex(LocalContract.SyncResultEntry.TABLE_NAME, LocalContract.SyncResultEntry.COLUMN_NAME_RESULT));
        db.execSQL(sqlDropIndex(LocalContract.SyncResultEntry.TABLE_NAME, LocalContract.SyncResultEntry.COLUMN_NAME_APPLIED));

        db.execSQL(sqlDropIndex(LocalContract.FavoriteEntry.TABLE_NAME, LocalContract.FavoriteEntry.COLUMN_NAME_NAME));
        db.execSQL(sqlDropLeftIndexWithTag(LocalContract.LinkEntry.TABLE_NAME));
        db.execSQL(sqlDropRightIndexWithTag(LocalContract.LinkEntry.TABLE_NAME));
        db.execSQL(sqlDropCreatedIndexWithTag(LocalContract.LinkEntry.TABLE_NAME));

        db.execSQL(sqlDropLeftIndexWithTag(LocalContract.NoteEntry.TABLE_NAME));
        db.execSQL(sqlDropRightIndexWithTag(LocalContract.NoteEntry.TABLE_NAME));
        db.execSQL(sqlDropCreatedIndexWithTag(LocalContract.NoteEntry.TABLE_NAME));

        db.execSQL(sqlDropLeftIndexWithTag(LocalContract.FavoriteEntry.TABLE_NAME));
        db.execSQL(sqlDropRightIndexWithTag(LocalContract.FavoriteEntry.TABLE_NAME));
        db.execSQL(sqlDropCreatedIndexWithTag(LocalContract.FavoriteEntry.TABLE_NAME));

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
