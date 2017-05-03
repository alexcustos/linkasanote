package com.bytesforge.linkasanote.laano.notes.conflictresolution;

import com.bytesforge.linkasanote.FragmentScoped;

import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {NotesConflictResolutionPresenterModule.class})
public interface NotesConflictResolutionComponent {

    void inject(NotesConflictResolutionDialog notesConflictResolutionDialog);
}
