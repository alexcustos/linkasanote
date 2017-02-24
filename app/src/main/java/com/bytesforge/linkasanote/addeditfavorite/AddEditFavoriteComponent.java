package com.bytesforge.linkasanote.addeditfavorite;

import com.bytesforge.linkasanote.ApplicationComponent;
import com.bytesforge.linkasanote.FragmentScoped;

import dagger.Component;

@FragmentScoped
@Component(dependencies = {ApplicationComponent.class},
        modules = {AddEditFavoritePresenterModule.class})
public interface AddEditFavoriteComponent {

    void inject(AddEditFavoriteActivity addEditFavoriteActivity);
}
