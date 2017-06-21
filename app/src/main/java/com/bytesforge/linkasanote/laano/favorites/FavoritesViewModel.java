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

package com.bytesforge.linkasanote.laano.favorites;

import android.content.Context;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.widget.FrameLayout;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.laano.BaseItemViewModel;

import static com.google.common.base.Preconditions.checkNotNull;

// NOTE: global viewModel, applied to fragment and every Item
public class FavoritesViewModel extends BaseItemViewModel implements FavoritesContract.ViewModel {

    private static final String TAG = FavoritesViewModel.class.getSimpleName();

    public static final String FILTER_OR_GATE_PREFIX = "*";
    public static final String FILTER_AND_GATE_PREFIX = "&";

    private Context context;

    public enum SnackbarId {
        DATABASE_ERROR,
        CONFLICT_RESOLUTION_SUCCESSFUL, CONFLICT_RESOLUTION_ERROR,
        CONFLICTED_ERROR, CLOUD_ERROR, SAVE_SUCCESS, DELETE_SUCCESS}

    @Bindable
    public SnackbarId snackbarId;

    public FavoritesViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
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
            case CONFLICTED_ERROR:
                Snackbar.make(view,
                        R.string.dialog_favorite_conflicted_error,
                        Snackbar.LENGTH_LONG).show();
                break;
            case CLOUD_ERROR:
                Snackbar.make(view,
                        R.string.dialog_favorite_cloud_error,
                        Snackbar.LENGTH_LONG).show();
                break;
            case SAVE_SUCCESS:
                Snackbar.make(view,
                        R.string.dialog_favorite_save_success,
                        Snackbar.LENGTH_LONG).show();
                break;
            case DELETE_SUCCESS:
                Snackbar.make(view,
                        R.string.dialog_favorite_delete_success,
                        Snackbar.LENGTH_LONG).show();
                break;
            default:
                throw new IllegalArgumentException("Unexpected snackbar has been requested");
        }
    }

    @Override
    public void notifyChange() {
        snackbarId = null;
        super.notifyChange();
    }

    public String getFilterPrefix(boolean andGate) {
        return andGate ? FILTER_AND_GATE_PREFIX : FILTER_OR_GATE_PREFIX;
    }

    public int getFavoriteBackground(boolean conflicted) {
        if (conflicted) {
            return ContextCompat.getColor(context, R.color.item_conflicted);
        }
        return ContextCompat.getColor(context, android.R.color.transparent);
    }

    public int getFilterBackground(@NonNull String favoriteId, boolean conflicted, String filterId) {
        checkNotNull(favoriteId);
        if (!conflicted && !isActionMode() && favoriteId.equals(filterId)) {
            return ContextCompat.getColor(context, R.color.item_filter);
        }
        return ContextCompat.getColor(context, android.R.color.transparent);
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

    @Override
    public void showConflictedErrorSnackbar() {
        snackbarId = SnackbarId.CONFLICTED_ERROR;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showCloudErrorSnackbar() {
        snackbarId = SnackbarId.CLOUD_ERROR;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showSaveSuccessSnackbar() {
        snackbarId = SnackbarId.SAVE_SUCCESS;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showDeleteSuccessSnackbar() {
        snackbarId = SnackbarId.DELETE_SUCCESS;
        notifyPropertyChanged(BR.snackbarId);
    }
}
