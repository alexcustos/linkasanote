package com.bytesforge.linkasanote.manageaccounts;

import com.bytesforge.linkasanote.FragmentScoped;

import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {ManageAccountsPresenterModule.class})
public interface ManageAccountsComponent {

    void inject(ManageAccountsActivity manageAccountsActivity);
}
