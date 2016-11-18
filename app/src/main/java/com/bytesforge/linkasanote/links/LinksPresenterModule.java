package com.bytesforge.linkasanote.links;

import dagger.Module;
import dagger.Provides;

@Module
public class LinksPresenterModule {

    private final LinksContract.View view;

    public LinksPresenterModule(LinksContract.View view) {
        this.view = view;
    }

    @Provides
    LinksContract.View provideLinksContractView() {
        return view;
    }
}
