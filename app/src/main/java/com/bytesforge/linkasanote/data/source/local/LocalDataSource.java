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

import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class LocalDataSource implements DataSource {

    private static final String TAG = LocalDataSource.class.getSimpleName();

    private ContentResolver contentResolver;

    public LocalDataSource(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    // Links

    @Override
    public Single<List<Link>> getLinks() {
        final String selection = LocalContract.LinkEntry.COLUMN_NAME_DELETED + " = ?";
        final String[] selectionArgs = {"0"};

        Observable<Link> linkObservable = Observable.generate(() -> {
            return contentResolver.query(
                    LocalContract.LinkEntry.buildLinksUri(),
                    LocalContract.LinkEntry.LINK_COLUMNS,
                    selection, selectionArgs, null);
        }, (cursor, listEmitter) -> {
            if (cursor == null) {
                listEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (cursor.moveToNext()) {
                listEmitter.onNext(Link.from(cursor));
            } else {
                listEmitter.onComplete();
            }
            return cursor;
        }, Cursor::close);
        // TODO: find why cast cannot be applied without variable
        return linkObservable.toList();
    }

    @Override
    public Single<Link> getLink(@NonNull String linkId) {
        return null;
    }

    @Override
    public void saveLink(@NonNull Link link) {
        ContentValues values = checkNotNull(link).getContentValues();
        contentResolver.insert(LocalContract.LinkEntry.buildLinksUri(), values);
    }

    @Override
    public void deleteAllLinks() {
        contentResolver.delete(LocalContract.LinkEntry.buildLinksUri(), null, null);
    }

    // Notes

    @Override
    public Single<List<Note>> getNotes() {
        return null;
    }

    @Override
    public Single<Note> getNote(@NonNull String noteId) {
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
    public Single<List<Favorite>> getFavorites() {
        // TODO: maybe 'OR conflicted' records are required
        final String selection = LocalContract.FavoriteEntry.COLUMN_NAME_DELETED + " = ?";
        final String[] selectionArgs = {"0"};

        Observable<Favorite> favoriteObservable = Observable.generate(() -> {
            return contentResolver.query(
                    LocalContract.FavoriteEntry.buildFavoritesUri(),
                    LocalContract.FavoriteEntry.FAVORITE_COLUMNS,
                    selection, selectionArgs, null);
        }, (cursor, favoriteEmitter) -> {
            if (cursor == null) {
                favoriteEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (!cursor.moveToNext()) {
                favoriteEmitter.onComplete();
                return cursor;
            }
            int rowIdIndex = cursor.getColumnIndexOrThrow(LocalContract.FavoriteEntry._ID);
            String rowId = cursor.getString(rowIdIndex);
            Uri favoriteTagsUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = getTagsFrom(favoriteTagsUri).blockingGet();
            favoriteEmitter.onNext(Favorite.from(cursor, tags));

            return cursor;
        }, Cursor::close);

        return favoriteObservable.toList(); // NOTE: when no Items it returns empty List
    }

    @Override
    public Single<Favorite> getFavorite(final @NonNull String favoriteId) {
        checkNotNull(favoriteId);

        return Observable.fromCallable(() -> {
            Cursor cursor = contentResolver.query(
                    LocalContract.FavoriteEntry.buildFavoritesUriWith(favoriteId),
                    LocalContract.FavoriteEntry.FAVORITE_COLUMNS, null, null, null);
            if (cursor == null) return null; // NOTE: NullPointerException
            else if (!cursor.moveToLast()) {
                cursor.close();
                throw new NoSuchElementException("The requested favorite was not found");
            }
            int rowIndex = cursor.getColumnIndexOrThrow(LocalContract.FavoriteEntry._ID);
            String rowId = cursor.getString(rowIndex);
            Uri favoriteTagsUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = getTagsFrom(favoriteTagsUri).blockingGet();
            try {
                return Favorite.from(cursor, tags);
            } finally {
                cursor.close();
            }
        }).firstOrError();
    }

    @Override
    public void saveFavorite(final @NonNull Favorite favorite) {
        checkNotNull(favorite);

        favorite.setSyncState(SyncState.State.UNSYNCED);
        ContentValues values = favorite.getContentValues();
        Uri favoriteUri = contentResolver.insert(
                LocalContract.FavoriteEntry.buildFavoritesUri(), values);
        if (favoriteUri == null) {
            throw new NullPointerException("Provider must return URI or throw exception");
        }

        // OPTIMIZATION: just add "/tag" to favoriteUri
        String rowId = LocalContract.FavoriteEntry.getFavoriteId(favoriteUri);
        Uri uri = LocalContract.FavoriteEntry.buildTagsDirUriWith(rowId);
        List<Tag> tags = favorite.getTags();
        if (tags != null) {
            for (Tag tag : tags) insertTag(tag, uri);
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
    public Single<List<Tag>> getTags() {
        return getTagsFrom(LocalContract.TagEntry.buildTagsUri());
    }

    private Single<List<Tag>> getTagsFrom(@NonNull Uri uri) {
        checkNotNull(uri);

        Observable<Tag> tagObservable = Observable.generate(() -> {
            return contentResolver.query(
                    uri, LocalContract.TagEntry.TAG_COLUMNS,
                    null, null, null);
        }, (cursor, tagEmitter) -> {
            if (cursor == null) {
                tagEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (cursor.moveToNext()) {
                tagEmitter.onNext(Tag.from(cursor));
            } else {
                tagEmitter.onComplete();
            }
            return cursor;
        }, Cursor::close);

        return tagObservable.toList();
    }

    @Override
    public Single<Tag> getTag(@NonNull String tagName) {
        checkNotNull(tagName);

        return Observable.fromCallable(() -> {
            Cursor cursor = contentResolver.query(
                    LocalContract.TagEntry.buildTagsUriWith(tagName),
                    LocalContract.TagEntry.TAG_COLUMNS,
                    null, null, null);
            if (cursor == null) return null;
            else if (!cursor.moveToLast()) {
                cursor.close();
                throw new NoSuchElementException("The requested tag was not found");
            }
            try {
                return Tag.from(cursor);
            } finally {
                cursor.close();
            }
        }).firstOrError();
    }

    @Override
    public void saveTag(@NonNull Tag tag) {
        checkNotNull(tag);

        insertTag(tag, LocalContract.TagEntry.buildTagsUri());
    }

    private void insertTag(@NonNull Tag tag, @NonNull Uri uri) {
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
