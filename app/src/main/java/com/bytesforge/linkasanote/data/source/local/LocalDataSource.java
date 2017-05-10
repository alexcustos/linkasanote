package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

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
public class LocalDataSource {

    private static final String TAG = LocalDataSource.class.getSimpleName();

    private final LocalLinks<Link> localLinks;
    private final LocalFavorites<Favorite> localFavorites;
    private final LocalNotes<Note> localNotes;
    private final LocalTags localTags;

    public LocalDataSource(
            @NonNull LocalLinks<Link> localLinks,
            @NonNull LocalFavorites<Favorite> localFavorites,
            @NonNull LocalNotes<Note> localNotes,
            @NonNull LocalTags localTags) {
        this.localLinks = checkNotNull(localLinks);
        this.localFavorites = checkNotNull(localFavorites);
        this.localNotes = checkNotNull(localNotes);
        this.localTags = checkNotNull(localTags);
    }

    // Links

    public Observable<Link> getLinks() {
        return localLinks.getActive();
    }

    public Single<Link> getLink(@NonNull final String linkId) {
        checkNotNull(linkId);
        return localLinks.get(linkId);
    }

    public Single<DataSource.ItemState> saveLink(@NonNull final Link link) {
        checkNotNull(link);
        return localLinks.save(link)
                .map(success -> success ? DataSource.ItemState.DEFERRED : null);
    }

    @VisibleForTesting
    public void deleteAllLinks() {
        localLinks.delete().blockingGet();
    }

    public Single<DataSource.ItemState> deleteLink(@NonNull String linkId) {
        checkNotNull(linkId);
        return localLinks.getSyncState(linkId)
                .flatMap(state -> {
                    boolean neverSynced = (!state.isSynced() && state.getETag() == null);
                    if (neverSynced) {
                        return localLinks.delete(linkId)
                                .map(success -> success ? DataSource.ItemState.DELETED : null);
                    } else {
                        SyncState deletedState = new SyncState(state, SyncState.State.DELETED);
                        return localLinks.update(linkId, deletedState)
                                .map(success -> success ? DataSource.ItemState.DEFERRED : null);
                    }
                })
                .onErrorReturn(throwable -> throwable instanceof NoSuchElementException
                        ? DataSource.ItemState.DELETED : null);
    }

    public Single<Boolean> isConflictedLinks() {
        return localLinks.isConflicted();
    }

    public Single<Boolean> isUnsyncedLinks() {
        return localLinks.isUnsynced();
    }

    public Single<Boolean> autoResolveLinkConflict(@NonNull String linkId) {
        checkNotNull(linkId);
        return localLinks.autoResolveConflict(linkId);
    }

    // Favorites

    public Observable<Favorite> getFavorites() {
        return localFavorites.getActive();
    }

    public Single<Favorite> getFavorite(@NonNull final String favoriteId) {
        checkNotNull(favoriteId);
        return localFavorites.get(favoriteId);
    }

    public Single<DataSource.ItemState> saveFavorite(@NonNull final Favorite favorite) {
        checkNotNull(favorite);
        return localFavorites.save(favorite)
                .map(success -> success ? DataSource.ItemState.DEFERRED : null);
    }

    @VisibleForTesting
    public void deleteAllFavorites() {
        localFavorites.delete().blockingGet();
    }

