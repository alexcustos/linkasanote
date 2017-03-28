package com.bytesforge.linkasanote.data.source;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface DataSource {

    Single<List<Link>> getLinks();
    Single<Link> getLink(@NonNull String linkId);
    void saveLink(@NonNull Link link);
    void deleteAllLinks();

    Single<List<Note>> getNotes();
    Single<Note> getNote(@NonNull String noteId);
    void saveNote(@NonNull Note note);
    void deleteAllNotes();

    Observable<Favorite> getFavorites();
    Single<Favorite> getFavorite(@NonNull String favoriteId);
    void saveFavorite(@NonNull Favorite favorite);
    void deleteAllFavorites();
    void deleteFavorite(@NonNull String favoriteId);
    Single<Boolean> isConflictedFavorites();

    Observable<Tag> getTags();
    Single<Tag> getTag(@NonNull String tagId);
    void saveTag(@NonNull Tag tag);
    void deleteAllTags();
}
