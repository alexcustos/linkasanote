package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Provider;
import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalNotes {

    private static final Uri NOTE_URI = LocalContract.NoteEntry.buildUri();

    private final Context context;
    private final ContentResolver contentResolver;
    private final LocalTags localTags;

    public LocalNotes(
            @NonNull Context context, @NonNull ContentResolver contentResolver,
            @NonNull LocalTags localTags) {
        this.context = checkNotNull(context);
        this.contentResolver = checkNotNull(contentResolver);
        this.localTags = checkNotNull(localTags);
    }

    private Single<Note> buildNote(final Note note) {
        Single<Note> singleNote = Single.just(note);
        Uri noteTagUri = LocalContract.NoteEntry.buildTagsDirUriWith(note.getRowId());
        Single<List<Tag>> singleTags = localTags.getTags(noteTagUri).toList();
        return Single.zip(singleNote, singleTags, Note::new);
    }

    // Operations

    public Observable<Note> getNotes() {
        return getNotes(NOTE_URI, null, null, null);
    }

    public Observable<Note> getNotes(
            final String selection, final String[] selectionArgs, final String sortOrder) {
        return getNotes(NOTE_URI, selection, selectionArgs, sortOrder);
    }

    public Observable<Note> getNotes(final Uri uri) {
        final String sortOrder = LocalContract.TagEntry.COLUMN_NAME_CREATED + " DESC";
        return getNotes(uri, null, null, sortOrder);
    }

    public Observable<Note> getNotes(
            final Uri uri,
            final String selection, final String[] selectionArgs, final String sortOrder) {
        Observable<Note> notesGenerator = Observable.generate(() -> {
            return contentResolver.query(
                    uri, LocalContract.NoteEntry.NOTE_COLUMNS,
                    selection, selectionArgs, sortOrder);
        }, (cursor, noteEmitter) -> {
            if (cursor == null) {
                noteEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (cursor.moveToNext()) {
                noteEmitter.onNext(Note.from(cursor));
            } else {
                noteEmitter.onComplete();
            }
            return cursor;
        }, Cursor::close);
        return notesGenerator.flatMap(note -> buildNote(note).toObservable());
    }

    public Single<Note> getNote(final String noteId) {
        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    LocalContract.NoteEntry.buildUriWith(noteId),
                    LocalContract.NoteEntry.NOTE_COLUMNS, null, null, null)) {
                if (cursor == null) return null;

                if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested note was not found");
                }
                return Note.from(cursor);
            }
        }).flatMap(this::buildNote);
    }

    public Single<Long> saveNote(final Note note) {
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

    public Single<Integer> updateNote(final String noteId, final SyncState state) {
        return Single.fromCallable(() -> {
            ContentValues values = state.getContentValues();
            Uri uri = LocalContract.NoteEntry.buildUriWith(noteId);
            return contentResolver.update(uri, values, null, null);
        });
    }

    public Single<Integer> resetNotesSyncState() {
        return Single.fromCallable(() -> {
            SyncState state = new SyncState(SyncState.State.UNSYNCED);
            ContentValues values = state.getContentValues();
            return contentResolver.update(NOTE_URI, values, null, null);
        });
    }

    public Single<Integer> deleteNote(final String noteId) {
        Uri uri = LocalContract.NoteEntry.buildUriWith(noteId);
        return LocalDataSource.delete(contentResolver, uri);
    }

    public Single<Integer> deleteNotes() {
        return LocalDataSource.delete(contentResolver, NOTE_URI);
    }

    public Single<SyncState> getNoteSyncState(final String noteId) {
        Uri uri = LocalContract.NoteEntry.buildUriWith(noteId);
        return LocalDataSource.getSyncState(contentResolver, uri);
    }

    public Observable<SyncState> getNoteSyncStates() {
        return LocalDataSource.getSyncStates(contentResolver, NOTE_URI, null, null, null);
    }

    public Observable<String> getNoteIds() {
        return LocalDataSource.getIds(contentResolver, NOTE_URI);
    }

    public Single<Boolean> isConflictedNotes() {
        return LocalDataSource.isConflicted(contentResolver, NOTE_URI);
    }

    public Single<Integer> getNextDuplicatedNote(final String noteName) {
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

    public Single<Note> getMainNote(final String noteName) {
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
                return Note.from(cursor);
            }
        }).flatMap(this::buildNote);
    }
}
