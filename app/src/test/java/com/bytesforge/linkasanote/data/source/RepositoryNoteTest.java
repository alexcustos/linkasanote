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

package com.bytesforge.linkasanote.data.source;

import android.util.Log;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class RepositoryNoteTest {

    private final List<Note> NOTES;

    private Repository repository;

    @Mock
    private LocalDataSource localDataSource;

    @Mock
    private CloudDataSource cloudDataSource;

    private TestObserver<List<Note>> testNotesObserver;
    private TestObserver<Note> testNoteObserver;

    public RepositoryNoteTest() {
        NOTES = TestUtils.buildNotes();
    }

    @Before
    public void setupRepository() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Log.class);
        BaseSchedulerProvider schedulerProvider = new ImmediateSchedulerProvider();
        repository = new Repository(localDataSource, cloudDataSource, schedulerProvider);
    }

    @Test
    public void getNotes_requestsAllNotesFromLocalSource() {
        repository.noteCacheIsDirty = true;
        when(localDataSource.getNotes((String[]) isNull()))
                .thenReturn(Observable.fromIterable(NOTES));
        when(localDataSource.markNotesSyncResultsAsApplied()).thenReturn(Single.just(0));
        testNotesObserver = repository.getNotes().toList().test();
        testNotesObserver.assertValue(NOTES);
        assert repository.cachedNotes != null;
        assertThat(repository.cachedNotes.size(), is(NOTES.size()));
        Collection<Note> cachedNotes = repository.cachedNotes.values();
        Iterator iterator;
        int i;
        for (i = 0, iterator = cachedNotes.iterator(); iterator.hasNext(); i++) {
            Note cachedNote = (Note) iterator.next();
            assertThat(cachedNote.getId(), is(NOTES.get(i).getId()));
        }
    }

    @Test
    public void getNote_requestsSingleNoteFromLocalSource() {
        repository.noteCacheIsDirty = true;
        Note note = NOTES.get(0);
        String noteId = note.getId();
        when(localDataSource.getNote(eq(noteId))).thenReturn(Single.just(note));

        testNoteObserver = repository.getNote(noteId).test();
        testNoteObserver.assertValue(note);
        assertThat(repository.noteCacheIsDirty, is(true));
    }

    @Test
    public void saveNote_savesNoteToLocalAndCloudStorage() {
        Note note = NOTES.get(0);
        String noteId = note.getId();
        when(localDataSource.saveNote(eq(note)))
                .thenReturn(Single.just(DataSource.ItemState.DEFERRED));
        when(cloudDataSource.saveNote(eq(noteId)))
                .thenReturn(Single.just(DataSource.ItemState.SAVED));

        TestObserver<DataSource.ItemState> saveNoteObserver =
                repository.saveNote(note, true).test();
        saveNoteObserver.assertValues(
                DataSource.ItemState.DEFERRED, DataSource.ItemState.SAVED);
        assertThat(repository.noteCacheIsDirty, is(true));
        assert repository.dirtyNotes != null;
        assertThat(repository.dirtyNotes.contains(noteId), is(true));
    }

    @Test
    public void deleteNote_deleteNoteFromLocalAndCloudStorage() {
        int size = NOTES.size();
        Note note = NOTES.get(0);
        String noteId = note.getId();
        // Cache
        when(localDataSource.getNotes((String[]) isNull()))
                .thenReturn(Observable.fromIterable(NOTES));
        when(localDataSource.markNotesSyncResultsAsApplied()).thenReturn(Single.just(0));
        testNotesObserver = repository.getNotes().toList().test();
        testNotesObserver.assertValue(NOTES);
        assert repository.cachedNotes != null;
        assertThat(repository.cachedNotes.size(), is(size));
        // Preconditions
        when(localDataSource.deleteNote(eq(noteId)))
                .thenReturn(Single.just(DataSource.ItemState.DEFERRED));
        when(cloudDataSource.deleteNote(eq(noteId), any(long.class)))
                .thenReturn(Single.just(DataSource.ItemState.DELETED));
        when(localDataSource.getNote(eq(noteId))).thenReturn(Single.just(note));
        // Test
        TestObserver<DataSource.ItemState> deleteNoteObserver =
                repository.deleteNote(noteId, true, 0).test();
        deleteNoteObserver.assertValues(
                DataSource.ItemState.DEFERRED, DataSource.ItemState.DELETED);
        assertThat(repository.noteCacheIsDirty, is(false));
        assertThat(repository.cachedNotes.size(), is(size - 1));
        Collection<Note> cachedNotes = repository.cachedNotes.values();
        Iterator iterator;
        int i;
        for (i = 0, iterator = cachedNotes.iterator(); iterator.hasNext(); i++) {
            if (noteId.equals(NOTES.get(i).getId())) continue;

            Note cachedNote = (Note) iterator.next();
            assertThat(cachedNote.getId(), is(NOTES.get(i).getId()));
        }
    }
}
