package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Provider;
import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalLinks {

    private static final Uri LINK_URI = LocalContract.LinkEntry.buildUri();

    private final Context context;
    private final ContentResolver contentResolver;
    private final LocalTags localTags;
    private final LocalNotes localNotes;

    public LocalLinks(
            @NonNull Context context, @NonNull ContentResolver contentResolver,
            @NonNull LocalTags localTags, @NonNull LocalNotes localNotes) {
        this.context = checkNotNull(context);
        this.contentResolver = checkNotNull(contentResolver);
        this.localTags = checkNotNull(localTags);
        this.localNotes = checkNotNull(localNotes);
    }

    private Single<Link> buildLink(final Link link) {
        Single<Link> singleLink = Single.just(link);
        Uri linkTagUri = LocalContract.LinkEntry.buildTagsDirUriWith(link.getRowId());
        Single<List<Tag>> singleTags = localTags.getTags(linkTagUri).toList();

        // OPTIMIZATION: the requested Note can be replaced with the cached one in repository or vice versa
        Uri linkNoteUri = LocalContract.LinkEntry.buildNotesDirUriWith(link.getId());
        Single<List<Note>> singleNotes = localNotes.getNotes(linkNoteUri).toList();
        return Single.zip(singleLink, singleTags, singleNotes, Link::new);
    }

    // Operations

    public Observable<Link> getLinks() {
        return getLinks(LINK_URI, null, null, null);
    }

    public Observable<Link> getLinks(
            final String selection, final String[] selectionArgs, final String sortOrder) {
        return getLinks(LINK_URI, selection, selectionArgs, sortOrder);
    }

    public Observable<Link> getLinks(final Uri uri) {
        final String sortOrder = LocalContract.TagEntry.COLUMN_NAME_CREATED + " DESC";
        return getLinks(uri, null, null, sortOrder);
    }

    public Observable<Link> getLinks(
            final Uri uri,
            final String selection, final String[] selectionArgs, final String sortOrder) {
        Observable<Link> linksGenerator = Observable.generate(() -> {
            return contentResolver.query(
                    uri, LocalContract.LinkEntry.LINK_COLUMNS,
                    selection, selectionArgs, sortOrder);
        }, (cursor, linkEmitter) -> {
            if (cursor == null) {
                linkEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (cursor.moveToNext()) {
                linkEmitter.onNext(Link.from(cursor));
            } else {
                linkEmitter.onComplete();
            }
            return cursor;
        }, Cursor::close);
        return linksGenerator.flatMap(link -> buildLink(link).toObservable());
    }

    public Single<Link> getLink(final String linkId) {
        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    LocalContract.LinkEntry.buildUriWith(linkId),
                    LocalContract.LinkEntry.LINK_COLUMNS, null, null, null)) {
                if (cursor == null) return null;

                if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested link was not found");
                }
                return Link.from(cursor);
            }
        }).flatMap(this::buildLink);
    }

    public Single<Long> saveLink(final Link link) {
        return Single.fromCallable(() -> {
            ContentValues values = link.getContentValues();
            Uri linkUri = contentResolver.insert(LINK_URI, values);
            if (linkUri == null) {
                throw new NullPointerException("Provider must return URI or throw exception");
            }
            String rowId = LocalContract.LinkEntry.getIdFrom(linkUri);
            Uri linkTagUri = LocalContract.LinkEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = link.getTags();
            if (tags != null) {
                Observable.zip(
                        Observable.fromIterable(tags), Observable.just(linkTagUri).repeat(),
                        (tag, uri) -> localTags.saveTag(tag, uri).blockingGet())
                        .toList().blockingGet();
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
            return contentResolver.update(LINK_URI, values, null, null);
        });
    }

    public Single<Integer> deleteLink(final String linkId) {
        Uri uri = LocalContract.LinkEntry.buildUriWith(linkId);
        return LocalDataSource.delete(contentResolver, uri);
    }

    public Single<Integer> deleteLinks() {
        return LocalDataSource.delete(contentResolver, LINK_URI);
    }

    public Single<SyncState> getLinkSyncState(final String linkId) {
        Uri uri = LocalContract.LinkEntry.buildUriWith(linkId);
        return LocalDataSource.getSyncState(contentResolver, uri);
    }

    public Observable<SyncState> getLinkSyncStates() {
        return LocalDataSource.getSyncStates(contentResolver, LINK_URI, null, null, null);
    }

    public Observable<String> getLinkIds() {
        return LocalDataSource.getIds(contentResolver, LINK_URI);
    }

    public Single<Boolean> isConflictedLinks() {
        return LocalDataSource.isConflicted(contentResolver, LINK_URI);
    }

    public Single<Integer> getNextDuplicatedLink(final String linkName) {
        final String[] columns = new String[]{
                "MAX(" + LocalContract.LinkEntry.COLUMN_NAME_DUPLICATED + ") + 1"};
        final String selection = LocalContract.LinkEntry.COLUMN_NAME_LINK + " = ?";
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
        final String selection = LocalContract.LinkEntry.COLUMN_NAME_LINK + " = ?" +
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
                return Link.from(cursor);
            }
        }).flatMap(this::buildLink);
    }
}
