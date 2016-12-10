package com.bytesforge.linkasanote.data.source;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class Repository implements DataSource {

    private final DataSource localDataSource;
    private final DataSource cloudDataSource;

    @VisibleForTesting
    @Nullable
    Map<String, Link> cachedLinks;

    @VisibleForTesting
    boolean cacheIsDirty = false;

    @Inject
    public Repository(
            @Local DataSource localDataSource,
            @Cloud DataSource cloudDataSource) {
        this.localDataSource = localDataSource;
        this.cloudDataSource = cloudDataSource;
    }

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
                .flatMap((links) -> {
                    return Observable.from(links)
                            .doOnNext((link) -> {
                                cachedLinks.put(link.getId(), link);
                            })
                            .toList();
                });
    }

    @Override
    public Observable<Link> getLink(@NonNull String linkId) {
        checkNotNull(linkId);

        final Link cachedLink = getLinkWithId(linkId);
        if (cachedLink != null) {
            return Observable.just(cachedLink);
        }

        if (cachedLinks == null) {
            cachedLinks = new LinkedHashMap<>();
        }

        return localDataSource.getLink(linkId)
                .doOnNext((link) -> {
                    cachedLinks.put(linkId, link);
                })
                .first();
    }

    @Override
    public void saveLink(@NonNull Link link) {
        checkNotNull(link);

        localDataSource.saveLink(link);
        cloudDataSource.saveLink(link);
    }

    @Nullable
    private Link getLinkWithId(@NonNull String id) {
        checkNotNull(id);

        if (cachedLinks == null || cachedLinks.isEmpty()) {
            return null;
        } else {
            return cachedLinks.get(id);
        }
    }

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
    public Observable<List<Favorite>> getFavorites() {
        return null;
    }

    @Override
    public Observable<Favorite> getFavorite(@NonNull String favoriteId) {
        return null;
    }

    @Override
    public void saveFavorite(@NonNull Favorite favorite) {
    }

    @Override
    public Observable<List<Tag>> getTags() {
        return null;
    }

    @Override
    public Observable<Tag> getTag(@NonNull String tagId) {
        return null;
    }

    @Override
    public void saveTag(@NonNull Tag tag) {
    }
}
