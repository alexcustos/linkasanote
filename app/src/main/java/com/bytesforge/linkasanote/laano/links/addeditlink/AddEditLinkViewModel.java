package com.bytesforge.linkasanote.laano.links.addeditlink;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.laano.TagsCompletionView;
import com.google.common.base.Strings;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class AddEditLinkViewModel extends BaseObservable implements
        AddEditLinkContract.ViewModel {

    public static final String STATE_LINK_LINK = "LINK_LINK";
    public static final String STATE_LINK_NAME = "LINK_NAME";
    public static final String STATE_LINK_DISABLED = "LINK_DISABLED";
    public static final String STATE_ADD_BUTTON = "ADD_BUTTON";
    public static final String STATE_ADD_BUTTON_TEXT = "ADD_BUTTON_TEXT";
    public static final String STATE_LINK_ERROR_TEXT = "LINK_ERROR_TEXT";

    public final ObservableField<String> linkLink = new ObservableField<>();
    public final ObservableField<String> linkName = new ObservableField<>();
    public final ObservableBoolean linkDisabled = new ObservableBoolean();
    public final ObservableBoolean addButton = new ObservableBoolean();
    private int addButtonText;

    private TagsCompletionView linkTags;
    private Context context;
    private AddEditLinkContract.Presenter presenter;

    public enum SnackbarId {LINK_EMPTY, LINK_NOT_FOUND};

    @Bindable
    public SnackbarId snackbarId;

    @Bindable
    public String linkErrorText;

    public AddEditLinkViewModel(@NonNull Context context) {
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

        outState.putString(STATE_LINK_LINK, linkLink.get());
        outState.putString(STATE_LINK_NAME, linkName.get());
        outState.putBoolean(STATE_LINK_DISABLED, linkDisabled.get());
        outState.putBoolean(STATE_ADD_BUTTON, addButton.get());
        outState.putInt(STATE_ADD_BUTTON_TEXT, addButtonText);
        outState.putString(STATE_LINK_ERROR_TEXT, linkErrorText);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);

        linkLink.set(state.getString(STATE_LINK_LINK));
        linkName.set(state.getString(STATE_LINK_NAME));
        linkDisabled.set(state.getBoolean(STATE_LINK_DISABLED));
        addButton.set(state.getBoolean(STATE_ADD_BUTTON));
        addButtonText = state.getInt(STATE_ADD_BUTTON_TEXT);
        linkErrorText = state.getString(STATE_LINK_ERROR_TEXT);
    }

    @Override
    public Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();

        defaultState.putString(STATE_LINK_LINK, null);
        defaultState.putString(STATE_LINK_NAME, null);
        defaultState.putBoolean(STATE_LINK_DISABLED, false);
        defaultState.putBoolean(STATE_ADD_BUTTON, false);
        int addButtonText = presenter.isNewLink()
                ? R.string.add_edit_link_new_button_title
                : R.string.add_edit_link_edit_button_title;
        defaultState.putInt(STATE_ADD_BUTTON_TEXT, addButtonText);
        defaultState.putString(STATE_LINK_ERROR_TEXT, null);

        return defaultState;
    }

    @Override
    public void setPresenter(@NonNull AddEditLinkContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setTagsCompletionView(@NonNull TagsCompletionView completionView) {
        linkTags = completionView;
    }

    @BindingAdapter({"snackbarId"})
    public static void showSnackbar(CoordinatorLayout view, SnackbarId snackbarId) {
        if (snackbarId == null) return;

        switch (snackbarId) {
            case LINK_EMPTY:
                Snackbar.make(view,
                        R.string.add_edit_link_warning_empty,
                        Snackbar.LENGTH_LONG).show();
                break;
            case LINK_NOT_FOUND:
                Snackbar.make(view,
                        R.string.add_edit_link_warning_not_existed,
                        Snackbar.LENGTH_LONG).show();
                break;
            default:
                throw new IllegalArgumentException("Unexpected snackbar has been requested");
        }
    }

    @BindingAdapter({"linkError"})
    public static void showLinkError(TextInputLayout layout, @Nullable String linkErrorText) {
        if (linkErrorText != null) {
            layout.setError(linkErrorText);
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
        linkTags.performCompletion();
        // NOTE: there is no way to pass these values directly to the presenter
        presenter.saveLink(linkLink.get(), linkName.get(), linkDisabled.get(), linkTags.getObjects());
    }

    @Override
    public void enableAddButton() {
        addButton.set(true);
    }

    @Override
    public void disableAddButton() {
        addButton.set(false);
    }

    private boolean isLinkValid() {
        return !Strings.isNullOrEmpty(linkLink.get());
    }

    @Override
    public boolean isValid() {
        return isLinkValid();
    }

    @Override
    public void showEmptyLinkSnackbar() {
        snackbarId = SnackbarId.LINK_EMPTY;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showLinkNotFoundSnackbar() {
        snackbarId = SnackbarId.LINK_NOT_FOUND;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showDuplicateKeyError() {
        linkErrorText = context.getResources().getString(
                R.string.add_edit_link_error_link_duplicated);
        notifyPropertyChanged(BR.linkErrorText);
    }

    @Override
    public void hideLinkError() {
        linkErrorText = null;
        notifyPropertyChanged(BR.linkErrorText);
    }

    @Override
    public void afterLinkChanged() {
        hideLinkError();
        checkAddButton();
    }

    @Override
    public void checkAddButton() {
        if (isValid()) enableAddButton();
        else disableAddButton();
    }

    @Override
    public void populateLink(@NonNull Link link) {
        checkNotNull(link);
        linkLink.set(link.getLink());
        linkName.set(link.getName());
        linkDisabled.set(link.isDisabled());
        setLinkTags(link.getTags());
        checkAddButton();
    }

    @Override
    public void setLinkLink(String linkLink) {
        this.linkLink.set(linkLink);
    }

    @Override
    public void setLinkName(String linkName) {
        this.linkName.set(linkName);
    }

    @Override
    public void setStateLinkDisabled(boolean disabled) {
        this.linkDisabled.set(disabled);
    }

    private void setLinkTags(List<Tag> tags) {
        linkTags.clear();
        if (tags == null || tags.isEmpty()) return;

        for (Tag tag : tags) {
            linkTags.addObject(tag);
        }
    }

    @Override
    public void setLinkTags(String[] tags) {
        linkTags.clear();
        if (tags == null || tags.length <= 0) return;

        for (String tag : tags) {
            linkTags.addObject(new Tag(tag));
        }
    }
}
