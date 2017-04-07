package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class LocalDataSource implements DataSource {

    private static final String TAG = LocalDataSource.class.getSimpleName();

    private final ContentResolver contentResolver;
    private final LocalFavorites localFavorites;
    private final LocalTags localTags;

    public LocalDataSource(
            @NonNull ContentResolver contentResolver, @NonNull LocalFavorites localFavorites,
            @NonNull LocalTags localTags) {
        this.contentResolver = checkNotNull(contentResolver);
        this.localFavorites = checkNotNull(localFavorites);
        this.localTags = checkNotNull(localTags);
    }

    // Links

    @Override
    public Single<List<Link>> getLinks() {
        final String selection = LocalContract.LinkEntry.COLUMN_NAME_DELETED + " = ?";
        final String[] selectionArgs = {"0"};

        Observable<Link> linkObservable = Observable.generate(() -> {
            return contentResolver.query(
                    LocalContract.LinkEntry.buildLinksUri(),
                    LocalContract.LinkEntry.LINK_COLUMNS,
                    selection, selectionArgs, null);
        }, (cursor, listEmitter) -> {
            if (cursor == null) {
                listEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (cursor.moveToNext()) {
                listEmitter.onNext(Link.from(cursor));
            } else {
                listEmitter.onComplete();
            }
            return cursor;
        }, Cursor::close);
        // TODO: find why cast cannot be applied without variable
        return linkObservable.toList();
    }

    @Override
    public Single<Link> getLink(@NonNull String linkId) {
        return null;
    }

    @Override
    public void saveLink(@NonNull Link link) {
        ContentValues values = checkNotNull(link).getContentValues();
        contentResolver.insert(LocalContract.LinkEntry.buildLinksUri(), values);
    }

    @Override
    public void deleteAllLinks() {
        contentResolver.delete(LocalContract.LinkEntry.buildLinksUri(), null, null);
    }

    // Notes

    @Override
    public Single<List<Note>> getNotes() {
        return null;
    }

    @Override
    public Single<Note> getNote(@NonNull String noteId) {
        return null;
    }

    @Override
    public void saveNote(@NonNull Note note) {
    }

    @Override
    public void deleteAllNotes() {
        contentResolver.delete(LocalContract.NoteEntry.buildNotesUri(), null, null);
    }

    // Favorites

    @Override
    public Observable<Favorite> getFavorites() {
        final String selection = LocalContract.FavoriteEntry.COLUMN_NAME_DELETED + " = ?" +
                " OR " + LocalContract.FavoriteEntry.COLUMN_NAME_CONFLICTED + " = ?";
        final String[] selectionArgs = {"0", "1"};
        final String sortOrder = LocalContract.FavoriteEntry.COLUMN_NAME_NAME + " ASC";

        return localFavorites.getFavorites(selection, selectionArgs, sortOrder);
    }

    @Override
    public Single<Favorite> getFavorite(@NonNull final String favoriteId) {
        checkNotNull(favoriteId);
        return localFavorites.getFavorite(favoriteId);
    }

    @Override
    public void saveFavorite(@NonNull final Favorite favorite) {
        checkNotNull(favorite);

        long rowId = localFavorites.saveFavorite(favorite).blockingGet();
        if (rowId <= 0) {
            Log.e(TAG, "Favorite was not saved [" + favorite.getId() + "]");
        }
    }

    @Override
    public void deleteAllFavorites() {
        localFavorites.deleteFavorites().blockingGet();
    }

    @Override
    public void deleteFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);

        SyncState state;
        try {
            state = localFavorites.getFavoriteSyncState(favoriteId).blockingGet();
        } catch (NoSuchElementException e) {
            return; // Nothing to delete
        } // let throw NullPointerException
        int numRows;
        if (!state.isSynced() && state.getETag() == null) {
            // NOTE: if one has never been synced
            numRows = localFavorites.deleteFavorite(favoriteId).blockingGet();
        } else {
            SyncState deletedState = new SyncState(state, SyncState.State.DELETED);
            numRows = localFavorites.updateFavorite(favoriteId, deletedState)
                    .blockingGet();
        }
        if (numRows != 1) {
            Log.e(TAG, "Unexpected number of rows processed [" + numRows + ", id=" + favoriteId + "]");
        }
    }

    @Override
    public Single<Boolean> isConflictedFavorites() {
        return localFavorites.isConflictedFavorites();
    }

    // Tags

    @Override
    public Observable<Tag> getTags() {
        return localTags.getTags(LocalContract.TagEntry.buildTagsUri());
    }

    @Override
    public Single<Tag> getTag(@NonNull String tagName) {
        return localTags.getTag(checkNotNull(tagName));
    }

    @Override
    public void saveTag(@NonNull Tag tag) {
        localTags.saveTag(checkNotNull(tag), LocalContract.TagEntry.buildTagsUri());
    }

    @Override
    public void deleteAllTags() {
        localTags.deleteTags().blockingGet();
    }

    // Statics

    public static Single<Integer> delete(final ContentResolver contentResolver, final Uri uri) {
        return Single.fromCallable(() -> contentResolver.delete(uri, null, null));
    }

    public static Single<SyncState> getSyncState(
            final ContentResolver contentResolver, final Uri uri) {
        return Single.fromCallable(() -> {
            Cursor cursor = contentResolver.query(
                    uri, LocalContract.SYNC_STATE_COLUMNS, null, null, null);
            if (cursor == null) return null; // NOTE: NullPointerException

            if (!cursor.moveToLast()) {
                cursor.close();
                throw new NoSuchElementException("The requested favorite was not found");
            }
            try {
                return SyncState.from(cursor);
            } finally {
                cursor.close();
            }
        });
    }

    public static Observable<SyncState> getSyncStates(
            final ContentResolver contentResolver, final Uri uri,
            final String selection, final String[] selectionArgs, final String sortOrder) {
        return Observable.generate(() -> {
            return contentResolver.query(
                    uri, LocalContract.SYNC_STATE_COLUMNS, selection, selectionArgs, sortOrder);
        }, (cursor, stateEmitter) -> {
            if (cursor == null) {
                stateEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (!cursor.moveToNext()) {
                stateEmitter.onComplete();
                return cursor;
            }
            stateEmitter.onNext(SyncState.from(cursor));
            return cursor;
        }, Cursor::close);
    }

    public static Observable<String> getIds(
            final ContentResolver contentResolver, final Uri uri) {
        String[] columns = new String[]{LocalContract.COMMON_NAME_ENTRY_ID};

        return Observable.generate(() -> {
            return contentResolver.query(uri, columns, null, null, null);
        }, (cursor, emitter) -> {
            if (cursor == null) {
                emitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (!cursor.moveToNext()) {
                emitter.onComplete();
                return cursor;
            }
            String id = cursor.getString(cursor.getColumnIndexOrThrow(
                    LocalContract.COMMON_NAME_ENTRY_ID));
            emitter.onNext(id);
            return cursor;
        }, Cursor::close);
    }

    // NOTE: won't work with *_ITEM queries
    public static Single<Boolean> isConflicted(
            final ContentResolver contentResolver, final Uri uri) {
        final String[] columns = new String[]{"COUNT(" + BaseColumns._ID + ")"};
        final String selection = LocalContract.COMMON_NAME_CONFLICTED + " = ?";
        final String[] selectionArgs = {"1"};

        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    uri, columns, selection, selectionArgs, null)) {
                if (cursor == null) {
                    return null; // NOTE: NullPointerException
                }
                return cursor.moveToLast() && cursor.getLong(0) > 0;
            }
        });
    }
}
