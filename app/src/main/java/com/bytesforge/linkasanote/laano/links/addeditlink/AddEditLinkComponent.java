package com.bytesforge.linkasanote.laano.links.addeditlink;

import com.bytesforge.linkasanote.FragmentScoped;

import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {AddEditLinkPresenterModule.class})
public interface AddEditLinkComponent {

    void inject(AddEditLinkActivity addEditLinkActivity);
}
