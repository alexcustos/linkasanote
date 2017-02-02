package com.bytesforge.linkasanote.addeditfavorite;

import android.support.annotation.Nullable;

import dagger.Module;
import dagger.Provides;

@Module
public class AddEditFavoritePresenterModule {

    private final AddEditFavoriteContract.View view;
    private String favoriteId;

    public AddEditFavoritePresenterModule(
            AddEditFavoriteContract.View view, @Nullable String favoriteId) {
        this.view = view;
        this.favoriteId = favoriteId;
    }

    @Provides
    AddEditFavoriteContract.View provideAddEditFavoriteContractView() {
        return view;
    }

    @Provides
    @Nullable
    @FavoriteId
    String provideFavoriteId() {
        return favoriteId;
    }
}
