package com.bytesforge.linkasanote.laano.favorites;

import javax.inject.Inject;

public final class FavoritesPresenter implements FavoritesContract.Presenter {

    private final FavoritesContract.View favoritesView;

    @Inject
    public FavoritesPresenter(FavoritesContract.View favoritesView) {
        this.favoritesView = favoritesView;
    }

    @Override
    public void start() {

    }
}
