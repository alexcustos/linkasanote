package com.bytesforge.linkasanote.laano.links;

import javax.inject.Inject;

public final class LinksPresenter implements LinksContract.Presenter {

    private final LinksContract.View view;

    @Inject
    public LinksPresenter(LinksContract.View view) {
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
    public void addLink() {

    }

    @Override
    public boolean isConflicted() {
        return false;
    }

    @Override
    public void updateTabNormalState() {
    }
}
