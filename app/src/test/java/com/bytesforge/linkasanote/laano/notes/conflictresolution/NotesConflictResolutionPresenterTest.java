package com.bytesforge.linkasanote.laano.notes.conflictresolution;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.cloud.CloudNotes;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.NoSuchElementException;

import io.reactivex.Single;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotesConflictResolutionPresenterTest {

    @Mock
    Repository repository;

    @Mock
    LocalNotes localNotes;

    @Mock
    CloudNotes cloudNotes;

    @Mock
    NotesConflictResolutionContract.View view;

    @Mock
    NotesConflictResolutionContract.ViewModel viewModel;

    private static final String E_TAGL = "abcdefghigklmnopqrstuvwxwz";
    private static final String E_TAGC = "zwxwvutsrqponmlkgihgfedcba";

    private BaseSchedulerProvider schedulerProvider;
    private NotesConflictResolutionPresenter presenter;
    private Note defaultNote;
    private String noteId;

    @Before
    public void setupNotesConflictResolutionPresenter() {
        MockitoAnnotations.initMocks(this);
        schedulerProvider = new ImmediateSchedulerProvider();
        noteId = TestUtils.KEY_PREFIX + 'A';
        defaultNote = new Note(noteId, "Note", TestUtils.TAGS);
        presenter = new NotesConflictResolutionPresenter(
                repository, localNotes, cloudNotes,
                view, viewModel, schedulerProvider, defaultNote.getId());
    }

    @Test
    public void notConflictedNote_finishesActivityWithSuccess() {
        SyncState state = new SyncState(SyncState.State.SYNCED);
        Note note = new Note(defaultNote, state);
        when(localNotes.getNote(eq(noteId)))
                .thenReturn(Single.fromCallable(() -> note));
        presenter.subscribe();
        verify(repository).refreshNotes();
        verify(view).finishActivity();
    }

    @Test
    public void wrongId_finishesActivityWithSuccess() {
        when(localNotes.getNote(eq(noteId)))
                .thenReturn(Single.error(new NoSuchElementException()));
        presenter.subscribe();
        verify(repository).refreshNotes();
        verify(view).finishActivity();
    }

    @Test
    public void databaseError_showsErrorThenTriesToLoadCloudNote() {
        when(localNotes.getNote(eq(noteId)))
                .thenReturn(Single.error(new NullPointerException()));
        when(cloudNotes.downloadNote(eq(noteId)))
                .thenReturn(Single.fromCallable(() -> defaultNote));
        presenter.subscribe();
        verify(viewModel).showDatabaseError();
        verify(viewModel).populateCloudNote(eq(defaultNote));
    }

    @Test
    public void duplicatedNote_populatesToCloudThenLoadsMainToLocal() {
        SyncState state = new SyncState(E_TAGL, 1); // duplicated
        Note note = new Note(defaultNote, state);
        Note mainNote = new Note(
                TestUtils.KEY_PREFIX + 'B', "Note", TestUtils.TAGS);
        when(localNotes.getNote(eq(noteId)))
                .thenReturn(Single.fromCallable(() -> note));
        when(localNotes.getMainNote(eq(note.getNote())))
                .thenReturn(Single.fromCallable(() -> mainNote));
        when(viewModel.isCloudPopulated()).thenReturn(true);
        presenter.subscribe();
        verify(viewModel).populateCloudNote(eq(note));
        verify(viewModel).populateLocalNote(eq(mainNote));
    }

    @Test
    public void duplicatedNoteWithNoMainRecord_resolvesConflictAutomatically() {
        SyncState state = new SyncState(E_TAGL, 1); // duplicated
        Note note = new Note(defaultNote, state);
        when(localNotes.getNote(eq(noteId)))
                .thenReturn(Single.fromCallable(() -> note));
        when(localNotes.getMainNote(eq(note.getNote())))
                .thenReturn(Single.error(new NoSuchElementException()));
        when(viewModel.isCloudPopulated()).thenReturn(true);
        when(localNotes.updateNote(eq(noteId), any(SyncState.class)))
                .thenReturn(Single.fromCallable(() -> 1));
        presenter.subscribe();
        verify(viewModel).populateCloudNote(eq(note));
        verify(repository).refreshNotes();
        verify(view).finishActivity();
    }
}