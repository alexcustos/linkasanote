package com.bytesforge.linkasanote.about;

import javax.inject.Inject;

public final class AboutPresenter implements AboutContract.Presenter {

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
        view.showGplV3TermsAlertDialog();
    }

    @Override
    public void onLicenseTermsApacheV2Click() {
        view.showApacheV2TermsAlertDialog();
    }

    @Override
    public void onLicenseTermsMitClick() {
        view.showMitTermsAlertDialog();
    }
}
