package com.bytesforge.linkasanote.laano.links;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class LinksPresenterModule {

    private final Context context; // NOTE: Activity context
    private final LinksContract.View view;

    public LinksPresenterModule(Context context, LinksContract.View view) {
        this.context = context;
        this.view = view;
    }

    @Provides
    public LinksContract.View provideLinksContractView() {
        return view;
    }

    @Provides
    public LinksContract.ViewModel provideLinksContractViewModel() {
        return new LinksViewModel(context);
    }
}
