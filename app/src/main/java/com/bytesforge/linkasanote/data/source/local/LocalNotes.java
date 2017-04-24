package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Item;
import com.bytesforge.linkasanote.data.ItemFactory;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Provider;
import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalNotes<T extends Item> implements LocalItem<T> {

    private static final Uri NOTE_URI = LocalContract.NoteEntry.buildUri();

    private final Context context;
    private final ContentResolver contentResolver;
    private final LocalTags localTags;
    private final ItemFactory<T> factory;

    public LocalNotes(
            @NonNull Context context, @NonNull ContentResolver contentResolver,
            @NonNull LocalTags localTags, @NonNull ItemFactory<T> factory) {
        this.context = checkNotNull(context);
        this.contentResolver = checkNotNull(contentResolver);
        this.localTags = checkNotNull(localTags);
        this.factory = checkNotNull(factory);
    }

    private Single<T> buildNote(final T note) {
        Single<T> singleNote = Single.just(note);
        Uri noteTagUri = LocalContract.NoteEntry.buildTagsDirUriWith(note.getRowId());
        Single<List<Tag>> singleTags = localTags.getTags(noteTagUri).toList();
        return Single.zip(singleNote, singleTags, factory::build);
    }

    // Operations

    @Override
    public Observable<T> getAll() {
        return get(NOTE_URI, null, null, null);
    }

    @Override
    public Observable<T> getActive() {
        final String selection = LocalContract.NoteEntry.COLUMN_NAME_DELETED + " = ?" +
                " OR " + LocalContract.NoteEntry.COLUMN_NAME_CONFLICTED + " = ?";
        final String[] selectionArgs = {"0", "1"};
        final String sortOrder = LocalContract.NoteEntry.COLUMN_NAME_CREATED + " DESC";

        return get(NOTE_URI, selection, selectionArgs, sortOrder);
    }

    @Override
    public Observable<T> getUnsynced() {
        final String selection = LocalContract.NoteEntry.COLUMN_NAME_SYNCED + " = ?";
        final String[] selectionArgs = {"0"};

        return get(NOTE_URI, selection, selectionArgs, null);
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
        Observable<T> notesGenerator = Observable.generate(() -> {
            return contentResolver.query(
                    uri, LocalContract.NoteEntry.NOTE_COLUMNS,
                    selection, selectionArgs, sortOrder);
        }, (cursor, noteEmitter) -> {
            if (cursor == null) {
                noteEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (cursor.moveToNext()) {
                noteEmitter.onNext(factory.from(cursor));
            } else {
                noteEmitter.onComplete();
            }
            return cursor;
        }, Cursor::close);
        return notesGenerator.flatMap(note -> buildNote(note).toObservable());
    }

    @Override
    public Single<T> get(final String noteId) {
        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    LocalContract.NoteEntry.buildUriWith(noteId),
                    LocalContract.NoteEntry.NOTE_COLUMNS, null, null, null)) {
                if (cursor == null) return null;

                if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested note was not found");
                }
                return factory.from(cursor);
            }
        }).flatMap(this::buildNote);
    }

    @Override
    public Single<Long> save(final T note) {
        return Single.fromCallable(() -> {
            ContentValues values = note.getContentValues();
            Uri noteUri = contentResolver.insert(
                    LocalContract.NoteEntry.buildUri(), values);
            if (noteUri == null) {
                throw new NullPointerException("Provider must return URI or throw exception");
            }
            String rowId = LocalContract.NoteEntry.getIdFrom(noteUri);
            Uri noteTagUri = LocalContract.NoteEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = note.getTags();
            if (tags != null) {
                Observable.zip(
                        Observable.fromIterable(tags), Observable.just(noteTagUri).repeat(),
                        (tag, uri) -> localTags.saveTag(tag, uri).blockingGet())
                        .toList().blockingGet();
            }
            return Long.parseLong(rowId);
        });
    }

    @Override
    public Single<Long> saveDuplicated(final T note) {
        return getNextDuplicated(note.getDuplicatedKey())
                .flatMap(duplicated -> {
                    SyncState state = new SyncState(note.getETag(), duplicated);
                    return Single.just(factory.build(note, state));
                }).flatMap(this::save);
    }

    @Override
    public Single<Integer> update(final String noteId, final SyncState state) {
        return Single.fromCallable(() -> {
            ContentValues values = state.getContentValues();
            Uri uri = LocalContract.NoteEntry.buildUriWith(noteId);
            return contentResolver.update(uri, values, null, null);
        });
    }

    @Override
    public Single<Integer> resetSyncState() {
        return Single.fromCallable(() -> {
            ContentValues values = new ContentValues();
            values.put(LocalContract.NoteEntry.COLUMN_NAME_SYNCED, false);
            final String selection = LocalContract.NoteEntry.COLUMN_NAME_SYNCED + " = ?";
            final String[] selectionArgs = {"1"};
            return contentResolver.update(NOTE_URI, values, selection, selectionArgs);
        });
    }

    @Override
    public Single<Integer> delete(final String noteId) {
        Uri uri = LocalContract.NoteEntry.buildUriWith(noteId);
        return LocalDataSource.delete(contentResolver, uri);
    }

    @Override
    public Single<Integer> delete() {
        return LocalDataSource.delete(contentResolver, NOTE_URI);
    }

    @Override
    public Single<SyncState> getSyncState(final String noteId) {
        Uri uri = LocalContract.NoteEntry.buildUriWith(noteId);
        return LocalDataSource.getSyncState(contentResolver, uri);
    }

    @Override
    public Observable<SyncState> getSyncStates() {
        return LocalDataSource.getSyncStates(contentResolver, NOTE_URI, null, null, null);
    }

    @Override
    public Observable<String> getIds() {
        return LocalDataSource.getIds(contentResolver, NOTE_URI);
    }

    @Override
    public Single<Boolean> isConflicted() {
        return LocalDataSource.isConflicted(contentResolver, NOTE_URI);
    }

    private Single<Integer> getNextDuplicated(final String noteName) {
        final String[] columns = new String[]{
                "MAX(" + LocalContract.NoteEntry.COLUMN_NAME_DUPLICATED + ") + 1"};
        final String selection = LocalContract.NoteEntry.COLUMN_NAME_NOTE + " = ?";
        final String[] selectionArgs = {noteName};

        return Single.fromCallable(() -> {
            try (Cursor cursor = Provider.rawQuery(context,
                    LocalContract.NoteEntry.TABLE_NAME,
                    columns, selection, selectionArgs, null)) {
                if (cursor.moveToLast()) {
                    return cursor.getInt(0);
                }
                return 0;
            }
        });
    }

    @Override
    public Single<T> getMain(final String noteName) {
        final String selection = LocalContract.NoteEntry.COLUMN_NAME_NOTE + " = ?" +
                " AND " + LocalContract.NoteEntry.COLUMN_NAME_DUPLICATED + " = ?";
        final String[] selectionArgs = {noteName, "0"};

        return Single.fromCallable(() -> {
            try (Cursor cursor = Provider.rawQuery(context,
                    LocalContract.NoteEntry.TABLE_NAME,
                    LocalContract.NoteEntry.NOTE_COLUMNS,
                    selection, selectionArgs, null)) {
                if (cursor == null) {
                    return null; // NOTE: NullPointerException
                } else if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested note was not found");
                }
                return factory.from(cursor);
            }
        }).flatMap(this::buildNote);
    }
}
