package com.bytesforge.linkasanote.laano.links.conflictresolution;

import android.content.Context;

import com.bytesforge.linkasanote.laano.links.LinkId;

import dagger.Module;
import dagger.Provides;

@Module
public class LinksConflictResolutionPresenterModule {

    private final Context context;
    private final LinksConflictResolutionContract.View view;
    private String linkId;

    public LinksConflictResolutionPresenterModule(
            Context context, LinksConflictResolutionContract.View view, String linkId) {
        this.context = context;
        this.view = view;
        this.linkId = linkId;
    }

    @Provides
    public LinksConflictResolutionContract.View provideLinksConflictResolutionContractView() {
        return view;
    }

    @Provides
    public LinksConflictResolutionContract.ViewModel provideLinksConflictResolutionContractViewModel() {
        return new LinksConflictResolutionViewModel(context);
    }

    @Provides
    @LinkId
    public String provideLinkId() {
        return linkId;
    }
}
