/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
