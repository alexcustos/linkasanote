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

package com.bytesforge.linkasanote.about;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.ActivityAboutBinding;
import com.bytesforge.linkasanote.utils.ActivityUtils;

import javax.inject.Inject;

public class AboutActivity extends AppCompatActivity {

    @Inject
    AboutPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // App links: Tools -> App Links Assistant Step 2: when data handling is required
        ActivityAboutBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_about);
        // Toolbar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.actionbar_title_about);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        // Fragment
        AboutFragment fragment = (AboutFragment) getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        if (fragment == null) {
            fragment = AboutFragment.newInstance();
            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), fragment, R.id.content_frame);
        }
        // Presenter
        LaanoApplication application = (LaanoApplication) getApplication();
        application.getApplicationComponent()
                .getAboutComponent(new AboutPresenterModule(this, fragment))
                .inject(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
