package com.bytesforge.linkasanote.laano.notes;

import javax.inject.Inject;

public final class NotesPresenter implements NotesContract.Presenter {

    private final NotesContract.View linksView;

    @Inject
    public NotesPresenter(NotesContract.View linksView) {
        this.linksView = linksView;
    }

    @Override
    public void start() {

    }
}
