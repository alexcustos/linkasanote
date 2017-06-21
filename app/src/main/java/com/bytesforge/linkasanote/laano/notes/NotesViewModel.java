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

package com.bytesforge.linkasanote.laano.notes;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.widget.FrameLayout;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.laano.BaseItemViewModel;

import java.util.ArrayList;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

// NOTE: global viewModel, applied to fragment and every Item
public class NotesViewModel extends BaseItemViewModel implements NotesContract.ViewModel {

    private static final String TAG = NotesViewModel.class.getSimpleName();

    public static final String FILTER_PREFIX = "#";

    private static final String STATE_EXPAND_BY_DEFAULT = "EXPAND_BY_DEFAULT";
    private static final String STATE_TOGGLE_IDS = "TOGGLE_IDS";

    private boolean expandByDefault;
    private ArrayList<String> toggledIds;

    private Context context;
    private Resources resources;

    @Bindable
    public SnackbarId snackbarId;

    @Bindable
    public boolean visibilityChanged; // NOTE: notification helper

    public enum SnackbarId {
        DATABASE_ERROR,
        CONFLICT_RESOLUTION_SUCCESSFUL, CONFLICT_RESOLUTION_ERROR,
        CONFLICTED_ERROR, CLOUD_ERROR, SAVE_SUCCESS, DELETE_SUCCESS}

    public NotesViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
        resources = context.getResources();
    }

    @Override
    public void saveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_EXPAND_BY_DEFAULT, expandByDefault);
        outState.putStringArrayList(STATE_TOGGLE_IDS, toggledIds);
        super.saveInstanceState(outState);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        expandByDefault = state.getBoolean(STATE_EXPAND_BY_DEFAULT);
        toggledIds = state.getStringArrayList(STATE_TOGGLE_IDS);
        super.applyInstanceState(state); // notifyChange
    }

    @Override
    protected Bundle getDefaultInstanceState() {
        Bundle defaultState = super.getDefaultInstanceState();
        defaultState.putBoolean(STATE_EXPAND_BY_DEFAULT, false);
        defaultState.putStringArrayList(STATE_TOGGLE_IDS, new ArrayList<>(0));
        return defaultState;
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
                        R.string.dialog_note_conflict_resolved_success,
                        Snackbar.LENGTH_LONG).show();
                break;
            case CONFLICT_RESOLUTION_ERROR:
                Snackbar.make(view,
                        R.string.dialog_note_conflict_resolved_error,
                        Snackbar.LENGTH_LONG).show();
                break;
            case CONFLICTED_ERROR:
                Snackbar.make(view,
                        R.string.dialog_note_conflicted_error,
                        Snackbar.LENGTH_LONG).show();
                break;
            case CLOUD_ERROR:
                Snackbar.make(view,
                        R.string.dialog_note_cloud_error,
                        Snackbar.LENGTH_LONG).show();
                break;
            case SAVE_SUCCESS:
                Snackbar.make(view,
                        R.string.dialog_note_save_success,
                        Snackbar.LENGTH_LONG).show();
                break;
            case DELETE_SUCCESS:
                Snackbar.make(view,
                        R.string.dialog_note_delete_success,
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

    public String getFilterPrefix() {
        return FILTER_PREFIX;
    }

    public String getToggleDescription(String noteId, boolean changed) {
        if (isVisible(noteId)) {
            return resources.getString(R.string.card_button_collapse_note_description);
        } else {
            return resources.getString(R.string.card_button_expand_note_description);
        }
    }

    public int getNoteBackground(boolean conflicted) {
        if (conflicted) {
            return ContextCompat.getColor(context, R.color.item_conflicted);
        }
        return ContextCompat.getColor(context, android.R.color.transparent);
    }

    public int getNoteReadingModeBackground(
            @NonNull String noteId, boolean conflicted, String filterId) {
        if (conflicted) {
            return ContextCompat.getColor(context, R.color.item_conflicted);
        } else if (noteId.equals(filterId)){
            return ContextCompat.getColor(context, R.color.item_reading_mode_filter);
        }
        return ContextCompat.getColor(context, android.R.color.transparent);
    }

    public int getNoteNoteBackground(boolean conflicted) {
        if (conflicted) {
            return ContextCompat.getColor(context, R.color.note_conflicted_background);
        }
        return ContextCompat.getColor(context, R.color.note_background);
    }

    public int getFilterBackground(@NonNull String noteId, String filterId) {
        checkNotNull(noteId);
        if (!isActionMode() && noteId.equals(filterId)) {
            return ContextCompat.getColor(context, R.color.item_filter);
        }
        return ContextCompat.getColor(context, android.R.color.transparent);
    }

    // Visibility

    @Override
    public void setExpandByDefault(boolean expandByDefault) {
        if (this.expandByDefault != expandByDefault) {
            this.expandByDefault = expandByDefault;
            toggledIds.clear();
        }
    }

    public boolean isVisible(String id, boolean changed) {
        return isVisible(id);
    }

    @Override
    public boolean isVisible(String id) {
        return expandByDefault != toggledIds.contains(id);
    }

    @Override
    public void toggleVisibility(@NonNull String id) {
        checkNotNull(id);
        if (!toggledIds.remove(id)) {
            toggledIds.add(id);
        }
        notifyPropertyChanged(BR.visibilityChanged);
    }

    @Override
    public void setVisibility(@NonNull String[] ids, boolean expand) {
        checkNotNull(ids);
        toggledIds.clear();
        if (expand && !expandByDefault) {
            toggledIds.addAll(Arrays.asList(ids));
        } else if (!expand && expandByDefault) {
            toggledIds.addAll(Arrays.asList(ids));
        }
        notifyChange();
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
