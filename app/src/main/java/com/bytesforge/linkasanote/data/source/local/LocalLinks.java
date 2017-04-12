package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Provider;
import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalLinks {

    private final Context context;
    private final ContentResolver contentResolver;
    private final LocalTags localTags;

    public LocalLinks(
            @NonNull Context context, @NonNull ContentResolver contentResolver,
            @NonNull LocalTags localTags) {
        this.context = checkNotNull(context);
        this.contentResolver = checkNotNull(contentResolver);
        this.localTags = checkNotNull(localTags);
    }

    public Observable<Link> getLinks() {
        return getLinks(null, null, null);
    }

    public Observable<Link> getLinks(
            final String selection, final String[] selectionArgs, final String sortOrder) {
        return Observable.generate(() -> {
            return contentResolver.query(
                    LocalContract.LinkEntry.buildUri(),
                    LocalContract.LinkEntry.LINK_COLUMNS,
                    selection, selectionArgs, sortOrder);
        }, (cursor, linkEmitter) -> {
            if (cursor == null) {
                linkEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (!cursor.moveToNext()) {
                linkEmitter.onComplete();
                return cursor;
            }
            String rowId = LocalContract.rowIdFrom(cursor);
            Uri linkTagsUri = LocalContract.LinkEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = localTags.getTags(linkTagsUri).toList().blockingGet();
            linkEmitter.onNext(Link.from(cursor, tags));

            return cursor;
        }, Cursor::close);
    }

    public Single<Link> getLink(final String linkId) {
        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    LocalContract.LinkEntry.buildUriWith(linkId),
                    LocalContract.LinkEntry.LINK_COLUMNS, null, null, null)) {
                if (cursor == null) {
                    return null; // NOTE: NullPointerException
                } else if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested link was not found");
                }
                String rowId = LocalContract.rowIdFrom(cursor);
                Uri linkTagsUri = LocalContract.LinkEntry.buildTagsDirUriWith(rowId);
                List<Tag> tags = localTags.getTags(linkTagsUri).toList().blockingGet();
                return Link.from(cursor, tags);
            }
        });
    }

    public Single<Long> saveLink(final Link link) {
        return Single.fromCallable(() -> {
            ContentValues values = link.getContentValues();
            Uri linkUri = contentResolver.insert(
                    LocalContract.LinkEntry.buildUri(), values);
            if (linkUri == null) {
                throw new NullPointerException("Provider must return URI or throw exception");
            }
            String rowId = LocalContract.LinkEntry.getIdFrom(linkUri);
            Uri uri = LocalContract.LinkEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = link.getTags();
            if (tags != null) {
                for (Tag tag : tags) {
                    localTags.saveTag(tag, uri).blockingGet();
                }
            }
            return Long.parseLong(rowId);
        });
    }

    public Single<Integer> updateLink(final String linkId, final SyncState state) {
        return Single.fromCallable(() -> {
            ContentValues values = state.getContentValues();
            Uri uri = LocalContract.LinkEntry.buildUriWith(linkId);
            return contentResolver.update(uri, values, null, null);
        });
    }

    public Single<Integer> resetLinksSyncState() {
        return Single.fromCallable(() -> {
            SyncState state = new SyncState(SyncState.State.UNSYNCED);
            ContentValues values = state.getContentValues();
            Uri uri = LocalContract.LinkEntry.buildUri();
            return contentResolver.update(uri, values, null, null);
        });
    }

    public Single<Integer> deleteLink(final String linkId) {
        Uri uri = LocalContract.LinkEntry.buildUriWith(linkId);
        return LocalDataSource.delete(contentResolver, uri);
    }

    public Single<Integer> deleteLinks() {
        Uri uri = LocalContract.LinkEntry.buildUri();
        return LocalDataSource.delete(contentResolver, uri);
    }

    public Single<SyncState> getLinkSyncState(final String linkId) {
        Uri uri = LocalContract.LinkEntry.buildUriWith(linkId);
        return LocalDataSource.getSyncState(contentResolver, uri);
    }

    public Observable<SyncState> getLinkSyncStates() {
        Uri uri = LocalContract.LinkEntry.buildUri();
        return LocalDataSource.getSyncStates(contentResolver, uri, null, null, null);
    }

    public Observable<String> getLinkIds() {
        Uri uri = LocalContract.LinkEntry.buildUri();
        return LocalDataSource.getIds(contentResolver, uri);
    }

    public Single<Boolean> isConflictedLinks() {
        Uri uri = LocalContract.LinkEntry.buildUri();
        return LocalDataSource.isConflicted(contentResolver, uri);
    }

    public Single<Integer> getNextDuplicated(final String linkName) {
        final String[] columns = new String[]{
                "MAX(" + LocalContract.LinkEntry.COLUMN_NAME_DUPLICATED + ") + 1"};
        final String selection = LocalContract.LinkEntry.COLUMN_NAME_NAME + " = ?";
        final String[] selectionArgs = {linkName};

        return Single.fromCallable(() -> {
            try (Cursor cursor = Provider.rawQuery(context,
                    LocalContract.LinkEntry.TABLE_NAME,
                    columns, selection, selectionArgs, null)) {
                if (cursor.moveToLast()) {
                    return cursor.getInt(0);
                }
                return 0;
            }
        });
    }

    public Single<Link> getMainLink(final String linkName) {
        final String selection = LocalContract.LinkEntry.COLUMN_NAME_NAME + " = ?" +
                " AND " + LocalContract.LinkEntry.COLUMN_NAME_DUPLICATED + " = ?";
        final String[] selectionArgs = {linkName, "0"};

        return Single.fromCallable(() -> {
            try (Cursor cursor = Provider.rawQuery(context,
                    LocalContract.LinkEntry.TABLE_NAME,
                    LocalContract.LinkEntry.LINK_COLUMNS,
                    selection, selectionArgs, null)) {
                if (cursor == null) {
                    return null; // NOTE: NullPointerException
                } else if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested link was not found");
                }
                String rowId = LocalContract.rowIdFrom(cursor);
                Uri linkTagsUri = LocalContract.LinkEntry.buildTagsDirUriWith(rowId);
                List<Tag> tags = localTags.getTags(linkTagsUri).toList().blockingGet();
                return Link.from(cursor, tags);
            }
        });
    }
}
