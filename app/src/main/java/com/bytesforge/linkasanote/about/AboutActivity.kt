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
package com.bytesforge.linkasanote.about

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bytesforge.linkasanote.LaanoApplication
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.databinding.ActivityAboutBinding
import com.bytesforge.linkasanote.utils.ActivityUtils
import javax.inject.Inject

class AboutActivity : AppCompatActivity() {
    @JvmField
    @Inject
    var presenter: AboutPresenter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App links: Tools -> App Links Assistant Step 2: when data handling is required
        val binding: ActivityAboutBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_about)
        // Toolbar
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(R.string.actionbar_title_about)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
        }
        // Fragment
        var fragment = supportFragmentManager
            .findFragmentById(R.id.content_frame) as AboutFragment?
        if (fragment == null) {
            fragment = AboutFragment.Companion.newInstance()
            ActivityUtils.addFragmentToActivity(
                supportFragmentManager, fragment, R.id.content_frame
            )
        }
        // Presenter
        val application = application as LaanoApplication
        application.applicationComponent
            .getAboutComponent(AboutPresenterModule(this, fragment))
            .inject(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}