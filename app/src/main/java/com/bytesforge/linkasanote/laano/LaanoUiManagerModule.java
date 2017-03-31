package com.bytesforge.linkasanote.laano;

import android.support.design.widget.TabLayout;
import android.view.Menu;

import dagger.Module;
import dagger.Provides;

@Module
public class LaanoUiManagerModule {

    private final LaanoUiManager laanoUiManager;

    public LaanoUiManagerModule(
            LaanoActivity laanoActivity, TabLayout tabLayout, Menu drawerMenu,
            LaanoDrawerHeaderViewModel headerViewModel, LaanoFragmentPagerAdapter pagerAdapter) {
        laanoUiManager = new LaanoUiManager(
                laanoActivity, tabLayout, drawerMenu, headerViewModel, pagerAdapter);
    }

    @Provides
    public LaanoUiManager provideLaanoUiManager() {
        return laanoUiManager;
    }
}
