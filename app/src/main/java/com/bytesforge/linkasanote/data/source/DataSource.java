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

package com.bytesforge.linkasanote.data.source;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface DataSource {

    enum ItemState {
        DEFERRED, // SyncState has been successfully updated to UNSYNCED (for any operation)
        DELETED, // Item is actually deleted from the DB
        DUPLICATED, // Temporary mark to notify callback and to avoid CompositeException
        SAVED, // SyncState has been changed to SYNCED
        CONFLICTED, // Item has been market as CONFLICTED
        ERROR_LOCAL, // DB error
        ERROR_CLOUD, // Sync error
        ERROR_EXTRA // Any kind of error with additional cleanup operation (Link's Notes)
    }

    interface Callback {

        void onRepositoryDelete(@NonNull String id, @NonNull ItemState itemState);
        void onRepositorySave(@NonNull String id, @NonNull ItemState itemState);
    }

    void addLinksCallback(@NonNull DataSource.Callback callback);
    void removeLinksCallback(@NonNull DataSource.Callback callback);
    boolean isLinkCacheDirty();
    boolean isLinkCacheNeedRefresh();
    Observable<Link> getLinks();
    Single<Link> getLink(@NonNull String linkId);
    Observable<ItemState> saveLink(@NonNull Link link, boolean syncable);
    Single<ItemState> syncSavedLink(@NonNull String linkId);
    Observable<ItemState> deleteLink(
            @NonNull String linkId, boolean syncable, long started, boolean deleteNotes);
    Single<Boolean> isConflictedLinks();
    Single<Boolean> isUnsyncedLinks();
    Single<Boolean> autoResolveLinkConflict(@NonNull String linkId);
    void refreshLinks();
    void refreshLink(@NonNull String linkId);
    void checkLinksSyncLog();
    void removeCachedLink(@NonNull String linkId);
    int getLinkCacheSize();

    void addFavoritesCallback(@NonNull DataSource.Callback callback);
    void removeFavoritesCallback(@NonNull DataSource.Callback callback);
    boolean isFavoriteCacheDirty();
    boolean isFavoriteCacheNeedRefresh();
    Observable<Favorite> getFavorites();
    Single<Favorite> getFavorite(@NonNull String favoriteId);
    Observable<ItemState> saveFavorite(@NonNull Favorite favorite, boolean syncable);
    Single<ItemState> syncSavedFavorite(@NonNull String favoriteId);
    Observable<ItemState> deleteFavorite(
            @NonNull String favoriteId, boolean syncable, long started);
    Single<Boolean> isConflictedFavorites();
    Single<Boolean> isUnsyncedFavorites();
    Single<Boolean> autoResolveFavoriteConflict(@NonNull String favoriteId);
    void refreshFavorites();
    void refreshFavorite(@NonNull String favoriteId);
    void checkFavoritesSyncLog();
    void removeCachedFavorite(@NonNull String favoriteId);
    int getFavoriteCacheSize();

    void addNotesCallback(@NonNull DataSource.Callback callback);
    void removeNotesCallback(@NonNull DataSource.Callback callback);
    boolean isNoteCacheDirty();
    boolean isNoteCacheNeedRefresh();
    Observable<Note> getNotes();
    Single<Note> getNote(@NonNull String noteId);
    Observable<ItemState> saveNote(@NonNull Note note, boolean syncable);
    Single<ItemState> syncSavedNote(@NonNull String noteId);
    Observable<ItemState> deleteNote(@NonNull String noteId, boolean syncable, long started);
    Single<Boolean> isConflictedNotes();
    Single<Boolean> isUnsyncedNotes();
    void refreshNotes();
    void refreshNote(@NonNull String noteId);
    void checkNotesSyncLog();
    void removeCachedNote(@NonNull String noteId);
    int getNoteCacheSize();

    Observable<Tag> getTags();
    Single<Tag> getTag(@NonNull String tagId);
    void saveTag(@NonNull Tag tag);

    Single<Boolean> isConflicted();
    Single<Boolean> isUnsynced();
    Single<Integer> getSyncStatus();
    Single<Long> resetSyncState();
}
