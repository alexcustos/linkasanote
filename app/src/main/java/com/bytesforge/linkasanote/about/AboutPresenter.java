package com.bytesforge.linkasanote.about;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

public final class AboutPresenter implements AboutContract.Presenter {

    private static final String TAG = AboutPresenter.class.getSimpleName();

    private final AboutContract.View view;
    private final AboutContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;

    @Inject
    public AboutPresenter(
            AboutContract.View view, AboutContract.ViewModel viewModel,
            BaseSchedulerProvider schedulerProvider) {
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
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
        showLicenseTerms("gpl-3.0.en.html");
    }

    @Override
    public void onLicenseTermsApacheV2Click() {
        showLicenseTerms("LICENSE-2.0.html");
    }

    @Override
    public void onLicenseTermsMitClick() {
        showLicenseTerms("MIT.html");
    }

    private void showLicenseTerms(@NonNull String assetName) {
        checkNotNull(assetName);
        viewModel.showProgressOverlay();
        Single.fromCallable(() -> view.getLicenseText(assetName))
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doFinally(viewModel::hideProgressOverlay)
                .subscribe(
                        view::showLicenseTermsAlertDialog,
                        throwable -> CommonUtils.logStackTrace(TAG, throwable));
    }
}
