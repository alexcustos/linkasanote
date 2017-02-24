package com.bytesforge.linkasanote.addeditfavorite;

import android.content.Context;
import android.support.annotation.Nullable;

import dagger.Module;
import dagger.Provides;

@Module
public class AddEditFavoritePresenterModule {

    private final Context context;
    private final AddEditFavoriteContract.View view;
    private String favoriteId;

    public AddEditFavoritePresenterModule(
            Context context, AddEditFavoriteContract.View view, @Nullable String favoriteId) {
        this.context = context;
        this.view = view;
        this.favoriteId = favoriteId;
    }

    @Provides
    AddEditFavoriteContract.View provideAddEditFavoriteContractView() {
        return view;
    }

    @Provides
    AddEditFavoriteContract.ViewModel provideAddEditFavoriteContractViewModel() {
        return new AddEditFavoriteViewModel(context);
    }

    @Provides
    @Nullable
    @FavoriteId
    String provideFavoriteId() {
        return favoriteId;
    }
}
