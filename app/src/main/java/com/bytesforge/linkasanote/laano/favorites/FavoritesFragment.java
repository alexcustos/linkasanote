package com.bytesforge.linkasanote.laano.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.BaseFragment;
import com.bytesforge.linkasanote.addeditfavorite.AddEditFavoriteActivity;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.databinding.FragmentLaanoFavoritesBinding;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class FavoritesFragment extends BaseFragment implements FavoritesContract.View {

    private FavoritesContract.Presenter presenter;
    private FavoritesContract.ViewModel viewModel;
    private FavoritesAdapter adapter;
    private ActionMode actionMode;

    public static FavoritesFragment newInstance() {
        return new FavoritesFragment();
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
    public void setPresenter(@NonNull FavoritesContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setViewModel(@NonNull FavoritesContract.ViewModel viewModel) {
        this.viewModel = checkNotNull(viewModel);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        FragmentLaanoFavoritesBinding binding =
                FragmentLaanoFavoritesBinding.inflate(inflater, container, false);
        viewModel.setInstanceState(savedInstanceState);
        binding.setViewModel((FavoritesViewModel) viewModel);
        // RecyclerView
        if (binding.rvFavorites != null) {
            setupFavoritesRecyclerView(binding.rvFavorites);
        }
        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.loadInstanceState(outState);
    }

    @Override
    public void showAddFavorite() {
        Intent intent = new Intent(getContext(), AddEditFavoriteActivity.class);
        startActivityForResult(intent, AddEditFavoriteActivity.REQUEST_ADD_FAVORITE);
    }

    @Override
    public void showFavorites(List<Favorite> favorites) {
        adapter.swapItems(favorites);
        viewModel.setFavoriteListSize(favorites.size());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setupFavoritesRecyclerView(RecyclerView rvFavorites) {
        List<Favorite> favorites = new ArrayList<>(0);
        adapter = new FavoritesAdapter(favorites, presenter, (FavoritesViewModel) viewModel);
        rvFavorites.setAdapter(adapter);
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void onFavoriteSelected(int position) {

    }
}
