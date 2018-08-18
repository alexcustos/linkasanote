/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bytesforge.linkasanote.laano.links;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.databinding.DialogDoNotShowCheckboxBinding;
import com.bytesforge.linkasanote.databinding.FragmentLaanoLinksBinding;
import com.bytesforge.linkasanote.laano.BaseItemFragment;
import com.bytesforge.linkasanote.laano.BaseItemViewModel;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.favorites.FavoritesViewModel;
import com.bytesforge.linkasanote.laano.links.addeditlink.AddEditLinkActivity;
import com.bytesforge.linkasanote.laano.links.addeditlink.AddEditLinkFragment;
import com.bytesforge.linkasanote.laano.links.conflictresolution.LinksConflictResolutionDialog;
import com.bytesforge.linkasanote.laano.notes.NotesViewModel;
import com.bytesforge.linkasanote.laano.notes.addeditnote.AddEditNoteActivity;
import com.bytesforge.linkasanote.laano.notes.addeditnote.AddEditNoteFragment;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.ActivityUtils;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class LinksFragment extends BaseItemFragment implements LinksContract.View {

    private static final String TAG = LinksFragment.class.getSimpleName();

    public static final int REQUEST_ADD_LINK = 1;
    public static final int REQUEST_EDIT_LINK = 2;
    public static final int REQUEST_LINK_CONFLICT_RESOLUTION = 3;
    public static final int REQUEST_ADD_NOTE = 4;

    private LinksContract.Presenter presenter;
    private LinksContract.ViewModel viewModel;
    private LinksAdapter adapter;
    private ActionMode actionMode;
    private LinearLayoutManager rvLayoutManager;
    private Parcelable rvLayoutState;
    private LinksActionModeCallback linksActionModeCallback;
    private FragmentActivity fragmentActivity;
    private FragmentManager fragmentManager;
    private Context context;

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
        fragmentActivity = getActivity();
        fragmentManager = getFragmentManager();
        context = getContext();

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        FragmentLaanoLinksBinding binding =
                FragmentLaanoLinksBinding.inflate(inflater, container, false);
        viewModel.setInstanceState(savedInstanceState);
        if (savedInstanceState == null) {
            viewModel.setExpandByDefault(presenter.isExpandLinks());
        }
        setRvLayoutState(savedInstanceState);
        binding.setViewModel((LinksViewModel) viewModel);
        // RecyclerView
        setupLinksRecyclerView(binding.rvLinks);
        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_links, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.toolbar_links_search);
        setSearchMenuItem(searchMenuItem);
    }

    private void setSearchMenuItem(MenuItem item) {
        SearchView searchView = (SearchView) item.getActionView();
        item.setOnActionExpandListener(
                new MenuItem.OnActionExpandListener() {

                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        if (fragmentActivity != null) {
                            fragmentActivity.invalidateOptionsMenu();
                        }
                        viewModel.setSearchText(null);
                        presenter.setFilterIsChanged(true);
                        presenter.loadLinks(false);
                        return true;
                    }
                });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.setSearchText(query);
                presenter.setFilterIsChanged(true);
                presenter.loadLinks(false);
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
            case R.id.toolbar_links_filter:
                showFilteringPopupMenu();
                break;
            case R.id.toolbar_links_action_mode:
                enableActionMode();
                break;
            case R.id.toolbar_links_expand_all:
                expandAllLinks();
                break;
            case R.id.toolbar_links_collapse_all:
                collapseAllLinks();
                break;
            case R.id.toolbar_links_clear_clipboard:
                if (context != null)
                    ActivityUtils.clearClipboard(context);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.saveInstanceState(outState);
        saveRvLayoutState(outState);
    }

    @Override
    public void startAddLinkActivity() {
        Intent intent = new Intent(context, AddEditLinkActivity.class);
        startActivityForResult(intent, REQUEST_ADD_LINK);
    }

    @Override
    public void showAddNote(@NonNull String linkId) {
        Intent intent = new Intent(context, AddEditNoteActivity.class);
        intent.putExtra(AddEditNoteFragment.ARGUMENT_RELATED_LINK_ID, linkId);
        startActivityForResult(intent, REQUEST_ADD_NOTE);
    }

    @Override
    public void showEditLink(@NonNull String linkId) {
        Intent intent = new Intent(context, AddEditLinkActivity.class);
        intent.putExtra(AddEditLinkFragment.ARGUMENT_LINK_ID, linkId);
        startActivityForResult(intent, REQUEST_EDIT_LINK);
    }

    @Override
    public void expandAllLinks() {
        String[] ids = getIds();
        if (ids.length > 0) {
            viewModel.setVisibility(ids, true);
        }
    }

    @Override
    public void collapseAllLinks() {
        String[] ids = getIds();
        if (ids.length > 0) {
            viewModel.setVisibility(ids, false);
        }
    }

    @Override
    public void showLinks(@NonNull List<Link> links) {
        checkNotNull(links);
        adapter.swapItems(links);
    }

    @Override
    public void addLinks(@NonNull List<Link> links) {
        checkNotNull(links);
        adapter.addItems(links);
    }

    @Override
    public void clearLinks() {
        // NOTE: viewModel's listSize must not be nulled here
        adapter.clear();
    }

    @Override
    public void updateView() {
        viewModel.setListSize(adapter.getItemCount());
        if (viewModel.isActionMode()) {
            enableActionMode();
        }
        applyRvLayoutState();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ADD_LINK:
                if (resultCode == Activity.RESULT_OK) {
                    //viewModel.showSaveSuccessSnackbar();
                    String linkId = data.getStringExtra(AddEditLinkFragment.ARGUMENT_LINK_ID);
                    presenter.syncSavedLink(linkId);
                }
                break;
            case REQUEST_EDIT_LINK:
                if (resultCode == Activity.RESULT_OK) {
                    //viewModel.showSaveSuccessSnackbar();
                    String linkId = data.getStringExtra(AddEditLinkFragment.ARGUMENT_LINK_ID);
                    presenter.syncSavedLink(linkId);
                }
                break;
            case REQUEST_LINK_CONFLICT_RESOLUTION:
                presenter.updateTabNormalState();
                // NOTE: force reload because of conflict resolution is a dialog
                presenter.loadLinks(false);
                if (resultCode == LinksConflictResolutionDialog.RESULT_OK) {
                    presenter.updateSyncStatus();
                    //viewModel.showConflictResolutionSuccessfulSnackbar();
                    Toast.makeText(context, R.string.toast_conflict_resolved,
                            Toast.LENGTH_SHORT).show();
                } else if (resultCode == LinksConflictResolutionDialog.RESULT_FAILED){
                    viewModel.showConflictResolutionErrorSnackbar();
                }
                break;
            case REQUEST_ADD_NOTE:
                if (resultCode == Activity.RESULT_OK) {
                    String linkId = data.getStringExtra(AddEditNoteFragment.ARGUMENT_RELATED_LINK_ID);
                    String noteId = data.getStringExtra(AddEditNoteFragment.ARGUMENT_NOTE_ID);
                    presenter.syncSavedNote(linkId, noteId);
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
        rvLayoutManager = new LinearLayoutManager(context);
        rvLinks.setLayoutManager(rvLayoutManager);
        ((SimpleItemAnimator) rvLinks.getItemAnimator()).setSupportsChangeAnimations(false);
    }

    private void setRvLayoutState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            rvLayoutState = savedInstanceState.getParcelable(
                    BaseItemViewModel.STATE_RECYCLER_LAYOUT);
        }
    }

    private void saveRvLayoutState(@NonNull Bundle outState) {
        checkNotNull(outState);
        outState.putParcelable(BaseItemViewModel.STATE_RECYCLER_LAYOUT,
                rvLayoutManager.onSaveInstanceState());
    }

    private void applyRvLayoutState() {
        if (rvLayoutManager != null && rvLayoutState != null) {
            rvLayoutManager.onRestoreInstanceState(rvLayoutState);
            rvLayoutState = null;
        }
    }

    private void showFilteringPopupMenu() {
        if (context == null || fragmentActivity == null) return;

        PopupMenu popupMenu = new PopupMenu(
                context, fragmentActivity.findViewById(R.id.toolbar_links_filter));
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
            // NOTE: also go to the start of the list if the current filter is selected
            scrollToPosition(0);
            return true;
        });
        Resources resources = context.getResources();
        MenuItem filterLinkMenuItem = menu.findItem(R.id.filter_link);
        filterLinkMenuItem.setVisible(false);

        MenuItem filterUnboundMenuItem = menu.findItem(R.id.filter_unbound);
        filterUnboundMenuItem.setVisible(false);

        MenuItem filterFavoriteMenuItem = menu.findItem(R.id.filter_favorite);
        boolean isFavoriteFilter = presenter.isFavoriteFilter();
        Boolean favoriteAndGate = presenter.isFavoriteAndGate();
        String filterFavoritePostfix;
        if (favoriteAndGate != null) {
            if (favoriteAndGate) {
                filterFavoritePostfix = " " + resources.getString(
                        R.string.filter_menu_item_postfix, FavoritesViewModel.FILTER_AND_GATE_PREFIX);
            } else {
                filterFavoritePostfix = " " + resources.getString(
                        R.string.filter_menu_item_postfix, FavoritesViewModel.FILTER_OR_GATE_PREFIX);
            }
        } else {
            filterFavoritePostfix = "";
        }
        filterFavoriteMenuItem.setTitle(
                resources.getString(R.string.filter_favorite) + filterFavoritePostfix);
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
        if (actionMode == null && fragmentActivity != null) {
            linksActionModeCallback = new LinksActionModeCallback();
            actionMode = ((AppCompatActivity) fragmentActivity)
                    .startSupportActionMode(linksActionModeCallback);
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
            presenter.selectLinkFilter();
        }
        if (actionMode != null) {
            actionMode = null;
            linksActionModeCallback = null;
        }
    }

    @Override
    public void selectionChanged(@NonNull String id) {
        checkNotNull(id);
        //adapter.notifyItemChanged(id);
        updateActionModeTitle();
        updateActionModeMenu();
    }

    @Override
    public void visibilityChanged(@NonNull String id) {
        checkNotNull(id);
        // NOTE: this notification is required, or else rvLinkNotes randomly turns blank
        adapter.notifyItemChanged(id);
    }

    @Override
    public int getPosition(String linkId) {
        return adapter.getPosition(linkId);
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
        if (actionMode == null || context == null) return;

        actionMode.setTitle(context.getResources().getString(
                R.string.laano_links_action_mode_selected,
                viewModel.getSelectedCount(), adapter.getItemCount()));
        if (adapter.getItemCount() <= 0) {
            finishActionMode();
        }
    }

    private void updateActionModeMenu() {
        if (linksActionModeCallback == null) return;

        int selected = viewModel.getSelectedCount();
        if (selected <= 0) {
            linksActionModeCallback.setDeleteEnabled(false);
        } else {
            linksActionModeCallback.setDeleteEnabled(true);
        }
    }

    @Override
    public void removeLink(@NonNull String linkId) {
        int position = adapter.removeItem(linkId);
        if (position >= 0) {
            viewModel.removeSelection(linkId);
            selectionChanged(linkId);
            viewModel.setListSize(adapter.getItemCount());
        }
    }

    @Override
    public void openLink(@NonNull Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, checkNotNull(uri));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            viewModel.showOpenLinkErrorSnackbar();
        }
    }

    @Override
    public void confirmLinksRemoval(ArrayList<String> selectedIds) {
        LinkRemovalConfirmationDialog dialog =
                LinkRemovalConfirmationDialog.newInstance(selectedIds);
        dialog.setTargetFragment(this, LinkRemovalConfirmationDialog.DIALOG_REQUEST_CODE);
        if (fragmentManager != null)
            dialog.show(fragmentManager, LinkRemovalConfirmationDialog.DIALOG_TAG);
    }

    public void deleteLinks(ArrayList<String> selectedIds, boolean deleteNotes) {
        presenter.deleteLinks(selectedIds, deleteNotes);
        finishActionMode();
    }

    @Override
    public void showConflictResolution(@NonNull String linkId) {
        checkNotNull(linkId);
        LinksConflictResolutionDialog dialog =
                LinksConflictResolutionDialog.newInstance(linkId);
        dialog.setTargetFragment(this, REQUEST_LINK_CONFLICT_RESOLUTION);
        if (fragmentManager != null)
            dialog.show(fragmentManager, LinksConflictResolutionDialog.DIALOG_TAG);
    }

    @Override
    public void showConflictResolutionWarning(@NonNull String linkId) {
        checkNotNull(linkId);
        LinkConflictResolutionWarningDialog dialog =
                LinkConflictResolutionWarningDialog.newInstance(linkId);
        dialog.setTargetFragment(this, LinkConflictResolutionWarningDialog.DIALOG_REQUEST_CODE);
        if (fragmentManager != null)
            dialog.show(fragmentManager, LinkConflictResolutionWarningDialog.DIALOG_TAG);
    }

    // NOTE: callback from AlertDialog
    public void setShowConflictResolutionWarning(boolean show) {
        presenter.setShowConflictResolutionWarning(show);
    }

    public class LinksActionModeCallback implements ActionMode.Callback {

        private MenuItem deleteMenuItem;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.action_mode_links, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            deleteMenuItem = menu.findItem(R.id.links_delete);
            deleteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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

    public static class LinkRemovalConfirmationDialog extends DialogFragment {

        private static final String ARGUMENT_SELECTED_IDS = "SELECTED_IDS";

        public static final String DIALOG_TAG = "LINK_REMOVAL_CONFIRMATION";
        public static final int DIALOG_REQUEST_CODE = 0;

        private ArrayList<String> selectedIds;

        public static LinkRemovalConfirmationDialog newInstance(ArrayList<String> selectedIds) {
            Bundle args = new Bundle();
            args.putStringArrayList(ARGUMENT_SELECTED_IDS, selectedIds);
            LinkRemovalConfirmationDialog dialog = new LinkRemovalConfirmationDialog();
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            if (args != null)
                selectedIds = args.getStringArrayList(ARGUMENT_SELECTED_IDS);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context dialogContext = getContext();
            LinksFragment targetFragment = (LinksFragment) getTargetFragment();
            if (dialogContext == null || targetFragment == null) {
                throw new IllegalStateException("Unexpected state in onCreateDialog()");
            }
            int length = selectedIds.size();
            return new AlertDialog.Builder(dialogContext)
                    .setTitle(R.string.links_delete_confirmation_title)
                    .setMessage(getResources().getQuantityString(
                            R.plurals.links_delete_confirmation_message, length, length))
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.dialog_button_delete, (dialog, which) ->
                            targetFragment.deleteLinks(selectedIds, true))
                    .setNeutralButton(R.string.dialog_button_keep_notes, (dialog, which) ->
                            targetFragment.deleteLinks(selectedIds, false))
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
            Bundle args = getArguments();
            if (args != null)
                linkId = args.getString(ARGUMENT_LINK_ID);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context dialogContext = getContext();
            LinksFragment targetFragment = (LinksFragment) getTargetFragment();
            if (dialogContext == null || targetFragment == null) {
                throw new IllegalStateException("Unexpected state in onCreateDialog()");
            }
            LayoutInflater inflater = LayoutInflater.from(dialogContext);
            DialogDoNotShowCheckboxBinding binding =
                    DialogDoNotShowCheckboxBinding.inflate(inflater, null, false);
            CheckBox checkBox = binding.doNotShowCheckbox;

            return new AlertDialog.Builder(dialogContext)
                    .setView(binding.getRoot())
                    .setTitle(R.string.links_conflict_resolution_warning_title)
                    .setMessage(R.string.links_conflict_resolution_warning_message)
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.dialog_button_continue, (dialog, which) -> {
                        targetFragment.setShowConflictResolutionWarning(!checkBox.isChecked());
                        targetFragment.showConflictResolution(linkId);
                    })
                    .setNegativeButton(R.string.dialog_button_cancel, (dialog, which) ->
                            targetFragment.setShowConflictResolutionWarning(!checkBox.isChecked()))
                    .create();
        }
    }
}
