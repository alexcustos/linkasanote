package com.bytesforge.linkasanote.data.source.cloud;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;

import java.util.List;

import javax.inject.Singleton;

import rx.Observable;

@Singleton
public class CloudDataSource implements DataSource {

    public CloudDataSource() {
    }

    @Override
    public Observable<List<Link>> getLinks() {
        return null;
    }

    @Override
    public Observable<Link> getLink(@NonNull String linkId) {
        return null;
    }

    @Override
    public void saveLink(@NonNull Link link) {
    }

    @Override
    public void deleteAllLinks() {
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
    public void deleteAllNotes() {
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
    public void deleteAllFavorites() {
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

    @Override
    public void deleteAllTags() {
    }
}
