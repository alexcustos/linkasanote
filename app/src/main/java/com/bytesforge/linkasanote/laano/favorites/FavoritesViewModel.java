package com.bytesforge.linkasanote.laano.favorites;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableInt;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseBooleanArray;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.utils.SparseBooleanParcelableArray;

import static com.google.common.base.Preconditions.checkNotNull;

// NOTE: global viewModel, applied to fragment and every Item
public class FavoritesViewModel extends BaseObservable implements FavoritesContract.ViewModel {

    private static final String STATE_ACTION_MODE = "ACTION_MODE";
    private static final String STATE_LIST_SIZE = "LIST_SIZE";
    private static final String STATE_SELECTED_IDS = "SELECTED_IDS";

    public final ObservableBoolean actionMode = new ObservableBoolean();
    public final ObservableInt favoriteListSize = new ObservableInt(0);

    private FavoritesContract.Presenter presenter;
    private Context context;

    private SparseBooleanArray selectedIds;

    public FavoritesViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
    }

    @Bindable
    public boolean isFavoriteListEmpty() {
        return favoriteListSize.get() <= 0;
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
    public void loadInstanceState(@NonNull Bundle outState) {
        checkNotNull(outState);

        outState.putBoolean(STATE_ACTION_MODE, actionMode.get());
        outState.putInt(STATE_LIST_SIZE, favoriteListSize.get());
        outState.putParcelable(STATE_SELECTED_IDS, new SparseBooleanParcelableArray(selectedIds));
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);

        actionMode.set(state.getBoolean(STATE_ACTION_MODE));
        favoriteListSize.set(state.getInt(STATE_LIST_SIZE));
        selectedIds = state.getParcelable(STATE_SELECTED_IDS);

        notifyChange();
    }

    private Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();

        defaultState.putBoolean(STATE_ACTION_MODE, false);
        defaultState.putInt(STATE_LIST_SIZE, 0);
        defaultState.putParcelable(STATE_SELECTED_IDS, new SparseBooleanParcelableArray());

        return defaultState;
    }

    @Override
    public void setPresenter(@NonNull FavoritesContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    public void setFavoriteListSize(int favoriteListSize) {
        this.favoriteListSize.set(favoriteListSize);
        notifyPropertyChanged(BR.favoriteListEmpty);
    }

    @Override
    public boolean isActionMode() {
        return actionMode.get();
    }

    @Override
    public void enableActionMode() {
        actionMode.set(true);
        notifyChange(); // NOTE: otherwise, the only current Item will be notified
    }

    @Override
    public void disableActionMode() {
        actionMode.set(false);
        notifyChange();
    }

    // Selection

    @Override
    public boolean isSelected(int position) {
        return selectedIds.get(position);
    }

    @Override
    public void toggleSelection(int position) {
        if (isSelected(position)) {
            selectedIds.delete(position);
        } else {
            selectedIds.put(position, true);
        }
    }

    @Override
    public void removeSelection() {
        selectedIds.clear();
    }

    @Override
    public void removeSelection(int position) {
        selectedIds.delete(position);
    }

    @Override
    public int getSelectedCount() {
        return selectedIds.size();
    }

    @Override
    public SparseBooleanArray getSelectedIds() {
        return selectedIds;
    }
}
