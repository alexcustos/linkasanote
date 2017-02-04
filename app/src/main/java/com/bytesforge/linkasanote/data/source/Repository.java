package com.bytesforge.linkasanote.data.source;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;

import static com.bytesforge.linkasanote.utils.UuidUtils.isKeyValidUuid;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class Repository implements DataSource {

    private final DataSource localDataSource;
    private final DataSource cloudDataSource;

    @VisibleForTesting
    @Nullable
    Map<String, Link> cachedLinks;

    @VisibleForTesting
    @Nullable
    Map<String, Link> cachedNotes;

    @VisibleForTesting
    @Nullable
    Map<String, Favorite> cachedFavorites;

    @VisibleForTesting
    @Nullable
    Map<String, Tag> cachedTags;

    // TODO: implement the cache invalidation
    @VisibleForTesting
    public boolean cacheIsDirty = false;

    @Inject
    public Repository(
            @Local DataSource localDataSource,
            @Cloud DataSource cloudDataSource) {
        this.localDataSource = localDataSource;
        this.cloudDataSource = cloudDataSource;
    }

    // Links

    @Override
    public Observable<List<Link>> getLinks() {
        if (cachedLinks != null && !cacheIsDirty) {
            return Observable.from(cachedLinks.values()).toList();
        } else if (cachedLinks == null) {
            cachedLinks = new LinkedHashMap<>();
        }
        return getAndCacheLocalLinks();
    }

    private Observable<List<Link>> getAndCacheLocalLinks() {
        return localDataSource.getLinks()
                .flatMap(links -> Observable.from(links)
                        .doOnNext(link -> cachedLinks.put(link.getId(), link))
                        .toList());
    }

    @Override
    public Observable<Link> getLink(@NonNull String linkId) {
        checkNotNull(linkId);

        final Link cachedLink = getCachedLink(linkId);
        if (cachedLink != null) {
            return Observable.just(cachedLink);
        }

        if (cachedLinks == null) {
            cachedLinks = new LinkedHashMap<>();
        }
        return localDataSource.getLink(linkId)
                .doOnNext(link -> cachedLinks.put(linkId, link))
                .first();
    }

    @Nullable
    private Link getCachedLink(@NonNull String id) {
        checkNotNull(id);

        if (cachedLinks == null || cachedLinks.isEmpty()) {
            return null;
        } else {
            return cachedLinks.get(id);
        }
    }

    @Override
    public void saveLink(@NonNull Link link) {
        checkNotNull(link);

        localDataSource.saveLink(link);
        cloudDataSource.saveLink(link);

        if (cachedLinks == null) {
            cachedLinks = new LinkedHashMap<>();
        }
        cachedLinks.put(link.getId(), link);
    }

    @Override
    public void deleteAllLinks() {
        localDataSource.deleteAllLinks();
        cloudDataSource.deleteAllLinks();

        if (cachedLinks == null) {
            cachedLinks = new LinkedHashMap<>();
        }
        cachedLinks.clear();
    }

    // Notes

    @Override
    public Observable<List<Note>> getNotes() {
        return null;
    }

    @Override
    public Observable<Note> getNote(@NonNull String noteId) {
        return null;
    }

    @Override
    public void saveNote(@NonNull Note note) {
    }

    @Override
    public void deleteAllNotes() {
        localDataSource.deleteAllNotes();
        cloudDataSource.deleteAllNotes();

        if (cachedNotes == null) {
            cachedNotes = new LinkedHashMap<>();
        }
        cachedNotes.clear();
    }

    // Favorites

    // TODO: implement cloudFavorites
    @Override
    public Observable<List<Favorite>> getFavorites() {
        if (cachedFavorites != null && !cacheIsDirty) {
            return Observable.from(cachedFavorites.values()).toList();
        }

        Observable<List<Favorite>> localFavorites = getAndCacheLocalFavorites();

        Observable<List<Favorite>> cloudFavorites =
                Observable.just(Collections.<Favorite>emptyList());

        return Observable.concat(localFavorites, cloudFavorites)
                .filter(favorites -> !favorites.isEmpty())
                .first();
    }

    private Observable<List<Favorite>> getAndCacheLocalFavorites() {
        if (cachedFavorites == null) {
            cachedFavorites = new LinkedHashMap<>();
        }
        if (cachedTags == null) {
            cachedTags = new LinkedHashMap<>();
        }
        return localDataSource.getFavorites()
                .flatMap(favorites -> Observable.from(favorites)
                        .doOnNext(favorite -> {
                            // NOTE: cache invalidation required when all items are requested
                            cachedFavorites.put(favorite.getId(), favorite);
                            List<Tag> tags = favorite.getTags();
                            if (tags != null) {
                                for (Tag tag : tags) cachedTags.put(tag.getName(), tag);
                            }
                        })
                        .toList());
    }

    @Override
    public Observable<Favorite> getFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        if (!isKeyValidUuid(favoriteId)) {
            throw new InvalidParameterException(
                    "getFavorite() called with invalid UUID ID [" + favoriteId + "]");
        }

        final Favorite cachedFavorite = getCachedFavorite(favoriteId);
        if (cachedFavorite != null) {
            return Observable.just(cachedFavorite);
        }

        if (cachedFavorites == null) {
            cachedFavorites = new LinkedHashMap<>();
        }
        if (cachedTags == null) {
            cachedTags = new LinkedHashMap<>();
        }
        Observable<Favorite> localFavorite = localDataSource.getFavorite(favoriteId)
                .doOnNext(favorite -> {
                    cachedFavorites.put(favoriteId, favorite);
                    List<Tag> tags = favorite.getTags();
                    if (tags != null) {
                        for (Tag tag : tags) cachedTags.put(tag.getName(), tag);
                    }
                })
                .first();

        Observable<Favorite> cloudFavorite =
                Observable.<Favorite>just(null).concatWith(Observable.<Favorite>never());

        return Observable.concat(localFavorite, cloudFavorite)
                .first()
                .map(favorite -> {
                    if (favorite == null) {
                        throw new NoSuchElementException(
                                "No favorite found with ID [" + favoriteId + "]");
                    }
                    return favorite;
                });
    }

    @Nullable
    private Favorite getCachedFavorite(@NonNull String id) {
        checkNotNull(id);

        if (cachedFavorites == null || cachedFavorites.isEmpty()) {
            return null;
        } else {
            return cachedFavorites.get(id);
        }
    }

    @Override
    public void saveFavorite(@NonNull Favorite favorite) {
        checkNotNull(favorite);

        localDataSource.saveFavorite(favorite);
        cloudDataSource.saveFavorite(favorite);

        // Favorite
        if (cachedFavorites == null) {
            cachedFavorites = new LinkedHashMap<>();
        }
        cachedFavorites.put(favorite.getId(), favorite);

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
    }

    @Override
    public void deleteAllFavorites() {
        localDataSource.deleteAllFavorites();
        cloudDataSource.deleteAllFavorites();

        if (cachedFavorites == null) {
            cachedFavorites = new LinkedHashMap<>();
        }
        cachedFavorites.clear();
    }

    // Tags: tag is part of the object and should be bound with the object

    @Override
    public Observable<List<Tag>> getTags() {
        if (cachedTags != null && !cacheIsDirty) {
            return Observable.from(cachedTags.values()).toList();
        } else if (cachedTags == null) {
            cachedTags = new LinkedHashMap<>();
        }
        return getAndCacheLocalTags();
    }

    private Observable<List<Tag>> getAndCacheLocalTags() {
        return localDataSource.getTags()
                .flatMap(tags -> Observable.from(tags)
                        .doOnNext(tag -> cachedTags.put(tag.getName(), tag))
                        .toList());
    }

    @Override
    public Observable<Tag> getTag(@NonNull String tagName) {
        checkNotNull(tagName);

        final Tag cachedTag = getCachedTag(tagName);
        if (cachedTag != null) {
            return Observable.just(cachedTag);
        }

        if (cachedTags == null) {
            cachedTags = new LinkedHashMap<>();
        }
        return localDataSource.getTag(tagName)
                .doOnNext(tag -> cachedTags.put(tagName, tag))
                .first();
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

    @Override
    public void deleteAllTags() {
        localDataSource.deleteAllTags();
        if (cachedTags == null) {
            cachedTags = new LinkedHashMap<>();
        }
        cachedTags.clear();
    }
}
