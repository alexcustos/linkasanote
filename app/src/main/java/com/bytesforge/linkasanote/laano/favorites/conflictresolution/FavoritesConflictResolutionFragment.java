package com.bytesforge.linkasanote.laano.favorites.conflictresolution;

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.FragmentFavoriteConflictResolutionBinding;

import static com.google.common.base.Preconditions.checkNotNull;

public class FavoritesConflictResolutionFragment extends Fragment implements
        FavoritesConflictResolutionContract.View {

    public static final String ARGUMENT_FAVORITE_ID = "FAVORITE_ID";

    FavoritesConflictResolutionContract.Presenter presenter;
    FavoritesConflictResolutionContract.ViewModel viewModel;

    public static FavoritesConflictResolutionFragment newInstance() {
        return new FavoritesConflictResolutionFragment();
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
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void setPresenter(@NonNull FavoritesConflictResolutionContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setViewModel(@NonNull FavoritesConflictResolutionContract.ViewModel viewModel) {
        this.viewModel = checkNotNull(viewModel);
    }

    @Override
    public void finishActivity() {
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    @Override
    public void cancelActivity() {
        getActivity().setResult(FavoritesConflictResolutionActivity.RESULT_FAILED);
        getActivity().finish();
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        FragmentFavoriteConflictResolutionBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_favorite_conflict_resolution, container, false);
        viewModel.setInstanceState(savedInstanceState);
        binding.setPresenter(presenter);
        binding.setViewModel((FavoritesConflictResolutionViewModel) viewModel);
        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.saveInstanceState(outState);
    }
}
