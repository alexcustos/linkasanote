package com.bytesforge.linkasanote.manageaccounts;

import com.bytesforge.linkasanote.ApplicationComponent;
import com.bytesforge.linkasanote.FragmentScoped;

import dagger.Component;

@FragmentScoped
@Component(dependencies = {ApplicationComponent.class},
        modules = {ManageAccountsPresenterModule.class})
public interface ManageAccountsComponent {

    void inject(ManageAccountsActivity manageAccountsActivity);
}
