package com.bytesforge.linkasanote.laano.links.addeditlink;

import android.content.Context;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.laano.links.LinkId;

import dagger.Module;
import dagger.Provides;

@Module
public class AddEditLinkPresenterModule {

    private final Context context;
    private final AddEditLinkContract.View view;
    private String linkId;

    public AddEditLinkPresenterModule(
            Context context, AddEditLinkContract.View view, @Nullable String linkId) {
        this.context = context;
        this.view = view;
        this.linkId = linkId;
    }

    @Provides
    public AddEditLinkContract.View provideAddEditLinkContractView() {
        return view;
    }

    @Provides
    public AddEditLinkContract.ViewModel provideAddEditLinkContractViewModel() {
        return new AddEditLinkViewModel(context);
    }

    @Provides
    @Nullable
    @LinkId
    public String provideLinkId() {
        return linkId;
    }
}
