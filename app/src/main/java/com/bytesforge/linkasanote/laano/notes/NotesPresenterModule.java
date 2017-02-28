package com.bytesforge.linkasanote.laano.notes;

import dagger.Module;
import dagger.Provides;

@Module
public class NotesPresenterModule {

    private final NotesContract.View view;

    public NotesPresenterModule(NotesContract.View view) {
        this.view = view;
    }

    @Provides
    public NotesContract.View provideNotesContractView() {
        return view;
    }
}
