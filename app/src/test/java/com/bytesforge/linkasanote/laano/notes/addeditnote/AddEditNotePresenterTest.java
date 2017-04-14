package com.bytesforge.linkasanote.laano.notes.addeditnote;

import android.database.sqlite.SQLiteConstraintException;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
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
import static org.mockito.Mockito.doThrow;
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
                repository, view, viewModel, schedulerProvider, null);
    }

    @Test
    public void loadAllTagsFromRepository_loadsItIntoView() {
        List<Tag> tags = NOTES.get(NOTES.size() - 1).getTags();
        assertNotNull(tags);
        when(repository.getTags()).thenReturn(Observable.fromIterable(tags));
        presenter.loadTags();
        verify(view).swapTagsCompletionViewItems(tags);
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
        presenter.saveNote(defaultNote.getNote(), defaultNote.getTags());
        verify(repository).saveNote(any(Note.class));
        verify(view).finishActivity();
    }

    @Test
    public void saveEmptyNote_showsEmptyError() {
        presenter.saveNote("", new ArrayList<>());
        presenter.saveNote("", defaultNote.getTags());
        verify(viewModel, times(2)).showEmptyNoteSnackbar();
    }

    @Test
    public void saveExistingNote_finishesActivity() {
        // Edit Note Presenter
        AddEditNotePresenter presenter = new AddEditNotePresenter(
                repository, view, viewModel, schedulerProvider, defaultNote.getId());
        presenter.saveNote(defaultNote.getNote(), defaultNote.getTags());
        verify(repository).saveNote(any(Note.class));
        verify(view).finishActivity();
    }

    @Test
    public void saveNoteWithExistedName_showsDuplicateError() {
        doThrow(new SQLiteConstraintException()).when(repository).saveNote(any(Note.class));
        presenter.saveNote(defaultNote.getNote(), defaultNote.getTags());
        verify(view, never()).finishActivity();
        verify(viewModel).showDuplicateKeyError();
    }

    @Test
    public void populateNote_callsRepositoryAndUpdateViewOnSuccess() {
        final String noteId = defaultNote.getId();
        when(repository.getNote(noteId)).thenReturn(Single.just(defaultNote));
        // Edit Note Presenter
        AddEditNotePresenter presenter = new AddEditNotePresenter(
                repository, view, viewModel, schedulerProvider, noteId);
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
                repository, view, viewModel, schedulerProvider, noteId);
        presenter.populateNote();
        verify(repository).getNote(noteId);
        verify(viewModel, never()).populateNote(any(Note.class));
        verify(viewModel).showNoteNotFoundSnackbar();
    }
}