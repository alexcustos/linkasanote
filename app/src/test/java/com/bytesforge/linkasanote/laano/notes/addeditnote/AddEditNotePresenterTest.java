package com.bytesforge.linkasanote.laano.notes.addeditnote;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddEditNotePresenterTest {

    @Mock
    private Repository repository;

    @Mock
    private AddEditNoteContract.View view;

    @Mock
    private AddEditNoteContract.ViewModel viewModel;

    @Mock
    private Settings settings;

    private BaseSchedulerProvider schedulerProvider;
    private AddEditNotePresenter presenter;

    @Captor
    ArgumentCaptor<List<Tag>> tagListCaptor;

    private final List<Note> NOTES;
    private Note defaultNote;

    public AddEditNotePresenterTest() {
        NOTES = TestUtils.buildNotes();
        defaultNote = NOTES.get(0);
    }

    @Before
    public void setLinksPresenter() throws Exception {
        MockitoAnnotations.initMocks(this);
        schedulerProvider = new ImmediateSchedulerProvider();
        when(view.isActive()).thenReturn(true);
        presenter = new AddEditNotePresenter(
                repository, view, viewModel, schedulerProvider, settings, null, null);
    }

    @Test
    public void loadAllTagsFromRepository_loadsItIntoView() {
        List<Tag> tags = defaultNote.getTags();
        assertNotNull(tags);
        when(repository.getTags()).thenReturn(Observable.fromIterable(tags));
        presenter.loadTags();
        verify(view).swapTagsCompletionViewItems(eq(tags));
    }

    @Test
    public void loadEmptyListOfTags_loadsEmptyListIntoView() {
        when(repository.getTags()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        presenter.loadTags();
        verify(view).swapTagsCompletionViewItems(tagListCaptor.capture());
        assertEquals(tagListCaptor.getValue().size(), 0);
    }

    @Test
    public void saveNewNoteToRepository_finishesActivity() {
        String noteNote = defaultNote.getNote();
        String linkId = defaultNote.getLinkId();
        List<Tag> noteTags = defaultNote.getTags();
        when(repository.saveNote(any(Note.class), eq(false)))
                .thenReturn(Observable.just(DataSource.ItemState.DEFERRED));

        presenter.saveNote(noteNote, noteTags);
        verify(repository).refreshNotes();
        verify(view).finishActivity(any(String.class), eq(linkId));
    }

    @Test
    public void saveEmptyNote_showsEmptyError() {
        presenter.saveNote("", new ArrayList<>());
        presenter.saveNote("", defaultNote.getTags());
        verify(viewModel, times(2)).showEmptyNoteSnackbar();
    }

    @Test
    public void saveExistingNote_finishesActivity() {
        String noteId = defaultNote.getId();
        String linkId = defaultNote.getLinkId();
        String noteNote = defaultNote.getNote();
        List<Tag> noteTags = defaultNote.getTags();
        when(repository.saveNote(any(Note.class), eq(false)))
                .thenReturn(Observable.just(DataSource.ItemState.DEFERRED));
        // Edit Note Presenter
        AddEditNotePresenter presenter = new AddEditNotePresenter(
                repository, view, viewModel, schedulerProvider, settings, noteId, linkId);
        presenter.saveNote(noteNote, noteTags);
        verify(repository).refreshNotes();
        verify(view).finishActivity(eq(noteId), eq(linkId));
    }

    @Test
    public void populateNote_callsRepositoryAndUpdateViewOnSuccess() {
        final String noteId = defaultNote.getId();
        when(repository.getNote(noteId)).thenReturn(Single.just(defaultNote));
        // Edit Note Presenter
        AddEditNotePresenter presenter = new AddEditNotePresenter(
                repository, view, viewModel, schedulerProvider, settings, noteId, defaultNote.getLinkId());
        presenter.populateNote();
        verify(repository).getNote(noteId);
        verify(viewModel).populateNote(any(Note.class));
    }

    @Test
    public void populateNote_callsRepositoryAndShowsWarningOnError() {
        final String noteId = defaultNote.getId();
        when(repository.getNote(noteId)).thenReturn(Single.error(new NoSuchElementException()));
        // Edit Note Presenter
        AddEditNotePresenter presenter = new AddEditNotePresenter(
                repository, view, viewModel, schedulerProvider, settings, noteId, defaultNote.getLinkId());
        presenter.populateNote();
        verify(repository).getNote(noteId);
        verify(viewModel, never()).populateNote(any(Note.class));
        verify(viewModel).showNoteNotFoundSnackbar();
    }
}