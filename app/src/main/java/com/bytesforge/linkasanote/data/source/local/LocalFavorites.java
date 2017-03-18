package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

public final class LocalFavorites {

    private LocalFavorites() {
    }

    public static Observable<Favorite> getFavorites(
            final ContentResolver contentResolver,
            final String selection, final String[] selectionArgs, final String sortOrder) {
        return Observable.generate(() -> {
            return contentResolver.query(
                    LocalContract.FavoriteEntry.buildFavoritesUri(),
                    LocalContract.FavoriteEntry.FAVORITE_COLUMNS,
                    selection, selectionArgs, sortOrder);
        }, (cursor, favoriteEmitter) -> {
            if (cursor == null) {
                favoriteEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (!cursor.moveToNext()) {
                favoriteEmitter.onComplete();
                return cursor;
            }
            String rowId = LocalContract.rowIdFrom(cursor);
            Uri favoriteTagsUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = LocalTags.getTags(contentResolver, favoriteTagsUri)
                    .toList().blockingGet();
            favoriteEmitter.onNext(Favorite.from(cursor, tags));

            return cursor;
        }, Cursor::close);
    }

    public static Single<Favorite> getFavorite(
            final ContentResolver contentResolver, final String favoriteId) {
        return Single.fromCallable(() -> {
            Cursor cursor = contentResolver.query(
                    LocalContract.FavoriteEntry.buildFavoritesUriWith(favoriteId),
                    LocalContract.FavoriteEntry.FAVORITE_COLUMNS, null, null, null);
            if (cursor == null) return null; // NOTE: NullPointerException

            if (!cursor.moveToLast()) {
                cursor.close();
                throw new NoSuchElementException("The requested favorite was not found");
            }
            String rowId = LocalContract.rowIdFrom(cursor);
            Uri favoriteTagsUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = LocalTags.getTags(contentResolver, favoriteTagsUri)
                    .toList().blockingGet();
            try {
                return Favorite.from(cursor, tags);
            } finally {
                cursor.close();
            }
        });
    }

    public static Single<Long> saveFavorite(
            final ContentResolver contentResolver, final Favorite favorite) {
        return Single.fromCallable(() -> {
            ContentValues values = favorite.getContentValues();
            Uri favoriteUri = contentResolver.insert(
                    LocalContract.FavoriteEntry.buildFavoritesUri(), values);
            if (favoriteUri == null) {
                throw new NullPointerException("Provider must return URI or throw exception");
            }
            String rowId = LocalContract.FavoriteEntry.getFavoriteId(favoriteUri);
            Uri uri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = favorite.getTags();
            if (tags != null) {
                for (Tag tag : tags) {
                    LocalTags.saveTag(contentResolver, tag, uri).blockingGet();
                }
            }
            return Long.parseLong(rowId);
        });
    }

    public static Single<Integer> updateFavorite(
            final ContentResolver contentResolver, final String favoriteId, final SyncState state) {
        return Single.fromCallable(() -> {
            ContentValues values = state.getContentValues();
            Uri uri = LocalContract.FavoriteEntry.buildFavoritesUriWith(favoriteId);
            return contentResolver.update(uri, values, null, null);
        });
    }

    public static Single<Integer> deleteFavorite(
            final ContentResolver contentResolver, final String favoriteId) {
        Uri uri = LocalContract.FavoriteEntry.buildFavoritesUriWith(favoriteId);
        return LocalDataSource.delete(contentResolver, uri);
    }

    public static Single<Integer> deleteFavorites(final ContentResolver contentResolver) {
        Uri uri = LocalContract.FavoriteEntry.buildFavoritesUri();
        return LocalDataSource.delete(contentResolver, uri);
    }

    public static Single<SyncState> getFavoriteSyncState(
            final ContentResolver contentResolver, final String favoriteId) {
        Uri uri = LocalContract.FavoriteEntry.buildFavoritesUriWith(favoriteId);
        return LocalDataSource.getSyncState(contentResolver, uri);
    }

    public static Observable<SyncState> getFavoriteSyncStates(
            final ContentResolver contentResolver) {
        Uri uri = LocalContract.FavoriteEntry.buildFavoritesUri();
        return LocalDataSource.getSyncStates(contentResolver, uri, null, null, null);
    }

    public static Observable<String> getFavoriteIds(final ContentResolver contentResolver) {
        Uri uri = LocalContract.FavoriteEntry.buildFavoritesUri();
        return LocalDataSource.getIds(contentResolver, uri);
    }

    // TODO: search filter must work similar, so it must be called with the ContentProvider
    public static Single<Integer> getNextDuplicated(
            final Context context, final String favoriteName) {
        return Single.fromCallable(() -> {
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            SQLiteDatabase db = databaseHelper.getReadableDatabase();

            String selection = LocalContract.FavoriteEntry.COLUMN_NAME_NAME + " = ?";
            String[] selectionArgs = {favoriteName};
            try (Cursor cursor = db.query(
                    LocalContract.FavoriteEntry.TABLE_NAME, new String[]{"MAX(duplicated) + 1"},
                    selection, selectionArgs, null, null, null)) {
                if (cursor.moveToLast()) {
                    return cursor.getInt(0);
                }
                return 0;
            }
        });
    }
}
