package com.bytesforge.linkasanote.laano.links;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.BaseFragment;
import com.bytesforge.linkasanote.databinding.FragmentLaanoLinksBinding;

import static com.google.common.base.Preconditions.checkNotNull;

public class LinksFragment extends BaseFragment implements LinksContract.View {

    private LinksContract.Presenter presenter;

    public static LinksFragment newInstance() {
        return new LinksFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        FragmentLaanoLinksBinding binding =
            FragmentLaanoLinksBinding.inflate(inflater, container, false);
        binding.textView.setText(getTitle());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.unsubscribe();
    }

    @Override
    public void setPresenter(@NonNull LinksContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }
}
