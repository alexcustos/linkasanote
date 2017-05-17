package com.bytesforge.linkasanote.data.source;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.utils.CommonUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class ProviderNotesTest extends ProviderTestCase2<Provider> {

    private final String KEY_PREFIX = CommonUtils.charRepeat('A', 21);
    private final String[] ENTRY_KEYS;
    private final List<Tag> NOTE_TAGS;
    private final String[] NOTE_NAMES;

    private ContentResolver contentResolver;
    private Provider provider;

    public ProviderNotesTest() {
        super(Provider.class, LocalContract.CONTENT_AUTHORITY);

        ENTRY_KEYS = new String[]{KEY_PREFIX + 'A', KEY_PREFIX + 'B'};
        NOTE_NAMES = new String[]{"Note", "Note #2"};
        NOTE_TAGS = new ArrayList<Tag>() {{
            add(new Tag("first"));
            add(new Tag("second"));
            add(new Tag("third"));
        }};
    }

    @Before
    @Override
    public void setUp() throws Exception {
        setContext(InstrumentationRegistry.getTargetContext());
        super.setUp();

        contentResolver = getMockContentResolver();
        provider = getProvider();
    }

    @Test
    public void provider_insertNoteEntry() {
        final String noteId = ENTRY_KEYS[0];
        final Note note = new Note(noteId, NOTE_NAMES[0], null, NOTE_TAGS);

        insertNoteOnly(note);
        Note savedNote = queryNoteOnly(noteId, NOTE_TAGS);
        assertEquals(note, savedNote);
    }

    @Test
    public void provider_insertNoteEntryWithTags() {
        final String noteId = ENTRY_KEYS[0];
        final Note note = new Note(noteId, NOTE_NAMES[0], null, NOTE_TAGS);

        insertNoteWithTags(note);
        Note savedNote = queryNoteWithTags(noteId);
        assertEquals(note, savedNote);
    }

    @Test
    public void provider_deleteNoteButLeaveTags() {
        final String noteId = ENTRY_KEYS[0];
        final Note note = new Note(noteId, NOTE_NAMES[0], null, NOTE_TAGS);

        insertNoteWithTags(note);
        List<Tag> tags = queryAllTags();
        assertEquals(NOTE_TAGS, tags);

        int numRows = deleteNote(noteId);
        assertThat(numRows, equalTo(1));

        tags = queryAllTags();
        assertEquals(NOTE_TAGS, tags);
    }

    @Test
    public void provider_updateNoteEntry() {
        final String noteId = ENTRY_KEYS[0];
        final Note note = new Note(noteId, NOTE_NAMES[0], null, NOTE_TAGS);
        insertNoteWithTags(note);

        NOTE_TAGS.add(new Tag("four"));
        final Note updatedNote = new Note(noteId, NOTE_NAMES[1], null, NOTE_TAGS);
        insertNoteWithTags(updatedNote);

        Note savedNote = queryNoteWithTags(noteId);
        assertEquals(updatedNote, savedNote);

        List<Tag> tags = queryAllTags();
        assertEquals(NOTE_TAGS, tags);
    }

    private int deleteNote(String noteId) {
        final Uri noteUri = LocalContract.NoteEntry.buildUriWith(noteId);

        String selection = LocalContract.NoteEntry.COLUMN_NAME_ENTRY_ID + " = ?";
        String[] selectionArgs = new String[]{noteId};
        return contentResolver.delete(noteUri, selection, selectionArgs);
    }

    @NonNull
    private String insertNoteOnly(Note note) {
        final Uri notesUri = LocalContract.NoteEntry.buildUri();

        Uri newNoteUri = contentResolver.insert(
                notesUri, note.getContentValues());
        assertNotNull(newNoteUri);

        String newNoteRowId = LocalContract.NoteEntry.getIdFrom(newNoteUri);
        assertNotNull(newNoteRowId);
        assertTrue(Long.parseLong(newNoteRowId) > 0);
        return newNoteRowId;
    }

    @NonNull
    private String insertNoteTag(String noteRowId, Tag tag) {
        final Uri noteTagsUri = LocalContract.NoteEntry.buildTagsDirUriWith(noteRowId);

        ContentValues values = tag.getContentValues();
        Uri newTagUri = contentResolver.insert(noteTagsUri, values);
        assertNotNull(newTagUri);

        String newTagRowId = LocalContract.TagEntry.getIdFrom(newTagUri);
        assertNotNull(newTagRowId);
        assertTrue(Long.parseLong(newTagRowId) > 0);
        return newTagRowId;
    }

    @NonNull
    private String insertNoteWithTags(Note note) {
        String noteRowId = insertNoteOnly(note);
        List<Tag> tags = note.getTags();
        assertNotNull(tags);
        for (Tag tag : tags) insertNoteTag(noteRowId, tag);

        List<Tag> savedTags = queryNoteTags(noteRowId);
        assertEquals(tags, savedTags);

        return noteRowId;
    }


    @NonNull
    private Note queryNoteOnly(String noteId, List<Tag> tags) {
        assertNotNull(noteId);
        assertNotNull(tags);
        final Uri noteUri = LocalContract.NoteEntry.buildUriWith(noteId);

        Cursor cursor = provider.query(noteUri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertThat(cursor.getCount(), equalTo(1));
        try {
            cursor.moveToLast();
            return new Note(Note.from(cursor), tags);
        } finally {
            cursor.close();
        }
    }

    @NonNull
    private Note queryNoteWithTags(String noteId) {
        assertNotNull(noteId);
        final Uri noteUri = LocalContract.NoteEntry.buildUriWith(noteId);

        Cursor cursor = provider.query(noteUri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertThat(cursor.getCount(), equalTo(1));
        try {
            cursor.moveToLast();
            String rowId = LocalContract.rowIdFrom(cursor);
            return new Note(Note.from(cursor), queryNoteTags(rowId));
        } finally {
            cursor.close();
        }
    }

    @NonNull
    private List<Tag> queryNoteTags(String noteRowId) {
        final Uri noteTagsUri = LocalContract.NoteEntry.buildTagsDirUriWith(noteRowId);

        Cursor cursor = provider.query(noteTagsUri, null, null, new String[]{}, null);
        assertNotNull(cursor);

        List<Tag> tags = new ArrayList<>();
        while (cursor.moveToNext()) tags.add(Tag.from(cursor));
        cursor.close();

        return tags;
    }

    @NonNull
    private List<Tag> queryAllTags() {
        final Uri tagsUri = LocalContract.TagEntry.buildUri();
        Cursor cursor = provider.query(tagsUri, null, null, new String[]{}, null);
        assertNotNull(cursor);

        List<Tag> tags = new ArrayList<>();
        while (cursor.moveToNext()) tags.add(Tag.from(cursor));
        cursor.close();

        return tags;
    }
}
