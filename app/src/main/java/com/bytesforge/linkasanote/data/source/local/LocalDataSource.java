package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.squareup.sqlbrite.BriteContentResolver;

import java.util.List;

import javax.inject.Singleton;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class LocalDataSource implements DataSource {

    private ContentResolver contentResolver;
    private BriteContentResolver briteResolver;

    @NonNull
    private Func1<Cursor, Link> linkMapperFunction;

    public LocalDataSource(ContentResolver contentResolver, BriteContentResolver briteResolver) {
        this.contentResolver = contentResolver;
        this.briteResolver = briteResolver;

        /*linkMapperFunction = new Func1<Cursor, Link>() {
            @Override
            public Link call(Cursor cursor) {
                return Link.from(cursor);
            }
        };*/
        linkMapperFunction = Link::from;
    }

    @Override
    public Observable<List<Link>> getLinks() {
        return briteResolver.createQuery(
                PersistenceContract.LinkEntry.buildLinksUri(),
                PersistenceContract.LinkEntry.LINK_COLUMNS,
                null, null, null, false)
            .mapToList(linkMapperFunction);
    }

    @Override
    public Observable<Link> getLink(@NonNull String linkId) {
        return null;
    }

    @Override
    public void saveLink(@NonNull Link link) {
        checkNotNull(link);

        ContentValues values = link.getContentValues();
        contentResolver.insert(PersistenceContract.LinkEntry.buildLinksUri(), values);
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
