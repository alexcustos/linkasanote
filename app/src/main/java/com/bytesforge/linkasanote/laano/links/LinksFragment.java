package com.bytesforge.linkasanote.laano.links;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
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
import android.widget.CheckBox;

import com.bytesforge.linkasanote.BaseFragment;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.databinding.DialogDoNotShowCheckboxBinding;
import com.bytesforge.linkasanote.databinding.FragmentLaanoLinksBinding;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.favorites.FavoritesViewModel;
import com.bytesforge.linkasanote.laano.links.addeditlink.AddEditLinkActivity;
import com.bytesforge.linkasanote.laano.links.addeditlink.AddEditLinkFragment;
import com.bytesforge.linkasanote.laano.links.conflictresolution.LinksConflictResolutionDialog;
import com.bytesforge.linkasanote.laano.notes.NotesViewModel;
import com.bytesforge.linkasanote.laano.notes.addeditnote.AddEditNoteActivity;
import com.bytesforge.linkasanote.laano.notes.addeditnote.AddEditNoteFragment;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class LinksFragment extends BaseFragment implements LinksContract.View {

    public static final int REQUEST_ADD_LINK = 1;
    public static final int REQUEST_EDIT_LINK = 2;
    public static final int REQUEST_LINK_CONFLICT_RESOLUTION = 3;
    public static final int REQUEST_ADD_NOTE = 4;

    private LinksContract.Presenter presenter;
    private LinksContract.ViewModel viewModel;
    private LinksAdapter adapter;
    private ActionMode actionMode;
    private LinearLayoutManager rvLayoutManager;

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
        setupLinksRecyclerView(binding.rvLinks);
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
            case R.id.toolbar_links_expand_all:
                viewModel.expandAllLinks();
                break;
            case R.id.toolbar_links_collapse_all:
                viewModel.collapseAllLinks();
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
    public void showAddNote(@NonNull String linkId) {
        Intent intent = new Intent(getContext(), AddEditNoteActivity.class);
        intent.putExtra(AddEditNoteFragment.ARGUMENT_RELATED_LINK_ID, linkId);
        startActivityForResult(intent, REQUEST_ADD_NOTE);
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
                    // NOTE: the item position will not be changed, but one can be filtered in or out
                    // OPTIMIZATION: replace the only invalidated items in the cache
                    adapter.notifyDataSetChanged();
                }
                break;
            case REQUEST_LINK_CONFLICT_RESOLUTION:
                adapter.notifyDataSetChanged();
                presenter.updateTabNormalState();
                // NOTE: force reload because of conflict resolution is a dialog
                presenter.loadLinks(false);
                if (resultCode == LinksConflictResolutionDialog.RESULT_OK) {
                    viewModel.showConflictResolutionSuccessfulSnackbar();
                } else if (resultCode == LinksConflictResolutionDialog.RESULT_FAILED){
                    viewModel.showConflictResolutionErrorSnackbar();
                }
                break;
            case REQUEST_ADD_NOTE:
                if (resultCode == Activity.RESULT_OK) {
                    String linkId = data.getStringExtra(AddEditNoteFragment.ARGUMENT_RELATED_LINK_ID);
                    adapter.notifyItemChanged(linkId);
                    presenter.loadLinks(true);
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
        rvLayoutManager = new LinearLayoutManager(getContext());
        rvLinks.setLayoutManager(rvLayoutManager);
    }

    private void showFilteringPopupMenu() {
        PopupMenu popupMenu = new PopupMenu(
                getContext(), getActivity().findViewById(R.id.toolbar_links_filter));
        Menu menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.filter, menu);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.filter_all:
                    presenter.setFilterType(FilterType.ALL);
                    break;
                case R.id.filter_favorite:
                    presenter.setFilterType(FilterType.FAVORITE);
                    break;
                case R.id.filter_note:
                    presenter.setFilterType(FilterType.NOTE);
                    break;
                case R.id.filter_no_tags:
                    presenter.setFilterType(FilterType.NO_TAGS);
                    break;
                case R.id.filter_conflicted:
                    presenter.setFilterType(FilterType.CONFLICTED);
                    break;
            }
            return true;
        });
        Resources resources = getContext().getResources();
        MenuItem filterLinkMenuItem = menu.findItem(R.id.filter_link);
        filterLinkMenuItem.setVisible(false);

        MenuItem filterUnboundMenuItem = menu.findItem(R.id.filter_unbound);
        filterUnboundMenuItem.setVisible(false);

        MenuItem filterFavoriteMenuItem = menu.findItem(R.id.filter_favorite);
        boolean isFavoriteFilter = presenter.isFavoriteFilter();
        filterFavoriteMenuItem.setTitle(
                resources.getString(R.string.filter_favorite) + " " + resources.getString(
                        R.string.filter_menu_item_postfix, FavoritesViewModel.FILTER_PREFIX));
        filterFavoriteMenuItem.setEnabled(isFavoriteFilter);

        MenuItem filterNoteMenuItem = menu.findItem(R.id.filter_note);
        boolean isNoteFilter = presenter.isNoteFilter();
        filterNoteMenuItem.setTitle(
                resources.getString(R.string.filter_note) + " " + resources.getString(
                        R.string.filter_menu_item_postfix, NotesViewModel.FILTER_PREFIX));
        filterNoteMenuItem.setEnabled(isNoteFilter);
        popupMenu.show();
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
    public void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish(); // NOTE: will call destroyActionMode
        }
    }

    private void destroyActionMode() {
        if (viewModel.isActionMode()) {
            viewModel.disableActionMode();
        }
        if (actionMode != null) {
            actionMode = null;
        }
    }

    @Override
    public void selectionChanged(int position) {
        //adapter.notifyItemChanged(position);
        updateActionModeTitle();
    }

    @Override
    public void linkVisibilityChanged(int position) {
        //adapter.notifyItemChanged(position);
    }

    @Override
    public int getPosition(String linkId) {
        return adapter.getPosition(linkId);
    }

    @Override
    public void scrollToPosition(int position) {
        if (rvLayoutManager != null) {
            rvLayoutManager.scrollToPositionWithOffset(position, 0);
        }
    }

    private void updateActionModeTitle() {
        if (actionMode != null) {
            actionMode.setTitle(getContext().getResources().getString(
                    R.string.laano_links_action_mode_selected,
                    viewModel.getSelectedCount(), adapter.getItemCount()));
            if (adapter.getItemCount() <= 0) {
                finishActionMode();
            }
        }
    }

    @Override
    public String removeLink(int position) {
        Link link = adapter.removeItem(position);
        selectionChanged(position);
        viewModel.setLinkListSize(adapter.getItemCount());
        return link.getId();
    }

    @Override
    public void openLink(@NonNull Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, checkNotNull(uri));
        startActivity(intent);
    }

    @Override
    public void confirmLinksRemoval(int[] selectedIds) {
        LinkRemovalConfirmationDialog dialog =
                LinkRemovalConfirmationDialog.newInstance(selectedIds);
        dialog.setTargetFragment(this, LinkRemovalConfirmationDialog.DIALOG_REQUEST_CODE);
        dialog.show(getFragmentManager(), LinkRemovalConfirmationDialog.DIALOG_TAG);
    }

    public void deleteLinks(int[] selectedIds, boolean deleteNotes) {
        presenter.deleteLinks(selectedIds, deleteNotes);
    }

    @Override
    public void showConflictResolution(@NonNull String linkId) {
        checkNotNull(linkId);
        LinksConflictResolutionDialog dialog =
                LinksConflictResolutionDialog.newInstance(linkId);
        dialog.setTargetFragment(this, REQUEST_LINK_CONFLICT_RESOLUTION);
        dialog.show(getFragmentManager(), LinksConflictResolutionDialog.DIALOG_TAG);
    }

    @Override
    public void showConflictResolutionWarning(@NonNull String linkId) {
        checkNotNull(linkId);
        LinkConflictResolutionWarningDialog dialog =
                LinkConflictResolutionWarningDialog.newInstance(linkId);
        dialog.setTargetFragment(this, LinkConflictResolutionWarningDialog.DIALOG_REQUEST_CODE);
        dialog.show(getFragmentManager(), LinkConflictResolutionWarningDialog.DIALOG_TAG);
    }

    // NOTE: callback from AlertDialog
    public void setShowConflictResolutionWarning(boolean show) {
        presenter.setShowConflictResolutionWarning(show);
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
                    .setPositiveButton(R.string.dialog_button_delete, (dialog, which) ->
                            ((LinksFragment) getTargetFragment()).deleteLinks(selectedIds, true))
                    .setNeutralButton(R.string.dialog_button_keep_notes, (dialog, which) ->
                            ((LinksFragment) getTargetFragment()).deleteLinks(selectedIds, false))
                    .setNegativeButton(R.string.dialog_button_cancel, null)
                    .create();
        }
    }

    public static class LinkConflictResolutionWarningDialog extends DialogFragment {

        public static final String ARGUMENT_LINK_ID = "LINK_ID";

        public static final String DIALOG_TAG = "LINK_CONFLICT_RESOLUTION_WARNING";
        public static final int DIALOG_REQUEST_CODE = 0;

        private String linkId;

        public static LinkConflictResolutionWarningDialog newInstance(@NonNull String linkId) {
            checkNotNull(linkId);
            Bundle args = new Bundle();
            args.putString(ARGUMENT_LINK_ID, linkId);
            LinkConflictResolutionWarningDialog dialog = new LinkConflictResolutionWarningDialog();
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            linkId = getArguments().getString(ARGUMENT_LINK_ID);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            DialogDoNotShowCheckboxBinding binding =
                    DialogDoNotShowCheckboxBinding.inflate(inflater, null, false);
            CheckBox checkBox = binding.doNotShowCheckbox;

            return new AlertDialog.Builder(context)
                    .setView(binding.getRoot())
                    .setTitle(R.string.links_conflict_resolution_warning_title)
                    .setMessage(R.string.links_conflict_resolution_warning_message)
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.dialog_button_continue, (dialog, which) -> {
                        LinksFragment fragment = (LinksFragment) getTargetFragment();
                        fragment.setShowConflictResolutionWarning(!checkBox.isChecked());
                        fragment.showConflictResolution(linkId);
                    })
                    .setNegativeButton(R.string.dialog_button_cancel, (dialog, which) -> {
                        LinksFragment fragment = (LinksFragment) getTargetFragment();
                        fragment.setShowConflictResolutionWarning(!checkBox.isChecked());
                    })
                    .create();
        }
    }
}
