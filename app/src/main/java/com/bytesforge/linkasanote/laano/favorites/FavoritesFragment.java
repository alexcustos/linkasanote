package com.bytesforge.linkasanote.laano.favorites;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.BaseFragment;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.addeditfavorite.AddEditFavoriteActivity;
import com.bytesforge.linkasanote.addeditfavorite.AddEditFavoriteFragment;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.databinding.FragmentLaanoFavoritesBinding;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class FavoritesFragment extends BaseFragment implements FavoritesContract.View {

    public static final int REQUEST_ADD_FAVORITE = 1;
    public static final int REQUEST_EDIT_FAVORITE = 2;

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
        if (viewModel.isActionMode()) {
            enableActionMode();
        }
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_favorites, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_favorite_action_mode:
                enableActionMode();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.loadInstanceState(outState);
    }

    @Override
    public void showAddFavorite() {
        Intent intent = new Intent(getContext(), AddEditFavoriteActivity.class);
        startActivityForResult(intent, REQUEST_ADD_FAVORITE);
    }

    @Override
    public void showEditFavorite(@NonNull String favoriteId) {
        Intent intent = new Intent(getContext(), AddEditFavoriteActivity.class);
        intent.putExtra(AddEditFavoriteFragment.ARGUMENT_EDIT_FAVORITE_ID, favoriteId);
        startActivityForResult(intent, REQUEST_EDIT_FAVORITE);
    }

    @Override
    public void showFavorites(List<Favorite> favorites) {
        adapter.swapItems(favorites);
        viewModel.setFavoriteListSize(favorites.size());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ADD_FAVORITE:
                break;
            case REQUEST_EDIT_FAVORITE:
                if (resultCode == Activity.RESULT_OK) {
                }
                break;
            default:
                throw new IllegalStateException("The result received from the unexpected activity");
        }
    }

    private void setupFavoritesRecyclerView(RecyclerView rvFavorites) {
        List<Favorite> favorites = new ArrayList<>(0);
        adapter = new FavoritesAdapter(favorites, presenter, (FavoritesViewModel) viewModel);
        rvFavorites.setAdapter(adapter);
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void enableActionMode() {
        if (!viewModel.isActionMode()) {
            viewModel.enableActionMode();
        }
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(
                    new FavoritesActionModeCallback());
        }
        updateActionModeTitle();
    }

    @Override
    public void disableActionMode() {
        if (actionMode != null) {
            actionMode.finish(); // NOTE: will call destroyActionMode
        }
    }

    private void destroyActionMode() {
        if (viewModel.isActionMode()) {
            SparseBooleanArray selected = viewModel.getSelectedIds().clone();
            viewModel.removeSelection();
            for (int i = 0; i < selected.size(); i++) {
                if (selected.valueAt(i)) {
                    adapter.notifyItemChanged(selected.keyAt(i));
                }
            }
            viewModel.disableActionMode();
        }
        if (actionMode != null) {
            actionMode = null;
        }
    }

    @Override
    public void selectionChanged(int position) {
        adapter.notifyItemChanged(position);
        updateActionModeTitle();
    }

    private void updateActionModeTitle() {
        if (actionMode != null) {
            actionMode.setTitle(getContext().getResources().getString(
                    R.string.laano_favorites_action_mode_selected, viewModel.getSelectedCount()));
            if (adapter.getItemCount() <= 0) {
                disableActionMode();
            }
        } // if
    }

    @Override
    public Favorite removeFavorite(int position) {
        Favorite favorite = adapter.removeItem(position);
        selectionChanged(position);
        viewModel.setFavoriteListSize(adapter.getItemCount());

        return favorite;
    }

    public class FavoritesActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.action_mode_favorites, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.findItem(R.id.favorites_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.favorites_delete:
                    presenter.onDeleteClick();
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Unknown ActionMode item [" + item.getItemId() + "]");
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            destroyActionMode();
        }
    }
}
