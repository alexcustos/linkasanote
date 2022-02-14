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

import com.bytesforge.linkasanote.data.Favorite
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.sync.files.JsonFile
import android.accounts.Account
import android.content.*
import com.bytesforge.linkasanote.data.source.local.LocalContract
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
import android.preference.PreferenceManager
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import dagger.Module

@Module
class SettingsModule {
    @Provides
    @Singleton
    fun provideSharedPreferences(context: Context?): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun provideSettings(context: Context, sharedPreferences: SharedPreferences): Settings {
        return Settings(context, sharedPreferences)
    }
}