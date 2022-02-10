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

import javax.inject.Inject

class AboutPresenter @Inject constructor(
    private val view: AboutContract.View,
    private val viewModel: AboutContract.ViewModel
) : AboutContract.Presenter {
    @Inject
    fun setupView() {
        view.setPresenter(this)
        view.setViewModel(viewModel)
        viewModel.setPresenter(this)
    }

    override fun subscribe() {}
    override fun unsubscribe() {}
    override fun onLaunchGooglePlay() {
        view.showGooglePlay()
    }

    override fun onLicenseTermsGplV3Click() {
        view.showLicenseTermsAlertDialog("gpl-3.0.en.html")
    }

    override fun onLicenseTermsApacheV2Click() {
        view.showLicenseTermsAlertDialog("LICENSE-2.0.html")
    }

    override fun onLicenseTermsMitClick() {
        view.showLicenseTermsAlertDialog("MIT.html")
    }

    companion object {
        private val TAG = AboutPresenter::class.java.simpleName
    }
}