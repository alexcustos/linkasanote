package com.bytesforge.linkasanote.links;

import javax.inject.Inject;

public final class LinksPresenter implements LinksContract.Presenter {

    private final LinksContract.View linksView;

    @Inject
    public LinksPresenter(LinksContract.View linksView) {
        this.linksView = linksView;
    }

    @Override
    public void start() {

    }
}
