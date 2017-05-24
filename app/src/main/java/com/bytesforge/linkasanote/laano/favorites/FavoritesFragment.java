package com.bytesforge.linkasanote.laano.favorites;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.databinding.FragmentLaanoFavoritesBinding;
import com.bytesforge.linkasanote.laano.BaseItemFragment;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.favorites.addeditfavorite.AddEditFavoriteActivity;
import com.bytesforge.linkasanote.laano.favorites.addeditfavorite.AddEditFavoriteFragment;
import com.bytesforge.linkasanote.laano.favorites.conflictresolution.FavoritesConflictResolutionDialog;
import com.bytesforge.linkasanote.settings.Settings;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class FavoritesFragment extends BaseItemFragment implements FavoritesContract.View {

    public static final int REQUEST_ADD_FAVORITE = 1;
    public static final int REQUEST_EDIT_FAVORITE = 2;
    public static final int REQUEST_FAVORITE_CONFLICT_RESOLUTION = 3;

    private FavoritesContract.Presenter presenter;
    private FavoritesContract.ViewModel viewModel;
    private FavoritesAdapter adapter;
    private ActionMode actionMode;
    private LinearLayoutManager rvLayoutManager;
    private FavoritesActionModeCallback favoritesActionModeCallback;

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
        setupFavoritesRecyclerView(binding.rvFavorites);
        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_favorites, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.toolbar_favorites_search);
        setSearchMenuItem(searchMenuItem);
    }

    private void setSearchMenuItem(MenuItem item) {
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        MenuItemCompat.setOnActionExpandListener(
                item, new MenuItemCompat.OnActionExpandListener() {

                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        getActivity().supportInvalidateOptionsMenu();
                        viewModel.setSearchText(null);
                        presenter.setFilterIsChanged(true);
                        presenter.loadFavorites(false);
                        return true;
                    }
                });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.setSearchText(query);
                presenter.setFilterIsChanged(true);
                presenter.loadFavorites(false);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
        if (viewModel.getSearchText() != null) {
            item.expandActionView();
            searchView.setQuery(viewModel.getSearchText(), false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_favorites_filter:
                showFilteringPopupMenu();
                break;
            case R.id.toolbar_favorites_action_mode:
                enableActionMode();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.saveInstanceState(outState);
    }

    @Override
    public void startAddFavoriteActivity() {
        Intent intent = new Intent(getContext(), AddEditFavoriteActivity.class);
        startActivityForResult(intent, REQUEST_ADD_FAVORITE);
    }

    @Override
    public void showEditFavorite(@NonNull String favoriteId) {
        Intent intent = new Intent(getContext(), AddEditFavoriteActivity.class);
        intent.putExtra(AddEditFavoriteFragment.ARGUMENT_FAVORITE_ID, favoriteId);
        startActivityForResult(intent, REQUEST_EDIT_FAVORITE);
    }

    @Override
    public void showFavorites(@NonNull List<Favorite> favorites) {
        checkNotNull(favorites);
        adapter.swapItems(favorites);

        viewModel.setListSize(favorites.size());
        if (viewModel.isActionMode()) {
            enableActionMode();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ADD_FAVORITE:
                if (resultCode == Activity.RESULT_OK) {
                    presenter.loadFavorites(false);
                    //viewModel.showSaveSuccessSnackbar();
                    String favoriteId = data.getStringExtra(AddEditFavoriteFragment.ARGUMENT_FAVORITE_ID);
                    presenter.syncSavedFavorite(favoriteId);
                }
                break;
            case REQUEST_EDIT_FAVORITE:
                if (resultCode == Activity.RESULT_OK) {
                    presenter.loadFavorites(false);
                    //viewModel.showSaveSuccessSnackbar();
                    String favoriteId = data.getStringExtra(AddEditFavoriteFragment.ARGUMENT_FAVORITE_ID);
                    presenter.syncSavedFavorite(favoriteId);
                }
                break;
            case REQUEST_FAVORITE_CONFLICT_RESOLUTION:
                presenter.updateTabNormalState();
                // NOTE: force reload because of conflict resolution is a dialog
                presenter.loadFavorites(false);
                if (resultCode == FavoritesConflictResolutionDialog.RESULT_OK) {
                    presenter.updateSyncStatus();
                    //viewModel.showConflictResolutionSuccessfulSnackbar();
                    Toast.makeText(getContext(), R.string.toast_conflict_resolved,
                            Toast.LENGTH_SHORT).show();
                } else if (resultCode == FavoritesConflictResolutionDialog.RESULT_FAILED){
                    viewModel.showConflictResolutionErrorSnackbar();
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
        rvLayoutManager = new LinearLayoutManager(getContext());
        rvFavorites.setLayoutManager(rvLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                rvFavorites.getContext(), rvLayoutManager.getOrientation());
        rvFavorites.addItemDecoration(dividerItemDecoration);
    }

    private void showFilteringPopupMenu() {
        PopupMenu popupMenu = new PopupMenu(
                getContext(), getActivity().findViewById(R.id.toolbar_favorites_filter));
        Menu menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.filter, menu);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.filter_all:
                    presenter.setFilterType(FilterType.ALL);
                    break;
                case R.id.filter_conflicted:
                    presenter.setFilterType(FilterType.CONFLICTED);
                    break;
            }
            presenter.loadFavorites(false);
            return true;
        });
        MenuItem filterLinkMenuItem = menu.findItem(R.id.filter_link);
        filterLinkMenuItem.setVisible(false);

        MenuItem filterFavoriteMenuItem = menu.findItem(R.id.filter_favorite);
        filterFavoriteMenuItem.setVisible(false);

        MenuItem filterNoteMenuItem = menu.findItem(R.id.filter_note);
        filterNoteMenuItem.setVisible(false);

        MenuItem filterNoTagsMenuItem = menu.findItem(R.id.filter_no_tags);
        filterNoTagsMenuItem.setVisible(false);

        MenuItem filterUnboundMenuItem = menu.findItem(R.id.filter_unbound);
        filterUnboundMenuItem.setVisible(false);
        popupMenu.show();
    }

    @Override
    public void enableActionMode() {
        if (!viewModel.isActionMode()) {
            viewModel.enableActionMode();
        }
        if (actionMode == null) {
            favoritesActionModeCallback = new FavoritesActionModeCallback();
            actionMode = ((AppCompatActivity) getActivity())
                    .startSupportActionMode(favoritesActionModeCallback);
        }
        updateActionModeTitle();
        updateActionModeMenu();
    }

    @Override
    public void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish(); // NOTE: will call destroyActionMode
        }
    }

    private void destroyActionMode() {
        if (viewModel.isActionMode()) {
            viewModel.disableActionMode();
            presenter.selectFavoriteFilter();
        }
        if (actionMode != null) {
            actionMode = null;
            favoritesActionModeCallback = null;
        }
    }

    @Override
    public void selectionChanged(@NonNull String id) {
        checkNotNull(id);
        updateActionModeTitle();
        updateActionModeMenu();
    }

    @Override
    public int getPosition(String favoriteId) {
        return adapter.getPosition(favoriteId);
    }

    @Override
    @NonNull
    public String[] getIds() {
        return adapter.getIds();
    }

    @Override
    public void scrollToPosition(int position) {
        if (rvLayoutManager != null) {
            rvLayoutManager.scrollToPositionWithOffset(position, 0);
        }
    }

    private void updateActionModeTitle() {
        if (actionMode == null) return;

        actionMode.setTitle(getContext().getResources().getString(
                R.string.laano_favorites_action_mode_selected,
                viewModel.getSelectedCount(), adapter.getItemCount()));
        if (adapter.getItemCount() <= 0) {
            finishActionMode();
        }
    }

    private void updateActionModeMenu() {
        if (favoritesActionModeCallback == null) return;

        int selected = viewModel.getSelectedCount();
        if (selected <= 0) {
            favoritesActionModeCallback.setDeleteEnabled(false);
        } else {
            favoritesActionModeCallback.setDeleteEnabled(true);
        }
    }

    @Override
    public void removeFavorite(@NonNull String favoriteId) {
        viewModel.removeSelection(favoriteId);
        adapter.removeItem(favoriteId);
        selectionChanged(favoriteId);
        viewModel.setListSize(adapter.getItemCount());
    }

    @Override
    public void confirmFavoritesRemoval(ArrayList<String> selectedIds) {
        FavoriteRemovalConfirmationDialog dialog =
                FavoriteRemovalConfirmationDialog.newInstance(selectedIds);
        dialog.setTargetFragment(this, FavoriteRemovalConfirmationDialog.DIALOG_REQUEST_CODE);
        dialog.show(getFragmentManager(), FavoriteRemovalConfirmationDialog.DIALOG_TAG);
    }

    public void removeFavorites(ArrayList<String> selectedIds) {
        presenter.deleteFavorites(selectedIds);
        finishActionMode();
    }

    @Override
    public void showConflictResolution(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        FavoritesConflictResolutionDialog dialog =
                FavoritesConflictResolutionDialog.newInstance(favoriteId);
        dialog.setTargetFragment(this, REQUEST_FAVORITE_CONFLICT_RESOLUTION);
        dialog.show(getFragmentManager(), FavoritesConflictResolutionDialog.DIALOG_TAG);
    }

    public class FavoritesActionModeCallback implements ActionMode.Callback {

        private MenuItem deleteMenuItem;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.action_mode_favorites, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            deleteMenuItem = menu.findItem(R.id.favorites_delete);
            deleteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.favorites_select_all).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.favorites_delete:
                    presenter.onDeleteClick();
                    break;
                case R.id.favorites_select_all:
                    presenter.onSelectAllClick();
                    updateActionModeTitle();
                    updateActionModeMenu();
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

        public void setDeleteEnabled(boolean enabled) {
            if (deleteMenuItem == null) return;

            deleteMenuItem.setEnabled(enabled);
            if (enabled) {
                deleteMenuItem.getIcon().setAlpha(255);
            } else {
                deleteMenuItem.getIcon().setAlpha((int) (255.0 * Settings.GLOBAL_ICON_ALPHA_DISABLED));
            }
        }
    }

    public static class FavoriteRemovalConfirmationDialog extends DialogFragment {

        private static final String ARGUMENT_SELECTED_IDS = "SELECTED_IDS";

        public static final String DIALOG_TAG = "FAVORITE_REMOVAL_CONFIRMATION";
        public static final int DIALOG_REQUEST_CODE = 0;

        private ArrayList<String> selectedIds;

        public static FavoriteRemovalConfirmationDialog newInstance(ArrayList<String> selectedIds) {
            Bundle args = new Bundle();
            args.putStringArrayList(ARGUMENT_SELECTED_IDS, selectedIds);
            FavoriteRemovalConfirmationDialog dialog = new FavoriteRemovalConfirmationDialog();
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            selectedIds = getArguments().getStringArrayList(ARGUMENT_SELECTED_IDS);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int length = selectedIds.size();
            return new AlertDialog.Builder(getContext())
                    .setTitle(R.string.favorites_delete_confirmation_title)
                    .setMessage(getResources().getQuantityString(
                            R.plurals.favorites_delete_confirmation_message, length, length))
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.dialog_button_delete, (dialog, which) ->
                            ((FavoritesFragment) getTargetFragment()).removeFavorites(selectedIds))
                    .setNegativeButton(R.string.dialog_button_cancel, null)
                    .create();
        }
    }
}
