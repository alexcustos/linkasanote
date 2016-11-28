package com.bytesforge.linkasanote.laano.favorites;

import javax.inject.Inject;

public final class FavoritesPresenter implements FavoritesContract.Presenter {

    private final FavoritesContract.View linksView;

    @Inject
    public FavoritesPresenter(FavoritesContract.View linksView) {
        this.linksView = linksView;
    }

    @Override
    public void start() {

    }
}
