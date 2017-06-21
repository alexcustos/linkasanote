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
import android.util.Pair;

import com.bytesforge.linkasanote.data.SyncResult;
import com.bytesforge.linkasanote.settings.Settings;

import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalSyncResults {

    private static final String TAG = LocalSyncResults.class.getSimpleName();

    private static Uri SYNC_RESULT_URI;

    private final ContentResolver contentResolver;

    public LocalSyncResults(@NonNull ContentResolver contentResolver) {
        this.contentResolver = checkNotNull(contentResolver);
        SYNC_RESULT_URI = LocalContract.SyncResultEntry.buildUri();
    }

    public Observable<SyncResult> getFresh() {
        final String selection =
                "datetime(" + LocalContract.SyncResultEntry.COLUMN_NAME_STARTED + " / 1000, 'unixepoch')" +
                        " > datetime('now', ?) AND " +
                        LocalContract.SyncResultEntry.COLUMN_NAME_RESULT + " <> ?";
        final String[] selectionArgs = {
                "-" + Settings.GLOBAL_SYNC_LOG_KEEPING_PERIOD_DAYS + " day",
                LocalContract.SyncResultEntry.Result.RELATED.name()};
        final String sortOrder = LocalContract.SyncResultEntry.COLUMN_NAME_CREATED + " ASC";
        return get(SYNC_RESULT_URI, selection, selectionArgs, sortOrder);
    }

    private Observable<SyncResult> get(
            final Uri uri,
            final String selection, final String[] selectionArgs, final String sortOrder) {
        return Observable.generate(() -> {
            return contentResolver.query(uri, LocalContract.SyncResultEntry.SYNC_RESULT_COLUMNS,
                    selection, selectionArgs, sortOrder);
        }, (cursor, linkEmitter) -> {
            if (cursor == null) {
                linkEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (cursor.moveToNext()) {
                linkEmitter.onNext(SyncResult.from(cursor));
            } else {
                linkEmitter.onComplete();
            }
            return cursor;
        }, Cursor::close);
    }

    public Observable<Pair<String, LocalContract.SyncResultEntry.Result>> getIds(
            @NonNull final String entry) {
        checkNotNull(entry);
        final AtomicLong threshold = new AtomicLong();
        return getThreshold()
                .toObservable()
                .flatMap(maxRowId -> {
                    threshold.set(maxRowId);
                    Uri uri = LocalContract.SyncResultEntry.buildUriWith(entry);
                    String selection = LocalContract.SyncResultEntry.COLUMN_NAME_APPLIED + " = ?" +
                            " AND " + LocalContract.SyncResultEntry._ID + " <= ?";
                    String[] selectionArgs = {"0", maxRowId.toString()};
                    return getEntryIds(uri, selection, selectionArgs, null);
                })
                .doOnComplete(() -> {
                    markAsApplied(entry, threshold.get()).blockingGet();
                });
    }

    private Single<Long> getThreshold() {
        final String[] columns = new String[]{"MAX(" + LocalContract.SyncResultEntry._ID + ")"};
        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver
                    .query(SYNC_RESULT_URI, columns, null, null, null)) {
                if (cursor == null) return null;

                if (cursor.moveToLast()) {
                    return cursor.getLong(0);
                }
                return Long.MAX_VALUE;
            }
        });
    }

    private Observable<Pair<String, LocalContract.SyncResultEntry.Result>> getEntryIds(
            final Uri uri,
            final String selection, final String[] selectionArgs, final String sortOrder) {
        final String[] columns = new String[]{
                "DISTINCT " + LocalContract.SyncResultEntry.COLUMN_NAME_ENTRY_ID,
                LocalContract.SyncResultEntry.COLUMN_NAME_RESULT};
        return Observable.generate(() -> {
            return contentResolver.query(uri, columns, selection, selectionArgs, sortOrder);
        }, (cursor, linkEmitter) -> {
            if (cursor == null) {
                linkEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (cursor.moveToNext()) {
                linkEmitter.onNext(Pair.create(cursor.getString(0),
                        LocalContract.SyncResultEntry.Result.valueOf(cursor.getString(1))));
            } else {
                linkEmitter.onComplete();
            }
            return cursor;
        }, Cursor::close);
    }

    public Single<Integer> markAsApplied(
            @NonNull final String entry, @NonNull final Long threshold) {
        checkNotNull(entry);
        checkNotNull(threshold);
        return Single.fromCallable(() -> {
            Uri uri = LocalContract.SyncResultEntry.buildUriWith(entry);
            String selection = LocalContract.SyncResultEntry.COLUMN_NAME_APPLIED + " = ?" +
                    " AND " + LocalContract.SyncResultEntry._ID + " <= ?";
            String[] selectionArgs = {"0",
                    threshold <= 0 ? Long.toString(Long.MAX_VALUE) : threshold.toString()};
            ContentValues values = new ContentValues();
            values.put(LocalContract.SyncResultEntry.COLUMN_NAME_APPLIED, true);
            return contentResolver.update(uri, values, selection, selectionArgs);
        });
    }

    public Single<Integer> cleanup() {
        return Single.fromCallable(() -> {
            String selection =
                    "datetime(" + LocalContract.SyncResultEntry.COLUMN_NAME_STARTED + " / 1000, 'unixepoch')" +
                            " <= datetime('now', ?)";
            String[] selectionArgs = {"-" + Settings.GLOBAL_SYNC_LOG_KEEPING_PERIOD_DAYS + " day"};
            return contentResolver.delete(SYNC_RESULT_URI, selection, selectionArgs);
        });
    }

    public Single<Boolean> log(@NonNull final SyncResult syncResult) {
        checkNotNull(syncResult);
        return Single.fromCallable(() -> {
            ContentValues values = syncResult.getContentValues();
            Uri syncResultUri = contentResolver.insert(SYNC_RESULT_URI, values);
            if (syncResultUri == null) {
                throw new NullPointerException("Provider must return URI or throw exception");
            }
            return LocalContract.SyncResultEntry.getIdFrom(syncResultUri);
        }).map(rowId -> rowId > 0);
    }
}
