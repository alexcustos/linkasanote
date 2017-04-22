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

    private static final Uri FAVORITE_URI = LocalContract.FavoriteEntry.buildUri();

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

    private Single<Favorite> buildFavorite(final Favorite favorite) {
        Single<Favorite> singleLink = Single.just(favorite);
        Uri favoriteTagUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(favorite.getRowId());
        Single<List<Tag>> singleTags = localTags.getTags(favoriteTagUri).toList();
        return Single.zip(singleLink, singleTags, Favorite::new);
    }

    // Operations

    public Observable<Favorite> getFavorites() {
        return getFavorites(FAVORITE_URI, null, null, null);
    }

    public Observable<Favorite> getFavorites(
            final String selection, final String[] selectionArgs, final String sortOrder) {
        return getFavorites(FAVORITE_URI, selection, selectionArgs, sortOrder);
    }

    public Observable<Favorite> getFavorites(final Uri uri) {
        final String sortOrder = LocalContract.TagEntry.COLUMN_NAME_CREATED + " DESC";
        return getFavorites(uri, null, null, sortOrder);
    }

    public Observable<Favorite> getFavorites(
            final Uri uri,
            final String selection, final String[] selectionArgs, final String sortOrder) {
        Observable<Favorite> favoritesGenerator = Observable.generate(() -> {
            return contentResolver.query(
                    uri, LocalContract.FavoriteEntry.FAVORITE_COLUMNS,
                    selection, selectionArgs, sortOrder);
        }, (cursor, favoriteEmitter) -> {
            if (cursor == null) {
                favoriteEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (cursor.moveToNext()) {
                favoriteEmitter.onNext(Favorite.from(cursor));
            } else {
                favoriteEmitter.onComplete();
            }
            return cursor;
        }, Cursor::close);
        return favoritesGenerator.flatMap(favorite -> buildFavorite(favorite).toObservable());
    }

    public Single<Favorite> getFavorite(final String favoriteId) {
        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    LocalContract.FavoriteEntry.buildUriWith(favoriteId),
                    LocalContract.FavoriteEntry.FAVORITE_COLUMNS, null, null, null)) {
                if (cursor == null) return null;

                if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested favorite was not found");
                }
                return Favorite.from(cursor);
            }
        }).flatMap(this::buildFavorite);
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
            Uri favoriteTagUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = favorite.getTags();
            if (tags != null) {
                Observable.zip(
                        Observable.fromIterable(tags), Observable.just(favoriteTagUri).repeat(),
                        (tag, uri) -> localTags.saveTag(tag, uri).blockingGet())
                        .toList().blockingGet();
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
            return contentResolver.update(FAVORITE_URI, values, null, null);
        });
    }

    public Single<Integer> deleteFavorite(final String favoriteId) {
        Uri uri = LocalContract.FavoriteEntry.buildUriWith(favoriteId);
        return LocalDataSource.delete(contentResolver, uri);
    }

    public Single<Integer> deleteFavorites() {
        return LocalDataSource.delete(contentResolver, FAVORITE_URI);
    }

    public Single<SyncState> getFavoriteSyncState(final String favoriteId) {
        Uri uri = LocalContract.FavoriteEntry.buildUriWith(favoriteId);
        return LocalDataSource.getSyncState(contentResolver, uri);
    }

    public Observable<SyncState> getFavoriteSyncStates() {
        return LocalDataSource.getSyncStates(contentResolver, FAVORITE_URI, null, null, null);
    }

    public Observable<String> getFavoriteIds() {
        return LocalDataSource.getIds(contentResolver, FAVORITE_URI);
    }

    public Single<Boolean> isConflictedFavorites() {
        return LocalDataSource.isConflicted(contentResolver, FAVORITE_URI);
    }

    public Single<Integer> getNextDuplicatedFavorite(final String favoriteName) {
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
                return Favorite.from(cursor);
            }
        }).flatMap(this::buildFavorite);
    }
}
