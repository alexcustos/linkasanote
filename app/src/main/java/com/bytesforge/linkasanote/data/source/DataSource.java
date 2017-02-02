package com.bytesforge.linkasanote.data.source;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;

import java.util.List;

import rx.Observable;

public interface DataSource {

    Observable<List<Link>> getLinks();
    Observable<Link> getLink(@NonNull String linkId);
    void saveLink(@NonNull Link link);
    void deleteAllLinks();

    Observable<List<Note>> getNotes();
    Observable<Note> getNote(@NonNull String noteId);
    void saveNote(@NonNull Note note);
    void deleteAllNotes();

    Observable<List<Favorite>> getFavorites();
    Observable<Favorite> getFavorite(@NonNull String favoriteId);
    void saveFavorite(@NonNull Favorite favorite);
    void deleteAllFavorites();

    Observable<List<Tag>> getTags();
    Observable<Tag> getTag(@NonNull String tagId);
    void saveTag(@NonNull Tag tag);
    void deleteAllTags();
}
