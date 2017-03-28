package com.bytesforge.linkasanote.laano.favorites.conflictresolution;

import com.bytesforge.linkasanote.FragmentScoped;

import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {FavoritesConflictResolutionPresenterModule.class})
public interface FavoritesConflictResolutionComponent {

    void inject(FavoritesConflictResolutionActivity favoritesConflictResolutionActivity);
}
