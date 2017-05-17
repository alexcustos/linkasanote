package com.bytesforge.linkasanote.laano.notes.conflictresolution;

import android.util.Log;

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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.NoSuchElementException;

import io.reactivex.Single;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
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
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Log.class);
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
