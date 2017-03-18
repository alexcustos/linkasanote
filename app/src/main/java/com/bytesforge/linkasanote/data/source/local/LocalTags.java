package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.bytesforge.linkasanote.data.Tag;

import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

public final class LocalTags {

    private LocalTags() {
    }

    public static Observable<Tag> getTags(final ContentResolver contentResolver, final Uri uri) {
        return Observable.generate(() -> {
            return contentResolver.query(
                    uri, LocalContract.TagEntry.TAG_COLUMNS, null, null, null);
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

    public static Single<Tag> getTag(final ContentResolver contentResolver, String tagName) {
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

    public static Single<Uri> saveTag(final ContentResolver contentResolver, final Tag tag, final Uri uri) {
        return Single.fromCallable(() -> {
            ContentValues values = tag.getContentValues();
            return contentResolver.insert(uri, values);
        });
    }

    public static Single<Integer> deleteTag(
            final ContentResolver contentResolver, final String tagName) {
        Uri uri = LocalContract.TagEntry.buildTagsUriWith(tagName);
        return LocalDataSource.delete(contentResolver, uri);
    }

    public static Single<Integer> deleteTags(final ContentResolver contentResolver) {
        Uri uri = LocalContract.TagEntry.buildTagsUri();
        return LocalDataSource.delete(contentResolver, uri);
    }
}
