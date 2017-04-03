package com.bytesforge.linkasanote.about;

import com.bytesforge.linkasanote.FragmentScoped;

import dagger.Subcomponent;

@FragmentScoped
@Subcomponent(modules = {AboutPresenterModule.class})
public interface AboutComponent {

    void inject(AboutActivity aboutActivity);
}
