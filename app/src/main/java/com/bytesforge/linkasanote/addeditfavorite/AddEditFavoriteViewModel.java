package com.bytesforge.linkasanote.addeditfavorite;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.widget.LinearLayout;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Tag;
import com.google.common.base.Strings;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class AddEditFavoriteViewModel extends BaseObservable implements
        AddEditFavoriteContract.ViewModel {

    public static final String STATE_FAVORITE_NAME = "FAVORITE_NAME";
    public static final String STATE_ADD_BUTTON = "ADD_BUTTON";
    public static final String STATE_ADD_BUTTON_TEXT = "ADD_BUTTON_TEXT";
    public static final String STATE_NAME_ERROR_TEXT = "NAME_ERROR_TEXT";

    public final ObservableField<String> favoriteName = new ObservableField<>();
    public final ObservableBoolean addButton = new ObservableBoolean(false);
    private int addButtonText;

    private FavoriteTagsCompletionView favoriteTags;
    private Context context;
    private AddEditFavoriteContract.Presenter presenter;

    public enum SnackbarId {FAVORITE_EMPTY, FAVORITE_NOT_FOUND};

    @Bindable
    public SnackbarId snackbarId;

    @Bindable
    public String nameErrorText;

    public AddEditFavoriteViewModel(@NonNull Context context) {
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
    public void saveInstanceState(@NonNull Bundle outState) {
        checkNotNull(outState);

        outState.putString(STATE_FAVORITE_NAME, favoriteName.get());
        outState.putBoolean(STATE_ADD_BUTTON, addButton.get());
        outState.putInt(STATE_ADD_BUTTON_TEXT, addButtonText);
        outState.putString(STATE_NAME_ERROR_TEXT, nameErrorText);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);

        favoriteName.set(state.getString(STATE_FAVORITE_NAME));
        addButton.set(state.getBoolean(STATE_ADD_BUTTON));
        addButtonText = state.getInt(STATE_ADD_BUTTON_TEXT);
        nameErrorText = state.getString(STATE_NAME_ERROR_TEXT);
    }

    @Override
    public Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();

        defaultState.putString(STATE_FAVORITE_NAME, null);
        defaultState.putBoolean(STATE_ADD_BUTTON, false);
        int addButtonText = presenter.isNewFavorite()
                ? R.string.add_edit_favorite_new_button_title
                : R.string.add_edit_favorite_edit_button_title;
        defaultState.putInt(STATE_ADD_BUTTON_TEXT, addButtonText);
        defaultState.putString(STATE_NAME_ERROR_TEXT, null);

        return defaultState;
    }

    @Override
    public void setPresenter(@NonNull AddEditFavoriteContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setTagsCompletionView(@NonNull FavoriteTagsCompletionView completionView) {
        favoriteTags = completionView;
    }

    @BindingAdapter({"snackbarId"})
    public static void showSnackbar(LinearLayout view, SnackbarId snackbarId) {
        if (snackbarId == null) return;

        switch (snackbarId) {
            case FAVORITE_EMPTY:
                Snackbar.make(view,
                        R.string.add_edit_favorite_warning_empty,
                        Snackbar.LENGTH_LONG).show();
                break;
            case FAVORITE_NOT_FOUND:
                Snackbar.make(view,
                        R.string.add_edit_favorite_warning_not_existed,
                        Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    @BindingAdapter({"nameError"})
    public static void showNameError(TextInputLayout layout, @Nullable String nameErrorText) {
        if (nameErrorText != null) {
            layout.setError(nameErrorText);
            layout.setErrorEnabled(true);
            layout.requestFocus();
        } else {
            layout.setErrorEnabled(false);
        }
    }

    @Bindable
    public String getAddButtonText() {
        return context.getResources().getString(addButtonText);
    }

    public void onAddButtonClick() {
        favoriteTags.performCompletion();
        // NOTE: there is no way to pass these values directly to the presenter
        presenter.saveFavorite(favoriteName.get(), favoriteTags.getObjects());
    }

    @Override
    public void enableAddButton() {
        addButton.set(true);
    }

    @Override
    public void disableAddButton() {
        addButton.set(false);
    }

    private boolean isNameValid() {
        return !Strings.isNullOrEmpty(favoriteName.get());
    }

    private boolean isTagsValid() {
        return favoriteTags.getText().length() >= favoriteTags.getThreshold();
    }

    @Override
    public boolean isValid() {
        return isNameValid() && isTagsValid();
    }

    @Override
    public void showEmptyFavoriteSnackbar() {
        snackbarId = SnackbarId.FAVORITE_EMPTY;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showFavoriteNotFoundSnackbar() {
        snackbarId = SnackbarId.FAVORITE_NOT_FOUND;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showDuplicateKeyError() {
        nameErrorText = context.getResources().getString(
                R.string.add_edit_favorite_error_name_duplicated);
        notifyPropertyChanged(BR.nameErrorText);
    }

    @Override
    public void hideNameError() {
        nameErrorText = null;
        notifyPropertyChanged(BR.nameErrorText);
    }

    @Override
    public void afterNameChanged() {
        hideNameError();
        checkAddButton();
    }

    @Override
    public void afterTagsChanged() {
        checkAddButton();
    }

    @Override
    public void checkAddButton() {
        if (isValid()) enableAddButton();
        else disableAddButton();
    }

    @Override
    public void setFavoriteTags(List<Tag> tags) {
        if (tags == null) {
            favoriteTags.clear();
            return;
        }
        for (Tag tag : tags) {
            favoriteTags.addObject(tag);
        }
    }
}
