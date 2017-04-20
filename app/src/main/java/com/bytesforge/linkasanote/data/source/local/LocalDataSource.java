package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.sync.SyncState;

import java.util.NoSuchElementException;

import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class LocalDataSource implements DataSource {

    private static final String TAG = LocalDataSource.class.getSimpleName();

    private final ContentResolver contentResolver;
    private final LocalLinks localLinks;
    private final LocalFavorites localFavorites;
    private final LocalNotes localNotes;
    private final LocalTags localTags;

    public LocalDataSource(
            @NonNull ContentResolver contentResolver,
            @NonNull LocalLinks localLinks,
            @NonNull LocalFavorites localFavorites,
            @NonNull LocalNotes localNotes,
            @NonNull LocalTags localTags) {
        this.contentResolver = checkNotNull(contentResolver);
        this.localLinks = checkNotNull(localLinks);
        this.localFavorites = checkNotNull(localFavorites);
        this.localNotes = checkNotNull(localNotes);
        this.localTags = checkNotNull(localTags);
    }

    // Links

    @Override
    public Observable<Link> getLinks() {
        final String selection = LocalContract.LinkEntry.COLUMN_NAME_DELETED + " = ?" +
                " OR " + LocalContract.LinkEntry.COLUMN_NAME_CONFLICTED + " = ?";
        final String[] selectionArgs = {"0", "1"};
        final String sortOrder = LocalContract.LinkEntry.COLUMN_NAME_CREATED + " DESC";

        return localLinks.getLinks(selection, selectionArgs, sortOrder);
    }

    @Override
    public Single<Link> getLink(@NonNull final String linkId) {
        checkNotNull(linkId);
        return localLinks.getLink(linkId);
    }

    @Override
    public void saveLink(@NonNull final Link link) {
        checkNotNull(link);

        long rowId = localLinks.saveLink(link).blockingGet();
        if (rowId <= 0) {
            Log.e(TAG, "Link was not saved [" + link.getId() + "]");
        }
    }

    @Override
    public void deleteAllLinks() {
        localLinks.deleteLinks().blockingGet();
    }

    @Override
    public void deleteLink(@NonNull String linkId) {
        checkNotNull(linkId);

        SyncState state;
        try {
            state = localLinks.getLinkSyncState(linkId).blockingGet();
        } catch (NoSuchElementException e) {
            return; // Nothing to delete
        } // let throw NullPointerException
        int numRows;
        if (!state.isSynced() && state.getETag() == null) {
            // NOTE: if one has never been synced
            numRows = localLinks.deleteLink(linkId).blockingGet();
        } else {
            SyncState deletedState = new SyncState(state, SyncState.State.DELETED);
            numRows = localLinks.updateLink(linkId, deletedState)
                    .blockingGet();
        }
        if (numRows != 1) {
            Log.e(TAG, "Unexpected number of rows processed [" + numRows + ", id=" + linkId + "]");
        }
    }

    @Override
    public Single<Boolean> isConflictedLinks() {
        return localLinks.isConflictedLinks();
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

    // Notes

    @Override
    public Observable<Note> getNotes() {
        final String selection = LocalContract.NoteEntry.COLUMN_NAME_DELETED + " = ?" +
                " OR " + LocalContract.NoteEntry.COLUMN_NAME_CONFLICTED + " = ?";
        final String[] selectionArgs = {"0", "1"};
        final String sortOrder = LocalContract.NoteEntry.COLUMN_NAME_CREATED + " DESC";

        return localNotes.getNotes(selection, selectionArgs, sortOrder);
    }

    @Override
    public Single<Note> getNote(@NonNull final String noteId) {
        checkNotNull(noteId);
        return localNotes.getNote(noteId);
    }

    @Override
    public void saveNote(@NonNull final Note note) {
        checkNotNull(note);

        long rowId = localNotes.saveNote(note).blockingGet();
        if (rowId <= 0) {
            Log.e(TAG, "Note was not saved [" + note.getId() + "]");
        }
    }

    @Override
    public void deleteAllNotes() {
        localNotes.deleteNotes().blockingGet();
    }

    @Override
    public void deleteNote(@NonNull String noteId) {
        checkNotNull(noteId);

        SyncState state;
        try {
            state = localNotes.getNoteSyncState(noteId).blockingGet();
        } catch (NoSuchElementException e) {
            return; // Nothing to delete
        } // let throw NullPointerException
        int numRows;
        if (!state.isSynced() && state.getETag() == null) {
            // NOTE: if one has never been synced
            numRows = localNotes.deleteNote(noteId).blockingGet();
        } else {
            SyncState deletedState = new SyncState(state, SyncState.State.DELETED);
            numRows = localNotes.updateNote(noteId, deletedState)
                    .blockingGet();
        }
        if (numRows != 1) {
            Log.e(TAG, "Unexpected number of rows processed [" + numRows + ", id=" + noteId + "]");
        }
    }

    @Override
    public Single<Boolean> isConflictedNotes() {
        return localNotes.isConflictedNotes();
    }

    // Tags

    @Override
    public Observable<Tag> getTags() {
        return localTags.getTags(LocalContract.TagEntry.buildUri());
    }

    @Override
    public Single<Tag> getTag(@NonNull String tagName) {
        return localTags.getTag(checkNotNull(tagName));
    }

    @Override
    public void saveTag(@NonNull Tag tag) {
        localTags.saveTag(checkNotNull(tag), LocalContract.TagEntry.buildUri());
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
        String[] columns = new String[]{BaseEntry.COLUMN_NAME_ENTRY_ID};

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
                    BaseEntry.COLUMN_NAME_ENTRY_ID));
            emitter.onNext(id);
            return cursor;
        }, Cursor::close);
    }

    // NOTE: won't work with *_ITEM queries
    public static Single<Boolean> isConflicted(
            final ContentResolver contentResolver, final Uri uri) {
        final String[] columns = new String[]{"COUNT(" + BaseEntry._ID + ")"};
        final String selection = BaseEntry.COLUMN_NAME_CONFLICTED + " = ?";
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
