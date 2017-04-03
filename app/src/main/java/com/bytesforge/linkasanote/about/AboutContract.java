package com.bytesforge.linkasanote.about;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;

public interface AboutContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull AboutContract.ViewModel viewModel);
        boolean isActive();

        void showGplV3TermsAlertDialog();
        void showApacheV2TermsAlertDialog();
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(Bundle outState);
        void applyInstanceState(@NonNull Bundle state);
        Bundle getDefaultInstanceState();
    }

    interface Presenter extends BasePresenter {

        void onLicenseTermsGplV3Click();
        void onLicenseTermsApacheV2Click();
    }
}
