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

import javax.inject.Inject;

public final class AboutPresenter implements AboutContract.Presenter {

    private static final String TAG = AboutPresenter.class.getSimpleName();

    private final AboutContract.View view;
    private final AboutContract.ViewModel viewModel;

    @Inject
    public AboutPresenter(AboutContract.View view, AboutContract.ViewModel viewModel) {
        this.view = view;
        this.viewModel = viewModel;
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
        viewModel.setPresenter(this);
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void unsubscribe() {
    }

    @Override
    public void onLaunchGooglePlay() {
        view.showGooglePlay();
    }

    @Override
    public void onLicenseTermsGplV3Click() {
        view.showLicenseTermsAlertDialog("gpl-3.0.en.html");
    }

    @Override
    public void onLicenseTermsApacheV2Click() {
        view.showLicenseTermsAlertDialog("LICENSE-2.0.html");
    }

    @Override
    public void onLicenseTermsMitClick() {
        view.showLicenseTermsAlertDialog("MIT.html");
    }
}
