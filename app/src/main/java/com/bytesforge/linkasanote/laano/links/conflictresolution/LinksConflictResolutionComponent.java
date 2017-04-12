package com.bytesforge.linkasanote.laano.links.conflictresolution;

import com.bytesforge.linkasanote.FragmentScoped;

import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {LinksConflictResolutionPresenterModule.class})
public interface LinksConflictResolutionComponent {

    void inject(LinksConflictResolutionDialog linksConflictResolutionDialog);
}
