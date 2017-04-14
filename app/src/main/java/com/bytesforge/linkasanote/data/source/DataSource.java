package com.bytesforge.linkasanote.data.source;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface DataSource {

    Observable<Link> getLinks();
    Single<Link> getLink(@NonNull String linkId);
    void saveLink(@NonNull Link link);
    void deleteAllLinks();
    void deleteLink(@NonNull String linkId);
    Single<Boolean> isConflictedLinks();

    Observable<Favorite> getFavorites();
    Single<Favorite> getFavorite(@NonNull String favoriteId);
    void saveFavorite(@NonNull Favorite favorite);
    void deleteAllFavorites();
    void deleteFavorite(@NonNull String favoriteId);
    Single<Boolean> isConflictedFavorites();

    Observable<Note> getNotes();
    Single<Note> getNote(@NonNull String noteId);
    void saveNote(@NonNull Note note);
    void deleteAllNotes();
    void deleteNote(@NonNull String noteId);
    Single<Boolean> isConflictedNotes();

    Observable<Tag> getTags();
    Single<Tag> getTag(@NonNull String tagId);
    void saveTag(@NonNull Tag tag);
    void deleteAllTags();
}
