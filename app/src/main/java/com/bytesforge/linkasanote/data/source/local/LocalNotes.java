package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Item;
import com.bytesforge.linkasanote.data.NoteFactory;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.google.common.collect.ObjectArrays;

import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalNotes<T extends Item> implements LocalItem<T> {

    private static final String TAG = LocalNotes.class.getSimpleName();

    // NOTE: static fails Mockito to mock this class
    private final Uri NOTE_URI;
    private final ContentResolver contentResolver;
    private final LocalTags localTags;
    private final NoteFactory<T> factory;

    public LocalNotes(
            @NonNull ContentResolver contentResolver,
            @NonNull LocalTags localTags, @NonNull NoteFactory<T> factory) {
        this.contentResolver = checkNotNull(contentResolver);
        this.localTags = checkNotNull(localTags);
        this.factory = checkNotNull(factory);
        NOTE_URI = LocalContract.NoteEntry.buildUri();
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
        return getActive(null);
    }

    @Override
    public Observable<T> getActive(final String[] noteIds) {
        String selection = LocalContract.NoteEntry.COLUMN_NAME_DELETED + " = ?" +
                " OR " + LocalContract.NoteEntry.COLUMN_NAME_CONFLICTED + " = ?";
        String[] selectionArgs = {"0", "1"};
        final String sortOrder = LocalContract.NoteEntry.COLUMN_NAME_CREATED + " DESC";

        int size = noteIds == null ? 0 : noteIds.length;
        if (size > 0) {
            selection = " (" + selection + ") AND " + LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID +
                    " IN (" + CommonUtils.strRepeat("?", size, ", ") + ")";
            selectionArgs = ObjectArrays.concat(selectionArgs, noteIds, String.class);
        }
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

    /**
     * @return Returns true if all tags were successfully saved
     */
    private Single<Boolean> saveTags(final long noteRowId, final List<Tag> tags) {
        if (noteRowId <= 0) {
            return Single.just(false);
        } else if (tags == null) {
            return Single.just(true);
        }
        Uri noteTagUri = LocalContract.NoteEntry.buildTagsDirUriWith(noteRowId);
        return Observable.fromIterable(tags)
                .flatMap(tag -> localTags.saveTag(tag, noteTagUri).toObservable())
                .count()
                .map(count -> count == tags.size())
                .onErrorReturn(throwable -> {
                    CommonUtils.logStackTrace(TAG, throwable);
                    return false;
                });
    }
    @Override
    public Single<Boolean> save(final T note) {
        return Single.fromCallable(() -> {
            ContentValues values = note.getContentValues();
            Uri noteUri = contentResolver.insert(NOTE_URI, values);
            if (noteUri == null) {
                throw new NullPointerException("Provider must return URI or throw exception");
            }
            String rowId = LocalContract.NoteEntry.getIdFrom(noteUri);
            return Long.parseLong(rowId);
        }).flatMap(rowId -> saveTags(rowId, note.getTags()));
    }

    @Override
    public Single<Boolean> saveDuplicated(final T note) {
        // NOTE: foreign key constraint is violated (orphaned Note)
        return Single.fromCallable(() -> factory.buildOrphaned(note))
                .flatMap(this::save);
    }

    @Override
    public Single<Boolean> update(final String noteId, final SyncState state) {
        return Single.fromCallable(() -> {
            ContentValues values = state.getContentValues();
            if (state.isDeleted()) {
                // NOTE: if conflict is detected, LinkId can be restored from the Cloud storage
                values.put(LocalContract.NoteEntry.COLUMN_NAME_LINK_ID, (String) null);
            }
            Uri uri = LocalContract.NoteEntry.buildUriWith(noteId);
            return contentResolver.update(uri, values, null, null);
        }).map(numRows -> numRows == 1);
    }

    @Override
    public Single<Integer> resetSyncState() {
        return Single.fromCallable(() -> {
            ContentValues values = new ContentValues();
            values.put(LocalContract.NoteEntry.COLUMN_NAME_ETAG, (String) null);
            values.put(LocalContract.NoteEntry.COLUMN_NAME_SYNCED, false);
            final String selection = LocalContract.NoteEntry.COLUMN_NAME_SYNCED + " = ?";
            final String[] selectionArgs = {"1"};
            return contentResolver.update(NOTE_URI, values, selection, selectionArgs);
        });
    }

    @Override
    public Single<Boolean> delete(final String noteId) {
        Uri uri = LocalContract.NoteEntry.buildUriWith(noteId);
        return Single.fromCallable(() -> contentResolver.delete(uri, null, null))
                .map(numRows -> numRows == 1);
    }

    @Override
    public Single<Integer> delete() {
        return Single.fromCallable(() -> contentResolver.delete(NOTE_URI, null, null));
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

    @Override
    public Single<Boolean> isUnsynced() {
        return LocalDataSource.isUnsynced(contentResolver, NOTE_URI);
    }

    @Override
    public Single<T> getMain(final String duplicatedKey) {
        throw new RuntimeException("getMain(): there is not duplicatedKey implementation available for the Notes");
    }

    @Override
    public Single<Boolean> autoResolveConflict(String noteId) {
        throw new RuntimeException("autoResolveConflict(): there is no Auto Conflict Resolution implementation available for the Notes");
    }
}
