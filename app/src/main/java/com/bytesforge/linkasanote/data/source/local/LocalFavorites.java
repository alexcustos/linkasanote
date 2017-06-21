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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.bytesforge.linkasanote.data.FavoriteFactory;
import com.bytesforge.linkasanote.data.Item;
import com.bytesforge.linkasanote.data.SyncResult;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.google.common.collect.ObjectArrays;

import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalFavorites<T extends Item> implements LocalItems<T> {

    private static final String TAG = LocalFavorites.class.getSimpleName();
    private static final String TAG_E = LocalFavorites.class.getCanonicalName();

    // NOTE: static fails Mockito to mock this class
    private final Uri FAVORITE_URI;
    private final ContentResolver contentResolver;
    private final LocalSyncResults localSyncResults;
    private final LocalTags localTags;
    private final FavoriteFactory<T> factory;

    public LocalFavorites(
            @NonNull ContentResolver contentResolver,
            @NonNull LocalSyncResults localSyncResults,
            @NonNull LocalTags localTags, @NonNull FavoriteFactory<T> factory) {
        this.contentResolver = checkNotNull(contentResolver);
        this.localSyncResults = checkNotNull(localSyncResults);
        this.localTags = checkNotNull(localTags);
        this.factory = checkNotNull(factory);
        FAVORITE_URI = LocalContract.FavoriteEntry.buildUri();
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
        return getActive(null);
    }

    @Override
    public Observable<T> getActive(final String[] favoriteIds) {
        String selection = LocalContract.FavoriteEntry.COLUMN_NAME_DELETED + " = ?" +
                " OR " + LocalContract.FavoriteEntry.COLUMN_NAME_CONFLICTED + " = ?";
        String[] selectionArgs = {"0", "1"};
        final String sortOrder = LocalContract.FavoriteEntry.COLUMN_NAME_NAME + " ASC";

        int size = favoriteIds == null ? 0 : favoriteIds.length;
        if (size > 0) {
            selection = " (" + selection + ") AND " + LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID +
                    " IN (" + CommonUtils.strRepeat("?", size, ", ") + ")";
            selectionArgs = ObjectArrays.concat(selectionArgs, favoriteIds, String.class);
        }
        return getByChunk(FAVORITE_URI, selection, selectionArgs, sortOrder);
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

    private Observable<T> getByChunk(
            final Uri uri,
            final String selection, final String[] selectionArgs, final String sortOrder) {
        final int chunkSize = Settings.GLOBAL_QUERY_CHUNK_SIZE;
        return LocalDataSource.getCount(contentResolver, uri, null, null)
                .toObservable()
                .flatMap(numRows -> Observable.rangeLong(0, numRows / chunkSize + 1))
                .flatMap(chunk -> {
                    Uri uriChunk = LocalContract.FavoriteEntry.appendUriWith(
                            uri, chunkSize, chunk * chunkSize);
                    return get(uriChunk, selection, selectionArgs, sortOrder);
                });
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
                    CommonUtils.logStackTrace(TAG_E, throwable);
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

    @Override
    public Single<Integer> getNextDuplicated(final String duplicatedKey) {
        final String[] columns = new String[]{
                "MAX(" + LocalContract.FavoriteEntry.COLUMN_NAME_DUPLICATED + ") + 1"};
        final String selection = LocalContract.FavoriteEntry.COLUMN_NAME_NAME + " = ?";
        final String[] selectionArgs = {duplicatedKey};

        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    FAVORITE_URI, columns, selection, selectionArgs, null)) {
                if (cursor == null) return null;

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
            try (Cursor cursor = contentResolver.query(
                    FAVORITE_URI, LocalContract.FavoriteEntry.FAVORITE_COLUMNS,
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

    @Override
    public Single<Boolean> logSyncResult(
            long started, @NonNull final String entryId,
            @NonNull final LocalContract.SyncResultEntry.Result result, boolean applied) {
        checkNotNull(entryId);
        checkNotNull(result);
        if (result == LocalContract.SyncResultEntry.Result.RELATED) {
            throw new RuntimeException("logSyncResult(): there is no RELATED item implementation available for Favorites");
        }
        SyncResult syncResult = new SyncResult(
                started, LocalContract.FavoriteEntry.TABLE_NAME, entryId, result, applied);
        return localSyncResults.log(syncResult);
    }

    @Override
    public Single<Boolean> logSyncResult(
            long started, @NonNull final String entryId,
            @NonNull final LocalContract.SyncResultEntry.Result result) {
        return logSyncResult(started, entryId, result, false);
    }

    @Override
    public Single<Integer> markSyncResultsAsApplied() {
        return localSyncResults.markAsApplied(LocalContract.FavoriteEntry.TABLE_NAME, 0L);
    }

    @Override
    public Observable<Pair<String, LocalContract.SyncResultEntry.Result>> getSyncResultsIds() {
        return localSyncResults.getIds(LocalContract.FavoriteEntry.TABLE_NAME);
    }
}
