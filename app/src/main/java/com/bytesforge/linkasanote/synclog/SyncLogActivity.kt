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
package com.bytesforge.linkasanote.synclog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bytesforge.linkasanote.LaanoApplication
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.databinding.ActivitySyncLogBinding
import com.bytesforge.linkasanote.settings.Settings
import com.bytesforge.linkasanote.utils.ActivityUtils
import javax.inject.Inject

class SyncLogActivity : AppCompatActivity() {
    @JvmField
    @Inject
    var presenter: SyncLogPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivitySyncLogBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_sync_log)
        // Toolbar
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar

        supportActionBar?.apply {
            title = resources.getQuantityString(
                R.plurals.actionbar_title_sync_log,
                Settings.GLOBAL_SYNC_LOG_KEEPING_PERIOD_DAYS,
                Settings.GLOBAL_SYNC_LOG_KEEPING_PERIOD_DAYS
            )
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        // Fragment
        var fragment = supportFragmentManager
            .findFragmentById(R.id.content_frame) as SyncLogFragment?
        if (fragment == null) {
            fragment = SyncLogFragment.Companion.newInstance()
            ActivityUtils.addFragmentToActivity(
                supportFragmentManager, fragment, R.id.content_frame
            )
        }
        // Presenter
        val application = application as LaanoApplication
        application.applicationComponent
            .getSyncLogComponent(SyncLogPresenterModule(this, fragment))
            .inject(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}