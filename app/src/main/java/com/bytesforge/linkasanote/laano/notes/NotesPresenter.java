package com.bytesforge.linkasanote.laano.notes;

import javax.inject.Inject;

public final class NotesPresenter implements NotesContract.Presenter {

    private final NotesContract.View view;

    @Inject
    public NotesPresenter(NotesContract.View view) {
        this.view = view;
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {

    }

    @Override
    public void onTabSelected() {

    }

    @Override
    public void onTabDeselected() {

    }

    @Override
    public void addNote() {

    }

    @Override
    public boolean isConflicted() {
        return false;
    }

    @Override
    public void updateTabNormalState() {

    }
}
