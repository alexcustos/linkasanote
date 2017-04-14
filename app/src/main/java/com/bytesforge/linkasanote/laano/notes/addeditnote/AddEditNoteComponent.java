package com.bytesforge.linkasanote.laano.notes.addeditnote;

import com.bytesforge.linkasanote.FragmentScoped;

import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {AddEditNotePresenterModule.class})
public interface AddEditNoteComponent {

    void inject(AddEditNoteActivity addEditNoteActivity);
}