    public Single<DataSource.ItemState> deleteFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        return localFavorites.getSyncState(favoriteId)
                .flatMap(state -> {
                    boolean neverSynced = (!state.isSynced() && state.getETag() == null);
                    if (neverSynced) {
                        return localFavorites.delete(favoriteId)
                                .map(success -> success ? DataSource.ItemState.DELETED : null);
                    } else {
                        SyncState deletedState = new SyncState(state, SyncState.State.DELETED);
                        return localFavorites.update(favoriteId, deletedState)
                                .map(success -> success ? DataSource.ItemState.DEFERRED : null);
                    }
                })
                .onErrorReturn(throwable -> throwable instanceof NoSuchElementException
                        ? DataSource.ItemState.DELETED : null);
    }

    public Single<Boolean> isConflictedFavorites() {
        return localFavorites.isConflicted();
    }

    public Single<Boolean> isUnsyncedFavorites() {
        return localFavorites.isUnsynced();
    }

    public Single<Boolean> autoResolveFavoriteConflict(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        return localFavorites.autoResolveConflict(favoriteId);
    }

    // Notes

    public Observable<Note> getNotes() {
        return localNotes.getActive();
    }

    public Observable<Note> getNotes(@NonNull final String linkId) {
        Uri linkNoteUri = LocalContract.LinkEntry.buildNotesDirUriWith(linkId);
        return localNotes.get(linkNoteUri);
    }

    public Single<Note> getNote(@NonNull final String noteId) {
        checkNotNull(noteId);
        return localNotes.get(noteId);
    }

    public Single<DataSource.ItemState> saveNote(@NonNull final Note note) {
        checkNotNull(note);
        return localNotes.save(note)
                .map(success -> success ? DataSource.ItemState.DEFERRED : null);
    }

    @VisibleForTesting
    public void deleteAllNotes() {
        localNotes.delete().blockingGet();
    }

    public Single<DataSource.ItemState> deleteNote(@NonNull String noteId) {
        checkNotNull(noteId);
        return localNotes.getSyncState(noteId)
                .flatMap(state -> {
                    boolean neverSynced = (!state.isSynced() && state.getETag() == null);
                    if (neverSynced) {
                        return localNotes.delete(noteId)
                                .map(success -> success ? DataSource.ItemState.DELETED : null);
                    } else {
                        SyncState deletedState = new SyncState(state, SyncState.State.DELETED);
                        return localNotes.update(noteId, deletedState)
                                .map(success -> success ? DataSource.ItemState.DEFERRED : null);
                    }
                })
                .onErrorReturn(throwable -> throwable instanceof NoSuchElementException
                        ? DataSource.ItemState.DELETED : null);
    }

    public Single<Boolean> isConflictedNotes() {
        return localNotes.isConflicted();
    }

    public Single<Boolean> isUnsyncedNotes() {
        return localNotes.isUnsynced();
    }

    // Tags

    public Observable<Tag> getTags() {
        return localTags.getTags(LocalContract.TagEntry.buildUri());
    }

    public Single<Tag> getTag(@NonNull String tagName) {
        return localTags.getTag(checkNotNull(tagName));
    }

    public void saveTag(@NonNull Tag tag) {
        localTags.saveTag(checkNotNull(tag), LocalContract.TagEntry.buildUri());
    }

    @VisibleForTesting
    public void deleteAllTags() {
        localTags.deleteTags().blockingGet();
    }

    // Statics

    public static Single<SyncState> getSyncState(
            final ContentResolver contentResolver, final Uri uri) {
        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    uri, LocalContract.SYNC_STATE_COLUMNS, null, null, null)) {
                if (cursor == null) return null;

                if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested favorite was not found");
                }
                return SyncState.from(cursor);
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
            if (cursor.moveToNext()) {
                stateEmitter.onNext(SyncState.from(cursor));
            } else {
                stateEmitter.onComplete();
            }
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
            if (cursor.moveToNext()) {
                String id = cursor.getString(
                        cursor.getColumnIndexOrThrow(BaseEntry.COLUMN_NAME_ENTRY_ID));
                emitter.onNext(id);
            } else {
                emitter.onComplete();
            }
            return cursor;
        }, Cursor::close);
    }

    /**
     * @return Returns true if there is at least one conflicted record or
     *         if the record exists when *_ITEM URI is provided (conflicted status will be ignored)
     */
    public static Single<Boolean> isConflicted(
            final ContentResolver contentResolver, final Uri uri) {
        return getCount(contentResolver, uri, BaseEntry.COLUMN_NAME_CONFLICTED, "1")
                .map(count -> count > 0);
    }

    /**
     * @return Returns true if there is at least one unsynced record or
     *         if the record exists when *_ITEM URI is provided (conflicted status will be ignored)
     */
    public static Single<Boolean> isUnsynced(
            final ContentResolver contentResolver, final Uri uri) {
        return getCount(contentResolver, uri, BaseEntry.COLUMN_NAME_SYNCED, "0")
                .map(count -> count > 0);
    }

    private static Single<Long> getCount(
            final ContentResolver contentResolver, final Uri uri,
            @NonNull final String column, @NonNull final String value) {
        checkNotNull(column);
        checkNotNull(value);
        final String[] columns = new String[]{"COUNT(" + BaseEntry._ID + ")"};
        final String selection = column + " = ?";
        final String[] selectionArgs = {value};

        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    uri, columns, selection, selectionArgs, null)) {
                if (cursor == null) {
                    return null; // NOTE: NullPointerException
                }
                return cursor.moveToLast() ? cursor.getLong(0) : 0L;
            }
        });
    }
}
