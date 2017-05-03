package com.bytesforge.linkasanote.laano.favorites;

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
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.utils.SparseBooleanParcelableArray;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

// NOTE: global viewModel, applied to fragment and every Item
public class FavoritesViewModel extends BaseObservable implements FavoritesContract.ViewModel {

    public static final String FILTER_PREFIX = "@";

    private static final String STATE_ACTION_MODE = "ACTION_MODE";
    private static final String STATE_LIST_SIZE = "LIST_SIZE";
    private static final String STATE_SELECTED_IDS = "SELECTED_IDS";
    private static final String STATE_FILTER_TYPE = "FILTER_TYPE";
    private static final String STATE_SEARCH_TEXT = "SEARCH_TEXT";
    private static final String STATE_PROGRESS_OVERLAY = "PROGRESS_OVERLAY";

    public final ObservableBoolean actionMode = new ObservableBoolean();
    public final ObservableInt favoriteListSize = new ObservableInt();

    private FavoritesContract.Presenter presenter;
    private LaanoUiManager laanoUiManager;
    private Context context;
    private Resources resources;

    private SparseBooleanArray selectedIds;
    private FilterType filterType;
    private String searchText;

    public enum SnackbarId {
        DATABASE_ERROR, CONFLICT_RESOLUTION_SUCCESSFUL, CONFLICT_RESOLUTION_ERROR};

    @Bindable
    public SnackbarId snackbarId;

    @Bindable
    public boolean progressOverlay;

    @Bindable
    public boolean selectionChanged; // NOTE: notification helper

    public FavoritesViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
        resources = context.getResources();
    }

    @Bindable
    public boolean isFavoriteListEmpty() {
        return favoriteListSize.get() <= 0;
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
            case CONFLICT_RESOLUTION_SUCCESSFUL:
                Snackbar.make(view,
                        R.string.dialog_favorite_conflict_resolved_success,
                        Snackbar.LENGTH_LONG).show();
                break;
            case CONFLICT_RESOLUTION_ERROR:
                Snackbar.make(view,
                        R.string.dialog_favorite_conflict_resolved_error,
                        Snackbar.LENGTH_LONG).show();
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
        outState.putInt(STATE_LIST_SIZE, favoriteListSize.get());
        outState.putParcelable(STATE_SELECTED_IDS, new SparseBooleanParcelableArray(selectedIds));
        outState.putInt(STATE_FILTER_TYPE, filterType.ordinal());
        outState.putString(STATE_SEARCH_TEXT, searchText);
        outState.putBoolean(STATE_PROGRESS_OVERLAY, progressOverlay);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);

        actionMode.set(state.getBoolean(STATE_ACTION_MODE));
        favoriteListSize.set(state.getInt(STATE_LIST_SIZE));
        selectedIds = state.getParcelable(STATE_SELECTED_IDS);
        setFilterType(FilterType.values()[state.getInt(STATE_FILTER_TYPE)]);
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
        defaultState.putInt(STATE_FILTER_TYPE, FilterType.ALL.ordinal());
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
    public void setPresenter(@NonNull FavoritesContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    public void setLaanoUiManager(@NonNull LaanoUiManager laanoUiManager) {
        this.laanoUiManager = checkNotNull(laanoUiManager);
    }

    public void setFavoriteListSize(int favoriteListSize) {
        this.favoriteListSize.set(favoriteListSize);
        notifyPropertyChanged(BR.favoriteListEmpty);
    }

    public String getFilterPrefix() {
        return FILTER_PREFIX;
    }

    public int getFavoriteBackground(String favoriteId, boolean conflicted, boolean changed) {
        if (conflicted) {
            return resources.getColor(R.color.item_conflicted, context.getTheme());
        }
        int position = presenter.getPosition(favoriteId);
        if (isSelected(position) && !isActionMode()) {
            return resources.getColor(R.color.item_favorite_selected, context.getTheme());
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
        presenter.selectFavoriteFilter();
        notifyChange();
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public void setFilterType(FilterType filterType) {
        this.filterType = filterType;
        laanoUiManager.setFilterType(LaanoFragmentPagerAdapter.FAVORITES_TAB, filterType);
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

    @Override
    public boolean isSelected(String favoriteId, boolean changed) {
        if (!isActionMode()) return false;

        int position = presenter.getPosition(favoriteId);
        return isSelected(position);
    }

    private boolean isSelected(int position) {
        return selectedIds.get(position);
    }

    @Override
    public void toggleSelection() {
        int listSize = favoriteListSize.get();
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

    // Snackbar

    @Override
    public void showDatabaseErrorSnackbar() {
        snackbarId = SnackbarId.DATABASE_ERROR;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showConflictResolutionSuccessfulSnackbar() {
        snackbarId = SnackbarId.CONFLICT_RESOLUTION_SUCCESSFUL;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showConflictResolutionErrorSnackbar() {
        snackbarId = SnackbarId.CONFLICT_RESOLUTION_ERROR;
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
