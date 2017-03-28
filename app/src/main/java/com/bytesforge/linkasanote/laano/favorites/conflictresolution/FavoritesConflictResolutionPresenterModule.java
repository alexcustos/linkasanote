package com.bytesforge.linkasanote.laano.favorites.conflictresolution;

import android.content.Context;

import com.bytesforge.linkasanote.laano.favorites.FavoriteId;

import dagger.Module;
import dagger.Provides;

@Module
public class FavoritesConflictResolutionPresenterModule {

    private final Context context;
    private final FavoritesConflictResolutionContract.View view;
    private String favoriteId;

    public FavoritesConflictResolutionPresenterModule(
            Context context, FavoritesConflictResolutionContract.View view, String favoriteId) {
        this.context = context;
        this.view = view;
        this.favoriteId = favoriteId;
    }

    @Provides
    public FavoritesConflictResolutionContract.View provideFavoritesConflictResolutionContractView() {
        return view;
    }

    @Provides
    public FavoritesConflictResolutionContract.ViewModel provideFavoritesConflictResolutionContractViewModel() {
        return new FavoritesConflictResolutionViewModel(context);
    }

    @Provides
    @FavoriteId
    public String provideFavoriteId() {
        return favoriteId;
    }
}
