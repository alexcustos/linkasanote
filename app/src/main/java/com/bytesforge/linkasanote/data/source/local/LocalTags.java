/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

    private static final Uri TAG_URI = LocalContract.TagEntry.buildUri();

    private final ContentResolver contentResolver;

    public LocalTags(@NonNull ContentResolver contentResolver) {
        this.contentResolver = checkNotNull(contentResolver);
    }

    // Operations

    public Observable<Tag> getTags() {
        return getTags(TAG_URI, null, null, null);
    }

    public Observable<Tag> getTags(
            final String selection, final String[] selectionArgs, final String sortOrder) {
        return getTags(TAG_URI, selection, selectionArgs, sortOrder);
    }

    public Observable<Tag> getTags(final Uri uri) {
        // NOTE: by default tags are sorted by the time when it was bound to the item
        return getTags(uri, null, null, null);
    }

    public Observable<Tag> getTags(
            final Uri uri,
            final String selection, final String[] selectionArgs, final String sortOrder) {
        return Observable.generate(() -> {
            return contentResolver.query(
                    uri, LocalContract.TagEntry.TAG_COLUMNS,
                    selection, selectionArgs, sortOrder);
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
            try (Cursor cursor = contentResolver.query(
                    LocalContract.TagEntry.buildUriWith(tagName),
                    LocalContract.TagEntry.TAG_COLUMNS, null, null, null)) {
                if (cursor == null) return null;

                if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested tag was not found");
                }
                return Tag.from(cursor);
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
        Uri uri = LocalContract.TagEntry.buildUriWith(tagName);
        return Single.fromCallable(() -> contentResolver.delete(uri, null, null));
    }

    public Single<Integer> deleteTags() {
        return Single.fromCallable(() -> contentResolver.delete(TAG_URI, null, null));
    }
}
