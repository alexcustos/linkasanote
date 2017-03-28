package com.bytesforge.linkasanote.laano;

import com.bytesforge.linkasanote.FragmentScoped;
import com.bytesforge.linkasanote.laano.favorites.FavoritesPresenterModule;
import com.bytesforge.linkasanote.laano.links.LinksPresenterModule;
import com.bytesforge.linkasanote.laano.notes.NotesPresenterModule;

import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {
        LinksPresenterModule.class,
        FavoritesPresenterModule.class,
        NotesPresenterModule.class,
        LaanoActionBarManagerModule.class})
public interface LaanoComponent {

    void inject(LaanoActivity activity);
}
