package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Item;
import com.bytesforge.linkasanote.data.ItemFactory;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Provider;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.utils.CommonUtils;

import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalFavorites<T extends Item> implements LocalItem<T> {

    private static final String TAG = LocalFavorites.class.getSimpleName();

    private static final Uri FAVORITE_URI = LocalContract.FavoriteEntry.buildUri();

    private final Context context;
    private final ContentResolver contentResolver;
    private final LocalTags localTags;
    private final ItemFactory<T> factory;

    public LocalFavorites(
            @NonNull Context context, @NonNull ContentResolver contentResolver,
            @NonNull LocalTags localTags, @NonNull ItemFactory<T> factory) {
        this.context = checkNotNull(context);
        this.contentResolver = checkNotNull(contentResolver);
        this.localTags = checkNotNull(localTags);
        this.factory = checkNotNull(factory);
    }

    private Single<T> buildFavorite(final T favorite) {
        Single<T> singleLink = Single.just(favorite);
        Uri favoriteTagUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(favorite.getRowId());
        Single<List<Tag>> singleTags = localTags.getTags(favoriteTagUri).toList();
        return Single.zip(singleLink, singleTags, factory::build);
    }

    // Operations

    @Override
    public Observable<T> getAll() {
        return get(FAVORITE_URI, null, null, null);
    }

    @Override
    public Observable<T> getActive() {
        final String selection = LocalContract.FavoriteEntry.COLUMN_NAME_DELETED + " = ?" +
                " OR " + LocalContract.FavoriteEntry.COLUMN_NAME_CONFLICTED + " = ?";
        final String[] selectionArgs = {"0", "1"};
        final String sortOrder = LocalContract.FavoriteEntry.COLUMN_NAME_NAME + " ASC";

        return get(FAVORITE_URI, selection, selectionArgs, sortOrder);
    }

    @Override
    public Observable<T> getUnsynced() {
        final String selection = LocalContract.FavoriteEntry.COLUMN_NAME_SYNCED + " = ?";
        final String[] selectionArgs = {"0"};

        return get(FAVORITE_URI, selection, selectionArgs, null);
    }

    @Override
    public Observable<T> get(final Uri uri) {
        final String sortOrder = LocalContract.TagEntry.COLUMN_NAME_CREATED + " DESC";
        return get(uri, null, null, sortOrder);
    }

    @Override
    public Observable<T> get(
            final Uri uri,
            final String selection, final String[] selectionArgs, final String sortOrder) {
        Observable<T> favoritesGenerator = Observable.generate(() -> {
            return contentResolver.query(
                    uri, LocalContract.FavoriteEntry.FAVORITE_COLUMNS,
                    selection, selectionArgs, sortOrder);
        }, (cursor, favoriteEmitter) -> {
            if (cursor == null) {
                favoriteEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (cursor.moveToNext()) {
                favoriteEmitter.onNext(factory.from(cursor));
            } else {
                favoriteEmitter.onComplete();
            }
            return cursor;
        }, Cursor::close);
        return favoritesGenerator.flatMap(favorite -> buildFavorite(favorite).toObservable());
    }

    @Override
    public Single<T> get(final String favoriteId) {
        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    LocalContract.FavoriteEntry.buildUriWith(favoriteId),
                    LocalContract.FavoriteEntry.FAVORITE_COLUMNS, null, null, null)) {
                if (cursor == null) return null;

                if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested favorite was not found");
                }
                return factory.from(cursor);
            }
        }).flatMap(this::buildFavorite);
    }

    /**
     * @return Returns true if all tags were successfully saved
     */
    private Single<Boolean> saveTags(final long favoriteRowId, final List<Tag> tags) {
        if (favoriteRowId <= 0) {
            return Single.just(false);
        } else if (tags == null) {
            return Single.just(true);
        }
        Uri favoriteTagUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(favoriteRowId);
        return Observable.fromIterable(tags)
                .flatMap(tag -> localTags.saveTag(tag, favoriteTagUri).toObservable())
                .count()
                .map(count -> count == tags.size())
                .onErrorReturn(throwable -> {
                    CommonUtils.logStackTrace(TAG, throwable);
                    return false;
                });
    }

    @Override
    public Single<Boolean> save(final T favorite) {
        return Single.fromCallable(() -> {
            ContentValues values = favorite.getContentValues();
            Uri favoriteUri = contentResolver.insert(FAVORITE_URI, values);
            if (favoriteUri == null) {
                throw new NullPointerException("Provider must return URI or throw exception");
            }
            String rowId = LocalContract.FavoriteEntry.getIdFrom(favoriteUri);
            return Long.parseLong(rowId);
        }).flatMap(rowId -> saveTags(rowId, favorite.getTags()));
    }

    @Override
    public Single<Boolean> saveDuplicated(final T favorite) {
        return getNextDuplicated(favorite.getDuplicatedKey())
                .flatMap(duplicated -> {
                    SyncState state = new SyncState(favorite.getETag(), duplicated);
                    return Single.just(factory.build(favorite, state));
                }).flatMap(this::save);
    }

    @Override
    public Single<Boolean> update(final String favoriteId, final SyncState state) {
        return Single.fromCallable(() -> {
            ContentValues values = state.getContentValues();
            Uri uri = LocalContract.FavoriteEntry.buildUriWith(favoriteId);
            return contentResolver.update(uri, values, null, null);
        }).map(numRows -> numRows == 1);
    }

    @Override
    public Single<Integer> resetSyncState() {
        return Single.fromCallable(() -> {
            ContentValues values = new ContentValues();
            values.put(LocalContract.FavoriteEntry.COLUMN_NAME_ETAG, (String) null);
            values.put(LocalContract.FavoriteEntry.COLUMN_NAME_SYNCED, false);
            final String selection = LocalContract.FavoriteEntry.COLUMN_NAME_SYNCED + " = ?";
            final String[] selectionArgs = {"1"};
            return contentResolver.update(FAVORITE_URI, values, selection, selectionArgs);
        });
    }

    @Override
    public Single<Boolean> delete(final String favoriteId) {
        Uri uri = LocalContract.FavoriteEntry.buildUriWith(favoriteId);
        return Single.fromCallable(() -> contentResolver.delete(uri, null, null))
                .map(numRows -> numRows == 1);
    }

    @Override
    public Single<Integer> delete() {
        return Single.fromCallable(() -> contentResolver.delete(FAVORITE_URI, null, null));
    }

    @Override
    public Single<SyncState> getSyncState(final String favoriteId) {
        Uri uri = LocalContract.FavoriteEntry.buildUriWith(favoriteId);
        return LocalDataSource.getSyncState(contentResolver, uri);
    }

    @Override
    public Observable<SyncState> getSyncStates() {
        return LocalDataSource.getSyncStates(contentResolver, FAVORITE_URI, null, null, null);
    }

    @Override
    public Observable<String> getIds() {
        return LocalDataSource.getIds(contentResolver, FAVORITE_URI);
    }

    @Override
    public Single<Boolean> isConflicted() {
        return LocalDataSource.isConflicted(contentResolver, FAVORITE_URI);
    }

    @Override
    public Single<Boolean> isUnsynced() {
        return LocalDataSource.isUnsynced(contentResolver, FAVORITE_URI);
    }

    private Single<Integer> getNextDuplicated(final String duplicatedKey) {
        final String[] columns = new String[]{
                "MAX(" + LocalContract.FavoriteEntry.COLUMN_NAME_DUPLICATED + ") + 1"};
        final String selection = LocalContract.FavoriteEntry.COLUMN_NAME_NAME + " = ?";
        final String[] selectionArgs = {duplicatedKey};

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

    @Override
    public Single<T> getMain(final String duplicatedKey) {
        final String selection = LocalContract.FavoriteEntry.COLUMN_NAME_NAME + " = ?" +
                " AND " + LocalContract.FavoriteEntry.COLUMN_NAME_DUPLICATED + " = ?";
        final String[] selectionArgs = {duplicatedKey, "0"};

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
                return factory.from(cursor);
            }
        }).flatMap(this::buildFavorite);
    }

    /**
     * @return Returns true if conflict has been successfully resolved
     */
    @Override
    public Single<Boolean> autoResolveConflict(final String favoriteId) {
        return get(favoriteId)
                .map(favorite -> {
                    if (!favorite.isDuplicated()) {
                        return !favorite.isConflicted();
                    }
                    try {
                        getMain(favorite.getDuplicatedKey()).blockingGet();
                        return false;
                    } catch (NoSuchElementException e) {
                        SyncState state = new SyncState(SyncState.State.SYNCED);
                        return update(favoriteId, state).blockingGet();
                    }
                });
    }
}
