package com.bytesforge.linkasanote.laano.favorites;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class FavoritesPresenterModule {

    private final Context context; // NOTE: Activity context
    private final FavoritesContract.View view;

    public FavoritesPresenterModule(Context context, FavoritesContract.View view) {
        this.context = context;
        this.view = view;
    }

    @Provides
    public FavoritesContract.View provideFavoritesContractView() {
        return view;
    }

    @Provides
    public FavoritesContract.ViewModel provideFavoritesContractViewModel() {
        return new FavoritesViewModel(context);
    }
}
