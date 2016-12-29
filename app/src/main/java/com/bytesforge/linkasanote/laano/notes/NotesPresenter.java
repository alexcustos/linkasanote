package com.bytesforge.linkasanote.laano.notes;

import javax.inject.Inject;

public final class NotesPresenter implements NotesContract.Presenter {

    private final NotesContract.View notesView;

    @Inject
    public NotesPresenter(NotesContract.View notesView) {
        this.notesView = notesView;
    }

    @Override
    public void start() {

    }
}
