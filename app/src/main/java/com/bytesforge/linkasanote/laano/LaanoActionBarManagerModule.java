package com.bytesforge.linkasanote.laano;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;

import dagger.Module;
import dagger.Provides;

@Module
public class LaanoActionBarManagerModule {

    private final LaanoActionBarManager laanoActionBarManager;

    public LaanoActionBarManagerModule(
            Context context, ActionBar actionBar, TabLayout tabLayout,
            LaanoFragmentPagerAdapter pagerAdapter) {
        laanoActionBarManager = new LaanoActionBarManager(
                context, actionBar, tabLayout, pagerAdapter);
    }

    @Provides
    public LaanoActionBarManager provideLaanoActionBarManager() {
        return laanoActionBarManager;
    }
}
