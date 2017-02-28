package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.sync.SyncState;
import com.squareup.sqlbrite.BriteContentResolver;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import rx.Observable;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class LocalDataSource implements DataSource {

    private static final String TAG = LocalDataSource.class.getSimpleName();

    private ContentResolver contentResolver;
    private BriteContentResolver briteResolver;

    public LocalDataSource(ContentResolver contentResolver, BriteContentResolver briteResolver) {
        this.contentResolver = contentResolver;
        this.briteResolver = briteResolver;
    }

    // Links

    @Override
    public Observable<List<Link>> getLinks() {
        return briteResolver.createQuery(
                LocalContract.LinkEntry.buildLinksUri(),
                LocalContract.LinkEntry.LINK_COLUMNS,
                null, null, null, false)
                .mapToList(Link::from);
    }

    @Override
    public Observable<Link> getLink(@NonNull String linkId) {
        return null;
    }

    @Override
    public void saveLink(@NonNull Link link) {
        checkNotNull(link);

        ContentValues values = link.getContentValues();
        contentResolver.insert(LocalContract.LinkEntry.buildLinksUri(), values);
    }

    @Override
    public void deleteAllLinks() {
        contentResolver.delete(LocalContract.LinkEntry.buildLinksUri(), null, null);
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
        contentResolver.delete(LocalContract.NoteEntry.buildNotesUri(), null, null);
    }

    // Favorites

    @Override
    public Observable<List<Favorite>> getFavorites() {
        final String selection = LocalContract.FavoriteEntry.COLUMN_NAME_DELETED + " = ?";
        final String[] selectionArgs = {"0"};
        return briteResolver.createQuery(
                LocalContract.FavoriteEntry.buildFavoritesUri(),
                LocalContract.FavoriteEntry.FAVORITE_COLUMNS,
                selection, selectionArgs, null, false)
                .map(query -> {
                    Cursor cursor = query.run();
                    if (cursor == null) return null;

                    List<Favorite> favorites = new ArrayList<>();
                    int rowIdIndex = cursor.getColumnIndexOrThrow(LocalContract.FavoriteEntry._ID);
                    while (cursor.moveToNext()) {
                        String rowId = cursor.getString(rowIdIndex);
                        Uri favoriteTagsUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
                        List<Tag> tags = getTagsFrom(favoriteTagsUri)
                                .toBlocking()
                                .single();
                        favorites.add(Favorite.from(cursor, tags));
                    }
                    return favorites;
                })
                .first();
    }

    @Override
    public Observable<Favorite> getFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);

        return Observable.fromCallable(() -> {
            Cursor cursor = contentResolver.query(
                    LocalContract.FavoriteEntry.buildFavoritesUriWith(favoriteId),
                    LocalContract.FavoriteEntry.FAVORITE_COLUMNS, null, null, null);
            if (cursor == null) {
                return null;
            } else if (cursor.getCount() <= 0) {
                cursor.close();
                return null;
            }
            cursor.moveToFirst();
            int rowIndex = cursor.getColumnIndexOrThrow(LocalContract.FavoriteEntry._ID);
            String rowId = cursor.getString(rowIndex);
            Uri favoriteTagsUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = getTagsFrom(favoriteTagsUri)
                    .toBlocking().single();
            Favorite favorite = Favorite.from(cursor, tags);
            cursor.close();

            return favorite;
        });
    }

    @Override
    public void saveFavorite(@NonNull Favorite favorite) {
        checkNotNull(favorite);

        ContentValues values = favorite.getContentValues();
        Uri favoriteUri = contentResolver.insert(
                LocalContract.FavoriteEntry.buildFavoritesUri(), values);

        // OPTIMIZATION: just add "/tag" to favoriteUri
        String rowId = LocalContract.FavoriteEntry.getFavoriteId(favoriteUri);
        Uri uri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
        List<Tag> tags = favorite.getTags();
        if (tags != null) {
            for (Tag tag : tags) saveTagTo(tag, uri);
        }
    }

    @Override
    public void deleteAllFavorites() {
        contentResolver.delete(LocalContract.FavoriteEntry.buildFavoritesUri(), null, null);
    }

    @Override
    public void deleteFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);

        Uri uri = LocalContract.FavoriteEntry.buildFavoritesUriWith(favoriteId);
        ContentValues values = SyncState.getSyncStateValues(SyncState.State.DELETED);
        int numRows = contentResolver.update(uri, values, null, null);
        if (numRows != 1) {
            Log.w(TAG, "deleteFavorite(): updated unexpected number of rows "
                    + "[" + numRows + ", id=" + favoriteId + "]");
        }
    }

    // Tags

    @Override
    public Observable<List<Tag>> getTags() {
        return getTagsFrom(LocalContract.TagEntry.buildTagsUri());
    }

    private Observable<List<Tag>> getTagsFrom(@NonNull Uri uri) {
        return briteResolver.createQuery(
                uri, LocalContract.TagEntry.TAG_COLUMNS,
                null, null, null, false)
                .mapToList(Tag::from)
                .first(); // Otherwise observable not always be completed
    }

    @Override
    public Observable<Tag> getTag(@NonNull String tagName) {
        checkNotNull(tagName);

        return briteResolver.createQuery(
                LocalContract.TagEntry.buildTagsUriWith(tagName),
                LocalContract.TagEntry.TAG_COLUMNS,
                null, null, null, false)
                .mapToOneOrDefault(Tag::from, null);
    }

    @Override
    public void saveTag(@NonNull Tag tag) {
        checkNotNull(tag);

        saveTagTo(tag, LocalContract.TagEntry.buildTagsUri());
    }

    private void saveTagTo(@NonNull Tag tag, @NonNull Uri uri) {
        checkNotNull(tag);
        checkNotNull(uri);

        ContentValues values = tag.getContentValues();
        contentResolver.insert(uri, values);
    }

    @Override
    public void deleteAllTags() {
        contentResolver.delete(LocalContract.TagEntry.buildTagsUri(), null, null);
    }
}
