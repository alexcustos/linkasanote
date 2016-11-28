package com.bytesforge.linkasanote.laano.favorites;

import dagger.Module;
import dagger.Provides;

@Module
public class FavoritesPresenterModule {

    private final FavoritesContract.View view;

    public FavoritesPresenterModule(FavoritesContract.View view) {
        this.view = view;
    }

    @Provides
    FavoritesContract.View provideLinksContractView() {
        return view;
    }
}
