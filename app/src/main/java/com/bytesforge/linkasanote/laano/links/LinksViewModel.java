package com.bytesforge.linkasanote.laano.links;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.BindingConversion;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableInt;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.Html;
import android.text.Spanned;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.ActivityUtils;
import com.bytesforge.linkasanote.utils.SparseBooleanParcelableArray;
import com.google.common.base.Strings;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

// NOTE: global viewModel, applied to fragment and every Item
public class LinksViewModel extends BaseObservable implements LinksContract.ViewModel {

    private static final float PROGRESS_OVERLAY_ALPHA = 0.4f;
    private static final long PROGRESS_OVERLAY_DURATION = 200; // ms
    private static final long PROGRESS_OVERLAY_SHOW_DELAY = 200; // ms

    private static final String STATE_ACTION_MODE = "ACTION_MODE";
    private static final String STATE_LIST_SIZE = "LIST_SIZE";
    private static final String STATE_SELECTED_IDS = "SELECTED_IDS";
    private static final String STATE_FILTER_TYPE = "FILTER_TYPE";
    private static final String STATE_SEARCH_TEXT = "SEARCH_TEXT";
    private static final String STATE_PROGRESS_OVERLAY = "PROGRESS_OVERLAY";

    public final ObservableBoolean actionMode = new ObservableBoolean();
    public final ObservableInt linkListSize = new ObservableInt();

    private LinksContract.Presenter presenter;
    private LaanoUiManager laanoUiManager;
    private Resources resources;

    private SparseBooleanArray selectedIds;
    private LinksFilterType filterType;
    private String searchText;

    public enum SnackbarId {
        DATABASE_ERROR, CONFLICT_RESOLUTION_SUCCESSFUL, CONFLICT_RESOLUTION_ERROR};

    @Bindable
    public SnackbarId snackbarId;

    @Bindable
    public boolean progressOverlay;

    public LinksViewModel(@NonNull Context context) {
        resources = checkNotNull(context).getResources();
    }

    @BindingConversion
    public static ColorDrawable convertColorToDrawable(int color) {
        return new ColorDrawable(color);
    }

    @Bindable
    public boolean isLinkListEmpty() {
        return linkListSize.get() <= 0;
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
            default:
                throw new IllegalArgumentException("Unexpected snackbar has been requested");
        }
    }

    @BindingAdapter({"progressOverlay"})
    public static void showProgressOverlay(FrameLayout view, boolean progressOverlay) {
        if (progressOverlay) {
            ActivityUtils.animateAlpha(view, View.VISIBLE, PROGRESS_OVERLAY_ALPHA,
                    PROGRESS_OVERLAY_DURATION, PROGRESS_OVERLAY_SHOW_DELAY);
        } else {
            ActivityUtils.animateAlpha(view, View.GONE, 0, PROGRESS_OVERLAY_DURATION, 0);
        }
    }

    @BindingAdapter({"enabled"})
    public static void setImageButtonEnabled(ImageButton view, boolean enabled) {
        view.setClickable(enabled);
        view.setFocusable(enabled);
        view.setEnabled(enabled);

        if (enabled) view.setAlpha(1.0f);
        else view.setAlpha(Settings.GLOBAL_IMAGE_BUTTON_ALPHA_DISABLED);
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
        outState.putInt(STATE_LIST_SIZE, linkListSize.get());
        outState.putParcelable(STATE_SELECTED_IDS, new SparseBooleanParcelableArray(selectedIds));
        outState.putInt(STATE_FILTER_TYPE, filterType.ordinal());
        outState.putString(STATE_SEARCH_TEXT, searchText);
        outState.putBoolean(STATE_PROGRESS_OVERLAY, progressOverlay);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);

        actionMode.set(state.getBoolean(STATE_ACTION_MODE));
        linkListSize.set(state.getInt(STATE_LIST_SIZE));
        selectedIds = state.getParcelable(STATE_SELECTED_IDS);
        setFilterType(LinksFilterType.values()[state.getInt(STATE_FILTER_TYPE)]);
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
        defaultState.putInt(STATE_FILTER_TYPE, LinksFilterType.LINKS_ALL.ordinal());
        defaultState.putString(STATE_SEARCH_TEXT, null);
        defaultState.putBoolean(STATE_PROGRESS_OVERLAY, false);

        return defaultState;
    }

    @Override
    public void setPresenter(@NonNull LinksContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    public void setLaanoUiManager(@NonNull LaanoUiManager laanoUiManager) {
        this.laanoUiManager = checkNotNull(laanoUiManager);
    }

    public void setLinkListSize(int linkListSize) {
        this.linkListSize.set(linkListSize);
        notifyPropertyChanged(BR.linkListEmpty);
    }

    public String getLinkCounter(int counter) {
        return "(" + counter + ")";
    }

    public Spanned getTags(String tags) {
        if (Strings.isNullOrEmpty(tags)) return null;

        String tagsCaption = resources.getString(R.string.card_link_tags_caption).toUpperCase();
        return Html.fromHtml("<strong>" + tagsCaption + ":</strong> " + tags,
                Html.FROM_HTML_MODE_LEGACY);
    }

    @Override
    public boolean isActionMode() {
        return actionMode.get();
    }

    @Override
    public void enableActionMode() {
        actionMode.set(true);
        snackbarId = null; // TODO: get rid of this workaround
        notifyChange(); // NOTE: otherwise, the only current Item will be notified
    }

    @Override
    public void disableActionMode() {
        actionMode.set(false);
        snackbarId = null;
        notifyChange();
    }

    @Override
    public LinksFilterType getFilterType() {
        return filterType;
    }

    @Override
    public void setFilterType(LinksFilterType filterType) {
        this.filterType = filterType;
        laanoUiManager.setLinkFilterType(filterType);
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
    public boolean isSelected(String linkId) {
        int position = presenter.getPosition(linkId);
        return isSelected(position);
    }

    private boolean isSelected(int position) {
        return selectedIds.get(position);
    }

    @Override
    public void toggleSelection() {
        int listSize = linkListSize.get();
        if (listSize == Integer.MAX_VALUE || listSize <= 0) return;
        if (selectedIds.size() > listSize / 2) {
            selectedIds.clear();
        } else {
            for (int i = 0; i < listSize; i++) {
                selectedIds.put(i, true);
            }
        }
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
    public int[] getSelectedIds() {
        List<Integer> ids = new LinkedList<>();
        for (int i = selectedIds.size() - 1; i >= 0; i--) { // NOTE: reverse order
            if (selectedIds.valueAt(i)) {
                ids.add(selectedIds.keyAt(i));
            }
        }
        return ids.stream().mapToInt(i -> i).toArray();
    }

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
