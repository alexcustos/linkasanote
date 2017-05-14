package com.bytesforge.linkasanote.laano.notes.addeditnote;

import android.content.Context;
import android.content.res.Resources;
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
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.laano.TagsCompletionView;
import com.google.common.base.Strings;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class AddEditNoteViewModel extends BaseObservable implements
        AddEditNoteContract.ViewModel {

    public static final String STATE_NOTE_NOTE = "NOTE_NOTE";
    public static final String STATE_ADD_BUTTON = "ADD_BUTTON";
    public static final String STATE_ADD_BUTTON_TEXT = "ADD_BUTTON_TEXT";
    public static final String STATE_NOTE_ERROR_TEXT = "NOTE_ERROR_TEXT";

    public final ObservableField<String> noteNote = new ObservableField<>();
    public final ObservableBoolean addButton = new ObservableBoolean();
    private int addButtonText;

    // NOTE: there is nothing to save here, these fields will be populated on every load
    public final ObservableField<String> linkStatus = new ObservableField<>();
    public final ObservableField<String> linkName = new ObservableField<>();
    public final ObservableField<String> linkLink = new ObservableField<>();

    private TagsCompletionView noteTags;
    private Resources resources;
    private AddEditNoteContract.Presenter presenter;

    public enum SnackbarId {
        DATABASE_ERROR, NOTE_EMPTY, NOTE_NOT_FOUND}

    @Bindable
    public SnackbarId snackbarId;

    @Bindable
    public String noteErrorText;

    public AddEditNoteViewModel(@NonNull Context context) {
        resources = checkNotNull(context).getResources();
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
        outState.putString(STATE_NOTE_NOTE, noteNote.get());
        outState.putBoolean(STATE_ADD_BUTTON, addButton.get());
        outState.putInt(STATE_ADD_BUTTON_TEXT, addButtonText);
        outState.putString(STATE_NOTE_ERROR_TEXT, noteErrorText);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        noteNote.set(state.getString(STATE_NOTE_NOTE));
        addButton.set(state.getBoolean(STATE_ADD_BUTTON));
        addButtonText = state.getInt(STATE_ADD_BUTTON_TEXT);
        noteErrorText = state.getString(STATE_NOTE_ERROR_TEXT);

        linkStatus.set(null);
        linkName.set(null);
        linkLink.set(null);

        notifyChange();
    }

    @Override
    public Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();

        defaultState.putString(STATE_NOTE_NOTE, null);
        defaultState.putBoolean(STATE_ADD_BUTTON, false);
        int addButtonText = presenter.isNewNote()
                ? R.string.add_edit_note_new_button_title
                : R.string.add_edit_note_edit_button_title;
        defaultState.putInt(STATE_ADD_BUTTON_TEXT, addButtonText);
        defaultState.putString(STATE_NOTE_ERROR_TEXT, null);

        return defaultState;
    }

    @Override
    public void setPresenter(@NonNull AddEditNoteContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setTagsCompletionView(@NonNull TagsCompletionView completionView) {
        noteTags = completionView;
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
            case NOTE_EMPTY:
                Snackbar.make(view,
                        R.string.add_edit_note_warning_empty,
                        Snackbar.LENGTH_LONG).show();
                break;
            case NOTE_NOT_FOUND:
                Snackbar.make(view,
                        R.string.add_edit_note_warning_not_existed,
                        Snackbar.LENGTH_LONG).show();
                break;
            default:
                throw new IllegalArgumentException("Unexpected snackbar has been requested");
        }
    }

    @BindingAdapter({"noteError"})
    public static void showNoteError(TextInputLayout layout, @Nullable String noteErrorText) {
        if (noteErrorText != null) {
            layout.setError(noteErrorText);
            layout.setErrorEnabled(true);
            layout.requestFocus();
        } else {
            layout.setErrorEnabled(false);
        }
    }

    @Bindable
    public String getAddButtonText() {
        return resources.getString(addButtonText);
    }

    public void onAddButtonClick() {
        noteTags.performCompletion();
        // NOTE: there is no way to pass these values directly to the presenter
        presenter.saveNote(noteNote.get(), noteTags.getObjects());
    }

    @Override
    public void enableAddButton() {
        addButton.set(true);
    }

    @Override
    public void disableAddButton() {
        addButton.set(false);
    }

    private boolean isNoteValid() {
        return !Strings.isNullOrEmpty(noteNote.get());
    }

    @Override
    public boolean isValid() {
        return isNoteValid();
    }

    @Override
    public boolean isEmpty() {
        return Strings.isNullOrEmpty(noteNote.get())
                && noteTags.getText().length() <= 0;
    }

    @Override
    public void showDatabaseErrorSnackbar() {
        snackbarId = SnackbarId.DATABASE_ERROR;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showEmptyNoteSnackbar() {
        snackbarId = SnackbarId.NOTE_EMPTY;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showNoteNotFoundSnackbar() {
        snackbarId = SnackbarId.NOTE_NOT_FOUND;
        notifyPropertyChanged(BR.snackbarId);
    }

    @Override
    public void showDuplicateKeyError() {
        noteErrorText = resources.getString(R.string.add_edit_note_error_note_duplicated);
        notifyPropertyChanged(BR.noteErrorText);
    }

    @Override
    public void showLinkStatusNoteWillBeUnbound() {
        linkStatus.set(resources.getString(R.string.add_edit_note_message_link_will_unbound));
    }

    @Override
    public void showLinkStatusLoading() {
        linkStatus.set(resources.getString(R.string.status_loading));
    }

    @Override
    public void hideLinkStatus() {
        linkStatus.set(null);
    }

    @Override
    public void hideNoteError() {
        noteErrorText = null;
        notifyPropertyChanged(BR.noteErrorText);
    }

    @Override
    public void afterNoteChanged() {
        hideNoteError();
        checkAddButton();
    }

    @Override
    public void checkAddButton() {
        if (isValid()) enableAddButton();
        else disableAddButton();
    }

    @Override
    public void populateNote(@NonNull Note note) {
        checkNotNull(note);
        noteNote.set(note.getNote());
        setNoteTags(note.getTags());
        checkAddButton();
    }

    @Override
    public void populateLink(@NonNull Link link) {
        checkNotNull(link);
        linkStatus.set(null);
        linkName.set(link.getName());
        linkLink.set(link.getLink());
    }

    @Override
    public void setNoteNote(String noteNote) {
        this.noteNote.set(noteNote);
    }

    private void setNoteTags(List<Tag> tags) {
        noteTags.clear();
        if (tags == null || tags.isEmpty()) return;

        for (Tag tag : tags) {
            noteTags.addObject(tag);
        }
    }

    @Override
    public void setNoteTags(String[] tags) {
        noteTags.clear();
        if (tags == null || tags.length <= 0) return;

        for (String tag : tags) {
            noteTags.addObject(new Tag(tag));
        }
    }
}
