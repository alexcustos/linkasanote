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

package com.bytesforge.linkasanote.laano;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BR;

import java.util.ArrayList;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BaseItemViewModel extends BaseObservable implements
        BaseItemViewModelInterface {

    private static final String TAG = BaseItemViewModel.class.getSimpleName();

    private static final String STATE_ACTION_MODE = "ACTION_MODE";
    private static final String STATE_LIST_SIZE = "LIST_SIZE";
    private static final String STATE_SELECTED_IDS = "SELECTED_IDS";
    private static final String STATE_FILTER_ID = "FILTER_ID";
    private static final String STATE_SEARCH_TEXT = "SEARCH_TEXT";
    private static final String STATE_PROGRESS_OVERLAY = "PROGRESS_OVERLAY";

    public static final String STATE_RECYCLER_LAYOUT = "RECYCLER_LAYOUT";

    public final ObservableBoolean actionMode = new ObservableBoolean();
    public final ObservableInt listSize = new ObservableInt();
    public final ObservableField<String> filterId = new ObservableField<>();

    private ArrayList<String> selectedIds;
    private String searchText;

    @Bindable
    public boolean progressOverlay;

    @Bindable
    public boolean selectionChanged; // NOTE: notification helper

    @Bindable
    public boolean isListEmpty() {
        return listSize.get() <= 0;
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
        outState.putInt(STATE_LIST_SIZE, listSize.get());
        outState.putStringArrayList(STATE_SELECTED_IDS, selectedIds);
        outState.putString(STATE_FILTER_ID, filterId.get());
        outState.putString(STATE_SEARCH_TEXT, searchText);
        outState.putBoolean(STATE_PROGRESS_OVERLAY, progressOverlay);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        actionMode.set(state.getBoolean(STATE_ACTION_MODE));
        listSize.set(state.getInt(STATE_LIST_SIZE));
        selectedIds = state.getStringArrayList(STATE_SELECTED_IDS);
        filterId.set(state.getString(STATE_FILTER_ID));
        searchText = state.getString(STATE_SEARCH_TEXT);
        progressOverlay = state.getBoolean(STATE_PROGRESS_OVERLAY);

        notifyChange();
    }

    protected Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();

        defaultState.putBoolean(STATE_ACTION_MODE, false);
        // NOTE: do not show empty list warning if empty state is not confirmed
        defaultState.putInt(STATE_LIST_SIZE, Integer.MAX_VALUE);
        defaultState.putStringArrayList(STATE_SELECTED_IDS, new ArrayList<>(0));
        defaultState.putString(STATE_FILTER_ID, null);
        defaultState.putString(STATE_SEARCH_TEXT, null);
        defaultState.putBoolean(STATE_PROGRESS_OVERLAY, false);

        return defaultState;
    }

    @Override
    public int getListSize() {
        return listSize.get();
    }

    @Override
    public void setListSize(int listSize) {
        this.listSize.set(listSize);
        notifyPropertyChanged(BR.listEmpty);
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
        notifyChange();
    }

    /**
     * @return Return true if the filter has been set
     */
    @Override
    public boolean toggleFilterId(@NonNull String filterId) {
        checkNotNull(filterId);
        if (filterId.equals(this.filterId.get())) {
            this.filterId.set(null);
            return false;
        } else {
            this.filterId.set(filterId);
            return true;
        }
    }

    @Override
    public void setFilterId(String filterId) {
        this.filterId.set(filterId);
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

    public boolean isSelected(String id, boolean changed) {
        return isSelected(id);
    }

    @Override
    public boolean isSelected(String id) {
        return selectedIds.contains(id);
    }

    @Override
    public void setSelection(String[] ids) {
        selectedIds.clear();
        if (ids != null) {
            selectedIds.addAll(Arrays.asList(ids));
        }
        notifyChange();
    }

    @Override
    public void toggleSelection(@NonNull String id) {
        checkNotNull(id);
        if (isSelected(id)) {
            selectedIds.remove(id);
        } else {
            selectedIds.add(id);
        }
        notifyPropertyChanged(BR.selectionChanged);
    }

    /**
     * @return Return true if the item has been selected
     */
    @Override
    public boolean toggleSingleSelection(@NonNull String id) {
        checkNotNull(id);
        boolean selected;
        int size = selectedIds.size();
        if (size == 1 && selectedIds.contains(id)) {
            selectedIds.remove(id);
            notifyPropertyChanged(BR.selectionChanged);
            selected = false;
        } else if (size <= 0) {
            selectedIds.add(id);
            notifyPropertyChanged(BR.selectionChanged);
            selected = true;
        } else {
            selectedIds.clear();
            selectedIds.add(id);
            notifyChange();
            selected = true;
        }
        return selected;
    }

    @Override
    public void setSingleSelection(@NonNull String id, boolean selected) {
        checkNotNull(id);
        int size = selectedIds.size();
        if (selectedIds.contains(id) != selected) {
            toggleSingleSelection(id);
        } else if (selected && size > 1) {
            selectedIds.clear();
            selectedIds.add(id);
            notifyChange();
        } else if (!selected && size > 0) {
            selectedIds.clear();
            notifyChange();
        }
    }

    @Override
    public void removeSelection() {
        if (selectedIds.size() > 0) {
            selectedIds.clear();
            notifyChange();
        }
    }

    @Override
    public void removeSelection(@NonNull String id) {
        checkNotNull(id);
        if (selectedIds.remove(id)) {
            notifyPropertyChanged(BR.selectionChanged);
        }
    }

    @Override
    public int getSelectedCount() {
        return selectedIds.size();
    }

    @Override
    public ArrayList<String> getSelectedIds() {
        return selectedIds;
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
