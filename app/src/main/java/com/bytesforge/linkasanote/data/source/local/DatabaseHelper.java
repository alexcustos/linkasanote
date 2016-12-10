package com.bytesforge.linkasanote.data.source.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "laano.sqlite";

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String DATETIME_TYPE = " DATETIME";
    private static final String BOOLEAN_TYPE = " INTEGER";

    private static final String SQL_CREATE_LINK_ENTRIES =
            "CREATE TABLE " + PersistenceContract.LinkEntry.TABLE_NAME + " (" +
                    PersistenceContract.LinkEntry._ID + INTEGER_TYPE + " PRIMARY_KEY," +
                    PersistenceContract.LinkEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + " UNIQUE," +
                    PersistenceContract.LinkEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                    PersistenceContract.LinkEntry.COLUMN_NAME_UPDATED + DATETIME_TYPE + "," +
                    PersistenceContract.LinkEntry.COLUMN_NAME_VALUE + TEXT_TYPE + "," +
                    PersistenceContract.LinkEntry.COLUMN_NAME_TITLE + TEXT_TYPE + "," +
                    PersistenceContract.LinkEntry.COLUMN_NAME_DISABLED + BOOLEAN_TYPE + "," +
                    PersistenceContract.LinkEntry.COLUMN_NAME_DELETED + BOOLEAN_TYPE + "," +
                    PersistenceContract.LinkEntry.COLUMN_NAME_SYNCED + BOOLEAN_TYPE +
            ");";

    private static final String SQL_CREATE_NOTE_ENTRIES =
            "CREATE TABLE " + PersistenceContract.NoteEntry.TABLE_NAME + " (" +
                    PersistenceContract.NoteEntry._ID + INTEGER_TYPE + " PRIMARY_KEY," +
                    PersistenceContract.NoteEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + " UNIQUE," +
                    PersistenceContract.NoteEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                    PersistenceContract.NoteEntry.COLUMN_NAME_UPDATED + DATETIME_TYPE + "," +
                    PersistenceContract.NoteEntry.COLUMN_NAME_EXCERPT + TEXT_TYPE + "," +
                    PersistenceContract.NoteEntry.COLUMN_NAME_DELETED + BOOLEAN_TYPE + "," +
                    PersistenceContract.NoteEntry.COLUMN_NAME_SYNCED + BOOLEAN_TYPE +
            ");";

    private static final String SQL_CREATE_FAVORITE_ENTRIES =
            "CREATE TABLE " + PersistenceContract.FavoriteEntry.TABLE_NAME + " (" +
                    PersistenceContract.FavoriteEntry._ID + INTEGER_TYPE + " PRIMARY_KEY," +
                    PersistenceContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + " UNIQUE," +
                    PersistenceContract.FavoriteEntry.COLUMN_NAME_ADDED + DATETIME_TYPE + "," +
                    PersistenceContract.FavoriteEntry.COLUMN_NAME_NAME + TEXT_TYPE + "," +
                    PersistenceContract.FavoriteEntry.COLUMN_NAME_SYNCED + BOOLEAN_TYPE +
            ");";

    private static final String SQL_CREATE_TAG_ENTRIES =
            "CREATE TABLE " + PersistenceContract.TagEntry.TABLE_NAME + " (" +
                    PersistenceContract.TagEntry._ID + INTEGER_TYPE + " PRIMARY_KEY," +
                    PersistenceContract.TagEntry.COLUMN_NAME_ADDED + DATETIME_TYPE + "," +
                    PersistenceContract.TagEntry.COLUMN_NAME_NAME + TEXT_TYPE + " UNIQUE" +
            ");";

    private static final String SQL_CREATE_LINK_TAG_ENTRIES =
            sqlCreateManyToManyEntries(
                    PersistenceContract.LinkEntry.TABLE_NAME,
                    PersistenceContract.TagEntry.TABLE_NAME);

    private static final String SQL_CREATE_NOTE_TAG_ENTRIES =
            sqlCreateManyToManyEntries(
                    PersistenceContract.NoteEntry.TABLE_NAME,
                    PersistenceContract.TagEntry.TABLE_NAME);

    private static final String SQL_CREATE_FAVORITE_TAG_ENTRIES =
            sqlCreateManyToManyEntries(
                    PersistenceContract.FavoriteEntry.TABLE_NAME,
                    PersistenceContract.TagEntry.TABLE_NAME);

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static String sqlCreateManyToManyEntries(String leftTable, String rightTable) {
        final String COLUMN_NAME_ADDED = "added";

        return "CREATE TABLE " + leftTable + "_" + rightTable + " (" +
                    BaseColumns._ID + INTEGER_TYPE + " PRIMARY_KEY," +
                    COLUMN_NAME_ADDED + DATETIME_TYPE + "," +
                    leftTable + "_id" + INTEGER_TYPE + "," +
                    rightTable + "_id" + INTEGER_TYPE + ");";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_LINK_ENTRIES);
        db.execSQL(SQL_CREATE_NOTE_ENTRIES);
        db.execSQL(SQL_CREATE_FAVORITE_ENTRIES);
        db.execSQL(SQL_CREATE_TAG_ENTRIES);

        db.execSQL(SQL_CREATE_LINK_TAG_ENTRIES);
        db.execSQL(SQL_CREATE_NOTE_TAG_ENTRIES);
        db.execSQL(SQL_CREATE_FAVORITE_TAG_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
