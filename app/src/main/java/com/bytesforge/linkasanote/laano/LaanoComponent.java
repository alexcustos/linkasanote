package com.bytesforge.linkasanote.laano;

import com.bytesforge.linkasanote.laano.favorites.FavoritesPresenterModule;
import com.bytesforge.linkasanote.laano.links.LinksPresenterModule;
import com.bytesforge.linkasanote.laano.notes.NotesPresenterModule;
import com.bytesforge.linkasanote.utils.FragmentScoped;

import dagger.Component;

@FragmentScoped
@Component(modules = {
        LinksPresenterModule.class,
        FavoritesPresenterModule.class,
        NotesPresenterModule.class})
public interface LaanoComponent {
    void inject(LaanoActivity activity);
}
