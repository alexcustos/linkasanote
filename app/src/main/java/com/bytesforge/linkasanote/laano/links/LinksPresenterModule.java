package com.bytesforge.linkasanote.laano.links;

import dagger.Module;
import dagger.Provides;

@Module
public class LinksPresenterModule {

    private final LinksContract.View view;

    public LinksPresenterModule(LinksContract.View view) {
        this.view = view;
    }

    @Provides
    public LinksContract.View provideLinksContractView() {
        return view;
    }
}
