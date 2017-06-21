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
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.laano.TagsCompletionView;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.google.common.base.Strings;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class AddEditNoteViewModel extends BaseObservable implements
        AddEditNoteContract.ViewModel {

    private static final String TAG = AddEditNoteViewModel.class.getSimpleName();

    private static final String STATE_NOTE_NOTE = "NOTE_NOTE";
    private static final String STATE_NOTE_SYNC_STATE = "NOTE_SYNC_STATE";
    private static final String STATE_ADD_BUTTON = "ADD_BUTTON";
    private static final String STATE_ADD_BUTTON_TEXT = "ADD_BUTTON_TEXT";
    private static final String STATE_NOTE_ERROR_TEXT = "NOTE_ERROR_TEXT";

    public final ObservableField<String> noteNote = new ObservableField<>();
    public final ObservableBoolean addButton = new ObservableBoolean();
    private int addButtonText;

    // NOTE: there is nothing to save here, these fields will be populated on every load
    public final ObservableField<String> linkStatus = new ObservableField<>();
    public final ObservableField<String> linkName = new ObservableField<>();
    public final ObservableField<String> linkLink = new ObservableField<>();

    private SyncState noteSyncState;
    private TagsCompletionView noteTags;
    private Context context;
    private Resources resources;
    private AddEditNoteContract.Presenter presenter;
    private boolean tagsHasFocus;

    public enum SnackbarId {
        DATABASE_ERROR, NOTE_EMPTY, NOTE_NOT_FOUND}

    @Bindable
    public SnackbarId snackbarId;

    @Bindable
    public String noteErrorText;

    public AddEditNoteViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
        resources = context.getResources();
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
        outState.putParcelable(STATE_NOTE_SYNC_STATE, noteSyncState);
        outState.putBoolean(STATE_ADD_BUTTON, addButton.get());
        outState.putInt(STATE_ADD_BUTTON_TEXT, addButtonText);
        outState.putString(STATE_NOTE_ERROR_TEXT, noteErrorText);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        noteNote.set(state.getString(STATE_NOTE_NOTE));
        noteSyncState = state.getParcelable(STATE_NOTE_SYNC_STATE);
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
        defaultState.putParcelable(STATE_NOTE_SYNC_STATE, null);
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
        String note = noteNote.get();
        if (note != null) {
            note = note.trim();
            noteNote.set(note);
        }
        presenter.saveNote(note, noteTags.getObjects());
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

    public void afterNoteChanged() {
        hideNoteError();
        checkAddButton();
    }

    public boolean onNoteTouch(View view, MotionEvent event) {
        EditText editText = (EditText) view;
        if (editText.getLineCount() > editText.getMaxLines()) {
            view.getParent().requestDisallowInterceptTouchEvent(true);
        }
        return false;
    }

    public void onTagsFocusChange(View view, boolean hasFocus) {
        tagsHasFocus = hasFocus;
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
        noteSyncState = note.getState();
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
        if (tagsHasFocus) {
            String tag = CommonUtils.strFirstLine(noteNote); // trimmed
            if (!Strings.isNullOrEmpty(tag)) {
                noteTags.addObject(new Tag(tag));
            } else {
                // NOTE: do not spam on auto fill in form
                showEmptyToast();
            }
        } else {
            this.noteNote.set(noteNote);
        }
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

    @Override
    public SyncState getNoteSyncState() {
        return noteSyncState;
    }

    @Override
    public void showTagsDuplicateRemovedToast() {
        Toast.makeText(context, R.string.toast_tags_duplicate_removed, Toast.LENGTH_SHORT).show();
    }

    private void showEmptyToast() {
        Toast.makeText(context, R.string.toast_empty, Toast.LENGTH_SHORT).show();
    }
}
