package com.bytesforge.linkasanote.addeditaccount;

import com.bytesforge.linkasanote.FragmentScoped;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudPresenterModule;

import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {NextcloudPresenterModule.class})
public interface AddEditAccountComponent {

    void inject(AddEditAccountActivity addEditAccountActivity);
}
