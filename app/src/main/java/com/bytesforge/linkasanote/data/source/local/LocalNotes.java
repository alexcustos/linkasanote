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

    public Observable<Note> getNotes() {
        return getNotes(null, null, null);
    }

    public Observable<Note> getNotes(
            final String selection, final String[] selectionArgs, final String sortOrder) {
        return Observable.generate(() -> {
            return contentResolver.query(
                    LocalContract.NoteEntry.buildUri(),
                    LocalContract.NoteEntry.NOTE_COLUMNS,
                    selection, selectionArgs, sortOrder);
        }, (cursor, noteEmitter) -> {
            if (cursor == null) {
                noteEmitter.onError(new NullPointerException("An error while retrieving the cursor"));
                return null;
            }
            if (!cursor.moveToNext()) {
                noteEmitter.onComplete();
                return cursor;
            }
            String rowId = LocalContract.rowIdFrom(cursor);
            Uri noteTagsUri = LocalContract.NoteEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = localTags.getTags(noteTagsUri).toList().blockingGet();
            noteEmitter.onNext(Note.from(cursor, tags));

            return cursor;
        }, Cursor::close);
    }

    public Single<Note> getNote(final String noteId) {
        return Single.fromCallable(() -> {
            try (Cursor cursor = contentResolver.query(
                    LocalContract.NoteEntry.buildUriWith(noteId),
                    LocalContract.NoteEntry.NOTE_COLUMNS, null, null, null)) {
                if (cursor == null) {
                    return null; // NOTE: NullPointerException
                } else if (!cursor.moveToLast()) {
                    throw new NoSuchElementException("The requested note was not found");
                }
                String rowId = LocalContract.rowIdFrom(cursor);
                Uri noteTagsUri = LocalContract.NoteEntry.buildTagsDirUriWith(rowId);
                List<Tag> tags = localTags.getTags(noteTagsUri).toList().blockingGet();
                return Note.from(cursor, tags);
            }
        });
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
            Uri uri = LocalContract.NoteEntry.buildTagsDirUriWith(rowId);
            List<Tag> tags = note.getTags();
            if (tags != null) {
                for (Tag tag : tags) {
                    localTags.saveTag(tag, uri).blockingGet();
                }
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
            Uri uri = LocalContract.NoteEntry.buildUri();
            return contentResolver.update(uri, values, null, null);
        });
    }

    public Single<Integer> deleteNote(final String noteId) {
        Uri uri = LocalContract.NoteEntry.buildUriWith(noteId);
        return LocalDataSource.delete(contentResolver, uri);
    }

    public Single<Integer> deleteNotes() {
        Uri uri = LocalContract.NoteEntry.buildUri();
        return LocalDataSource.delete(contentResolver, uri);
    }

    public Single<SyncState> getNoteSyncState(final String noteId) {
        Uri uri = LocalContract.NoteEntry.buildUriWith(noteId);
        return LocalDataSource.getSyncState(contentResolver, uri);
    }

    public Observable<SyncState> getNoteSyncStates() {
        Uri uri = LocalContract.NoteEntry.buildUri();
        return LocalDataSource.getSyncStates(contentResolver, uri, null, null, null);
    }

    public Observable<String> getNoteIds() {
        Uri uri = LocalContract.NoteEntry.buildUri();
        return LocalDataSource.getIds(contentResolver, uri);
    }

    public Single<Boolean> isConflictedNotes() {
        Uri uri = LocalContract.NoteEntry.buildUri();
        return LocalDataSource.isConflicted(contentResolver, uri);
    }

    public Single<Integer> getNextDuplicated(final String noteName) {
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
                String rowId = LocalContract.rowIdFrom(cursor);
                Uri noteTagsUri = LocalContract.NoteEntry.buildTagsDirUriWith(rowId);
                List<Tag> tags = localTags.getTags(noteTagsUri).toList().blockingGet();
                return Note.from(cursor, tags);
            }
        });
    }
}
