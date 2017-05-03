package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bytesforge.linkasanote.data.Item;
import com.bytesforge.linkasanote.data.ItemFactory;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Provider;
import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalLinks<T extends Item> implements LocalItem<T> {

    private static final String TAG = LocalLinks.class.getSimpleName();

    private static final Uri LINK_URI = LocalContract.LinkEntry.buildUri();

    private final Context context;
    private final ContentResolver contentResolver;
    private final LocalTags localTags;
    private final LocalNotes<Note> localNotes;
    private final ItemFactory<T> factory;

    public LocalLinks(
            @NonNull Context context, @NonNull ContentResolver contentResolver,
            @NonNull LocalTags localTags, @NonNull LocalNotes<Note> localNotes,
            @NonNull ItemFactory<T> factory) {
        this.context = checkNotNull(context);
        this.contentResolver = checkNotNull(contentResolver);
        this.localTags = checkNotNull(localTags);
        this.localNotes = checkNotNull(localNotes);
        this.factory = checkNotNull(factory);
    }

    private Single<T> buildLink(final T link) {
        Single<T> singleLink = Single.just(link);
        Uri linkTagUri = LocalContract.LinkEntry.buildTagsDirUriWith(link.getRowId());
        Single<List<Tag>> singleTags = localTags.getTags(linkTagUri).toList();

        // OPTIMIZATION: the requested Note can be replaced with the cached one in repository or vice versa
        Uri linkNoteUri = LocalContract.LinkEntry.buildNotesDirUriWith(link.getId());
        Single<List<Note>> singleNotes = localNotes.get(linkNoteUri).toList();
        return Single.zip(singleLink, singleTags, singleNotes, factory::build);
    }

    // Operations

    @Override
    public Observable<T> getAll() {
        return get(LINK_URI, null, null, null);
    }

    @Override
    public Observable<T> getActive() {
        final String selection = LocalContract.LinkEntry.COLUMN_NAME_DELETED + " = ?" +
                " OR " + LocalContract.LinkEntry.COLUMN_NAME_CONFLICTED + " = ?";
        final String[] selectionArgs = {"0", "1"};
        final String sortOrder = LocalContract.LinkEntry.COLUMN_NAME_CREATED + " DESC";

        return get(LINK_URI, selection, selectionArgs, sortOrder);
    }

    @Override
    public Observable<T> getUnsynced() {
        final String selection = LocalContract.LinkEntry.COLUMN_NAME_SYNCED + " = ?";
        final String[] selectionArgs = {"0"};

        return get(LINK_URI, selection, selectionArgs, null);
    }

    @Override
    public Observable<T> get(final Uri uri) {
        final String sortOrder = LocalContract.TagEntry.COLUMN_NAME_CREATED + " DESC";
        return get(uri, null, null, sortOrder);
    }

    @Override
    public Observable<T> get(
            final Uri uri,
            final String selection, final String[] selectionArgs, final String sortOrder) {
        Observable<T> linksGenerator = Observable.generate(() -> {
            return contentResolver.query(
                    uri, LocalContract.LinkEntry.LINK_COLUMNS,
                    selection, selectionArgs, sortOrder);
        }, (cursor, linkEmitter) -> {
            if (cursor == null) {
                linkEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (cursor.moveToNext()) {
                linkEmitter.onNext(factory.from(cursor));
            } else {
                linkEmitter.onComplete();
            }
            return cursor;
        }, Cursor::close);
        return linksGenerator.flatMap(link -> buildLink(link).toObservable());
    }

    @Override
    public Single<T> get(final String linkId) {
        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    LocalContract.LinkEntry.buildUriWith(linkId),
                    LocalContract.LinkEntry.LINK_COLUMNS, null, null, null)) {
                if (cursor == null) return null;

                if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested link was not found");
                }
                return factory.from(cursor);
            }
        }).flatMap(this::buildLink);
    }

    @Override
    public Single<Long> save(final T link) {
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

    @Override
    public Single<Long> saveDuplicated(final T link) {
        return getNextDuplicated(link.getDuplicatedKey())
                .flatMap(duplicated -> {
                    SyncState state = new SyncState(link.getETag(), duplicated);
                    return Single.just(factory.build(link, state));
                }).flatMap(this::save);
    }

    @Override
    // TODO: return Boolean for the update & delete like this one
    public Single<Integer> update(final String linkId, final SyncState state) {
        return Single.fromCallable(() -> {
            ContentValues values = state.getContentValues();
            Uri uri = LocalContract.LinkEntry.buildUriWith(linkId);
            // TODO: add check if syncState parts are not equal to the requested state
            return contentResolver.update(uri, values, null, null);
        });
    }

    @Override
    public Single<Integer> resetSyncState() {
        return Single.fromCallable(() -> {
            ContentValues values = new ContentValues();
            values.put(LocalContract.LinkEntry.COLUMN_NAME_ETAG, (String) null);
            values.put(LocalContract.LinkEntry.COLUMN_NAME_SYNCED, false);
            final String selection = LocalContract.LinkEntry.COLUMN_NAME_SYNCED + " = ?";
            final String[] selectionArgs = {"1"};
            return contentResolver.update(LINK_URI, values, selection, selectionArgs);
        });
    }

    @Override
    public Single<Integer> delete(final String linkId) {
        Uri uri = LocalContract.LinkEntry.buildUriWith(linkId);
        return LocalDataSource.delete(contentResolver, uri);
    }

    @Override
    public Single<Integer> delete() {
        return LocalDataSource.delete(contentResolver, LINK_URI);
    }

    @Override
    public Single<SyncState> getSyncState(final String linkId) {
        Uri uri = LocalContract.LinkEntry.buildUriWith(linkId);
        return LocalDataSource.getSyncState(contentResolver, uri);
    }

    @Override
    public Observable<SyncState> getSyncStates() {
        return LocalDataSource.getSyncStates(contentResolver, LINK_URI, null, null, null);
    }

    @Override
    public Observable<String> getIds() {
        return LocalDataSource.getIds(contentResolver, LINK_URI);
    }

    @Override
    public Single<Boolean> isConflicted() {
        return LocalDataSource.isConflicted(contentResolver, LINK_URI);
    }

    private Single<Integer> getNextDuplicated(final String linkName) {
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

    @Override
    public Single<T> getMain(final String duplidatedKey) {
        final String selection = LocalContract.LinkEntry.COLUMN_NAME_LINK + " = ?" +
                " AND " + LocalContract.LinkEntry.COLUMN_NAME_DUPLICATED + " = ?";
        final String[] selectionArgs = {duplidatedKey, "0"};

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
                return factory.from(cursor);
            }
        }).flatMap(this::buildLink);
    }

    @Override
    public Single<Boolean> autoResolveConflict(final String linkId) {
        return get(linkId)
                .map(link -> {
                    if (!link.isDuplicated()) {
                        Log.e(TAG, "autoResolveConflict() was called on the Link which is not duplicated [" + linkId + "]");
                        return !link.isConflicted();
                    }
                    try {
                        getMain(link.getDuplicatedKey()).blockingGet();
                        return false;
                    } catch (NoSuchElementException e) {
                        SyncState state = new SyncState(SyncState.State.SYNCED);
                        int numRows = update(linkId, state).blockingGet();
                        return numRows == 1;
                    }
                });
    }
}
