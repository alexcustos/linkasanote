package com.bytesforge.linkasanote.links;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static com.google.common.base.Preconditions.checkNotNull;

public class LinksFragment extends Fragment implements LinksContract.View {

    private LinksContract.Presenter presenter;

    public LinksFragment() {
        // Requires empty public constructor
    }

    public static LinksFragment newInstance() {
        return new LinksFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void setPresenter(LinksContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }
}
