package com.bytesforge.linkasanote.laano;

import android.support.design.widget.TabLayout;
import android.view.Menu;

import com.bytesforge.linkasanote.settings.Settings;

import dagger.Module;
import dagger.Provides;

@Module
public class LaanoUiManagerModule {

    private final LaanoActivity laanoActivity;
    private final TabLayout tabLayout;
    private final Menu drawerMenu;
    private final LaanoDrawerHeaderViewModel headerViewModel;
    private final LaanoFragmentPagerAdapter pagerAdapter;

    private LaanoUiManager laanoUiManager;

    public LaanoUiManagerModule(
            LaanoActivity laanoActivity, TabLayout tabLayout, Menu drawerMenu,
            LaanoDrawerHeaderViewModel headerViewModel, LaanoFragmentPagerAdapter pagerAdapter) {
        this.laanoActivity = laanoActivity;
        this.tabLayout = tabLayout;
        this.drawerMenu = drawerMenu;
        this.headerViewModel = headerViewModel;
        this.pagerAdapter = pagerAdapter;
    }

    @Provides
    public LaanoUiManager provideLaanoUiManager(Settings settings) {
        if (laanoUiManager == null) {
            laanoUiManager = new LaanoUiManager(
                    laanoActivity, settings, tabLayout, drawerMenu, headerViewModel, pagerAdapter);
        }
        return laanoUiManager;
    }
}
