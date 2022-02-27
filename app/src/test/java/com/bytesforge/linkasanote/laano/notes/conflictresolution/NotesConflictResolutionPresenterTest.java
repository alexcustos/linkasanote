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

package com.bytesforge.linkasanote.laano.notes.conflictresolution;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalLinks;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.NoSuchElementException;

import io.reactivex.Single;

@RunWith(MockitoJUnitRunner.class)
public class NotesConflictResolutionPresenterTest {

    @Mock
    private Repository repository;

    @Mock
    private LocalNotes<Note> localNotes;

    @Mock
    private CloudItem<Note> cloudNotes;

    @Mock
    private LocalLinks<Link> localLinks;

    @Mock
    private NotesConflictResolutionContract.View view;

    @Mock
    private NotesConflictResolutionContract.ViewModel viewModel;

    @Mock
    private Settings settings;

    private static final String E_TAGL = "abcdefghigklmnopqrstuvwxwz";
    private static final String E_TAGC = "zwxwvutsrqponmlkgihgfedcba";

    private BaseSchedulerProvider schedulerProvider;
    private NotesConflictResolutionPresenter presenter;
    private Note defaultNote;
    private String noteId;

    @Before
    public void setupNotesConflictResolutionPresenter() {
        MockitoAnnotations.openMocks(this);
        schedulerProvider = new ImmediateSchedulerProvider();
        noteId = TestUtils.KEY_PREFIX + 'A';
        defaultNote = new Note(noteId, "Note", null, TestUtils.TAGS);
        presenter = new NotesConflictResolutionPresenter(
                repository, settings, localNotes, cloudNotes,
                localLinks, view, viewModel, schedulerProvider, defaultNote.getId());
    }

    @Test
    public void notConflictedNote_finishesActivityWithSuccess() {
        SyncState state = new SyncState(SyncState.State.SYNCED);
        Note note = new Note(defaultNote, state);
        when(localNotes.get(eq(noteId))).thenReturn(Single.just(note));
        presenter.subscribe();
        verify(repository).refreshNotes();
        verify(view).finishActivity();
    }

    @Test
    public void wrongId_finishesActivityWithSuccess() {
        when(localNotes.get(eq(noteId))).thenReturn(Single.error(new NoSuchElementException()));
        presenter.subscribe();
        verify(repository).refreshNotes();
        verify(view).finishActivity();
    }

    @Test
    public void databaseError_showsErrorThenTriesToLoadCloudNote() {
        when(localNotes.get(eq(noteId))).thenReturn(Single.error(new NullPointerException()));
        when(cloudNotes.download(eq(noteId))).thenReturn(Single.just(defaultNote));
        presenter.subscribe();
        verify(viewModel).showDatabaseError();
        verify(viewModel).populateCloudNote(eq(defaultNote), eq(false));
    }
}
