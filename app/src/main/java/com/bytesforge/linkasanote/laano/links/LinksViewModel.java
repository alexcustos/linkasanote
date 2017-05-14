package com.bytesforge.linkasanote.laano.links;

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
public class LinksViewModel extends BaseItemViewModel implements LinksContract.ViewModel {

    private static final String TAG = LinksViewModel.class.getSimpleName();

    public static final String FILTER_PREFIX = "#";

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
        OPEN_LINK_ERROR,
        CONFLICTED_ERROR, CLOUD_ERROR, SAVE_SUCCESS, DELETE_EXTRA_ERROR, DELETE_SUCCESS}

    public LinksViewModel(@NonNull Context context) {
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
                        R.string.dialog_link_conflict_resolved_success,
                        Snackbar.LENGTH_LONG).show();
                break;
            case CONFLICT_RESOLUTION_ERROR:
                Snackbar.make(view,
                        R.string.dialog_link_conflict_resolved_error,
                        Snackbar.LENGTH_LONG).show();
                break;
            case OPEN_LINK_ERROR:
                Snackbar.make(view,
                        R.string.dialog_link_open_error,
                        Snackbar.LENGTH_LONG).show();
                break;
            case CONFLICTED_ERROR:
                Snackbar.make(view,
                        R.string.dialog_link_conflicted_error,
                        Snackbar.LENGTH_LONG).show();
                break;
            case CLOUD_ERROR:
                Snackbar.make(view,
                        R.string.dialog_link_cloud_error,
                        Snackbar.LENGTH_LONG).show();
                break;
            case SAVE_SUCCESS:
                Snackbar.make(view,
                        R.string.dialog_link_save_success,
                        Snackbar.LENGTH_LONG).show();
                break;
            case DELETE_EXTRA_ERROR:
                Snackbar.make(view,
                        R.string.dialog_link_delete_extra_error,
                        Snackbar.LENGTH_LONG).show();
                break;
            case DELETE_SUCCESS:
                Snackbar.make(view,
                        R.string.dialog_link_delete_success,
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

    public String getLinkCounter(int counter) {
        return "(" + counter + ")";
    }

    public String getToggleDescription(String linkId, boolean changed) {
        if (isVisible(linkId)) {
            return resources.getString(R.string.card_button_collapse_notes_description);
        } else {
            return resources.getString(R.string.card_button_expand_notes_description);
        }
    }

    public int getLinkBackground(String linkId, boolean conflicted, boolean changed) {
        if (conflicted) {
            return ContextCompat.getColor(context, R.color.item_conflicted);
        }
        if (isSelected(linkId) && !isActionMode()) {
            return ContextCompat.getColor(context, R.color.item_link_selected);
        }
        return ContextCompat.getColor(context, android.R.color.transparent);
    }

    // Link Visibility

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
    public void showOpenLinkErrorSnackbar() {
        snackbarId = SnackbarId.OPEN_LINK_ERROR;
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
    public void showDeleteExtraErrorSnackbar() {
        snackbarId = SnackbarId.DELETE_EXTRA_ERROR;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showDeleteSuccessSnackbar() {
        snackbarId = SnackbarId.DELETE_SUCCESS;
        notifyPropertyChanged(BR.snackbarId);
    }
}
