package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Provider;
import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalFavorites {

    private final Context context;
    private final ContentResolver contentResolver;
    private final LocalTags localTags;

    public LocalFavorites(
            @NonNull Context context, @NonNull ContentResolver contentResolver,
            @NonNull LocalTags localTags) {
        this.context = checkNotNull(context);
        this.contentResolver = checkNotNull(contentResolver);
        this.localTags = checkNotNull(localTags);
    }

    public Observable<Favorite> getFavorites() {
        return getFavorites(null, null, null);
    }

    public Observable<Favorite> getFavorites(
            final String selection, final String[] selectionArgs, final String sortOrder) {
        return Observable.generate(() -> {
            return contentResolver.query(
                    LocalContract.FavoriteEntry.buildUri(),
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
            List<Tag> tags = localTags.getTags(favoriteTagsUri).toList().blockingGet();
            favoriteEmitter.onNext(Favorite.from(cursor, tags));

            return cursor;
        }, Cursor::close);
    }

    public Single<Favorite> getFavorite(final String favoriteId) {
        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    LocalContract.FavoriteEntry.buildUriWith(favoriteId),
                    LocalContract.FavoriteEntry.FAVORITE_COLUMNS, null, null, null)) {
                if (cursor == null) {
                    return null; // NOTE: NullPointerException
                } else if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested favorite was not found");
                }
                String rowId = LocalContract.rowIdFrom(cursor);
                Uri favoriteTagsUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
                List<Tag> tags = localTags.getTags(favoriteTagsUri).toList().blockingGet();
                return Favorite.from(cursor, tags);
            }
        });
    }

    public Single<Long> saveFavorite(final Favorite favorite) {
        return Single.fromCallable(() -> {
            ContentValues values = favorite.getContentValues();
            Uri favoriteUri = contentResolver.insert(
                    LocalContract.FavoriteEntry.buildUri(), values);
            if (favoriteUri == null) {
                throw new NullPointerException("Provider must return URI or throw exception");
            }
            String rowId = LocalContract.FavoriteEntry.getIdFrom(favoriteUri);
            Uri uri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = favorite.getTags();
            if (tags != null) {
                for (Tag tag : tags) {
                    localTags.saveTag(tag, uri).blockingGet();
                }
            }
            return Long.parseLong(rowId);
        });
    }

    public Single<Integer> updateFavorite(final String favoriteId, final SyncState state) {
        return Single.fromCallable(() -> {
            ContentValues values = state.getContentValues();
            Uri uri = LocalContract.FavoriteEntry.buildUriWith(favoriteId);
            return contentResolver.update(uri, values, null, null);
        });
    }

    public Single<Integer> resetFavoritesSyncState() {
        return Single.fromCallable(() -> {
            SyncState state = new SyncState(SyncState.State.UNSYNCED);
            ContentValues values = state.getContentValues();
            Uri uri = LocalContract.FavoriteEntry.buildUri();
            return contentResolver.update(uri, values, null, null);
        });
    }

    public Single<Integer> deleteFavorite(final String favoriteId) {
        Uri uri = LocalContract.FavoriteEntry.buildUriWith(favoriteId);
        return LocalDataSource.delete(contentResolver, uri);
    }

    public Single<Integer> deleteFavorites() {
        Uri uri = LocalContract.FavoriteEntry.buildUri();
        return LocalDataSource.delete(contentResolver, uri);
    }

    public Single<SyncState> getFavoriteSyncState(final String favoriteId) {
        Uri uri = LocalContract.FavoriteEntry.buildUriWith(favoriteId);
        return LocalDataSource.getSyncState(contentResolver, uri);
    }

    public Observable<SyncState> getFavoriteSyncStates() {
        Uri uri = LocalContract.FavoriteEntry.buildUri();
        return LocalDataSource.getSyncStates(contentResolver, uri, null, null, null);
    }

    public Observable<String> getFavoriteIds() {
        Uri uri = LocalContract.FavoriteEntry.buildUri();
        return LocalDataSource.getIds(contentResolver, uri);
    }

    public Single<Boolean> isConflictedFavorites() {
        Uri uri = LocalContract.FavoriteEntry.buildUri();
        return LocalDataSource.isConflicted(contentResolver, uri);
    }

    public Single<Integer> getNextDuplicated(final String favoriteName) {
        final String[] columns = new String[]{
                "MAX(" + LocalContract.FavoriteEntry.COLUMN_NAME_DUPLICATED + ") + 1"};
        final String selection = LocalContract.FavoriteEntry.COLUMN_NAME_NAME + " = ?";
        final String[] selectionArgs = {favoriteName};

        return Single.fromCallable(() -> {
            try (Cursor cursor = Provider.rawQuery(context,
                    LocalContract.FavoriteEntry.TABLE_NAME,
                    columns, selection, selectionArgs, null)) {
                if (cursor.moveToLast()) {
                    return cursor.getInt(0);
                }
                return 0;
            }
        });
    }

    public Single<Favorite> getMainFavorite(final String favoriteName) {
        final String selection = LocalContract.FavoriteEntry.COLUMN_NAME_NAME + " = ?" +
                " AND " + LocalContract.FavoriteEntry.COLUMN_NAME_DUPLICATED + " = ?";
        final String[] selectionArgs = {favoriteName, "0"};

        return Single.fromCallable(() -> {
            try (Cursor cursor = Provider.rawQuery(context,
                    LocalContract.FavoriteEntry.TABLE_NAME,
                    LocalContract.FavoriteEntry.FAVORITE_COLUMNS,
                    selection, selectionArgs, null)) {
                if (cursor == null) {
                    return null; // NOTE: NullPointerException
                } else if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested favorite was not found");
                }
                String rowId = LocalContract.rowIdFrom(cursor);
                Uri favoriteTagsUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
                List<Tag> tags = localTags.getTags(favoriteTagsUri).toList().blockingGet();
                return Favorite.from(cursor, tags);
            }
        });
    }
}
