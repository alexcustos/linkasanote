package com.bytesforge.linkasanote.laano.notes.conflictresolution;

import android.content.Context;

import com.bytesforge.linkasanote.laano.notes.NoteId;

import dagger.Module;
import dagger.Provides;

@Module
public class NotesConflictResolutionPresenterModule {

    private final Context context;
    private final NotesConflictResolutionContract.View view;
    private String noteId;

    public NotesConflictResolutionPresenterModule(
            Context context, NotesConflictResolutionContract.View view, String noteId) {
        this.context = context;
        this.view = view;
        this.noteId = noteId;
    }

    @Provides
    public NotesConflictResolutionContract.View provideNotesConflictResolutionContractView() {
        return view;
    }

    @Provides
    public NotesConflictResolutionContract.ViewModel provideNotesConflictResolutionContractViewModel() {
        return new NotesConflictResolutionViewModel(context);
    }

    @Provides
    @NoteId
    public String provideNoteId() {
        return noteId;
    }
}
