package com.bytesforge.linkasanote.laano.notes.addeditnote;

import android.content.Context;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.laano.notes.NoteId;

import dagger.Module;
import dagger.Provides;

@Module
public class AddEditNotePresenterModule {

    private final Context context;
    private final AddEditNoteContract.View view;
    private String noteId;

    public AddEditNotePresenterModule(
            Context context, AddEditNoteContract.View view, @Nullable String noteId) {
        this.context = context;
        this.view = view;
        this.noteId = noteId;
    }

    @Provides
    public AddEditNoteContract.View provideAddEditNoteContractView() {
        return view;
    }

    @Provides
    public AddEditNoteContract.ViewModel provideAddEditNoteContractViewModel() {
        return new AddEditNoteViewModel(context);
    }

    @Provides
    @Nullable
    @NoteId
    public String provideNoteId() {
        return noteId;
    }
}
