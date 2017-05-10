package com.bytesforge.linkasanote.data.source;

import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;
import com.bytesforge.linkasanote.sync.SyncAdapter;
import com.bytesforge.linkasanote.utils.CommonUtils;

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

    private final LocalDataSource localDataSource;
    private final CloudDataSource cloudDataSource;


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
    public Repository(LocalDataSource localDataSource, CloudDataSource cloudDataSource) {
        this.localDataSource = localDataSource;
        this.cloudDataSource = cloudDataSource;
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
    public Observable<Link> getLinks() {
        if (!linkCacheIsDirty && cachedLinks != null) {
            return Observable.fromIterable(cachedLinks.values());
        }
        return getAndCacheLocalLinks();
    }

    private Observable<Link> getAndCacheLocalLinks() {
        if (cachedLinks == null) {
            cachedLinks = new LinkedHashMap<>();
        }
        cachedLinks.clear();
        return localDataSource.getLinks()
                .doOnComplete(() -> linkCacheIsDirty = false)
                .doOnNext(link -> {
                    cachedLinks.put(link.getId(), link);
                });
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
        return localDataSource.getLink(linkId)
                .doOnSuccess(link -> {
                    // NOTE: single item request do not check cacheIsDirty flag
                    cachedLinks.put(linkId, link);
                    // NOTE: because the order of the Links is messed up
                    linkCacheIsDirty = true;
                });
    }

    @Nullable
    private Link getCachedLink(@NonNull String linkId) {
        checkNotNull(linkId);
        if (cachedLinks != null && !cachedLinks.isEmpty()) {
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
                            // NOTE: new Link has no rowId to bind to RecyclerView and it position is unknown
                            linkCacheIsDirty = true;
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
                    notifyLinksSaveCallbacks(linkId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: return null - NullPointerException; throw new - CompositeException
                    //       throw throwable - unhandled exception error
                    ItemState itemState;
                    if (throwable instanceof SQLiteConstraintException) {
                        itemState = ItemState.DUPLICATED;
                    } else {
                        CommonUtils.logStackTrace(TAG, throwable);
                        itemState = ItemState.ERROR_LOCAL;
                    }
                    notifyLinksSaveCallbacks(linkId, itemState);
                    return itemState;
                })
                .doOnSuccess(itemState -> {
                    if (itemState == ItemState.DUPLICATED) {
                        throw new SQLiteConstraintException("Links UNIQUE constraint failed [" + linkId + "]");
                    }
                })
                .toObservable();
        Observable<ItemState> cloudSavingObservable;
        if (syncable) {
            cloudSavingObservable = getCloudSaveLinkSingle(linkId).toObservable();
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
                        case SAVED: // visibility is not changed
                            break;
                        case CONFLICTED:
                            // OPTIMIZATION: invalidate only one item
                            linkCacheIsDirty = true;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Cloud saveLink()");
                    }
                    notifyLinksSaveCallbacks(linkId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: all errors include the local to retrieve and update the item state
                    CommonUtils.logStackTrace(TAG, throwable);
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
    }

    @Override
    public Observable<ItemState> deleteLink(
            @NonNull final String linkId, final boolean syncable, final boolean deleteNotes) {
        checkNotNull(linkId);
        // NOTE: it is unnecessary to maintain integrity from this point
        Observable<ItemState> deleteNotesObservable;
        if (deleteNotes) {
            deleteNotesObservable = localDataSource.getNotes(linkId)
                    .flatMap(note -> deleteNote(note.getId(), syncable))
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
                        CommonUtils.logStackTrace(TAG, throwable);
                        ItemState itemState = ItemState.ERROR_EXTRA;
                        notifyLinksDeleteCallbacks(linkId, itemState);
                        return itemState;
                    });
        } else {
            deleteNotesObservable = Observable.empty();
        }
        Observable<ItemState> localDeletionObservable = localDataSource.deleteLink(linkId)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case DEFERRED:
                        case DELETED:
                            if (cachedLinks == null) {
                                cachedLinks = new LinkedHashMap<>();
                            }
                            cachedLinks.remove(linkId);
                            // NOTE: the Note state can be implicitly changed to unbound
                            // OPTIMIZATION: retrieve the Link's Notes and invalidate it (if !deleteNotes)
                            noteCacheIsDirty = true;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Local deleteLink()");
                    }
                    notifyLinksDeleteCallbacks(linkId, itemState);
                })
                .onErrorReturn(throwable -> {
                    CommonUtils.logStackTrace(TAG, throwable);
                    ItemState itemState = ItemState.ERROR_LOCAL;
                    notifyLinksDeleteCallbacks(linkId, itemState);
                    return itemState;
                })
                .toObservable();
        Observable<ItemState> cloudDeletionObservable;
        if (syncable) {
            cloudDeletionObservable = getCloudDeleteLinkSingle(linkId).toObservable();
        } else {
            cloudDeletionObservable = Observable.empty();
        }
        return Observable.mergeDelayError(deleteNotesObservable, Observable.concat(
                localDeletionObservable, cloudDeletionObservable));
    }

    private Single<ItemState> getCloudDeleteLinkSingle(@NonNull final String linkId) {
        checkNotNull(linkId);
        return cloudDataSource.deleteLink(linkId)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case DELETED: // visibility was not changed
                            break;
                        case CONFLICTED:
                            linkCacheIsDirty = true; // need to be shown again
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Cloud deleteLink()");
                    }
                    notifyLinksDeleteCallbacks(linkId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: all errors include the local to retrieve and update the item state
                    CommonUtils.logStackTrace(TAG, throwable);
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
                    if (success) linkCacheIsDirty = true; // OPTIMIZATION: reload the only one item
                });
    }

    public void refreshLinks() {
        linkCacheIsDirty = true;
    }

    @Override
    public void refreshLink(@NonNull String linkId) {
        checkNotNull(linkId);
        if (dirtyLinks == null) {
            dirtyLinks = new HashSet<>();
        }
        dirtyLinks.add(linkId);
    }

    public void deleteCachedLink(@NonNull String linkId) {
        checkNotNull(linkId);
        if (cachedLinks == null) {
            cachedLinks = new LinkedHashMap<>();
        }
        cachedLinks.remove(linkId);
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
    public Observable<Favorite> getFavorites() {
        if (!favoriteCacheIsDirty && cachedFavorites != null) {
            return Observable.fromIterable(cachedFavorites.values());
        }
        return getAndCacheLocalFavorites();
    }

    private Observable<Favorite> getAndCacheLocalFavorites() {
        if (cachedFavorites == null) {
            cachedFavorites = new LinkedHashMap<>();
        }
        cachedFavorites.clear();
        return localDataSource.getFavorites()
                .doOnComplete(() -> favoriteCacheIsDirty = false)
                .doOnNext(favorite -> {
                    cachedFavorites.put(favorite.getId(), favorite);
                });
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
        return localDataSource.getFavorite(favoriteId)
                .doOnSuccess(favorite -> {
                    cachedFavorites.put(favoriteId, favorite);
                    // NOTE: because order of Favorites is messed up
                    favoriteCacheIsDirty = true;
                });
    }

    @Nullable
    private Favorite getCachedFavorite(@NonNull String id) {
        checkNotNull(id);
        if (cachedFavorites != null && !cachedFavorites.isEmpty()) {
            return cachedFavorites.get(id);
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
                            // NOTE: new Favorite has no rowId to bind to RecyclerView and it position is unknown
                            favoriteCacheIsDirty = true;
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
                    notifyFavoritesSaveCallbacks(favoriteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    ItemState itemState;
                    if (throwable instanceof SQLiteConstraintException) {
                        itemState = ItemState.DUPLICATED;
                    } else {
                        CommonUtils.logStackTrace(TAG, throwable);
                        itemState = ItemState.ERROR_LOCAL;
                    }
                    notifyFavoritesSaveCallbacks(favoriteId, itemState);
                    return itemState;
                })
                .doOnSuccess(itemState -> {
                    if (itemState == ItemState.DUPLICATED) {
                        throw new SQLiteConstraintException("Favorites UNIQUE constraint failed [" + favoriteId + "]");
                    }
                })
                .toObservable();
        Observable<ItemState> cloudSavingObservable;
        if (syncable) {
            cloudSavingObservable = getCloudSaveFavoriteSingle(favoriteId).toObservable();
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
                        case SAVED: // visibility is not changed
                            break;
                        case CONFLICTED:
                            // OPTIMIZATION: invalidate only one item
                            favoriteCacheIsDirty = true;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Cloud saveFavorite()");
                    }
                    notifyFavoritesSaveCallbacks(favoriteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: all errors include the local to retrieve and update the item state
                    CommonUtils.logStackTrace(TAG, throwable);
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
    }

    @Override
    public Observable<ItemState> deleteFavorite(@NonNull String favoriteId, boolean syncable) {
        checkNotNull(favoriteId);
        Observable<ItemState> localDeletionObservable = localDataSource.deleteFavorite(favoriteId)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case DEFERRED:
                        case DELETED:
                            if (cachedFavorites == null) {
                                cachedFavorites = new LinkedHashMap<>();
                            }
                            cachedFavorites.remove(favoriteId);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Local deleteFavorite()");
                    }
                    notifyFavoritesDeleteCallbacks(favoriteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    CommonUtils.logStackTrace(TAG, throwable);
                    ItemState itemState = ItemState.ERROR_LOCAL;
                    notifyFavoritesDeleteCallbacks(favoriteId, itemState);
                    return itemState;
                })
                .toObservable();
        Observable<ItemState> cloudDeletionObservable;
        if (syncable) {
            cloudDeletionObservable = getCloudDeleteFavoriteSingle(favoriteId).toObservable();
        } else {
            cloudDeletionObservable = Observable.empty();
        }
        return Observable.concat(localDeletionObservable, cloudDeletionObservable);
    }

    private Single<ItemState> getCloudDeleteFavoriteSingle(@NonNull final String favoriteId) {
        checkNotNull(favoriteId);
        return cloudDataSource.deleteFavorite(favoriteId)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case DELETED: // visibility was not changed
                            break;
                        case CONFLICTED:
                            favoriteCacheIsDirty = true; // need to be shown again
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Cloud deleteFavorite()");
                    }
                    notifyFavoritesDeleteCallbacks(favoriteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: all errors include the local to retrieve and update the item state
                    CommonUtils.logStackTrace(TAG, throwable);
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
                    if (success) favoriteCacheIsDirty = true; // OPTIMIZATION: reload the only one item
                });
    }

    // TODO: implement cache invalidation for one specific item
    public void refreshFavorites() {
        favoriteCacheIsDirty = true;
    }

    @Override
    public void refreshFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        if (dirtyFavorites == null) {
            dirtyFavorites = new HashSet<>();
        }
        dirtyFavorites.add(favoriteId);
    }

    // TODO: replace with refreshFavorite
    public void deleteCachedFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        if (cachedFavorites == null) {
            cachedFavorites = new LinkedHashMap<>();
        }
        cachedFavorites.remove(favoriteId);
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
    public Observable<Note> getNotes() {
        if (!noteCacheIsDirty && cachedNotes != null) {
            return Observable.fromIterable(cachedNotes.values());
        }
        return getAndCacheLocalNotes();
    }

    private Observable<Note> getAndCacheLocalNotes() {
        if (cachedNotes == null) {
            cachedNotes = new LinkedHashMap<>();
        }
        cachedNotes.clear();
        return localDataSource.getNotes()
                .doOnComplete(() -> noteCacheIsDirty = false)
                .doOnNext(note -> {
                    cachedNotes.put(note.getId(), note);
                });
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
        return localDataSource.getNote(noteId)
                .doOnSuccess(note -> {
                    cachedNotes.put(noteId, note);
                    // NOTE: because order of Notes is messed up
                    noteCacheIsDirty = true;
                });
    }

    @Nullable
    private Note getCachedNote(@NonNull String id) {
        checkNotNull(id);
        if (cachedNotes != null && !cachedNotes.isEmpty()) {
            return cachedNotes.get(id);
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
                            // NOTE: new Note has no rowId to bind to RecyclerView and it position is unknown
                            noteCacheIsDirty = true;
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
                    CommonUtils.logStackTrace(TAG, throwable);
                    ItemState itemState = ItemState.ERROR_LOCAL;
                    notifyNotesSaveCallbacks(noteId, itemState);
                    return itemState;
                })
                .toObservable();
        Observable<ItemState> cloudSavingObservable;
        if (syncable) {
            cloudSavingObservable = getCloudSaveNoteSingle(noteId).toObservable();
        } else {
            cloudSavingObservable = Observable.empty();
        }
        return Observable.concat(localSavingObservable, cloudSavingObservable);
    }

    private Single<ItemState> getCloudSaveNoteSingle(@NonNull final String noteId) {
        checkNotNull(noteId);
        return cloudDataSource.saveNote(noteId)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case SAVED: // visibility is not changed
                            break;
                        case CONFLICTED:
                            // OPTIMIZATION: invalidate only one item
                            noteCacheIsDirty = true;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Cloud saveNote()");
                    }
                    notifyNotesSaveCallbacks(noteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: all errors include the local to retrieve and update the item state
                    CommonUtils.logStackTrace(TAG, throwable);
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
    }

    public Observable<ItemState> deleteNote(@NonNull String noteId, boolean syncable) {
        checkNotNull(noteId);
        Observable<ItemState> localDeletionObservable = localDataSource.deleteNote(noteId)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case DEFERRED:
                        case DELETED:
                            if (cachedNotes == null) {
                                cachedNotes = new LinkedHashMap<>();
                            }
                            cachedNotes.remove(noteId);
                            // OPTIMIZATION: retrieve the Link the Note was bound and invalidate it
                            linkCacheIsDirty = true;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Local deleteNote()");
                    }
                    notifyNotesDeleteCallbacks(noteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    CommonUtils.logStackTrace(TAG, throwable);
                    ItemState itemState = ItemState.ERROR_LOCAL;
                    notifyNotesDeleteCallbacks(noteId, itemState);
                    return itemState;
                })
                .toObservable();
        Observable<ItemState> cloudDeletionObservable;
        if (syncable) {
            cloudDeletionObservable = getCloudDeleteNoteSingle(noteId).toObservable();
        } else {
            cloudDeletionObservable = Observable.empty();
        }
        return Observable.concat(localDeletionObservable, cloudDeletionObservable);
    }

    private Single<ItemState> getCloudDeleteNoteSingle(@NonNull final String noteId) {
        checkNotNull(noteId);
        return cloudDataSource.deleteNote(noteId)
                .doOnSuccess(itemState -> {
                    switch (itemState) {
                        case DELETED: // visibility was not changed
                            break;
                        case CONFLICTED:
                            noteCacheIsDirty = true; // need to be shown again
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state came from Cloud deleteNote()");
                    }
                    notifyNotesDeleteCallbacks(noteId, itemState);
                })
                .onErrorReturn(throwable -> {
                    // NOTE: all errors include the local to retrieve and update the item state
                    CommonUtils.logStackTrace(TAG, throwable);
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

    public void refreshNotes() {
        noteCacheIsDirty = true;
    }

    @Override
    public void refreshNote(@NonNull String noteId) {
        checkNotNull(noteId);
        if (dirtyNotes == null) {
            dirtyNotes = new HashSet<>();
        }
        dirtyNotes.add(noteId);
    }

    public void deleteCachedNote(@NonNull String noteId) {
        checkNotNull(noteId);
        if (cachedNotes == null) {
            cachedNotes = new LinkedHashMap<>();
        }
        cachedNotes.remove(noteId);
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
}
