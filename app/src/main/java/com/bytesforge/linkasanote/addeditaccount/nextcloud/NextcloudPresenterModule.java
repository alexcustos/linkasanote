package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import dagger.Module;
import dagger.Provides;

@Module
public class NextcloudPresenterModule {

    private final NextcloudContract.View view;
    private final NextcloudContract.ViewModel viewModel;

    public NextcloudPresenterModule(
            NextcloudContract.View view,
            NextcloudContract.ViewModel viewModel) {
        this.view = view;
        this.viewModel = viewModel;
    }

    @Provides
    NextcloudContract.View provideNextcloudContractView() {
        return view;
    }

    @Provides
    NextcloudContract.ViewModel provideNextcloudContractViewModel() {
        return viewModel;
    }
}
