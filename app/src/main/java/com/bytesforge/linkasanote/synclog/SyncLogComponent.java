package com.bytesforge.linkasanote.synclog;

import com.bytesforge.linkasanote.FragmentScoped;

import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {SyncLogPresenterModule.class})
public interface SyncLogComponent {

    void inject(SyncLogActivity syncLogActivity);
}
