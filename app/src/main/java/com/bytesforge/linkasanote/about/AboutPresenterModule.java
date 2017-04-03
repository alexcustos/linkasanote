package com.bytesforge.linkasanote.about;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class AboutPresenterModule {

    private final Context context;
    private AboutContract.View view;

    public AboutPresenterModule(Context context, AboutContract.View view) {
        this.context = context;
        this.view = view;
    }

    @Provides
    public AboutContract.View provideAboutContractView() {
        return view;
    }

    @Provides
    public AboutContract.ViewModel provideAboutContractViewModel() {
        return new AboutViewModel(context);
    }
}
