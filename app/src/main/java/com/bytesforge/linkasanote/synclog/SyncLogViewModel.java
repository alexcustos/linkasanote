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

package com.bytesforge.linkasanote.synclog;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableInt;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.SyncResult;
import com.bytesforge.linkasanote.utils.CommonUtils;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncLogViewModel extends BaseObservable implements SyncLogContract.ViewModel {

    private static final String STATE_LIST_SIZE = "LIST_SIZE";
    private static final String STATE_PROGRESS_OVERLAY = "PROGRESS_OVERLAY";

    public static final String STATE_RECYCLER_LAYOUT = "RECYCLER_LAYOUT";

    public final ObservableInt listSize = new ObservableInt();

    @Bindable
    public boolean progressOverlay;

    private Context context;

    public enum SnackbarId {
        DATABASE_ERROR}

    public SyncLogViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
    }

    @Bindable
    public SnackbarId snackbarId;

    @Bindable
    public boolean isListEmpty() {
        return listSize.get() <= 0;
    }

    @BindingAdapter({"snackbarId"})
    public static void showSnackbar(CoordinatorLayout view, SnackbarId snackbarId) {
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
    public void setPresenter(@NonNull SyncLogContract.Presenter presenter) {
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
        outState.putInt(STATE_LIST_SIZE, listSize.get());
        outState.putBoolean(STATE_PROGRESS_OVERLAY, progressOverlay);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        listSize.set(state.getInt(STATE_LIST_SIZE));
        progressOverlay = state.getBoolean(STATE_PROGRESS_OVERLAY);

        notifyChange();
    }

    @Override
    public Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();
        // NOTE: do not show empty list warning if empty state is not confirmed
        defaultState.putInt(STATE_LIST_SIZE, Integer.MAX_VALUE);
        defaultState.putBoolean(STATE_PROGRESS_OVERLAY, false);

        return defaultState;
    }

    @Override
    public int getListSize() {
        return listSize.get();
    }

    public String getSyncResult(int position, @NonNull SyncResult syncResult) {
        checkNotNull(syncResult);
        return position + ". " + syncResult.toString();
    }

    public String getStarted(long started) {
        Date date = new Date(started);
        return CommonUtils.formatDateTime(context, date);
    }

    public boolean isLast(int position) {
        return (position + 1 >= getListSize());
    }

    /**
     * @return Returns true if listSize has never been set before
     */
    @Override
    public boolean setListSize(int listSize) {
        boolean firstLoad = (this.listSize.get() == Integer.MAX_VALUE);
        this.listSize.set(listSize);
        notifyPropertyChanged(BR.listEmpty);
        return firstLoad;
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

    // Snackbar

    @Override
    public void showDatabaseErrorSnackbar() {
        snackbarId = SnackbarId.DATABASE_ERROR;
        notifyPropertyChanged(BR.snackbarId);
    }
}
