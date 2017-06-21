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

import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.util.Pair;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.SyncResult;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;
import com.bytesforge.linkasanote.sync.SyncAdapter;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.bytesforge.linkasanote.utils.UuidUtils.isKeyValidUuid;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class Repository implements DataSource {

    private static final String TAG = Repository.class.getSimpleName();
    private static final String TAG_E = Repository.class.getCanonicalName();

    private static final String SYNC_COMPLETE_TOKEN = ""; // NOTE: empty String

    private final LocalDataSource localDataSource;
    private final CloudDataSource cloudDataSource;
    private final BaseSchedulerProvider schedulerProvider;

    @VisibleForTesting
    @Nullable
    Map<String, Link> cachedLinks;

    @VisibleForTesting
    @Nullable
    Set<String> dirtyLinks;

    @VisibleForTesting
    @Nullable
    Map<String, Favorite> cachedFavorites;

    @VisibleForTesting
    @Nullable
    Set<String> dirtyFavorites;

    @VisibleForTesting
    @Nullable
    Map<String, Note> cachedNotes;

    @VisibleForTesting
    @Nullable
    Set<String> dirtyNotes;

    @VisibleForTesting
    @Nullable
    Map<String, Tag> cachedTags;

    @VisibleForTesting
    public boolean linkCacheIsDirty = true;

    @VisibleForTesting
    public boolean favoriteCacheIsDirty = true;

    @VisibleForTesting
    public boolean noteCacheIsDirty = true;

    private Set<DataSource.Callback> linksCallbacks;
    private Set<DataSource.Callback> favoritesCallbacks;
    private Set<DataSource.Callback> notesCallbacks;

    //@Inject NOTE: @Provides is needed for testing to mock Repository
    public Repository(
            LocalDataSource localDataSource, CloudDataSource cloudDataSource,
            BaseSchedulerProvider schedulerProvider) {
        this.localDataSource = localDataSource;
        this.cloudDataSource = cloudDataSource;
        this.schedulerProvider = schedulerProvider;
    }

    // Links

    @Override
    public void addLinksCallback(@NonNull DataSource.Callback callback) {
        checkNotNull(callback);
        if (linksCallbacks == null) {
            linksCallbacks = new HashSet<>();
        }
        linksCallbacks.add(callback);
    }

    @Override
    public void removeLinksCallback(@NonNull DataSource.Callback callback) {
        checkNotNull(callback);
        if (linksCallbacks != null) {
            linksCallbacks.remove(callback);
        }
    }

    private void notifyLinksDeleteCallbacks(
            @NonNull String linkId, @NonNull DataSource.ItemState itemState) {
        checkNotNull(linkId);
        checkNotNull(itemState);
        if (linksCallbacks != null) {
            for (DataSource.Callback linksCallback : linksCallbacks) {
                linksCallback.onRepositoryDelete(linkId, itemState);
            }
        }
    }

    private void notifyLinksSaveCallbacks(
            @NonNull String linkId, @NonNull DataSource.ItemState itemState) {
        checkNotNull(linkId);
        checkNotNull(itemState);
        if (linksCallbacks != null) {
            for (DataSource.Callback linksCallback : linksCallbacks) {
                linksCallback.onRepositorySave(linkId, itemState);
            }
        }
    }

    @Override
    public boolean isLinkCacheDirty() {
        return linkCacheIsDirty || cachedLinks == null;
    }

    @Override
    public boolean isLinkCacheNeedRefresh() {
        boolean needRefresh = dirtyLinks != null && !dirtyLinks.isEmpty();
        return isLinkCacheDirty() || needRefresh;
    }

    @Override
    public Observable<Link> getLinks() {
        return Observable.just(dirtyLinks != null && !dirtyLinks.isEmpty())
                .flatMap(needRefresh -> {
                    Log.d(TAG, "getLinks() [" + linkCacheIsDirty + "; cached=" +
                            (cachedLinks == null ? "NULL" : cachedLinks.size()) +  "; dirty=" +
                            (dirtyLinks == null ? "NULL" : dirtyLinks.size()) + "]");
                    if (!linkCacheIsDirty && cachedLinks != null && !needRefresh) {
                        return Observable.fromIterable(cachedLinks.values());
                    }
                    if (needRefresh && !linkCacheIsDirty) {
                        return refreshDirtyAndGetCachedLinks();
                    } else {
                        return getAndCacheLocalLinks();
                    }
                });
    }

    private Observable<Link> getAndCacheLocalLinks() {
        if (cachedLinks == null) {
            cachedLinks = new LinkedHashMap<>();
        }
        cachedLinks.clear();
        if (dirtyLinks == null) {
            dirtyLinks = new HashSet<>();
        }
        dirtyLinks.clear();
        Observable<Link> markSyncResultsAsAppliedObservable =
                localDataSource.markLinksSyncResultsAsApplied()
                        .toObservable()
                        .ignoreElements()
                        .toObservable();
        return Observable.concat(
                markSyncResultsAsAppliedObservable, localDataSource.getLinks(null))
                .doOnComplete(() -> linkCacheIsDirty = false)
                .doOnNext(link -> {
                    String linkId = link.getId();
                    Log.d(TAG, "getAndCacheLocalLinks() -> doOnNext() [" + linkId + "]");
                    cachedLinks.put(linkId, link);
                });
    }

    private Observable<Link> refreshDirtyAndGetCachedLinks() {
        if (cachedLinks == null) {
            cachedLinks = new LinkedHashMap<>();
        }
        if (dirtyLinks == null) {
            dirtyLinks = new HashSet<>();
        }
        HashSet<String> dirty = new HashSet<>(dirtyLinks);
        Observable<Link> refreshDirtyLinksObservable = Observable.fromIterable(dirty)
                .flatMap(id -> {
                    if (SYNC_COMPLETE_TOKEN.equals(id)) {
                        dirtyLinks.remove(id);
                        return localDataSource.getLinksSyncResultsIds();
                    } else {
                        return Observable.just(Pair.create(id, null));
                    }
                })
                .filter(pair -> {
                    if (pair.second == LocalContract.SyncResultEntry.Result.DELETED) {
                        removeCachedLink(pair.first);
                        return false;
                    }
                    return true;
                })
                .map(pair -> pair.first)
                .toList()
                .toObservable()
                .flatMap(linkIds -> {
                    int size = linkIds.size();
                    if (size > 0) {
                        String[] ids = linkIds.toArray(new String[size]);
                        return localDataSource.getLinks(ids);
                    } else {
                        return Observable.empty();
                    }
                })
                .doOnNext(link -> {
                    String linkId = link.getId();
                    Log.d(TAG, "refreshDirtyAndGetCachedLinks() -> doOnNext() [" + linkId + "]");
                    Link prevLink = cachedLinks.put(linkId, link);
                    if (prevLink == null) {
                        refreshLinks();
                        throw new IllegalStateException("New item has been found, reload is required");
                    }
                    dirtyLinks.remove(linkId);
                })
                .ignoreElements()
                .toObservable();
        return Observable.concat(
                refreshDirtyLinksObservable, Observable.fromIterable(cachedLinks.values()));
    }

    @Override
    public Single<Link> getLink(@NonNull String linkId) {
        checkNotNull(linkId);
        if (!isKeyValidUuid(linkId)) {
            throw new InvalidParameterException("getLink() called with invalid UUID");
        }
        final Link cachedLink = getCachedLink(linkId);
        if (cachedLink != null) {
            return Single.just(cachedLink);
        }
        return getAndCacheLocalLink(linkId);
    }

    private Single<Link> getAndCacheLocalLink(String linkId) {
        if (cachedLinks == null) {
            cachedLinks = new LinkedHashMap<>();
        }
        if (dirtyLinks == null) {
            dirtyLinks = new HashSet<>();
        }
        return localDataSource.getLink(linkId)
                .doOnSuccess(link -> {
                    Log.d(TAG, "getAndCacheLocalLink() -> doOnSuccess() [" + linkId + "]");
                    boolean replaced = cachedLinks.put(linkId, link) != null;
                    if (replaced) { // if was dirty
                        dirtyLinks.remove(linkId);
                    } else {
                        // NOTE: cache must be reloaded to place new Link to the right place
                        refreshLinks();
                    }
                });
    }

    @Nullable
    private Link getCachedLink(@NonNull String linkId) {
        checkNotNull(linkId);
        if (cachedLinks != null
                && (dirtyLinks == null || !dirtyLinks.contains(linkId))) {
            Log.d(TAG, "getCachedLink() [" + linkId + "]");
            return cachedLinks.get(linkId);
        }
        return null;
    }

    @Override
    public Observable<ItemState> saveLink(@NonNull final Link link, final boolean syncable) {
        checkNotNull(link);
        String linkId = link.getId();
        Observable<ItemState> localSavingObservable = localDataSource.saveLink(link)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case DEFERRED:
                            // Link
                            if (cachedLinks == null) {
                                cachedLinks = new LinkedHashMap<>();
                            }
                            // NOTE: new Link has no rowId to bind to RecyclerView
                            if (link.getRowId() < 0) {
                                Map<String, Link> map = new LinkedHashMap<>(cachedLinks);
                                cachedLinks.clear();
                                // NOTE: new Link goes to the top of the list
                                cachedLinks.put(linkId, link);
                                cachedLinks.putAll(map);
                            }
                            refreshLink(linkId);
                            // Tags
                            if (cachedTags == null) {
                                cachedTags = new LinkedHashMap<>();
                            }
                            List<Tag> tags = link.getTags();
                            if (tags != null) {
                                for (Tag tag : tags) {
                                    cachedTags.put(tag.getName(), tag);
                                }
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Local saveLink()");
                    }
                })
                .onErrorReturn(throwable -> {
                    // NOTE: return null - NullPointerException; throw new - CompositeException
                    //       throw throwable - unhandled exception error
                    ItemState itemState;
                    if (throwable instanceof SQLiteConstraintException) {
                        itemState = ItemState.DUPLICATED;
                    } else {
                        CommonUtils.logStackTrace(TAG_E, throwable);
                        itemState = ItemState.ERROR_LOCAL;
                    }
                    return itemState;
                })
                .doOnSuccess(itemState -> {
                    notifyLinksSaveCallbacks(linkId, itemState);
                    if (itemState == ItemState.DUPLICATED) {
                        throw new SQLiteConstraintException("Links UNIQUE constraint failed [" + linkId + "]");
                    }
                })
                .toObservable();
        Observable<ItemState> cloudSavingObservable;
        if (syncable) {
            cloudSavingObservable = getCloudSaveLinkSingle(linkId)
                    .subscribeOn(schedulerProvider.io())
                    .toObservable();
        } else {
            cloudSavingObservable = Observable.empty();
        }
        return Observable.concat(localSavingObservable, cloudSavingObservable);
    }

    private Single<ItemState> getCloudSaveLinkSingle(@NonNull final String linkId) {
        checkNotNull(linkId);
        return cloudDataSource.saveLink(linkId)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case SAVED:
                            refreshLink(linkId);
                            break;
                        case CONFLICTED:
                            refreshLink(linkId);
                            break;
                        case ERROR_LOCAL:
                        case ERROR_CLOUD:
                            refreshLink(linkId);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state from Cloud saveLink()");
                    }
                    notifyLinksSaveCallbacks(linkId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: all errors include the local to retrieve and update the item state
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    ItemState itemState = ItemState.ERROR_CLOUD;
                    notifyLinksSaveCallbacks(linkId, itemState);
                    return itemState;
                });
    }

    @Override
    public Single<ItemState> syncSavedLink(@NonNull final String linkId) {
        checkNotNull(linkId);
        return getCloudSaveLinkSingle(linkId);
    }

    @VisibleForTesting
    public void deleteAllLinks() {
        localDataSource.deleteAllLinks(); // blocking
        //cloudDataSource.deleteAllLinks();
        if (cachedLinks == null) {
            cachedLinks = new LinkedHashMap<>();
        }
        cachedLinks.clear();
        if (dirtyLinks == null) {
            dirtyLinks = new HashSet<>();
        }
        dirtyLinks.clear();
    }

    private Observable<ItemState> getDeleteLinkNotesObservable(
            @NonNull final String linkId, @NonNull List<Note> notes,
            final boolean syncable, final long started, final boolean deleteNotes) {
        Observable<ItemState> deleteNotesObservable;
        if (deleteNotes) {
            // Delete
            deleteNotesObservable = Observable.fromIterable(notes)
                    .flatMap(note -> deleteNote(note.getId(), syncable, started));
        } else {
            // Sync
            deleteNotesObservable = Observable.fromIterable(notes)
                    .flatMap(note -> {
                        String noteId = note.getId();
                        if (note.isDeleted() && !note.isConflicted()) {
                            return deleteNote(noteId, syncable, started);
                        } else {
                            SyncState state = new SyncState(note.getState(), SyncState.State.UNSYNCED);
                            Note unboundNote = new Note(note.getId(), note.getCreated(),
                                    note.getUpdated(), note.getNote(), null, note.getTags(), state);
                            return saveNote(unboundNote, syncable);
                        }
                    });
        }
        return deleteNotesObservable
                .filter(itemState -> itemState == ItemState.ERROR_LOCAL
                        || itemState == ItemState.ERROR_CLOUD)
                .toList()
                .toObservable()
                .flatMap(itemStates -> {
                    if (itemStates.size() > 0) {
                        ItemState itemState = ItemState.ERROR_EXTRA;
                        notifyLinksDeleteCallbacks(linkId, itemState);
                        return Observable.just(itemState);
                    }
                    return Observable.empty();
                })
                .onErrorReturn(throwable -> {
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    ItemState itemState = ItemState.ERROR_EXTRA;
                    notifyLinksDeleteCallbacks(linkId, itemState);
                    return itemState;
                });
    }

    @Override
    public Observable<ItemState> deleteLink(
            @NonNull final String linkId, final boolean syncable,
            final long started, final boolean deleteNotes) {
        checkNotNull(linkId);
        // NOTE: it is unnecessary to maintain integrity from this point
        Observable<ItemState> localDeletionObservable = localDataSource.deleteLink(linkId)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case DEFERRED:
                        case DELETED:
                            if (cachedLinks == null) {
                                cachedLinks = new LinkedHashMap<>();
                            }
                            removeCachedLink(linkId);
                            // NOTE: notes & it's cache will be updated in getDeleteLinkNotesObservable
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Local deleteLink()");
                    }
                    notifyLinksDeleteCallbacks(linkId, itemState);
                })
                .onErrorReturn(throwable -> {
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    ItemState itemState = ItemState.ERROR_LOCAL;
                    notifyLinksDeleteCallbacks(linkId, itemState);
                    return itemState;
                })
                .toObservable();
        Observable<ItemState> cloudDeletionObservable;
        if (syncable) {
            cloudDeletionObservable = getCloudDeleteLinkSingle(linkId, started)
                    .subscribeOn(schedulerProvider.io())
                    .toObservable();
        } else {
            cloudDeletionObservable = Observable.empty();
        }
        return localDataSource.getNotes(linkId)
                .toList()
                .toObservable()
                .flatMap(notes -> Observable.mergeDelayError(
                        Observable.concat(localDeletionObservable, cloudDeletionObservable),
                        getDeleteLinkNotesObservable(linkId, notes, syncable, started, deleteNotes)));
    }

    private Single<ItemState> getCloudDeleteLinkSingle(
            @NonNull final String linkId, long started) {
        checkNotNull(linkId);
        return cloudDataSource.deleteLink(linkId, started)
                .doOnSuccess(itemState -> {
                    // NOTE: flatMap() -> return .map() -> null: actually returns null to onSuccess
                    switch (itemState) {
                        case DELETED: // visibility was not changed
                            break;
                        case CONFLICTED:
                            refreshLinks(); // need to be shown again
                            break;
                        case ERROR_LOCAL:
                        case ERROR_CLOUD:
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Cloud deleteLink()");
                    }
                    notifyLinksDeleteCallbacks(linkId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: all errors include the local to retrieve and update the item state
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    ItemState itemState = ItemState.ERROR_CLOUD;
                    notifyLinksDeleteCallbacks(linkId, itemState);
                    return itemState;
                });
    }

    @Override
    public Single<Boolean> isConflictedLinks() {
        return localDataSource.isConflictedLinks();
    }

    @Override
    public Single<Boolean> isUnsyncedLinks() {
        return localDataSource.isUnsyncedLinks();
    }

    @Override
    public Single<Boolean> autoResolveLinkConflict(@NonNull String linkId) {
        checkNotNull(linkId);
        return localDataSource.autoResolveLinkConflict(linkId)
                .doOnSuccess(success -> {
                    if (success) refreshLink(linkId);
                });
    }

    @Override
    public void refreshLinks() {
        Log.d(TAG, "refreshLinks()");
        linkCacheIsDirty = true;
    }

    @Override
    public void refreshLink(@NonNull String linkId) {
        checkNotNull(linkId);
        Log.d(TAG, "refreshLink() [" + linkId + "]");
        if (dirtyLinks == null) {
            dirtyLinks = new HashSet<>();
        }
        dirtyLinks.add(linkId);
    }

    @Override
    public void checkLinksSyncLog() {
        Log.d(TAG, "checkLinksSyncLog()");
        if (dirtyLinks == null) {
            dirtyLinks = new HashSet<>();
        }
        dirtyLinks.add(SYNC_COMPLETE_TOKEN);
    }

    @Override
    public void removeCachedLink(@NonNull String linkId) {
        checkNotNull(linkId);
        Log.d(TAG, "removeCachedLink() [" + linkId + "]");
        if (cachedLinks == null) {
            cachedLinks = new LinkedHashMap<>();
        }
        cachedLinks.remove(linkId);
    }

    @Override
    public int getLinkCacheSize() {
        return cachedLinks == null ? 0 : cachedLinks.size();
    }

    // Favorites

    @Override
    public void addFavoritesCallback(@NonNull DataSource.Callback callback) {
        checkNotNull(callback);
        if (favoritesCallbacks == null) {
            favoritesCallbacks = new HashSet<>();
        }
        favoritesCallbacks.add(callback);
    }

    @Override
    public void removeFavoritesCallback(@NonNull DataSource.Callback callback) {
        checkNotNull(callback);
        if (favoritesCallbacks != null) {
            favoritesCallbacks.remove(callback);
        }
    }

    private void notifyFavoritesDeleteCallbacks(
            @NonNull String favoriteId, @NonNull DataSource.ItemState itemState) {
        checkNotNull(favoriteId);
        checkNotNull(itemState);
        if (favoritesCallbacks != null) {
            for (DataSource.Callback favoritesCallback : favoritesCallbacks) {
                favoritesCallback.onRepositoryDelete(favoriteId, itemState);
            }
        }
    }

    private void notifyFavoritesSaveCallbacks(
            @NonNull String favoriteId, @NonNull DataSource.ItemState itemState) {
        checkNotNull(favoriteId);
        checkNotNull(itemState);
        if (favoritesCallbacks != null) {
            for (DataSource.Callback favoritesCallback : favoritesCallbacks) {
                favoritesCallback.onRepositorySave(favoriteId, itemState);
            }
        }
    }

    @Override
    public boolean isFavoriteCacheDirty() {
        return favoriteCacheIsDirty || cachedFavorites == null;
    }

    @Override
    public boolean isFavoriteCacheNeedRefresh() {
        boolean needRefresh = dirtyFavorites != null && !dirtyFavorites.isEmpty();
        return isFavoriteCacheDirty() || needRefresh;
    }

    @Override
    public Observable<Favorite> getFavorites() {
        return Observable.just(dirtyFavorites != null && !dirtyFavorites.isEmpty())
                .flatMap(needRefresh -> {
                    Log.d(TAG, "getFavorites() [" + favoriteCacheIsDirty + "; cached=" +
                            (cachedFavorites == null ? "NULL" : cachedFavorites.size()) + "; dirty=" +
                            (dirtyFavorites == null ? "NULL" : dirtyFavorites.size()) + "]");
                    if (!favoriteCacheIsDirty && cachedFavorites != null && !needRefresh) {
                        return Observable.fromIterable(cachedFavorites.values());
                    }
                    if (needRefresh && !favoriteCacheIsDirty) {
                        return refreshDirtyAndGetCachedFavorites();
                    } else {
                        return getAndCacheLocalFavorites();
                    }
                });
    }

    private Observable<Favorite> getAndCacheLocalFavorites() {
        if (cachedFavorites == null) {
            cachedFavorites = new LinkedHashMap<>();
        }
        cachedFavorites.clear();
        if (dirtyFavorites == null) {
            dirtyFavorites = new HashSet<>();
        }
        dirtyFavorites.clear();
        Observable<Favorite> markSyncResultsAsAppliedObservable =
                localDataSource.markFavoritesSyncResultsAsApplied()
                        .toObservable()
                        .ignoreElements()
                        .toObservable();
        return Observable.concat(
                markSyncResultsAsAppliedObservable, localDataSource.getFavorites(null))
                .doOnComplete(() -> favoriteCacheIsDirty = false)
                .doOnNext(favorite -> {
                    String favoriteId = favorite.getId();
                    Log.d(TAG, "getAndCacheLocalFavorites() -> doOnNext() [" + favoriteId + "]");
                    cachedFavorites.put(favoriteId, favorite);
                });
    }

    private Observable<Favorite> refreshDirtyAndGetCachedFavorites() {
        if (cachedFavorites == null) {
            cachedFavorites = new LinkedHashMap<>();
        }
        if (dirtyFavorites == null) {
            dirtyFavorites = new HashSet<>();
        }
        HashSet<String> dirty = new HashSet<>(dirtyFavorites);
        Observable<Favorite> refreshDirtyFavoritesObservable = Observable.fromIterable(dirty)
                .flatMap(id -> {
                    if (SYNC_COMPLETE_TOKEN.equals(id)) {
                        dirtyFavorites.remove(id);
                        return localDataSource.getFavoritesSyncResultsIds();
                    } else {
                        return Observable.just(Pair.create(id, null));
                    }
                })
                .filter(pair -> {
                    if (pair.second == LocalContract.SyncResultEntry.Result.DELETED) {
                        removeCachedFavorite(pair.first);
                        return false;
                    }
                    return true;
                })
                .map(pair -> pair.first)
                .toList()
                .toObservable()
                .flatMap(favoriteIds -> {
                    int size = favoriteIds.size();
                    if (size > 0) {
                        String[] ids = favoriteIds.toArray(new String[size]);
                        return localDataSource.getFavorites(ids);
                    } else {
                        return Observable.empty();
                    }
                })
                .doOnNext(favorite -> {
                    String favoriteId = favorite.getId();
                    Log.d(TAG, "refreshDirtyAndGetCachedFavorites() -> doOnNext() [" + favoriteId + "]");
                    Favorite prevFavorite = cachedFavorites.put(favoriteId, favorite);
                    if (prevFavorite == null) {
                        refreshFavorites();
                        throw new IllegalStateException("New item has been found, reload is required");
                    }
                    dirtyFavorites.remove(favoriteId);
                })
                .ignoreElements()
                .toObservable();
        return Observable.concat(
                refreshDirtyFavoritesObservable, Observable.fromIterable(cachedFavorites.values()));
    }

    @Override
    public Single<Favorite> getFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        if (!isKeyValidUuid(favoriteId)) {
            throw new InvalidParameterException("getFavorite() called with invalid UUID");
        }
        final Favorite cachedFavorite = getCachedFavorite(favoriteId);
        if (cachedFavorite != null) {
            return Single.just(cachedFavorite);
        }
        return getAndCacheLocalFavorite(favoriteId);
    }

    private Single<Favorite> getAndCacheLocalFavorite(String favoriteId) {
        if (cachedFavorites == null) {
            cachedFavorites = new LinkedHashMap<>();
        }
        if (dirtyFavorites == null) {
            dirtyFavorites = new HashSet<>();
        }
        return localDataSource.getFavorite(favoriteId)
                .doOnSuccess(favorite -> {
                    Log.d(TAG, "getAndCacheLocalFavorite() -> doOnSuccess() [" + favoriteId + "]");
                    boolean replaced = cachedFavorites.put(favoriteId, favorite) != null;
                    if (replaced) { // if was dirty
                        dirtyFavorites.remove(favoriteId);
                    } else {
                        // NOTE: cache must be reloaded to place new Favorite to the right place
                        refreshFavorites();
                    }
                });
    }

    @Nullable
    private Favorite getCachedFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        if (cachedFavorites != null
                && (dirtyFavorites == null || !dirtyFavorites.contains(favoriteId))) {
            Log.d(TAG, "getCachedFavorite() [" + favoriteId + "]");
            return cachedFavorites.get(favoriteId);
        }
        return null;
    }

    @Override
    public Observable<ItemState> saveFavorite(@NonNull final Favorite favorite, final boolean syncable) {
        checkNotNull(favorite);
        String favoriteId = favorite.getId();
        Observable<ItemState> localSavingObservable = localDataSource.saveFavorite(favorite)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case DEFERRED:
                            // Favorite
                            if (cachedFavorites == null) {
                                cachedFavorites = new LinkedHashMap<>();
                            }
                            // NOTE: new Favorite has no rowId to bind to RecyclerView
                            Favorite cachedFavorite = cachedFavorites.get(favoriteId);
                            if (cachedFavorite != null) {
                                String cachedFavoriteName = cachedFavorite.getName();
                                if (cachedFavoriteName != null
                                        && cachedFavoriteName.equals(favorite.getName())) {
                                    refreshFavorite(favoriteId);
                                } else {
                                    refreshFavorites(); // NOTE: new Favorite's position is unknown
                                }
                            } else {
                                refreshFavorites(); // NOTE: new Favorite's position is unknown
                            }
                            // Tags
                            if (cachedTags == null) {
                                cachedTags = new LinkedHashMap<>();
                            }
                            List<Tag> tags = favorite.getTags();
                            if (tags != null) {
                                for (Tag tag : tags) {
                                    cachedTags.put(tag.getName(), tag);
                                }
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Local saveFavorite()");
                    }
                })
                .onErrorReturn(throwable -> {
                    ItemState itemState;
                    if (throwable instanceof SQLiteConstraintException) {
                        itemState = ItemState.DUPLICATED;
                    } else {
                        CommonUtils.logStackTrace(TAG_E, throwable);
                        itemState = ItemState.ERROR_LOCAL;
                    }
                    return itemState;
                })
                .doOnSuccess(itemState -> {
                    notifyFavoritesSaveCallbacks(favoriteId, itemState);
                    if (itemState == ItemState.DUPLICATED) {
                        throw new SQLiteConstraintException("Favorites UNIQUE constraint failed [" + favoriteId + "]");
                    }
                })
                .toObservable();
        Observable<ItemState> cloudSavingObservable;
        if (syncable) {
            cloudSavingObservable = getCloudSaveFavoriteSingle(favoriteId)
                    .subscribeOn(schedulerProvider.io())
                    .toObservable();
        } else {
            cloudSavingObservable = Observable.empty();
        }
        return Observable.concat(localSavingObservable, cloudSavingObservable);
    }

    private Single<ItemState> getCloudSaveFavoriteSingle(@NonNull final String favoriteId) {
        checkNotNull(favoriteId);
        return cloudDataSource.saveFavorite(favoriteId)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case SAVED:
                            refreshFavorite(favoriteId);
                            break;
                        case CONFLICTED:
                            refreshFavorite(favoriteId);
                            break;
                        case ERROR_LOCAL:
                        case ERROR_CLOUD:
                            refreshFavorite(favoriteId);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state from Cloud saveFavorite()");
                    }
                    notifyFavoritesSaveCallbacks(favoriteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: all errors include the local to retrieve and update the item state
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    ItemState itemState = ItemState.ERROR_CLOUD;
                    notifyFavoritesSaveCallbacks(favoriteId, itemState);
                    return itemState;
                });
    }

    @Override
    public Single<ItemState> syncSavedFavorite(@NonNull final String favoriteId) {
        checkNotNull(favoriteId);
        return getCloudSaveFavoriteSingle(favoriteId);
    }

    @VisibleForTesting
    public void deleteAllFavorites() {
        localDataSource.deleteAllFavorites(); // blocking
        //cloudDataSource.deleteAllFavorites();
        if (cachedFavorites == null) {
            cachedFavorites = new LinkedHashMap<>();
        }
        cachedFavorites.clear();
        if (dirtyFavorites == null) {
            dirtyFavorites = new HashSet<>();
        }
        dirtyFavorites.clear();
    }

    @Override
    public Observable<ItemState> deleteFavorite(
            @NonNull String favoriteId, boolean syncable, long started) {
        checkNotNull(favoriteId);
        Observable<ItemState> localDeletionObservable = localDataSource.deleteFavorite(favoriteId)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case DEFERRED:
                        case DELETED:
                            if (cachedFavorites == null) {
                                cachedFavorites = new LinkedHashMap<>();
                            }
                            removeCachedFavorite(favoriteId);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Local deleteFavorite()");
                    }
                    notifyFavoritesDeleteCallbacks(favoriteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    ItemState itemState = ItemState.ERROR_LOCAL;
                    notifyFavoritesDeleteCallbacks(favoriteId, itemState);
                    return itemState;
                })
                .toObservable();
        Observable<ItemState> cloudDeletionObservable;
        if (syncable) {
            cloudDeletionObservable = getCloudDeleteFavoriteSingle(favoriteId, started)
                    .subscribeOn(schedulerProvider.io())
                    .toObservable();
        } else {
            cloudDeletionObservable = Observable.empty();
        }
        return Observable.concat(localDeletionObservable, cloudDeletionObservable);
    }

    private Single<ItemState> getCloudDeleteFavoriteSingle(
            @NonNull final String favoriteId, long started) {
        checkNotNull(favoriteId);
        return cloudDataSource.deleteFavorite(favoriteId, started)
                .doOnSuccess(itemState -> {
                    // NOTE: flatMap() -> return .map() -> null: actually returns null to onSuccess
                    switch (itemState) {
                        case DELETED: // visibility was not changed
                            break;
                        case CONFLICTED:
                            refreshFavorites(); // need to be shown again
                            break;
                        case ERROR_LOCAL:
                        case ERROR_CLOUD:
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state from Cloud deleteFavorite()");
                    }
                    notifyFavoritesDeleteCallbacks(favoriteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: all errors include the local to retrieve and update the item state
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    ItemState itemState = ItemState.ERROR_CLOUD;
                    notifyFavoritesDeleteCallbacks(favoriteId, itemState);
                    return itemState;
                });
    }

    @Override
    public Single<Boolean> isConflictedFavorites() {
        return localDataSource.isConflictedFavorites();
    }

    @Override
    public Single<Boolean> isUnsyncedFavorites() {
        return localDataSource.isUnsyncedFavorites();
    }

    @Override
    public Single<Boolean> autoResolveFavoriteConflict(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        return localDataSource.autoResolveFavoriteConflict(favoriteId)
                .doOnSuccess(success -> {
                    if (success) refreshFavorite(favoriteId);
                });
    }

    @Override
    public void refreshFavorites() {
        Log.d(TAG, "refreshFavorites()");
        favoriteCacheIsDirty = true;
    }

    @Override
    public void refreshFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        Log.d(TAG, "refreshFavorite() [" + favoriteId + "]");
        if (dirtyFavorites == null) {
            dirtyFavorites = new HashSet<>();
        }
        dirtyFavorites.add(favoriteId);
    }

    @Override
    public void checkFavoritesSyncLog() {
        Log.d(TAG, "checkFavoritesSyncLog()");
        if (dirtyFavorites == null) {
            dirtyFavorites = new HashSet<>();
        }
        dirtyFavorites.add(SYNC_COMPLETE_TOKEN);
    }

    @Override
    public void removeCachedFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        Log.d(TAG, "removeCachedFavorite() [" + favoriteId + "]");
        if (cachedFavorites == null) {
            cachedFavorites = new LinkedHashMap<>();
        }
        cachedFavorites.remove(favoriteId);
    }

    @Override
    public int getFavoriteCacheSize() {
        return cachedFavorites == null ? 0 : cachedFavorites.size();
    }

    // Notes

    @Override
    public void addNotesCallback(@NonNull DataSource.Callback callback) {
        checkNotNull(callback);
        if (notesCallbacks == null) {
            notesCallbacks = new HashSet<>();
        }
        notesCallbacks.add(callback);
    }

    @Override
    public void removeNotesCallback(@NonNull DataSource.Callback callback) {
        checkNotNull(callback);
        if (notesCallbacks != null) {
            notesCallbacks.remove(callback);
        }
    }

    private void notifyNotesDeleteCallbacks(
            @NonNull String noteId, @NonNull DataSource.ItemState itemState) {
        checkNotNull(noteId);
        checkNotNull(itemState);
        if (notesCallbacks != null) {
            for (DataSource.Callback notesCallback : notesCallbacks) {
                notesCallback.onRepositoryDelete(noteId, itemState);
            }
        }
    }

    private void notifyNotesSaveCallbacks(
            @NonNull String noteId, @NonNull DataSource.ItemState itemState) {
        checkNotNull(noteId);
        checkNotNull(itemState);
        if (notesCallbacks != null) {
            for (DataSource.Callback notesCallback : notesCallbacks) {
                notesCallback.onRepositorySave(noteId, itemState);
            }
        }
    }

    @Override
    public boolean isNoteCacheDirty() {
        return noteCacheIsDirty || cachedNotes == null;
    }

    @Override
    public boolean isNoteCacheNeedRefresh() {
        boolean needRefresh = dirtyNotes != null && !dirtyNotes.isEmpty();
        return isNoteCacheDirty() || needRefresh;
    }

    @Override
    public Observable<Note> getNotes() {
        return Observable.just(dirtyNotes != null && !dirtyNotes.isEmpty())
                .flatMap(needRefresh -> {
                    Log.d(TAG, "getNotes() [" + noteCacheIsDirty + "; cached=" +
                            (cachedNotes == null ? "NULL" : cachedNotes.size()) + "; dirty=" +
                            (dirtyNotes == null ? "NULL" : dirtyNotes.size()) + "]");
                    if (!noteCacheIsDirty && cachedNotes != null && !needRefresh) {
                        return Observable.fromIterable(cachedNotes.values());
                    }
                    if (needRefresh && !noteCacheIsDirty) {
                        return refreshDirtyAndGetCachedNotes();
                    } else {
                        return getAndCacheLocalNotes();
                    }
                });
    }

    private Observable<Note> getAndCacheLocalNotes() {
        if (cachedNotes == null) {
            cachedNotes = new LinkedHashMap<>();
        }
        cachedNotes.clear();
        if (dirtyNotes == null) {
            dirtyNotes = new HashSet<>();
        }
        dirtyNotes.clear();
        Observable<Note> markSyncResultsAsAppliedObservable =
                localDataSource.markNotesSyncResultsAsApplied()
                        .toObservable()
                        .ignoreElements()
                        .toObservable();
        return Observable.concat(
                markSyncResultsAsAppliedObservable, localDataSource.getNotes((String[]) null))
                .doOnComplete(() -> noteCacheIsDirty = false)
                .doOnNext(note -> {
                    String noteId = note.getId();
                    Log.d(TAG, "getAndCacheLocalNotes() -> doOnNext() [" + noteId + "]");
                    cachedNotes.put(noteId, note);
                });
    }

    private Observable<Note> refreshDirtyAndGetCachedNotes() {
        if (cachedNotes == null) {
            cachedNotes = new LinkedHashMap<>();
        }
        if (dirtyNotes == null) {
            dirtyNotes = new HashSet<>();
        }
        HashSet<String> dirty = new HashSet<>(dirtyNotes);
        Observable<Note> refreshDirtyNotesObservable = Observable.fromIterable(dirty)
                .flatMap(id -> {
                    if (SYNC_COMPLETE_TOKEN.equals(id)) {
                        dirtyNotes.remove(id);
                        return localDataSource.getNotesSyncResultsIds();
                    } else {
                        return Observable.just(Pair.create(id, null));
                    }
                })
                .filter(pair -> {
                    if (pair.second == LocalContract.SyncResultEntry.Result.DELETED) {
                        removeCachedNote(pair.first);
                        return false;
                    }
                    return true;
                })
                .map(pair -> pair.first)
                .toList()
                .toObservable()
                .flatMap(noteIds -> {
                    int size = noteIds.size();
                    if (size > 0) {
                        String[] ids = noteIds.toArray(new String[size]);
                        return localDataSource.getNotes(ids);
                    } else {
                        return Observable.empty();
                    }
                })
                .doOnNext(note -> {
                    String noteId = note.getId();
                    Log.d(TAG, "refreshDirtyAndGetCachedNotes() -> doOnNext() [" + noteId + "]");
                    Note prevNote = cachedNotes.put(noteId, note);
                    if (prevNote == null) {
                        refreshNotes();
                        throw new IllegalStateException("New item has been found, reload is required");
                    }
                    dirtyNotes.remove(noteId);
                })
                .ignoreElements()
                .toObservable();
        return Observable.concat(
                refreshDirtyNotesObservable, Observable.fromIterable(cachedNotes.values()));
    }

    @Override
    public Single<Note> getNote(@NonNull String noteId) {
        checkNotNull(noteId);
        if (!isKeyValidUuid(noteId)) {
            throw new InvalidParameterException("getNote() called with invalid UUID");
        }
        final Note cachedNote = getCachedNote(noteId);
        if (cachedNote != null) {
            return Single.just(cachedNote);
        }
        return getAndCacheLocalNote(noteId);
    }

    private Single<Note> getAndCacheLocalNote(String noteId) {
        if (cachedNotes == null) {
            cachedNotes = new LinkedHashMap<>();
        }
        if (dirtyNotes == null) {
            dirtyNotes = new HashSet<>();
        }
        return localDataSource.getNote(noteId)
                .doOnSuccess(note -> {
                    Log.d(TAG, "getAndCacheLocalNote() -> doOnSuccess() [" + noteId + "]");
                    boolean replaced = cachedNotes.put(noteId, note) != null;
                    if (replaced) { // if was dirty
                        dirtyNotes.remove(noteId);
                    } else {
                        // NOTE: cache must be reloaded to place new Note to the right place
                        refreshNotes();
                    }
                });
    }

    @Nullable
    private Note getCachedNote(@NonNull String noteId) {
        checkNotNull(noteId);
        if (cachedNotes != null
                && (dirtyNotes == null || !dirtyNotes.contains(noteId))) {
            Log.d(TAG, "getCachedNote() [" + noteId + "]");
            return cachedNotes.get(noteId);
        }
        return null;
    }

    @Override
    public Observable<ItemState> saveNote(@NonNull final Note note, final boolean syncable) {
        checkNotNull(note);
        String noteId = note.getId();
        Observable<ItemState> localSavingObservable = localDataSource.saveNote(note)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case DEFERRED:
                            // Note
                            if (cachedNotes == null) {
                                cachedNotes = new LinkedHashMap<>();
                            }
                            // NOTE: new Note has no rowId to bind to RecyclerView
                            if (note.getRowId() < 0) {
                                Map<String, Note> map = new LinkedHashMap<>(cachedNotes);
                                cachedNotes.clear();
                                // NOTE: new Note goes to the top of the list
                                cachedNotes.put(noteId, note);
                                cachedNotes.putAll(map);
                            }
                            refreshNote(noteId);
                            // Tags
                            if (cachedTags == null) {
                                cachedTags = new LinkedHashMap<>();
                            }
                            List<Tag> tags = note.getTags();
                            if (tags != null) {
                                for (Tag tag : tags) {
                                    cachedTags.put(tag.getName(), tag);
                                }
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Local saveNote()");
                    }
                    notifyNotesSaveCallbacks(noteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    ItemState itemState = ItemState.ERROR_LOCAL;
                    notifyNotesSaveCallbacks(noteId, itemState);
                    return itemState;
                })
                .toObservable();
        Observable<ItemState> cloudSavingObservable;
        if (syncable) {
            cloudSavingObservable = getCloudSaveNoteSingle(noteId)
                    .subscribeOn(schedulerProvider.io())
                    .toObservable();
        } else {
            cloudSavingObservable = Observable.empty();
        }
        return Observable.concat(localSavingObservable, cloudSavingObservable);
    }

    private Single<ItemState> getCloudSaveNoteSingle(@NonNull final String noteId) {
        checkNotNull(noteId);
        return cloudDataSource.saveNote(noteId)
                .doOnSuccess(itemState -> {
                    // NOTE: let the presenter to update related state
                    switch (itemState) {
                        case SAVED:
                            refreshNote(noteId);
                            break;
                        case CONFLICTED:
                            refreshNote(noteId);
                            break;
                        case ERROR_LOCAL:
                        case ERROR_CLOUD:
                            refreshNote(noteId);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state from Cloud saveNote()");
                    }
                    notifyNotesSaveCallbacks(noteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: all errors include the local to retrieve and update the item state
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    ItemState itemState = ItemState.ERROR_CLOUD;
                    notifyNotesSaveCallbacks(noteId, itemState);
                    return itemState;
                });
    }

    @Override
    public Single<ItemState> syncSavedNote(@NonNull final String noteId) {
        checkNotNull(noteId);
        return getCloudSaveNoteSingle(noteId);
    }

    @VisibleForTesting
    public void deleteAllNotes() {
        localDataSource.deleteAllNotes(); // blocking
        //cloudDataSource.deleteAllNotes();
        if (cachedNotes == null) {
            cachedNotes = new LinkedHashMap<>();
        }
        cachedNotes.clear();
        if (dirtyNotes == null) {
            dirtyNotes = new HashSet<>();
        }
        dirtyNotes.clear();
    }

    @Override
    public Observable<ItemState> deleteNote(
            @NonNull String noteId, boolean syncable, long started) {
        checkNotNull(noteId);
        Observable<ItemState> refreshLinkObservable = localDataSource.getNote(noteId)
                .toObservable()
                .doOnNext(note -> {
                    String linkId = note.getLinkId();
                    if (linkId != null) {
                        refreshLink(linkId);
                    }
                })
                .ignoreElements()
                .toObservable();
        Observable<ItemState> localDeletionObservable = localDataSource.deleteNote(noteId)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case DEFERRED:
                        case DELETED:
                            if (cachedNotes == null) {
                                cachedNotes = new LinkedHashMap<>();
                            }
                            removeCachedNote(noteId);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Local deleteNote()");
                    }
                    notifyNotesDeleteCallbacks(noteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    ItemState itemState = ItemState.ERROR_LOCAL;
                    notifyNotesDeleteCallbacks(noteId, itemState);
                    return itemState;
                })
                .toObservable();
        Observable<ItemState> cloudDeletionObservable;
        if (syncable) {
            cloudDeletionObservable = getCloudDeleteNoteSingle(noteId, started)
                    .subscribeOn(schedulerProvider.io())
                    .toObservable();
        } else {
            cloudDeletionObservable = Observable.empty();
        }
        return Observable.concat(refreshLinkObservable, localDeletionObservable,
                cloudDeletionObservable);
    }

    private Single<ItemState> getCloudDeleteNoteSingle(
            @NonNull final String noteId, long started) {
        checkNotNull(noteId);
        return cloudDataSource.deleteNote(noteId, started)
                .doOnSuccess(itemState -> {
                    // NOTE: flatMap() -> return .map() -> null: actually returns null to onSuccess
                    switch (itemState) {
                        case DELETED: // visibility was not changed
                            break;
                        case CONFLICTED:
                            refreshNotes(); // need to be shown again
                            break;
                        case ERROR_LOCAL:
                        case ERROR_CLOUD:
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state from Cloud deleteNote()");
                    }
                    notifyNotesDeleteCallbacks(noteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: all errors include the local to retrieve and update the item state
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    ItemState itemState = ItemState.ERROR_CLOUD;
                    notifyNotesDeleteCallbacks(noteId, itemState);
                    return itemState;
                });
    }

    @Override
    public Single<Boolean> isConflictedNotes() {
        return localDataSource.isConflictedNotes();
    }

    @Override
    public Single<Boolean> isUnsyncedNotes() {
        return localDataSource.isUnsyncedNotes();
    }

    @Override
    public void refreshNotes() {
        Log.d(TAG, "refreshNotes()");
        noteCacheIsDirty = true;
    }

    @Override
    public void refreshNote(@NonNull String noteId) {
        checkNotNull(noteId);
        Log.d(TAG, "refreshNote() [" + noteId + "]");
        if (dirtyNotes == null) {
            dirtyNotes = new HashSet<>();
        }
        dirtyNotes.add(noteId);
    }

    @Override
    public void checkNotesSyncLog() {
        Log.d(TAG, "checkNotesSyncLog()");
        if (dirtyNotes == null) {
            dirtyNotes = new HashSet<>();
        }
        dirtyNotes.add(SYNC_COMPLETE_TOKEN);
    }

    @Override
    public void removeCachedNote(@NonNull String noteId) {
        checkNotNull(noteId);
        Log.d(TAG, "removeCachedNote() [" + noteId + "]");
        if (cachedNotes == null) {
            cachedNotes = new LinkedHashMap<>();
        }
        cachedNotes.remove(noteId);
    }

    @Override
    public int getNoteCacheSize() {
        return cachedNotes == null ? 0 : cachedNotes.size();
    }

    // Tags: tag is part of the object and should be bound with the object

    @Override
    public Observable<Tag> getTags() {
        if (cachedTags != null) {
            return Observable.fromIterable(cachedTags.values());
        }
        return getAndCacheLocalTags();
    }

    private Observable<Tag> getAndCacheLocalTags() {
        if (cachedTags == null) {
            cachedTags = new LinkedHashMap<>();
        }
        return localDataSource.getTags()
                .doOnNext(tag -> cachedTags.put(tag.getName(), tag));
    }

    @Override
    public Single<Tag> getTag(@NonNull String tagName) {
        checkNotNull(tagName);
        final Tag cachedTag = getCachedTag(tagName);
        if (cachedTag != null) {
            return Single.just(cachedTag);
        }
        if (cachedTags == null) {
            cachedTags = new LinkedHashMap<>();
        }
        return localDataSource.getTag(tagName)
                .doOnSuccess(tag -> cachedTags.put(tagName, tag));
    }

    @Nullable
    private Tag getCachedTag(@NonNull String name) {
        checkNotNull(name);
        if (cachedTags == null || cachedTags.isEmpty()) {
            return null;
        } else {
            return cachedTags.get(name);
        }
    }

    @Override
    public void saveTag(@NonNull Tag tag) {
        checkNotNull(tag);
        localDataSource.saveTag(tag);
        if (cachedTags == null) {
            cachedTags = new LinkedHashMap<>();
        }
        cachedTags.put(tag.getName(), tag);
    }

    @VisibleForTesting
    public void deleteAllTags() {
        localDataSource.deleteAllTags();
        if (cachedTags == null) {
            cachedTags = new LinkedHashMap<>();
        }
        cachedTags.clear();
    }

    // Common

    @Override
    public Single<Boolean> isConflicted() {
        return isConflictedLinks()
                .flatMap(conflicted -> conflicted ? Single.just(true) : isConflictedFavorites())
                .flatMap(conflicted -> conflicted ? Single.just(true) : isConflictedNotes());
    }

    @Override
    public Single<Boolean> isUnsynced() {
        return isUnsyncedLinks()
                .flatMap(unsynced -> unsynced ? Single.just(true) : isUnsyncedFavorites())
                .flatMap(unsynced -> unsynced ? Single.just(true) : isUnsyncedNotes());
    }

    @Override
    public Single<Integer> getSyncStatus() {
        return isConflicted()
                .flatMap(conflicted -> {
                    if (conflicted) {
                        return Single.just(SyncAdapter.SYNC_STATUS_CONFLICT);
                    } else {
                        return isUnsynced().flatMap(unsynced -> {
                            if (unsynced) {
                                return Single.just(SyncAdapter.SYNC_STATUS_UNSYNCED);
                            }
                            return Single.just(SyncAdapter.SYNC_STATUS_SYNCED);
                        });
                    }
                });
    }

    @Override
    public Single<Long> resetSyncState() {
        return Observable.mergeDelayError(
                localDataSource.resetLinksSyncState()
                        .toObservable()
                        .doOnComplete(this::refreshLinks),
                localDataSource.resetFavoritesSyncState()
                        .toObservable()
                        .doOnComplete(this::refreshFavorites),
                localDataSource.resetNotesSyncState()
                        .toObservable()
                        .doOnComplete(this::refreshNotes))
                .count();
    }

    public Observable<SyncResult> getFreshSyncResults() {
        return localDataSource.getFreshSyncResults();
    }
}
