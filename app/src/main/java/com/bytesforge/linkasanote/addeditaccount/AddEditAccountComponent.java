package com.bytesforge.linkasanote.addeditaccount;

import com.bytesforge.linkasanote.ApplicationComponent;
import com.bytesforge.linkasanote.FragmentScoped;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudPresenterModule;

import dagger.Component;

@FragmentScoped
@Component(dependencies = {ApplicationComponent.class},
        modules = {NextcloudPresenterModule.class})
public interface AddEditAccountComponent {

    void inject(AddEditAccountActivity addEditAccountActivity);
}
