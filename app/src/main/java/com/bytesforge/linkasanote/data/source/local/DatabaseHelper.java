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
            "CREATE TABLE " + LocalContract.LinkEntry.TABLE_NAME + " (" +
                    LocalContract.LinkEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + " UNIQUE," +
                    LocalContract.LinkEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_UPDATED + DATETIME_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_VALUE + TEXT_TYPE + " UNIQUE," +
                    LocalContract.LinkEntry.COLUMN_NAME_TITLE + TEXT_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_DISABLED + BOOLEAN_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_DELETED + BOOLEAN_TYPE + "," +
                    LocalContract.LinkEntry.COLUMN_NAME_SYNCED + BOOLEAN_TYPE +
            ");";

    private static final String SQL_CREATE_NOTE_ENTRIES =
            "CREATE TABLE " + LocalContract.NoteEntry.TABLE_NAME + " (" +
                    LocalContract.NoteEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + " UNIQUE," +
                    LocalContract.NoteEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_UPDATED + DATETIME_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_EXCERPT + TEXT_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_DELETED + BOOLEAN_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_SYNCED + BOOLEAN_TYPE +
            ");";

    private static final String SQL_CREATE_FAVORITE_ENTRIES =
            "CREATE TABLE " + LocalContract.FavoriteEntry.TABLE_NAME + " (" +
                    LocalContract.FavoriteEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + " UNIQUE," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_ADDED + DATETIME_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_NAME + TEXT_TYPE + "," + // UNIQUE
                    LocalContract.FavoriteEntry.COLUMN_NAME_ETAG + TEXT_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_DUPLICATED + INTEGER_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_CONFLICTED + BOOLEAN_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_DELETED + BOOLEAN_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_SYNCED + BOOLEAN_TYPE + "," +
                    "UNIQUE (" + LocalContract.FavoriteEntry.COLUMN_NAME_NAME + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_DUPLICATED + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_SYNCED + ") ON CONFLICT ABORT" +
            ");";

    private static final String SQL_CREATE_TAG_ENTRIES =
            "CREATE TABLE " + LocalContract.TagEntry.TABLE_NAME + " (" +
                    LocalContract.TagEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    LocalContract.TagEntry.COLUMN_NAME_ADDED + DATETIME_TYPE + "," +
                    LocalContract.TagEntry.COLUMN_NAME_NAME + TEXT_TYPE + " UNIQUE" +
            ");";

    private static final String SQL_CREATE_LINK_NOTE_ENTRIES = sqlCreateTableManyToMany(
            LocalContract.LinkEntry.TABLE_NAME, LocalContract.NoteEntry.TABLE_NAME);
    private static final String SQL_CREATE_LINK_NOTE_TRIGGER = sqlCreateTriggerManyToMany(
            LocalContract.LinkEntry.TABLE_NAME, LocalContract.NoteEntry.TABLE_NAME);

    private static final String SQL_CREATE_LINK_TAG_ENTRIES =
            sqlCreateTableManyToManyWithTags(LocalContract.LinkEntry.TABLE_NAME);
    private static final String SQL_CREATE_LINK_TAG_TRIGGER =
            sqlCreateTriggerManyToManyWithTags(LocalContract.LinkEntry.TABLE_NAME);

    private static final String SQL_CREATE_NOTE_TAG_ENTRIES =
            sqlCreateTableManyToManyWithTags(LocalContract.NoteEntry.TABLE_NAME);
    private static final String SQL_CREATE_NOTE_TAG_TRIGGER =
            sqlCreateTriggerManyToManyWithTags(LocalContract.NoteEntry.TABLE_NAME);

    private static final String SQL_CREATE_FAVORITE_TAG_ENTRIES =
            sqlCreateTableManyToManyWithTags(LocalContract.FavoriteEntry.TABLE_NAME);
    private static final String SQL_CREATE_FAVORITE_TAG_TRIGGER =
            sqlCreateTriggerManyToManyWithTags(LocalContract.FavoriteEntry.TABLE_NAME);

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static String sqlCreateTableManyToManyWithTags(final String leftTable) {
        return sqlCreateTableManyToMany(leftTable, LocalContract.TagEntry.TABLE_NAME);
    }

    private static String sqlCreateTableManyToMany(
            final String leftTable, final String rightTable) {
        final String LID = leftTable + BaseColumns._ID;
        final String RID = rightTable + BaseColumns._ID;

        return "CREATE TABLE " + leftTable + "_" + rightTable + " (" +
                BaseColumns._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                LocalContract.MANY_TO_MANY_COMMON_NAME_ADDED + DATETIME_TYPE + "," +
                LID + INTEGER_TYPE + " REFERENCES " + leftTable + "(" + BaseColumns._ID + ")," +
                RID + INTEGER_TYPE + " REFERENCES " + rightTable + "(" + BaseColumns._ID + ")," +
                "UNIQUE (" + LID + "," + RID + ") ON CONFLICT ABORT);";
    }

    private static String sqlCreateTriggerManyToManyWithTags(final String leftTable) {
        return sqlCreateTriggerManyToMany(leftTable, LocalContract.TagEntry.TABLE_NAME);
    }

    private static String sqlCreateTriggerManyToMany(
            final String leftTable, final String rightTable) {
        final String LID = leftTable + BaseColumns._ID;
        final String refTable = leftTable + "_" + rightTable;

        return "CREATE TRIGGER " + refTable + "_delete AFTER DELETE ON " + leftTable +
                " BEGIN DELETE FROM " + refTable +
                " WHERE " + refTable + "." + LID + "=OLD." + BaseColumns._ID + "; END;";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_LINK_ENTRIES);
        db.execSQL(SQL_CREATE_NOTE_ENTRIES);
        db.execSQL(SQL_CREATE_FAVORITE_ENTRIES);
        db.execSQL(SQL_CREATE_TAG_ENTRIES);

        db.execSQL(SQL_CREATE_LINK_TAG_ENTRIES);
        db.execSQL(SQL_CREATE_LINK_TAG_TRIGGER);
        db.execSQL(SQL_CREATE_NOTE_TAG_ENTRIES);
        db.execSQL(SQL_CREATE_NOTE_TAG_TRIGGER);
        db.execSQL(SQL_CREATE_FAVORITE_TAG_TRIGGER);
        db.execSQL(SQL_CREATE_FAVORITE_TAG_ENTRIES);
        db.execSQL(SQL_CREATE_LINK_NOTE_ENTRIES);
        db.execSQL(SQL_CREATE_LINK_NOTE_TRIGGER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IS EXISTS " + LocalContract.LinkEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IS EXISTS " + LocalContract.NoteEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IS EXISTS " + LocalContract.FavoriteEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IS EXISTS " + LocalContract.TagEntry.TABLE_NAME);

        final String tagTable = LocalContract.TagEntry.TABLE_NAME;
        final String linkRefTable = LocalContract.LinkEntry.TABLE_NAME + "_" + tagTable;
        final String noteRefTable = LocalContract.NoteEntry.TABLE_NAME + "_" + tagTable;
        final String favoriteRefTable = LocalContract.FavoriteEntry.TABLE_NAME + "_" + tagTable;
        final String linkNoteRefTable = LocalContract.LinkEntry.TABLE_NAME + "_"
                + LocalContract.NoteEntry.TABLE_NAME;

        db.execSQL("DROP TABLE IS EXISTS " + linkRefTable);
        db.execSQL("DROP TRIGGER IF EXISTS " + linkRefTable + "_delete");
        db.execSQL("DROP TABLE IS EXISTS " + noteRefTable);
        db.execSQL("DROP TRIGGER IF EXISTS " + noteRefTable + "_delete");
        db.execSQL("DROP TABLE IS EXISTS " + favoriteRefTable);
        db.execSQL("DROP TRIGGER IF EXISTS " + favoriteRefTable + "_delete");
        db.execSQL("DROP TABLE IS EXISTS " + linkNoteRefTable);
        db.execSQL("DROP TRIGGER IS EXISTS " + linkNoteRefTable + "_delete");

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
