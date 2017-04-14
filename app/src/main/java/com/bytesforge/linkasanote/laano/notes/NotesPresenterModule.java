package com.bytesforge.linkasanote.laano.notes;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class NotesPresenterModule {

    private final Context context; // NOTE: Activity context
    private final NotesContract.View view;

    public NotesPresenterModule(Context context, NotesContract.View view) {
        this.context = context;
        this.view = view;
    }

    @Provides
    public NotesContract.View provideNotesContractView() {
        return view;
    }

    @Provides
    public NotesContract.ViewModel provideNotesContractViewModel() {
        return new NotesViewModel(context);
    }
}
