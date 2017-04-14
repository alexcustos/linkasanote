package com.bytesforge.linkasanote.laano.links;

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

import com.bytesforge.linkasanote.BaseFragment;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.databinding.FragmentLaanoLinksBinding;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.links.addeditlink.AddEditLinkActivity;
import com.bytesforge.linkasanote.laano.links.addeditlink.AddEditLinkFragment;
import com.bytesforge.linkasanote.laano.links.conflictresolution.LinksConflictResolutionDialog;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class LinksFragment extends BaseFragment implements LinksContract.View {

    public static final int REQUEST_ADD_LINK = 1;
    public static final int REQUEST_EDIT_LINK = 2;
    public static final int REQUEST_LINK_CONFLICT_RESOLUTION = 3;

    private LinksContract.Presenter presenter;
    private LinksContract.ViewModel viewModel;
    private LinksAdapter adapter;
    private ActionMode actionMode;

    public static LinksFragment newInstance() {
        return new LinksFragment();
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
    public void setPresenter(@NonNull LinksContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setViewModel(@NonNull LinksContract.ViewModel viewModel) {
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
        FragmentLaanoLinksBinding binding =
                FragmentLaanoLinksBinding.inflate(inflater, container, false);
        viewModel.setInstanceState(savedInstanceState);
        binding.setViewModel((LinksViewModel) viewModel);
        // RecyclerView
        if (binding.rvLinks != null) {
            setupLinksRecyclerView(binding.rvLinks);
        }
        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_links, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.toolbar_links_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        MenuItemCompat.setOnActionExpandListener(
                searchMenuItem, new MenuItemCompat.OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                getActivity().supportInvalidateOptionsMenu();
                viewModel.setSearchText(null);
                presenter.loadLinks(false);
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.setSearchText(query);
                presenter.loadLinks(false);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
        if (viewModel.getSearchText() != null) {
            searchMenuItem.expandActionView();
            searchView.setQuery(viewModel.getSearchText(), false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_links_filter:
                showFilteringPopupMenu();
                break;
            case R.id.toolbar_links_action_mode:
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
    public void showAddLink() {
        Intent intent = new Intent(getContext(), AddEditLinkActivity.class);
        startActivityForResult(intent, REQUEST_ADD_LINK);
    }

    @Override
    public void showEditLink(@NonNull String linkId) {
        Intent intent = new Intent(getContext(), AddEditLinkActivity.class);
        intent.putExtra(AddEditLinkFragment.ARGUMENT_EDIT_LINK_ID, linkId);
        startActivityForResult(intent, REQUEST_EDIT_LINK);
    }

    @Override
    public void showLinks(@NonNull List<Link> links) {
        checkNotNull(links);

        adapter.swapItems(links);
        viewModel.setLinkListSize(links.size());
        if (viewModel.isActionMode()) {
            enableActionMode();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ADD_LINK:
                if (resultCode == Activity.RESULT_OK) {
                    adapter.notifyDataSetChanged();
                }
                break;
            case REQUEST_EDIT_LINK:
                if (resultCode == Activity.RESULT_OK) {
                    adapter.notifyDataSetChanged();
                }
                break;
            case REQUEST_LINK_CONFLICT_RESOLUTION:
                adapter.notifyDataSetChanged();
                presenter.updateTabNormalState();
                presenter.loadLinks(false);
                if (resultCode == LinksConflictResolutionDialog.RESULT_OK) {
                    viewModel.showConflictResolutionSuccessfulSnackbar();
                } else if (resultCode == LinksConflictResolutionDialog.RESULT_FAILED){
                    viewModel.showConflictResolutionErrorSnackbar();
                }
                break;
            default:
                throw new IllegalStateException("The result received from the unexpected activity");
        }
    }

    private void setupLinksRecyclerView(RecyclerView rvLinks) {
        List<Link> links = new ArrayList<>(0);
        adapter = new LinksAdapter(links, presenter, (LinksViewModel) viewModel);
        rvLinks.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvLinks.setLayoutManager(layoutManager);
    }

    private void showFilteringPopupMenu() {
        PopupMenu menu = new PopupMenu(
                getContext(), getActivity().findViewById(R.id.toolbar_links_filter));
        menu.getMenuInflater().inflate(R.menu.filter, menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {

            switch (item.getItemId()) {
                case R.id.filter_all:
                    presenter.setFilterType(FilterType.ALL);
                    break;
                case R.id.filter_conflicted:
                    presenter.setFilterType(FilterType.CONFLICTED);
                    break;
            }
            presenter.loadLinks(false);
            return true;
        });
        menu.show();
    }

    @Override
    public void enableActionMode() {
        if (!viewModel.isActionMode()) {
            viewModel.enableActionMode();
        }
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(
                    new LinksActionModeCallback());
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
            int[] selectedIds = viewModel.getSelectedIds().clone();
            viewModel.removeSelection();
            for (int selectedId : selectedIds) {
                adapter.notifyItemChanged(selectedId);
            }
            viewModel.disableActionMode();
        }
        if (actionMode != null) actionMode = null;
    }

    @Override
    public void selectionChanged(int position) {
        adapter.notifyItemChanged(position);
        updateActionModeTitle();
    }

    @Override
    public int getPosition(String linkId) {
        return adapter.getPosition(linkId);
    }

    private void updateActionModeTitle() {
        if (actionMode != null) {
            actionMode.setTitle(getContext().getResources().getString(
                    R.string.laano_links_action_mode_selected,
                    viewModel.getSelectedCount(), adapter.getItemCount()));
            if (adapter.getItemCount() <= 0) {
                disableActionMode();
            }
        } // if
    }

    @Override
    public String removeLink(int position) {
        Link link = adapter.removeItem(position);
        selectionChanged(position);
        viewModel.setLinkListSize(adapter.getItemCount());
        return link.getId();
    }

    @Override
    public void confirmLinksRemoval(int[] selectedIds) {
        LinkRemovalConfirmationDialog dialog =
                LinkRemovalConfirmationDialog.newInstance(selectedIds);
        dialog.setTargetFragment(this, LinkRemovalConfirmationDialog.DIALOG_REQUEST_CODE);
        dialog.show(getFragmentManager(), LinkRemovalConfirmationDialog.DIALOG_TAG);
    }

    public void removeLinks(int[] selectedIds) {
        presenter.deleteLinks(selectedIds);
    }

    @Override
    public void showConflictResolution(@NonNull String linkId) {
        checkNotNull(linkId);

        LinksConflictResolutionDialog dialog =
                LinksConflictResolutionDialog.newInstance(linkId);
        dialog.setTargetFragment(this, REQUEST_LINK_CONFLICT_RESOLUTION);
        dialog.show(getFragmentManager(), LinksConflictResolutionDialog.DIALOG_TAG);
    }

    public class LinksActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.action_mode_links, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.findItem(R.id.links_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.links_select_all).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.links_delete:
                    presenter.onDeleteClick();
                    break;
                case R.id.links_select_all:
                    presenter.onSelectAllClick();
                    adapter.notifyDataSetChanged();
                    updateActionModeTitle();
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

    public static class LinkRemovalConfirmationDialog extends DialogFragment {

        private static final String ARGUMENT_SELECTED_IDS = "SELECTED_IDS";

        public static final String DIALOG_TAG = "LINK_REMOVAL_CONFIRMATION";
        public static final int DIALOG_REQUEST_CODE = 0;

        private int[] selectedIds;

        public static LinkRemovalConfirmationDialog newInstance(int[] selectedIds) {
            Bundle args = new Bundle();
            args.putIntArray(ARGUMENT_SELECTED_IDS, selectedIds);
            LinkRemovalConfirmationDialog dialog = new LinkRemovalConfirmationDialog();
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            selectedIds = getArguments().getIntArray(ARGUMENT_SELECTED_IDS);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int length = selectedIds.length;
            return new AlertDialog.Builder(getContext())
                    .setTitle(R.string.links_delete_confirmation_title)
                    .setMessage(getResources().getQuantityString(
                            R.plurals.links_delete_confirmation_message, length, length))
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.dialog_button_ok, (dialog, which) ->
                            ((LinksFragment) getTargetFragment()).removeLinks(selectedIds))
                    .setNegativeButton(R.string.dialog_button_cancel, null)
                    .create();
        }
    }
}
