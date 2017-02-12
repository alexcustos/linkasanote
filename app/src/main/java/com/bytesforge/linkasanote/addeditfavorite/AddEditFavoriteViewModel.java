package com.bytesforge.linkasanote.addeditfavorite;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.LinearLayout;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Tag;
import com.tokenautocomplete.TokenCompleteTextView;

import static com.google.common.base.Preconditions.checkNotNull;

public class AddEditFavoriteViewModel extends BaseObservable implements
        AddEditFavoriteContract.ViewModel, TokenCompleteTextView.TokenListener<Tag> {

    public static final String STATE_FAVORITE_NAME = "FAVORITE_NAME";
    public static final String STATE_ADD_BUTTON = "ADD_BUTTON";
    public static final String STATE_ADD_BUTTON_TEXT = "ADD_BUTTON_TEXT";

    public final ObservableField<String> favoriteName = new ObservableField<>();
    public final ObservableBoolean addButton = new ObservableBoolean(false);

    private final int addButtonText;
    private FavoriteTagsCompletionView favoriteTags;

    private Context context;
    private AddEditFavoriteContract.Presenter presenter;

    public enum SnackbarId {FAVORITE_EMPTY};

    @Bindable
    public SnackbarId snackbarId;

    public AddEditFavoriteViewModel(@NonNull Context context, @NonNull Bundle savedInstanceState) {
        this.context = checkNotNull(context);
        checkNotNull(savedInstanceState);

        favoriteName.set(savedInstanceState.getString(STATE_FAVORITE_NAME));
        addButton.set(savedInstanceState.getBoolean(STATE_ADD_BUTTON));
        addButtonText = savedInstanceState.getInt(STATE_ADD_BUTTON_TEXT);
    }

    @Override
    public void loadInstanceState(@NonNull Bundle outState) {
        checkNotNull(outState);

        outState.putString(STATE_FAVORITE_NAME, favoriteName.get());
        outState.putBoolean(STATE_ADD_BUTTON, addButton.get());
        outState.putInt(STATE_ADD_BUTTON_TEXT, addButtonText);
    }

    @Override
    public void setPresenter(@NonNull AddEditFavoriteContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setTagsCompletionView(@NonNull FavoriteTagsCompletionView completionView) {
        favoriteTags = completionView;
        favoriteTags.setTokenListener(this);
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
        }
    }

    @Bindable
    public String getAddButtonText() {
        return context.getResources().getString(addButtonText);
    }

    public void onAddButtonClick() {
        favoriteTags.performCompletion();
        presenter.saveFavorite(favoriteName.get(), favoriteTags.getObjects());
    }

    private void enableAddButton() {
        addButton.set(true);
    }

    private void disableAddButton() {
        addButton.set(false);
    }

    public void afterFavoriteDataChanged(Editable s) {
        if (TextUtils.isEmpty(favoriteName.get())
                || favoriteTags.getText().length() < favoriteTags.getThreshold()) {
            disableAddButton();
        } else {
            enableAddButton();
        }
    }

    @Override
    public void onTokenAdded(Tag tag) {
        afterFavoriteDataChanged(null);
    }

    @Override
    public void onTokenRemoved(Tag tag) {
        afterFavoriteDataChanged(null);
    }

    @Override
    public void showEmptyFavoriteSnackbar() {
        snackbarId = SnackbarId.FAVORITE_EMPTY;
        notifyPropertyChanged(BR.snackbarId);
    }
}
