package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Tag;

import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalTags {

    private final ContentResolver contentResolver;

    public LocalTags(@NonNull ContentResolver contentResolver) {
        this.contentResolver = checkNotNull(contentResolver);
    }


    public Observable<Tag> getTags(final Uri uri) {
        final String sortOrder = LocalContract.TagEntry.COLUMN_NAME_NAME + " ASC";
        return getTags(uri, null, null, sortOrder);
    }

    public Observable<Tag> getTags(
            final Uri uri,
            final String selection, final String[] selectionArgs, final String sortOrder) {
        return Observable.generate(() -> {
            return contentResolver.query(
                    uri, LocalContract.TagEntry.TAG_COLUMNS, selection, selectionArgs, sortOrder);
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
    }

    public Single<Tag> getTag(String tagName) {
        return Single.fromCallable(() -> {
            Cursor cursor = contentResolver.query(
                    LocalContract.TagEntry.buildTagsUriWith(tagName),
                    LocalContract.TagEntry.TAG_COLUMNS, null, null, null);
            if (cursor == null) return null;

            if (!cursor.moveToLast()) {
                cursor.close();
                throw new NoSuchElementException("The requested tag was not found");
            }
            try {
                return Tag.from(cursor);
            } finally {
                cursor.close();
            }
        });
    }

    public Single<Uri> saveTag(final Tag tag, final Uri uri) {
        return Single.fromCallable(() -> {
            ContentValues values = tag.getContentValues();
            return contentResolver.insert(uri, values);
        });
    }

    public Single<Integer> deleteTag(final String tagName) {
        Uri uri = LocalContract.TagEntry.buildTagsUriWith(tagName);
        return LocalDataSource.delete(contentResolver, uri);
    }

    public Single<Integer> deleteTags() {
        Uri uri = LocalContract.TagEntry.buildTagsUri();
        return LocalDataSource.delete(contentResolver, uri);
    }
}
