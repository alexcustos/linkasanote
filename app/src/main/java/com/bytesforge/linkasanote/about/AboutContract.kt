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
import com.bytesforge.linkasanote.BasePresenter
import com.bytesforge.linkasanote.BaseView

interface AboutContract {
    interface View : BaseView<Presenter?> {
        fun setViewModel(viewModel: ViewModel)
        val isActive: Boolean
        fun showGooglePlay()
        fun showLicenseTermsAlertDialog(licenseText: String)
    }

    interface ViewModel : BaseView<Presenter?> {
        fun setInstanceState(savedInstanceState: Bundle?)
        fun saveInstanceState(outState: Bundle)
        fun applyInstanceState(state: Bundle)
        val defaultInstanceState: Bundle
        fun showLaunchGooglePlayErrorSnackbar()
    }

    interface Presenter : BasePresenter {
        fun onLaunchGooglePlay()
        fun onLicenseTermsGplV3Click()
        fun onLicenseTermsApacheV2Click()
        fun onLicenseTermsMitClick()
    }
}