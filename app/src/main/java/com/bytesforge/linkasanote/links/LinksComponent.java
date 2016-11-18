package com.bytesforge.linkasanote.links;

import com.bytesforge.linkasanote.utils.FragmentScoped;

import dagger.Component;

@FragmentScoped
@Component(modules = LinksPresenterModule.class)
public interface LinksComponent {
    void inject(LinksActivity activity);
}
