package com.bytesforge.linkasanote.laano.notes;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableInt;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.SparseBooleanArray;
import android.widget.FrameLayout;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.utils.SparseBooleanParcelableArray;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

// NOTE: global viewModel, applied to fragment and every Item
public class NotesViewModel extends BaseObservable implements NotesContract.ViewModel {

    public static final String FILTER_PREFIX = "*";

    private static final String TAG = NotesViewModel.class.getSimpleName();

    private static final String STATE_ACTION_MODE = "ACTION_MODE";
    private static final String STATE_LIST_SIZE = "LIST_SIZE";
    private static final String STATE_SELECTED_IDS = "SELECTED_IDS";
    private static final String STATE_VISIBLE_NOTE_IDS = "VISIBLE_NOTE_IDS";
    private static final String STATE_SEARCH_TEXT = "SEARCH_TEXT";
    private static final String STATE_PROGRESS_OVERLAY = "PROGRESS_OVERLAY";

    public final ObservableBoolean actionMode = new ObservableBoolean();
    public final ObservableInt noteListSize = new ObservableInt();

    private NotesContract.Presenter presenter;
    private LaanoUiManager laanoUiManager; // TODO: remove
    private Context context;
    private Resources resources;

    private SparseBooleanArray selectedIds;
    private SparseBooleanArray visibleNoteIds;
    private String searchText;

    public enum SnackbarId {DATABASE_ERROR};

    @Bindable
    public SnackbarId snackbarId;

    @Bindable
    public boolean progressOverlay;

    @Bindable
    public boolean selectionChanged; // NOTE: notification helper

    @Bindable
    public boolean noteVisibilityChanged; // NOTE: notification helper

