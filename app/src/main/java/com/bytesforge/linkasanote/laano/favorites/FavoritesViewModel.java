package com.bytesforge.linkasanote.laano.favorites;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.databinding.library.baseAdapters.BR;

import static com.google.common.base.Preconditions.checkNotNull;

public class FavoritesViewModel extends BaseObservable implements FavoritesContract.ViewModel {

    public static final String STATE_ACTION_MODE = "ACTION_MODE";

    public final ObservableBoolean toLinksButton = new ObservableBoolean(true);
    public final ObservableBoolean selectedCheckbox = new ObservableBoolean(true);
    public final ObservableBoolean toNotesButton = new ObservableBoolean(true);
    public final ObservableBoolean editButton = new ObservableBoolean(true);

    private FavoritesContract.Presenter presenter;
    private Context context;

    private int favoriteListSize = 0;
    private boolean actionMode;

    public FavoritesViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
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

        outState.putBoolean(STATE_ACTION_MODE, actionMode);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);

        actionMode = state.getBoolean(STATE_ACTION_MODE);
        setActionMode(actionMode);

        notifyChange();
    }

    private Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();

        defaultState.putBoolean(STATE_ACTION_MODE, false);

        return defaultState;
    }

    @Override
    public void setPresenter(@NonNull FavoritesContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Bindable
    public boolean isFavoritesEmpty() {
        return favoriteListSize <= 0;
    }

    @Override
    public void setFavoriteListSize(int favoriteListSize) {
        this.favoriteListSize = favoriteListSize;
        notifyPropertyChanged(BR.favoritesEmpty);
    }

    public boolean isActionMode() {
        return actionMode;
    }

    public void setActionMode(boolean actionMode) {
        this.actionMode = actionMode;
        if (this.actionMode) {
            toLinksButton.set(false);
            selectedCheckbox.set(true);
            toNotesButton.set(false);
            editButton.set(true);
        } else {
            toLinksButton.set(true);
            selectedCheckbox.set(false);
            toNotesButton.set(true);
            editButton.set(false);
        }
    }
}
