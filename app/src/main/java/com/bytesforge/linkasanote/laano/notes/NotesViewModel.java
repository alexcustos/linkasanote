package com.bytesforge.linkasanote.laano.notes;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
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

    public static final String FILTER_PREFIX = "*";

    private static final String STATE_VISIBLE_IDS = "VISIBLE_IDS";

    private ArrayList<String> visibleIds;

    private Context context;
    private Resources resources;

    @Bindable
    public SnackbarId snackbarId;

    @Bindable
    public boolean visibilityChanged; // NOTE: notification helper

    public enum SnackbarId {
        DATABASE_ERROR,
        CONFLICT_RESOLUTION_SUCCESSFUL, CONFLICT_RESOLUTION_ERROR,
        CONFLICTED_ERROR, CLOUD_ERROR, SAVE_SUCCESS, DELETE_SUCCESS};

    public NotesViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
        resources = context.getResources();
    }

    @Override
    public void saveInstanceState(@NonNull Bundle outState) {
        outState.putStringArrayList(STATE_VISIBLE_IDS, visibleIds);
        super.saveInstanceState(outState);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        visibleIds = state.getStringArrayList(STATE_VISIBLE_IDS);
        super.applyInstanceState(state); // notifyChange
    }

    @Override
    protected Bundle getDefaultInstanceState() {
        Bundle defaultState = super.getDefaultInstanceState();
        defaultState.putStringArrayList(STATE_VISIBLE_IDS, new ArrayList<>(0));
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

    public int getNoteBackground(
            String noteId, boolean conflicted, boolean readingMode, boolean changed) {
        if (conflicted) {
            return resources.getColor(R.color.item_conflicted, context.getTheme());
        }
        if (isSelected(noteId) && !isActionMode()) {
            return resources.getColor(readingMode
                    ? R.color.item_note_reading_mode_selected
                    : R.color.item_note_normal_mode_selected, context.getTheme());
        }
        return resources.getColor(android.R.color.transparent, context.getTheme());
    }

    public int getNoteNoteBackground(boolean conflicted, boolean changed) {
        if (conflicted) {
            return resources.getColor(R.color.note_conflicted_background, context.getTheme());
        }
        return resources.getColor(R.color.note_background, context.getTheme());
    }

    // Visibility

    public boolean isVisible(String id, boolean changed) {
        return isVisible(id);
    }

    @Override
    public boolean isVisible(String id) {
        return visibleIds.contains(id);
    }

    @Override
    public void toggleVisibility(@NonNull String id) {
        checkNotNull(id);
        if (isVisible(id)) {
            visibleIds.remove(id);
        } else {
            visibleIds.add(id);
        }
        notifyPropertyChanged(BR.visibilityChanged);
    }

    @Override
    public void setVisibility(String[] ids) {
        visibleIds.clear();
        if (ids != null) {
            visibleIds.addAll(Arrays.asList(ids));
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
