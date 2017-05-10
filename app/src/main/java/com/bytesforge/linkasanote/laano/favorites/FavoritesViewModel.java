package com.bytesforge.linkasanote.laano.favorites;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.SparseBooleanArray;
import android.widget.FrameLayout;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.laano.BaseItemViewModel;

import static com.google.common.base.Preconditions.checkNotNull;

// NOTE: global viewModel, applied to fragment and every Item
public class FavoritesViewModel extends BaseItemViewModel implements FavoritesContract.ViewModel {

    private static final String TAG = FavoritesViewModel.class.getSimpleName();

    public static final String FILTER_PREFIX = "@";

    private Context context;
    private Resources resources;

    private SparseBooleanArray selectedIds;
    private String searchText;

    public enum SnackbarId {
        DATABASE_ERROR,
        CONFLICT_RESOLUTION_SUCCESSFUL, CONFLICT_RESOLUTION_ERROR,
        CONFLICTED_ERROR, CLOUD_ERROR, SAVE_SUCCESS, DELETE_SUCCESS};

    @Bindable
    public SnackbarId snackbarId;

    public FavoritesViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
        resources = context.getResources();
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

    public String getFilterPrefix() {
        return FILTER_PREFIX;
    }

    public int getFavoriteBackground(String favoriteId, boolean conflicted, boolean changed) {
        if (conflicted) {
            return resources.getColor(R.color.item_conflicted, context.getTheme());
        }
        if (isSelected(favoriteId) && !isActionMode()) {
            return resources.getColor(R.color.item_favorite_selected, context.getTheme());
        }
        return resources.getColor(android.R.color.transparent, context.getTheme());
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
