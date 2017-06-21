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

package com.bytesforge.linkasanote.laano.notes;

import android.util.Log;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class NotesPresenterTest {

    @Mock
    private Repository repository;

    @Mock
    private NotesContract.View view;

    @Mock
    private NotesContract.ViewModel viewModel;

    @Mock
    private LaanoUiManager laanoUiManager;

    @Mock
    private Settings settings;

    private NotesPresenter presenter;

    @Captor
    ArgumentCaptor<List<Note>> noteListCaptor;

    private final List<Note> NOTES;

    public NotesPresenterTest() {
        NOTES = TestUtils.buildNotes();
    }

    @Before
    public void setupNotesPresenter() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Log.class);
        BaseSchedulerProvider schedulerProvider = new ImmediateSchedulerProvider();
        when(view.isActive()).thenReturn(true);
        when(settings.getNotesFilterType()).thenReturn(FilterType.ALL);
        presenter = new NotesPresenter(
                repository, view, viewModel, schedulerProvider, laanoUiManager, settings);
    }

    @Test
    public void loadAllNotesFromRepository_loadsItIntoView() {
        when(repository.getNotes()).thenReturn(Observable.fromIterable(NOTES));
        presenter.loadNotes(true);
        verify(view).showNotes(NOTES);
    }

    @Test
    public void loadEmptyListOfNotes_showsEmptyList() {
        when(repository.getNotes()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        presenter.loadNotes(true);
        verify(view).showNotes(noteListCaptor.capture());
        assertEquals(noteListCaptor.getValue().size(), 0);
    }

    @Test
    public void clickOnAddNote_showAddNoteUiForUnboundNote() {
        presenter.showAddNote();
        verify(view).startAddNoteActivity(eq(null));
    }

    @Test
    public void clickOnEditNote_showEditNoteUi() {
        final String noteId = NOTES.get(0).getId();
        presenter.onEditClick(noteId);
        verify(view).showEditNote(eq(noteId));
    }

    @Test
    public void clickOnDeleteNote_showsConfirmNotesRemoval() {
        ArrayList<String> selectedIds = new ArrayList<String>() {{
            add(NOTES.get(0).getId());
            add(NOTES.get(2).getId());
        }};
        when(viewModel.getSelectedIds()).thenReturn(selectedIds);
        presenter.onDeleteClick();
        verify(view).confirmNotesRemoval(eq(selectedIds));
    }
}