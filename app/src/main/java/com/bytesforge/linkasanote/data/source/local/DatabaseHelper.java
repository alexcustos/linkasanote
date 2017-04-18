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
                    LocalContract.LinkEntry.COLUMN_NAME_SYNCED + ") ON CONFLICT ABORT" +
            ");";
    private static final String SQL_CREATE_LINK_ENTRY_ID_INDEX = sqlCreateIndex(
            LocalContract.LinkEntry.TABLE_NAME, LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID);

    // NOTE: note entry must not be unique, but duplicate filter is needed
    private static final String SQL_CREATE_NOTE_ENTRIES =
            "CREATE TABLE " + LocalContract.NoteEntry.TABLE_NAME + " (" +
                    LocalContract.NoteEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + " NOT NULL UNIQUE," +
                    LocalContract.NoteEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_UPDATED + DATETIME_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_NOTE + TEXT_TYPE + " NOT NULL," +
                    LocalContract.NoteEntry.COLUMN_NAME_LINK_ID + TEXT_TYPE + " REFERENCES " +
                    LocalContract.LinkEntry.TABLE_NAME + "(" +
                    LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID + ") ON DELETE SET NULL," +
                    LocalContract.NoteEntry.COLUMN_NAME_ETAG + TEXT_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_DUPLICATED + INTEGER_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_CONFLICTED + BOOLEAN_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_DELETED + BOOLEAN_TYPE + "," +
                    LocalContract.NoteEntry.COLUMN_NAME_SYNCED + BOOLEAN_TYPE +
            ");";
    private static final String SQL_CREATE_NOTE_ENTRY_ID_INDEX = sqlCreateIndex(
            LocalContract.NoteEntry.TABLE_NAME, LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID);
    private static final String SQL_CREATE_NOTE_LINK_ID_INDEX = sqlCreateIndex(
            LocalContract.NoteEntry.TABLE_NAME, LocalContract.NoteEntry.COLUMN_NAME_LINK_ID);

    private static final String SQL_CREATE_FAVORITE_ENTRIES =
            "CREATE TABLE " + LocalContract.FavoriteEntry.TABLE_NAME + " (" +
                    LocalContract.FavoriteEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + " NOT NULL UNIQUE," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_UPDATED + DATETIME_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_NAME + TEXT_TYPE + " NOT NULL," + // UNIQUE
                    LocalContract.FavoriteEntry.COLUMN_NAME_ETAG + TEXT_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_DUPLICATED + INTEGER_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_CONFLICTED + BOOLEAN_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_DELETED + BOOLEAN_TYPE + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_SYNCED + BOOLEAN_TYPE + "," +
                    "UNIQUE (" + LocalContract.FavoriteEntry.COLUMN_NAME_NAME + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_DUPLICATED + "," +
                    LocalContract.FavoriteEntry.COLUMN_NAME_SYNCED + ") ON CONFLICT ABORT" +
            ");";
    private static final String SQL_CREATE_FAVORITE_ENTRY_ID_INDEX = sqlCreateIndex(
            LocalContract.FavoriteEntry.TABLE_NAME, LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID);

    private static final String SQL_CREATE_TAG_ENTRIES =
            "CREATE TABLE " + LocalContract.TagEntry.TABLE_NAME + " (" +
                    LocalContract.TagEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    LocalContract.TagEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                    LocalContract.TagEntry.COLUMN_NAME_NAME + TEXT_TYPE + " UNIQUE" +
            ");";

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

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    private static String sqlCreateTableManyToManyWithTags(final String leftTable) {
        return sqlCreateTableManyToMany(leftTable, LocalContract.TagEntry.TABLE_NAME);
    }

    private static String sqlCreateTableManyToMany(
            final String leftTable, final String rightTable) {
        final String LID = leftTable + BaseEntry._ID;
        final String RID = rightTable + BaseEntry._ID;

        return "CREATE TABLE " + leftTable + "_" + rightTable + " (" +
                BaseEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                BaseEntry.COLUMN_NAME_CREATED + DATETIME_TYPE + "," +
                // TODO: replace trigger with ON DELETE CASCADE
                LID + INTEGER_TYPE + " REFERENCES " + leftTable + "(" + BaseEntry._ID + ")," +
                RID + INTEGER_TYPE + " REFERENCES " + rightTable + "(" + BaseEntry._ID + ")," +
                "UNIQUE (" + LID + "," + RID + ") ON CONFLICT ABORT);";
    }

    private static String sqlCreateTriggerManyToManyWithTags(final String leftTable) {
        return sqlCreateTriggerManyToMany(leftTable, LocalContract.TagEntry.TABLE_NAME);
    }

    private static String sqlCreateTriggerManyToMany(
            final String leftTable, final String rightTable) {
        final String LID = leftTable + BaseEntry._ID;
        final String refTable = leftTable + "_" + rightTable;

        return "CREATE TRIGGER " + refTable + "_delete AFTER DELETE ON " + leftTable +
                " BEGIN DELETE FROM " + refTable +
                " WHERE " + refTable + "." + LID + "=OLD." + BaseEntry._ID + "; END;";
    }

    private static String sqlCreateIndex(final String table, final String column) {
        return "CREATE INDEX " + table + "_" + column + "_index ON " + table + "(" + column + ")";
    }

    private static String sqlDropIndex(final String table, final String column) {
        return "DROP INDEX IF EXISTS " + table + "_" + column + "_index";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_LINK_ENTRIES);
        db.execSQL(SQL_CREATE_LINK_ENTRY_ID_INDEX);
        db.execSQL(SQL_CREATE_NOTE_ENTRIES);
        db.execSQL(SQL_CREATE_NOTE_ENTRY_ID_INDEX);
        db.execSQL(SQL_CREATE_NOTE_LINK_ID_INDEX);
        db.execSQL(SQL_CREATE_FAVORITE_ENTRIES);
        db.execSQL(SQL_CREATE_FAVORITE_ENTRY_ID_INDEX);
        db.execSQL(SQL_CREATE_TAG_ENTRIES);

        db.execSQL(SQL_CREATE_LINK_TAG_ENTRIES);
        db.execSQL(SQL_CREATE_LINK_TAG_TRIGGER);
        db.execSQL(SQL_CREATE_NOTE_TAG_ENTRIES);
        db.execSQL(SQL_CREATE_NOTE_TAG_TRIGGER);
        db.execSQL(SQL_CREATE_FAVORITE_TAG_TRIGGER);
        db.execSQL(SQL_CREATE_FAVORITE_TAG_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LocalContract.LinkEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LocalContract.NoteEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LocalContract.FavoriteEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LocalContract.TagEntry.TABLE_NAME);

        final String tagTable = LocalContract.TagEntry.TABLE_NAME;
        final String linkRefTable = LocalContract.LinkEntry.TABLE_NAME + "_" + tagTable;
        final String noteRefTable = LocalContract.NoteEntry.TABLE_NAME + "_" + tagTable;
        final String favoriteRefTable = LocalContract.FavoriteEntry.TABLE_NAME + "_" + tagTable;

        db.execSQL("DROP TABLE IF EXISTS " + linkRefTable);
        db.execSQL("DROP TRIGGER IF EXISTS " + linkRefTable + "_delete");
        db.execSQL("DROP TABLE IF EXISTS " + noteRefTable);
        db.execSQL("DROP TRIGGER IF EXISTS " + noteRefTable + "_delete");
        db.execSQL("DROP TABLE IF EXISTS " + favoriteRefTable);
        db.execSQL("DROP TRIGGER IF EXISTS " + favoriteRefTable + "_delete");

        sqlDropIndex(LocalContract.LinkEntry.TABLE_NAME, LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID);
        sqlDropIndex(LocalContract.NoteEntry.TABLE_NAME, LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID);
        sqlDropIndex(LocalContract.NoteEntry.TABLE_NAME, LocalContract.NoteEntry.COLUMN_NAME_LINK_ID);
        sqlDropIndex(LocalContract.FavoriteEntry.TABLE_NAME, LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID);

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
