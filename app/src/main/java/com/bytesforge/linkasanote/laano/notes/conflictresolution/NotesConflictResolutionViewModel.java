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

package com.bytesforge.linkasanote.laano.notes.conflictresolution;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotesConflictResolutionViewModel extends BaseObservable implements
        NotesConflictResolutionContract.ViewModel {

    private static final String STATE_LOCAL_STATE = "LOCAL_STATE";
    private static final String STATE_LOCAL_STATUS = "LOCAL_STATUS";
    private static final String STATE_LOCAL_INFO = "LOCAL_INFO";
    private static final String STATE_LOCAL_ID = "LOCAL_ID";
    private static final String STATE_LOCAL_NOTE = "LOCAL_NOTE";
    private static final String STATE_LOCAL_TAGS = "LOCAL_TAGS";
    private static final String STATE_LOCAL_DELETE_BUTTON = "LOCAL_DELETE_BUTTON";
    private static final String STATE_LOCAL_UPLOAD_BUTTON = "LOCAL_UPLOAD_BUTTON";

    private static final String STATE_CLOUD_STATE = "CLOUD_STATE";
    private static final String STATE_CLOUD_STATUS = "CLOUD_STATUS";
    private static final String STATE_CLOUD_INFO = "CLOUD_INFO";
    private static final String STATE_CLOUD_ID = "CLOUD_ID";
    private static final String STATE_CLOUD_NOTE = "CLOUD_NOTE";
    private static final String STATE_CLOUD_TAGS = "CLOUD_TAGS";
    private static final String STATE_CLOUD_DELETE_BUTTON = "CLOUD_DELETE_BUTTON";
    private static final String STATE_CLOUD_DOWNLOAD_BUTTON = "CLOUD_DOWNLOAD_BUTTON";
    private static final String STATE_CLOUD_RETRY_BUTTON = "CLOUD_RETRY_BUTTON";

    private static final String STATE_BUTTONS_ACTIVE = "BUTTONS_ACTIVE";
    private static final String STATE_PROGRESS_OVERLAY = "PROGRESS_OVERLAY";

    public final ObservableField<String> localState = new ObservableField<>();
    public final ObservableField<String> localStatus = new ObservableField<>();
    public final ObservableField<String> localInfo = new ObservableField<>();
    public final ObservableField<String> localNote = new ObservableField<>();
    public final ObservableBoolean localDeleteButton = new ObservableBoolean();
    public final ObservableBoolean localUploadButton = new ObservableBoolean();

    public final ObservableField<String> cloudState = new ObservableField<>();
    public final ObservableField<String> cloudStatus = new ObservableField<>();
    public final ObservableField<String> cloudInfo = new ObservableField<>();
    public final ObservableField<String> cloudNote = new ObservableField<>();
    public final ObservableBoolean cloudDeleteButton = new ObservableBoolean();
    public final ObservableBoolean cloudDownloadButton = new ObservableBoolean();
    public final ObservableBoolean cloudRetryButton = new ObservableBoolean();

    public final ObservableBoolean buttonsActive = new ObservableBoolean();

    private String localId;
    private String cloudId;

    private final Resources resources;

    public NotesConflictResolutionViewModel(Context context) {
        resources = context.getResources();
    }

    @Bindable
    public ArrayList<Tag> localTags;

    @Bindable
    public ArrayList<Tag> cloudTags;

    @Bindable
    public boolean progressOverlay;

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
        outState.putString(STATE_LOCAL_STATE, localState.get());
        outState.putString(STATE_LOCAL_STATUS, localStatus.get());
        outState.putString(STATE_LOCAL_INFO, localInfo.get());
        outState.putString(STATE_LOCAL_ID, localId);
        outState.putString(STATE_LOCAL_NOTE, localNote.get());
        outState.putParcelableArrayList(STATE_LOCAL_TAGS, localTags);
        outState.putBoolean(STATE_LOCAL_DELETE_BUTTON, localDeleteButton.get());
        outState.putBoolean(STATE_LOCAL_UPLOAD_BUTTON, localUploadButton.get());

        outState.putString(STATE_CLOUD_STATE, cloudState.get());
        outState.putString(STATE_CLOUD_STATUS, cloudStatus.get());
        outState.putString(STATE_CLOUD_INFO, cloudInfo.get());
        outState.putString(STATE_CLOUD_ID, cloudId);
        outState.putString(STATE_CLOUD_NOTE, cloudNote.get());
        outState.putParcelableArrayList(STATE_CLOUD_TAGS, cloudTags);
        outState.putBoolean(STATE_CLOUD_DELETE_BUTTON, cloudDeleteButton.get());
        outState.putBoolean(STATE_CLOUD_DOWNLOAD_BUTTON, cloudDownloadButton.get());
        outState.putBoolean(STATE_CLOUD_RETRY_BUTTON, cloudRetryButton.get());

        outState.putBoolean(STATE_BUTTONS_ACTIVE, buttonsActive.get());
        outState.putBoolean(STATE_PROGRESS_OVERLAY, progressOverlay);
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        localState.set(state.getString(STATE_LOCAL_STATE));
        localStatus.set(state.getString(STATE_LOCAL_STATUS));
        localInfo.set(state.getString(STATE_LOCAL_INFO));
        localId = state.getString(STATE_LOCAL_ID);
        localNote.set(state.getString(STATE_LOCAL_NOTE));
        localTags = state.getParcelableArrayList(STATE_LOCAL_TAGS);
        localDeleteButton.set(state.getBoolean(STATE_LOCAL_DELETE_BUTTON));
        localUploadButton.set(state.getBoolean(STATE_LOCAL_UPLOAD_BUTTON));

        cloudState.set(state.getString(STATE_CLOUD_STATE));
        cloudStatus.set(state.getString(STATE_CLOUD_STATUS));
        cloudInfo.set(state.getString(STATE_CLOUD_INFO));
        cloudId = state.getString(STATE_CLOUD_ID);
        cloudNote.set(state.getString(STATE_CLOUD_NOTE));
        cloudTags = state.getParcelableArrayList(STATE_CLOUD_TAGS);
        cloudDeleteButton.set(state.getBoolean(STATE_CLOUD_DELETE_BUTTON));
        cloudDownloadButton.set(state.getBoolean(STATE_CLOUD_DOWNLOAD_BUTTON));
        cloudRetryButton.set(state.getBoolean(STATE_CLOUD_RETRY_BUTTON));

        buttonsActive.set(state.getBoolean(STATE_BUTTONS_ACTIVE));
        progressOverlay = state.getBoolean(STATE_PROGRESS_OVERLAY);

        notifyChange();
    }

    private Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();

        defaultState.putString(STATE_LOCAL_STATE, null);
        defaultState.putString(STATE_LOCAL_STATUS, resources.getString(R.string.status_loading));
        defaultState.putString(STATE_LOCAL_INFO, null);
        defaultState.putString(STATE_LOCAL_ID, null);
        defaultState.putString(STATE_LOCAL_NOTE, null);
        defaultState.putParcelableArrayList(STATE_LOCAL_TAGS, null);
        defaultState.putBoolean(STATE_LOCAL_DELETE_BUTTON, false);
        defaultState.putBoolean(STATE_LOCAL_UPLOAD_BUTTON, false);

        defaultState.putString(STATE_CLOUD_STATE, null);
        defaultState.putString(STATE_CLOUD_STATUS, resources.getString(R.string.status_loading));
        defaultState.putString(STATE_CLOUD_INFO, null);
        defaultState.putString(STATE_CLOUD_ID, null);
        defaultState.putString(STATE_CLOUD_NOTE, null);
        defaultState.putParcelableArrayList(STATE_CLOUD_TAGS, null);
        defaultState.putBoolean(STATE_CLOUD_DELETE_BUTTON, false);
        defaultState.putBoolean(STATE_CLOUD_DOWNLOAD_BUTTON, false);
        defaultState.putBoolean(STATE_CLOUD_RETRY_BUTTON, false);

        defaultState.putBoolean(STATE_BUTTONS_ACTIVE, false);
        defaultState.putBoolean(STATE_PROGRESS_OVERLAY, false);

        return defaultState;
    }

    @Override
    public void setPresenter(@NonNull NotesConflictResolutionContract.Presenter presenter) {
    }

    @Override
    public void populateLocalNote(@NonNull Note note) {
        checkNotNull(note);
        localDeleteButton.set(false);
        localUploadButton.set(false);
        activateButtons();

        localId = note.getId();
        if (note.isConflicted()) {
            if (note.isDeleted()) {
                localState.set(resources.getString(R.string.dialog_note_conflict_state_deleted));
                localDeleteButton.set(true);
            } else {
                localState.set(resources.getString(R.string.dialog_note_conflict_state_updated));
            }
            localUploadButton.set(true);
        } else {
            localState.set(resources.getString(R.string.dialog_note_conflict_state_no_conflict));
            localDeleteButton.set(true);
        }
        String linkId = note.getLinkId();
        if (linkId == null) {
            localInfo.set(resources.getString(R.string.dialog_note_conflict_info_unbound));
        } else {
            localInfo.set(resources.getString(R.string.dialog_note_conflict_info_bound, linkId));
        }
        localNote.set(note.getNote());
        localTags = (ArrayList<Tag>) note.getTags();
        localStatus.set(null);
        notifyChange(); // NOTE: it is really needed
    }

    @Override
    public boolean isLocalPopulated() {
        return localStatus.get() == null;
    }

    @Override
    public void populateCloudNote(@NonNull Note note, boolean orphaned) {
        checkNotNull(note);
        cloudRetryButton.set(false);
        cloudDeleteButton.set(false);
        cloudDownloadButton.set(false);
        activateButtons();

        cloudId = note.getId();
        String linkId = note.getLinkId();
        if (orphaned) {
            cloudState.set(resources.getString(R.string.dialog_note_conflict_state_orphaned));
            cloudInfo.set(resources.getString(R.string.dialog_note_conflict_info_orphaned, linkId));
            cloudDeleteButton.set(true);
        } else {
            cloudState.set(resources.getString(R.string.dialog_note_conflict_state_updated));
            if (linkId == null) {
                cloudInfo.set(resources.getString(R.string.dialog_note_conflict_info_unbound));
            } else {
                cloudInfo.set(resources.getString(R.string.dialog_note_conflict_info_bound, linkId));
            }
            cloudDownloadButton.set(true);
        }
        cloudNote.set(note.getNote());
        cloudTags = (ArrayList<Tag>) note.getTags();
        cloudStatus.set(null);
        notifyChange();
    }

    @Override
    public boolean isCloudPopulated() {
        return cloudStatus.get() == null;
    }

    @Override
    public void showCloudNotFound() {
        cloudState.set(resources.getString(R.string.dialog_note_conflict_state_not_found));
        cloudStatus.set(null);
        localDeleteButton.set(true);
    }

    @Override
    public void showCloudDownloadError() {
        activateButtons();
        cloudState.set(resources.getString(R.string.dialog_note_conflict_state_error));
        cloudStatus.set(resources.getString(R.string.dialog_note_conflict_status_error_download));
        cloudRetryButton.set(true);
    }

    @Override
    public void showDatabaseError() {
        localState.set(resources.getString(R.string.dialog_note_conflict_state_error));
        localStatus.set(resources.getString(R.string.error_database));
    }

    @Override
    public void showCloudLoading() {
        cloudRetryButton.set(false);
        cloudDeleteButton.set(false);
        cloudDownloadButton.set(false);
        deactivateButtons();
        cloudState.set(null);
        cloudStatus.set(resources.getString(R.string.status_loading));
    }

    @Override
    public String getLocalId() {
        return localId;
    }

    @Override
    public String getCloudId() {
        return cloudId;
    }

    @Override
    public void activateButtons() {
        buttonsActive.set(true);

    }

    @Override
    public void deactivateButtons() {
        buttonsActive.set(false);
    }

    // Progress

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
