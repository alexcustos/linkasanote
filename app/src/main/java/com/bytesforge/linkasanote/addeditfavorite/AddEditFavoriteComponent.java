package com.bytesforge.linkasanote.addeditfavorite;

import com.bytesforge.linkasanote.FragmentScoped;

import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {AddEditFavoritePresenterModule.class})
public interface AddEditFavoriteComponent {

    void inject(AddEditFavoriteActivity addEditFavoriteActivity);
}
