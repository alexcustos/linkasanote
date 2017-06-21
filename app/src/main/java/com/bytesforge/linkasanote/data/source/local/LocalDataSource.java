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
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Pair;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.SyncResult;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.sync.SyncState;

import java.security.InvalidParameterException;
import java.util.NoSuchElementException;

import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class LocalDataSource {

    private static final String TAG = LocalDataSource.class.getSimpleName();

    private final LocalSyncResults localSyncResults;
    private final LocalLinks<Link> localLinks;
    private final LocalFavorites<Favorite> localFavorites;
    private final LocalNotes<Note> localNotes;
    private final LocalTags localTags;

    public LocalDataSource(
            @NonNull LocalSyncResults localSyncResults,
            @NonNull LocalLinks<Link> localLinks, @NonNull LocalFavorites<Favorite> localFavorites,
            @NonNull LocalNotes<Note> localNotes, @NonNull LocalTags localTags) {
        this.localSyncResults = checkNotNull(localSyncResults);
        this.localLinks = checkNotNull(localLinks);
        this.localFavorites = checkNotNull(localFavorites);
        this.localNotes = checkNotNull(localNotes);
        this.localTags = checkNotNull(localTags);
    }

    // Links

    public Observable<Link> getLinks() {
        return localLinks.getActive();
    }

    public Observable<Link> getLinks(String[] linkIds) {
        return localLinks.getActive(linkIds);
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
                        // NOTE: it is not a problem if the item was absent
                        return localLinks.delete(linkId)
                                .map(success -> DataSource.ItemState.DELETED);
                    } else {
                        SyncState deletedState = new SyncState(state, SyncState.State.DELETED);
                        return localLinks.update(linkId, deletedState)
                                .onErrorResumeNext(throwable -> throwable instanceof SQLiteConstraintException
                                        ? deferLinkAsDuplicated(linkId, state) : null)
                                .map(success -> success ? DataSource.ItemState.DEFERRED : null);
                    }
                })
                .onErrorReturn(throwable -> throwable instanceof NoSuchElementException
                        ? DataSource.ItemState.DELETED : null);
    }

    private Single<Boolean> deferLinkAsDuplicated(
            final @NonNull String linkId, final @NonNull SyncState state) {
        checkNotNull(linkId);
        checkNotNull(state);
        return localLinks.get(linkId)
                .flatMap(link -> localLinks.getNextDuplicated(link.getDuplicatedKey())
                        .map(duplicated -> {
                            SyncState duplicatedState = new SyncState(state.getETag(), duplicated);
                            return new SyncState(duplicatedState, SyncState.State.DELETED);
                        })
                        .flatMap(deletedState -> localLinks.update(linkId, deletedState)));
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

    public Single<Integer> resetLinksSyncState() {
        return localLinks.resetSyncState();
    }

    public Single<Integer> markLinksSyncResultsAsApplied() {
        return localLinks.markSyncResultsAsApplied();
    }

    public Observable<Pair<String, LocalContract.SyncResultEntry.Result>> getLinksSyncResultsIds() {
        return localLinks.getSyncResultsIds();
    }

    // Favorites

    public Observable<Favorite> getFavorites() {
        return localFavorites.getActive();
    }

    public Observable<Favorite> getFavorites(String[] favoriteIds) {
        return localFavorites.getActive(favoriteIds);
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
                        // NOTE: it is not a problem if the item was absent
                        return localFavorites.delete(favoriteId)
                                .map(success -> DataSource.ItemState.DELETED);
                    } else {
                        SyncState deletedState = new SyncState(state, SyncState.State.DELETED);
                        return localFavorites.update(favoriteId, deletedState)
                                .onErrorResumeNext(throwable -> throwable instanceof SQLiteConstraintException
                                        ? deferFavoriteAsDuplicated(favoriteId, state) : null)
                                .map(success -> success ? DataSource.ItemState.DEFERRED : null);
                    }
                })
                .onErrorReturn(throwable -> throwable instanceof NoSuchElementException
                        ? DataSource.ItemState.DELETED : null);
    }

    private Single<Boolean> deferFavoriteAsDuplicated(
            final @NonNull String favoriteId, final @NonNull SyncState state) {
        checkNotNull(favoriteId);
        checkNotNull(state);
        return localFavorites.get(favoriteId)
                .flatMap(favorite -> localFavorites.getNextDuplicated(favorite.getDuplicatedKey())
                        .map(duplicated -> {
                            SyncState duplicatedState = new SyncState(state.getETag(), duplicated);
                            return new SyncState(duplicatedState, SyncState.State.DELETED);
                        })
                        .flatMap(deletedState -> localFavorites.update(favoriteId, deletedState)));
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

    public Single<Integer> resetFavoritesSyncState() {
        return localFavorites.resetSyncState();
    }

    public Single<Integer> markFavoritesSyncResultsAsApplied() {
        return localFavorites.markSyncResultsAsApplied();
    }

    public Observable<Pair<String, LocalContract.SyncResultEntry.Result>> getFavoritesSyncResultsIds() {
        return localFavorites.getSyncResultsIds();
    }

    // Notes

    public Observable<Note> getNotes() {
        return localNotes.getActive();
    }

    public Observable<Note> getNotes(String[] noteIds) {
        return localNotes.getActive(noteIds);
    }

    public Observable<Note> getNotes(@NonNull final String linkId) {
        checkNotNull(linkId);
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
                        // NOTE: it is not a problem if the item was absent
                        return localNotes.delete(noteId)
                                .map(success -> DataSource.ItemState.DELETED);
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

    public Single<Integer> resetNotesSyncState() {
        return localNotes.resetSyncState();
    }

    public Single<Integer> markNotesSyncResultsAsApplied() {
        return localNotes.markSyncResultsAsApplied();
    }

    public Observable<Pair<String, LocalContract.SyncResultEntry.Result>> getNotesSyncResultsIds() {
        return localNotes.getSyncResultsIds();
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

    // SyncResults

    public Observable<SyncResult> getFreshSyncResults() {
        return localSyncResults.getFresh();
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
        final String[] columns = new String[]{BaseEntry.COLUMN_NAME_ENTRY_ID};

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

    public static Single<Long> getCount(
            final ContentResolver contentResolver, final Uri uri,
            final String column, final String value) {
        final String[] columns = new String[]{"COUNT(" + BaseEntry._ID + ")"};
        final String selection;
        final String[] selectionArgs;
        if (column == null ^ value == null) {
            throw new InvalidParameterException("getCount(): 'column' and 'value' both must be specified or both must be null");
        }
        if (column == null) {
            selection = null;
            selectionArgs = null;
        } else {
            selection = column + " = ?";
            selectionArgs = new String[]{value};
        }
        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    uri, columns, selection, selectionArgs, null)) {
                if (cursor == null) {
                    return null; // NullPointerException
                }
                return cursor.moveToLast() ? cursor.getLong(0) : 0L;
            }
        });
    }
}
