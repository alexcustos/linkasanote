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
package com.bytesforge.linkasanote.settings

import android.content.SharedPreferences
import com.bytesforge.linkasanote.data.Favorite
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.sync.files.JsonFile
import android.accounts.Account
import android.content.ContentResolver
import com.bytesforge.linkasanote.data.source.local.LocalContract
import android.content.PeriodicSync
import android.os.Bundle
import com.bytesforge.linkasanote.sync.SyncAdapter
import com.bytesforge.linkasanote.laano.FilterType
import com.bytesforge.linkasanote.laano.links.LinksPresenter
import com.bytesforge.linkasanote.laano.favorites.FavoritesPresenter
import com.bytesforge.linkasanote.laano.notes.NotesPresenter
import android.content.SharedPreferences.Editor
import dagger.Provides
import javax.inject.Singleton
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.bytesforge.linkasanote.settings.SettingsActivity
import com.bytesforge.linkasanote.settings.SettingsFragment
import com.bytesforge.linkasanote.utils.ActivityUtils
import androidx.core.app.NavUtils
import androidx.preference.PreferenceFragmentCompat
import javax.inject.Inject
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider
import androidx.fragment.app.FragmentActivity
import com.bytesforge.linkasanote.LaanoApplication
import com.bytesforge.linkasanote.ApplicationBackup
import android.widget.Toast
import com.bytesforge.linkasanote.utils.CommonUtils
import com.google.android.material.snackbar.Snackbar
import com.bytesforge.linkasanote.data.source.local.DatabaseHelper
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startIntent = intent
        val account = startIntent.getParcelableExtra<Account>(EXTRA_ACCOUNT)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setTitle(R.string.actionbar_title_settings)
        }
        val fragment: SettingsFragment = SettingsFragment.Companion.newInstance(account)
        ActivityUtils.replaceFragmentInActivity(
            supportFragmentManager, fragment, android.R.id.content
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_ACCOUNT = "ACCOUNT"
    }
}