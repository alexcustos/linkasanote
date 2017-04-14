package com.bytesforge.linkasanote.data.source;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Note;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryNoteTest {

    private final List<Note> NOTES;

    private Repository repository;

    @Mock
    private DataSource localDataSource;

    @Mock
    private DataSource cloudDataSource;

    private TestObserver<List<Note>> testNotesObserver;
    private TestObserver<Note> testNoteObserver;

    public RepositoryNoteTest() {
        NOTES = TestUtils.buildNotes();
    }

    @Before
    public void setupRepository() {
        MockitoAnnotations.initMocks(this);
        repository = new Repository(localDataSource, cloudDataSource);
    }

    @Test
    public void getNotes_requestsAllNotesFromLocalSource() {
        setNotesAvailable(localDataSource, NOTES);
        setNotesNotAvailable(cloudDataSource);

        testNotesObserver = repository.getNotes().toList().test();
        verify(localDataSource).getNotes();
        testNotesObserver.assertValue(NOTES);
    }

    @Test
    public void getNote_requestsSingleNoteFromLocalSource() {
        Note note = NOTES.get(0);

        setNoteAvailable(localDataSource, note);
        setNoteNotAvailable(cloudDataSource, note.getId());

        testNoteObserver = repository.getNote(note.getId()).test();
        verify(localDataSource).getNote(eq(note.getId()));
        testNoteObserver.assertValue(note);
    }

    @Test
    public void saveNote_savesNoteToLocalAndCloudStorage() {
        Note note = NOTES.get(0);

        repository.saveNote(note);
        verify(localDataSource).saveNote(note);
        verify(cloudDataSource).saveNote(note);
        assertThat(repository.noteCacheIsDirty, is(true));
    }

    @Test
    public void deleteAllNotes_deleteNotesFromLocalAndCloudStorage() {
        setNotesAvailable(localDataSource, NOTES);
        testNotesObserver = repository.getNotes().toList().test();
        assertThat(repository.cachedNotes.size(), is(NOTES.size()));

        repository.deleteAllNotes();
        verify(localDataSource).deleteAllNotes();
        verify(cloudDataSource, never()).deleteAllNotes();
        assertThat(repository.cachedNotes.size(), is(0));
    }

    // Data setup

    private void setNotesAvailable(DataSource dataSource, List<Note> notes) {
        when(dataSource.getNotes()).thenReturn(Observable.fromIterable(notes));
    }

    private void setNotesNotAvailable(DataSource dataSource) {
        when(dataSource.getNotes()).thenReturn(Observable.fromIterable(Collections.emptyList()));
    }

    private void setNoteAvailable(DataSource dataSource, Note note) {
        when(dataSource.getNote(eq(note.getId()))).thenReturn(Single.just(note));
    }

    private void setNoteNotAvailable(DataSource dataSource, String noteId) {
        when(dataSource.getNote(eq(noteId))).thenReturn(
                Single.error(new NoSuchElementException()));
    }
}