    public NotesViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
        resources = context.getResources();
    }

    @Bindable
    public boolean isNoteListEmpty() {
        return noteListSize.get() <= 0;
    }

    @BindingAdapter({"snackbarId"})
    public static void showSnackbar(FrameLayout view, SnackbarId snackbarId) {
        if (snackbarId == null) return;

        switch (snackbarId) {
            case DATABASE_ERROR:
                Snackbar.make(view, R.string.error_database, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.snackbar_button_ok, v -> { /* just inform */ })
                        .show();
                break;
            default:
                throw new IllegalArgumentException("Unexpected snackbar has been requested");
        }
    }

    @Override
    public void setInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            applyInstanceState(getDefaultInstanceState());
        } else {
            applyInstanceState(savedInstanceState);
        }
    }

    @Override
    public void saveInstanceState(@NonNull Bundle outState) {
        checkNotNull(outState);

        outState.putBoolean(STATE_ACTION_MODE, actionMode.get());
        outState.putInt(STATE_LIST_SIZE, noteListSize.get());
        outState.putParcelable(STATE_SELECTED_IDS, new SparseBooleanParcelableArray(selectedIds));
        outState.putParcelable(STATE_VISIBLE_NOTE_IDS, new SparseBooleanParcelableArray(visibleNoteIds));
        outState.putString(STATE_SEARCH_TEXT, searchText);
        outState.putBoolean(STATE_PROGRESS_OVERLAY, progressOverlay);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);

        actionMode.set(state.getBoolean(STATE_ACTION_MODE));
        noteListSize.set(state.getInt(STATE_LIST_SIZE));
        selectedIds = state.getParcelable(STATE_SELECTED_IDS);
        visibleNoteIds = state.getParcelable(STATE_VISIBLE_NOTE_IDS);
        searchText = state.getString(STATE_SEARCH_TEXT);
        progressOverlay = state.getBoolean(STATE_PROGRESS_OVERLAY);

        notifyChange();
    }

    private Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();

        defaultState.putBoolean(STATE_ACTION_MODE, false);
        // NOTE: do not show empty list warning if empty state is not confirmed
        defaultState.putInt(STATE_LIST_SIZE, Integer.MAX_VALUE);
        defaultState.putParcelable(STATE_SELECTED_IDS, new SparseBooleanParcelableArray());
        defaultState.putParcelable(STATE_VISIBLE_NOTE_IDS, new SparseBooleanParcelableArray());
        defaultState.putString(STATE_SEARCH_TEXT, null);
        defaultState.putBoolean(STATE_PROGRESS_OVERLAY, false);

        return defaultState;
    }

    @Override
    public void notifyChange() {
        snackbarId = null;
        super.notifyChange();
    }

    @Override
    public void setPresenter(@NonNull NotesContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    public void setLaanoUiManager(@NonNull LaanoUiManager laanoUiManager) {
        this.laanoUiManager = checkNotNull(laanoUiManager);
    }

    public void setNoteListSize(int noteListSize) {
        boolean firstLoad = (this.noteListSize.get() == Integer.MAX_VALUE);
        this.noteListSize.set(noteListSize);
        if (firstLoad) {
            if (presenter.isExpandNotes()) expandAllNotes();
            else collapseAllNotes();
        }
        notifyPropertyChanged(BR.noteListEmpty);
    }

    public String getFilterPrefix() {
        return FILTER_PREFIX;
    }

    public int getNoteBackground(String noteId, boolean conflicted, boolean changed) {
        if (conflicted) {
            return resources.getColor(R.color.item_conflicted, context.getTheme());
        }
        int position = presenter.getPosition(noteId);
        if (isSelected(position) && !isActionMode()) {
            return resources.getColor(R.color.item_note_selected, context.getTheme());
        }
        return resources.getColor(android.R.color.transparent, context.getTheme());
    }

    @Override
    public boolean isActionMode() {
        return actionMode.get();
    }

    @Override
    public void enableActionMode() {
        selectedIds.clear();
        actionMode.set(true);
        notifyChange();
    }

    @Override
    public void disableActionMode() {
        selectedIds.clear();
        actionMode.set(false);
        presenter.selectNoteFilter();
        notifyChange();
    }

    @Override
    public String getSearchText() {
        return searchText;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    // Selection

    public boolean isSelected(String noteId, boolean changed) {
        int position = presenter.getPosition(noteId);
        return isSelected(position);
    }

    private boolean isSelected(int position) {
        return selectedIds.get(position);
    }

    @Override
    public void toggleSelection() {
        int listSize = noteListSize.get();
        if (listSize == Integer.MAX_VALUE || listSize <= 0) return;
        if (selectedIds.size() > listSize / 2) {
            selectedIds.clear();
        } else {
            for (int i = 0; i < listSize; i++) {
                selectedIds.put(i, true);
            }
        }
        notifyChange();
    }

    @Override
    public void toggleSelection(int position) {
        if (isSelected(position)) {
            selectedIds.delete(position);
        } else {
            selectedIds.put(position, true);
        }
        notifyPropertyChanged(BR.selectionChanged);
    }

    /**
     * @return Return true if the item has been selected
     */
    @Override
    public boolean toggleSingleSelection(int position) {
        boolean selected;
        int size = selectedIds.size();
        if (size == 1 && selectedIds.get(position)) {
            selectedIds.delete(position);
            notifyPropertyChanged(BR.selectionChanged);
            selected = false;
        } else if (size <= 0) {
            selectedIds.put(position, true);
            notifyPropertyChanged(BR.selectionChanged);
            selected = true;
        } else {
            selectedIds.clear();
            selectedIds.put(position, true);
            notifyChange();
            selected = true;
        }
        return selected;
    }

    @Override
    public void setSingleSelection(int position, boolean selected) {
        int size = selectedIds.size();
        if (selectedIds.get(position) != selected) {
            toggleSingleSelection(position);
        } else if (selected && size > 1) {
            selectedIds.clear();
            selectedIds.put(position, true);
            notifyChange();
        } else if (!selected && size > 0) {
            selectedIds.clear();
            notifyChange();
        }
    }

    @Override
    public void removeSelection() {
        selectedIds.clear();
        notifyChange();
    }

    @Override
    public void removeSelection(int position) {
        selectedIds.delete(position);
        notifyPropertyChanged(BR.selectionChanged);
    }

    @Override
    public int getSelectedCount() {
        return selectedIds.size();
    }

    @Override
    public int[] getSelectedIds() {
        List<Integer> ids = new LinkedList<>();
        for (int i = selectedIds.size() - 1; i >= 0; i--) { // NOTE: reverse order
            if (selectedIds.valueAt(i)) {
                ids.add(selectedIds.keyAt(i));
            }
        }
        return ids.stream().mapToInt(i -> i).toArray();
    }

    // Note Visibility

    public boolean isNoteVisible(String noteId, boolean changed) {
        int position = presenter.getPosition(noteId);
        return isNoteVisible(position);
    }

    private boolean isNoteVisible(int position) {
        return visibleNoteIds.get(position);
    }

    @Override
    public void toggleNoteVisibility(int position) {
        if (isNoteVisible(position)) {
            visibleNoteIds.delete(position);
        } else {
            visibleNoteIds.put(position, true);
        }
        notifyPropertyChanged(BR.noteVisibilityChanged);
    }

    @Override
    public void expandAllNotes() {
        int listSize = noteListSize.get();
        if (listSize == Integer.MAX_VALUE || listSize <= 0) return;

        for (int i = 0; i < listSize; i++) {
            visibleNoteIds.put(i, true);
        }
        notifyChange();
    }

    @Override
    public void collapseAllNotes() {
        visibleNoteIds.clear();
        notifyChange();
    }

    // Snackbar

    @Override
    public void showDatabaseErrorSnackbar() {
        snackbarId = SnackbarId.DATABASE_ERROR;
        notifyPropertyChanged(BR.snackbarId);
    }

    // Progress

    @Override
    public void showProgressOverlay() {
        if (!progressOverlay) {
            progressOverlay = true;
            notifyPropertyChanged(BR.progressOverlay);
        }
    }

    @Override
    public void hideProgressOverlay() {
        if (progressOverlay) {
            progressOverlay = false;
            notifyPropertyChanged(BR.progressOverlay);
        }
    }
}
