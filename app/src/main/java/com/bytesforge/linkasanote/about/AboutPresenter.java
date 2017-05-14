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
