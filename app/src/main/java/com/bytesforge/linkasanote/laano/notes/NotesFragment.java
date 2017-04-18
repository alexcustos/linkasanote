package com.bytesforge.linkasanote.laano.notes;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
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
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.databinding.FragmentLaanoNotesBinding;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.favorites.FavoritesViewModel;
import com.bytesforge.linkasanote.laano.links.LinksViewModel;
import com.bytesforge.linkasanote.laano.notes.addeditnote.AddEditNoteActivity;
import com.bytesforge.linkasanote.laano.notes.addeditnote.AddEditNoteFragment;
import com.bytesforge.linkasanote.laano.notes.conflictresolution.NotesConflictResolutionDialog;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotesFragment extends BaseFragment implements NotesContract.View {

    public static final int REQUEST_ADD_NOTE = 1;
    public static final int REQUEST_EDIT_NOTE = 2;
    public static final int REQUEST_NOTE_CONFLICT_RESOLUTION = 3;

    private NotesContract.Presenter presenter;
    private NotesContract.ViewModel viewModel;
    private NotesAdapter adapter;
    private ActionMode actionMode;
    private LinearLayoutManager rvLayoutManager;

    public static NotesFragment newInstance() {
        return new NotesFragment();
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
    public void setPresenter(@NonNull NotesContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setViewModel(@NonNull NotesContract.ViewModel viewModel) {
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
        FragmentLaanoNotesBinding binding =
                FragmentLaanoNotesBinding.inflate(inflater, container, false);
        viewModel.setInstanceState(savedInstanceState);
        binding.setViewModel((NotesViewModel) viewModel);
        // RecyclerView
        if (binding.rvNotes != null) {
            setupNotesRecyclerView(binding.rvNotes);
        }
        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_notes, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.toolbar_notes_search);
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
                presenter.loadNotes(false);
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.setSearchText(query);
                presenter.loadNotes(false);
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
            case R.id.toolbar_notes_filter:
                showFilteringPopupMenu();
                break;
            case R.id.toolbar_notes_action_mode:
                enableActionMode();
                break;
            case R.id.toolbar_notes_expand_all:
                viewModel.expandAllNotes();
                break;
            case R.id.toolbar_notes_collapse_all:
                viewModel.collapseAllNotes();
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
    public void showAddNote(String linkId) {
        Intent intent = new Intent(getContext(), AddEditNoteActivity.class);
        if (linkId != null) {
            intent.putExtra(AddEditNoteFragment.ARGUMENT_RELATED_LINK_ID, linkId);
        }
        startActivityForResult(intent, REQUEST_ADD_NOTE);
    }

    @Override
    public void showEditNote(@NonNull String noteId) {
        Intent intent = new Intent(getContext(), AddEditNoteActivity.class);
        intent.putExtra(AddEditNoteFragment.ARGUMENT_EDIT_NOTE_ID, noteId);
        startActivityForResult(intent, REQUEST_EDIT_NOTE);
    }

    @Override
    public void showNotes(@NonNull List<Note> notes) {
        checkNotNull(notes);

        adapter.swapItems(notes);
        viewModel.setNoteListSize(notes.size());
        if (viewModel.isActionMode()) {
            enableActionMode();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ADD_NOTE:
                if (resultCode == Activity.RESULT_OK) {
                    adapter.notifyDataSetChanged();
                }
                break;
            case REQUEST_EDIT_NOTE:
                if (resultCode == Activity.RESULT_OK) {
                    adapter.notifyDataSetChanged();
                }
                break;
            case REQUEST_NOTE_CONFLICT_RESOLUTION:
                adapter.notifyDataSetChanged();
                presenter.updateTabNormalState();
                presenter.loadNotes(false);
                if (resultCode == NotesConflictResolutionDialog.RESULT_OK) {
                    viewModel.showConflictResolutionSuccessfulSnackbar();
                } else if (resultCode == NotesConflictResolutionDialog.RESULT_FAILED){
                    viewModel.showConflictResolutionErrorSnackbar();
                }
                break;
            default:
                throw new IllegalStateException("The result received from the unexpected activity");
        }
    }

    private void setupNotesRecyclerView(RecyclerView rvNotes) {
        List<Note> notes = new ArrayList<>(0);
        adapter = new NotesAdapter(notes, presenter, (NotesViewModel) viewModel);
        rvNotes.setAdapter(adapter);
        rvLayoutManager = new LinearLayoutManager(getContext());
        rvNotes.setLayoutManager(rvLayoutManager);
    }

    private void showFilteringPopupMenu() {
        PopupMenu popupMenu = new PopupMenu(
                getContext(), getActivity().findViewById(R.id.toolbar_notes_filter));
        Menu menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.filter, menu);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.filter_all:
                    presenter.setFilterType(FilterType.ALL);
                    break;
                case R.id.filter_link:
                    presenter.setFilterType(FilterType.LINK);
                    break;
                case R.id.filter_favorite:
                    presenter.setFilterType(FilterType.FAVORITE);
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
        MenuItem filterNoteMenuItem = menu.findItem(R.id.filter_note);
        filterNoteMenuItem.setVisible(false);

        MenuItem filterFavoriteMenuItem = menu.findItem(R.id.filter_favorite);
        boolean isFavoriteFilter = presenter.isFavoriteFilter();
        filterFavoriteMenuItem.setTitle(resources.getString(
                R.string.filter_favorite, FavoritesViewModel.FILTER_PREFIX));
        filterFavoriteMenuItem.setEnabled(isFavoriteFilter);

        MenuItem filterLinkMenuItem = menu.findItem(R.id.filter_link);
        boolean isLinkFilter = presenter.isLinkFilter();
        filterLinkMenuItem.setTitle(resources.getString(
                R.string.filter_link, LinksViewModel.FILTER_PREFIX));
        filterLinkMenuItem.setEnabled(isLinkFilter);
        popupMenu.show();
    }

    @Override
    public void enableActionMode() {
        if (!viewModel.isActionMode()) {
            viewModel.enableActionMode();
        }
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(
                    new NotesActionModeCallback());
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
            viewModel.disableActionMode();
        }
        if (actionMode != null) actionMode = null;
    }

    @Override
    public void selectionChanged(int position) {
        //adapter.notifyItemChanged(position);
        updateActionModeTitle();
    }

    @Override
    public void noteVisibilityChanged(int position) {
        //adapter.notifyItemChanged(position);
    }

    @Override
    public int getPosition(String noteId) {
        return adapter.getPosition(noteId);
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
                    R.string.laano_notes_action_mode_selected,
                    viewModel.getSelectedCount(), adapter.getItemCount()));
            if (adapter.getItemCount() <= 0) {
                disableActionMode();
            }
        } // if
    }

    @Override
    public String removeNote(int position) {
        Note note = adapter.removeItem(position);
        selectionChanged(position);
        viewModel.setNoteListSize(adapter.getItemCount());
        return note.getId();
    }

    @Override
    public void confirmNotesRemoval(int[] selectedIds) {
        NoteRemovalConfirmationDialog dialog =
                NoteRemovalConfirmationDialog.newInstance(selectedIds);
        dialog.setTargetFragment(this, NoteRemovalConfirmationDialog.DIALOG_REQUEST_CODE);
        dialog.show(getFragmentManager(), NoteRemovalConfirmationDialog.DIALOG_TAG);
    }

    public void removeNotes(int[] selectedIds) {
        presenter.deleteNotes(selectedIds);
    }

    @Override
    public void showConflictResolution(@NonNull String noteId) {
        checkNotNull(noteId);

        NotesConflictResolutionDialog dialog =
                NotesConflictResolutionDialog.newInstance(noteId);
        dialog.setTargetFragment(this, REQUEST_NOTE_CONFLICT_RESOLUTION);
        dialog.show(getFragmentManager(), NotesConflictResolutionDialog.DIALOG_TAG);
    }

    public class NotesActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.action_mode_notes, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.findItem(R.id.notes_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.notes_select_all).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.notes_delete:
                    presenter.onDeleteClick();
                    break;
                case R.id.notes_select_all:
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

    public static class NoteRemovalConfirmationDialog extends DialogFragment {

        private static final String ARGUMENT_SELECTED_IDS = "SELECTED_IDS";

        public static final String DIALOG_TAG = "NOTE_REMOVAL_CONFIRMATION";
        public static final int DIALOG_REQUEST_CODE = 0;

        private int[] selectedIds;

        public static NoteRemovalConfirmationDialog newInstance(int[] selectedIds) {
            Bundle args = new Bundle();
            args.putIntArray(ARGUMENT_SELECTED_IDS, selectedIds);
            NoteRemovalConfirmationDialog dialog = new NoteRemovalConfirmationDialog();
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
                    .setTitle(R.string.notes_delete_confirmation_title)
                    .setMessage(getResources().getQuantityString(
                            R.plurals.notes_delete_confirmation_message, length, length))
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.dialog_button_ok, (dialog, which) ->
                            ((NotesFragment) getTargetFragment()).removeNotes(selectedIds))
                    .setNegativeButton(R.string.dialog_button_cancel, null)
                    .create();
        }
    }
}
