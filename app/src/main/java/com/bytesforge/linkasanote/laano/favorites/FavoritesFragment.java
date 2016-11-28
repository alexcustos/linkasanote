package com.bytesforge.linkasanote.laano.favorites;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.BaseFragment;
import com.bytesforge.linkasanote.databinding.FragmentLaanoFavoritesBinding;

import static com.google.common.base.Preconditions.checkNotNull;

public class FavoritesFragment extends BaseFragment implements FavoritesContract.View {

    private FavoritesContract.Presenter presenter;

    public FavoritesFragment() {
        // Requires empty public constructor
    }

    public static FavoritesFragment newInstance() {
        return new FavoritesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        FragmentLaanoFavoritesBinding binding =
                FragmentLaanoFavoritesBinding.inflate(inflater, container, false);
        binding.textView.setText(getTitle());

        return binding.getRoot();
    }

    @Override
    public void setPresenter(FavoritesContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }
}
