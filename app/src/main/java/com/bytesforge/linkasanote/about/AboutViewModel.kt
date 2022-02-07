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

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.text.format.DateFormat
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableField
import com.bytesforge.linkasanote.BR
import com.bytesforge.linkasanote.BuildConfig
import com.bytesforge.linkasanote.R
import com.google.android.material.snackbar.Snackbar
import com.google.common.base.Preconditions

class AboutViewModel(context: Context) : BaseObservable(), AboutContract.ViewModel {
    @JvmField
    val appVersionText = ObservableField<String?>()
    @JvmField
    val appCopyrightText = ObservableField<String?>()
    private val resources: Resources

    enum class SnackbarId {
        ABOUT_LAUNCH_GOOGLE_PLAY_ERROR
    }

    @JvmField
    @Bindable
    var snackbarId: SnackbarId? = null
    override fun setPresenter(presenter: AboutContract.Presenter) {}
    override fun setInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            applyInstanceState(defaultInstanceState)
        } else {
            applyInstanceState(savedInstanceState)
        }
    }

    override fun saveInstanceState(outState: Bundle) {
        Preconditions.checkNotNull(outState)
        outState.putString(STATE_APP_VERSION_TEXT, appVersionText.get())
        outState.putString(STATE_APP_COPYRIGHT_TEXT, appCopyrightText.get())
    }

    override fun applyInstanceState(state: Bundle) {
        Preconditions.checkNotNull(state)
        appVersionText.set(state.getString(STATE_APP_VERSION_TEXT))
        appCopyrightText.set(state.getString(STATE_APP_COPYRIGHT_TEXT))
    }

    override val defaultInstanceState: Bundle
        get() {
            val defaultState = Bundle()
            var buildYear = DateFormat.format("yyyy", BuildConfig.BUILD_TIMESTAMP).toString()
            val createdYear = resources.getString(R.string.app_was_created_year)
            if (buildYear != createdYear) {
                buildYear = "$createdYear - $buildYear"
            }
            defaultState.putString(
                STATE_APP_VERSION_TEXT,
                resources.getString(R.string.about_app_version, BuildConfig.VERSION_NAME)
            )
            defaultState.putString(
                STATE_APP_COPYRIGHT_TEXT, resources.getString(
                    R.string.about_app_copyright,
                    buildYear,
                    resources.getString(R.string.app_author)
                )
            )
            return defaultState
        }

    // Snackbar
    override fun showLaunchGooglePlayErrorSnackbar() {
        snackbarId = SnackbarId.ABOUT_LAUNCH_GOOGLE_PLAY_ERROR
        notifyPropertyChanged(BR.snackbarId)
    }

    companion object {
        const val STATE_APP_VERSION_TEXT = "APP_VERSION_TEXT"
        const val STATE_APP_COPYRIGHT_TEXT = "APP_COPYRIGHT_TEXT"
        @JvmStatic
        @BindingAdapter("snackbarId")
        fun showSnackbar(view: CoordinatorLayout?, snackbarId: SnackbarId?) {
            if (snackbarId == null) return
            when (snackbarId) {
                SnackbarId.ABOUT_LAUNCH_GOOGLE_PLAY_ERROR -> Snackbar.make(
                    view!!,
                    R.string.about_launch_google_play_error,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    init {
        resources = Preconditions.checkNotNull(context).resources
    }
}