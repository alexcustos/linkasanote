package com.bytesforge.linkasanote.laano.notes.conflictresolution;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Note;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotesConflictResolutionViewModel extends BaseObservable implements
        NotesConflictResolutionContract.ViewModel {

    private static final String STATE_LOCAL_STATE = "LOCAL_STATE";
    private static final String STATE_LOCAL_STATUS = "LOCAL_STATUS";
    private static final String STATE_LOCAL_NAME = "LOCAL_NAME";
    private static final String STATE_LOCAL_TAGS = "LOCAL_TAGS";
    private static final String STATE_LOCAL_DELETE_BUTTON = "LOCAL_DELETE_BUTTON";
    private static final String STATE_LOCAL_UPLOAD_BUTTON = "LOCAL_UPLOAD_BUTTON";

    private static final String STATE_CLOUD_STATE = "CLOUD_STATE";
    private static final String STATE_CLOUD_STATUS = "CLOUD_STATUS";
    private static final String STATE_CLOUD_NAME = "CLOUD_NAME";
    private static final String STATE_CLOUD_TAGS = "CLOUD_TAGS";
    private static final String STATE_CLOUD_DELETE_BUTTON = "CLOUD_DELETE_BUTTON";
    private static final String STATE_CLOUD_DOWNLOAD_BUTTON = "CLOUD_DOWNLOAD_BUTTON";
    private static final String STATE_CLOUD_RETRY_BUTTON = "CLOUD_RETRY_BUTTON";

    private static final String STATE_BUTTONS_ACTIVE = "BUTTONS_ACTIVE";

    public final ObservableField<String> localState = new ObservableField<>();
    public final ObservableField<String> localStatus = new ObservableField<>();
    public final ObservableField<String> localName = new ObservableField<>();
    public final ObservableField<String> localTags = new ObservableField<>();
    public final ObservableBoolean localDeleteButton = new ObservableBoolean();
    public final ObservableBoolean localUploadButton = new ObservableBoolean();

    public final ObservableField<String> cloudState = new ObservableField<>();
    public final ObservableField<String> cloudStatus = new ObservableField<>();
    public final ObservableField<String> cloudName = new ObservableField<>();
    public final ObservableField<String> cloudTags = new ObservableField<>();
    public final ObservableBoolean cloudDeleteButton = new ObservableBoolean();
    public final ObservableBoolean cloudDownloadButton = new ObservableBoolean();
    public final ObservableBoolean cloudRetryButton = new ObservableBoolean();

    public final ObservableBoolean buttonsActive = new ObservableBoolean();

    private final Resources resources;
    private NotesConflictResolutionContract.Presenter presenter;

    public NotesConflictResolutionViewModel(Context context) {
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

        outState.putString(STATE_LOCAL_STATE, localState.get());
        outState.putString(STATE_LOCAL_STATUS, localStatus.get());
        outState.putString(STATE_LOCAL_NAME, localName.get());
        outState.putString(STATE_LOCAL_TAGS, localTags.get());
        outState.putBoolean(STATE_LOCAL_DELETE_BUTTON, localDeleteButton.get());
        outState.putBoolean(STATE_LOCAL_UPLOAD_BUTTON, localUploadButton.get());

        outState.putString(STATE_CLOUD_STATE, cloudState.get());
        outState.putString(STATE_CLOUD_STATUS, cloudStatus.get());
        outState.putString(STATE_CLOUD_NAME, cloudName.get());
        outState.putString(STATE_CLOUD_TAGS, cloudTags.get());
        outState.putBoolean(STATE_CLOUD_DELETE_BUTTON, cloudDeleteButton.get());
        outState.putBoolean(STATE_CLOUD_DOWNLOAD_BUTTON, cloudDownloadButton.get());
        outState.putBoolean(STATE_CLOUD_RETRY_BUTTON, cloudRetryButton.get());

        outState.putBoolean(STATE_BUTTONS_ACTIVE, buttonsActive.get());
    }

    @Override
    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);

        localState.set(state.getString(STATE_LOCAL_STATE));
        localStatus.set(state.getString(STATE_LOCAL_STATUS));
        localName.set(state.getString(STATE_LOCAL_NAME));
        localTags.set(state.getString(STATE_LOCAL_TAGS));
        localDeleteButton.set(state.getBoolean(STATE_LOCAL_DELETE_BUTTON));
        localUploadButton.set(state.getBoolean(STATE_LOCAL_UPLOAD_BUTTON));

        cloudState.set(state.getString(STATE_CLOUD_STATE));
        cloudStatus.set(state.getString(STATE_CLOUD_STATUS));
        cloudName.set(state.getString(STATE_CLOUD_NAME));
        cloudTags.set(state.getString(STATE_CLOUD_TAGS));
        cloudDeleteButton.set(state.getBoolean(STATE_CLOUD_DELETE_BUTTON));
        cloudDownloadButton.set(state.getBoolean(STATE_CLOUD_DOWNLOAD_BUTTON));
        cloudRetryButton.set(state.getBoolean(STATE_CLOUD_RETRY_BUTTON));

        buttonsActive.set(state.getBoolean(STATE_BUTTONS_ACTIVE));

        notifyChange();
    }

    private Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();

        defaultState.putString(STATE_LOCAL_STATE, null);
        defaultState.putString(STATE_LOCAL_STATUS,
                resources.getString(R.string.dialog_note_conflict_status_loading));
        defaultState.putString(STATE_LOCAL_NAME, null);
        defaultState.putString(STATE_LOCAL_TAGS, null);
        defaultState.putBoolean(STATE_LOCAL_DELETE_BUTTON, false);
        defaultState.putBoolean(STATE_LOCAL_UPLOAD_BUTTON, false);

        defaultState.putString(STATE_CLOUD_STATE, null);
        defaultState.putString(STATE_CLOUD_STATUS,
                resources.getString(R.string.dialog_note_conflict_status_loading));
        defaultState.putString(STATE_CLOUD_NAME, null);
        defaultState.putString(STATE_CLOUD_TAGS, null);
        defaultState.putBoolean(STATE_CLOUD_DELETE_BUTTON, false);
        defaultState.putBoolean(STATE_CLOUD_DOWNLOAD_BUTTON, false);
        defaultState.putBoolean(STATE_CLOUD_RETRY_BUTTON, false);

        defaultState.putBoolean(STATE_BUTTONS_ACTIVE, false);

        return defaultState;
    }

    @Override
    public void setPresenter(@NonNull NotesConflictResolutionContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void populateLocalNote(@NonNull Note note) {
        checkNotNull(note);
        localDeleteButton.set(false);
        localUploadButton.set(false);
        activateButtons();

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
        localName.set(note.getNote());
        localTags.set(note.getTagsAsString());
        localStatus.set(null);
        notifyChange(); // NOTE: it is really needed
    }

    @Override
    public boolean isLocalPopulated() {
        return localStatus.get() == null;
    }

    @Override
    public void populateCloudNote(@NonNull Note note) {
        checkNotNull(note);
        cloudRetryButton.set(false);
        cloudDeleteButton.set(false);
        cloudDownloadButton.set(false);
        activateButtons();

        if (note.isDuplicated()) { // NOTE: from database record
            cloudState.set(resources.getString(R.string.dialog_note_conflict_state_duplicated));
            cloudDeleteButton.set(true);
        } else {
            cloudState.set(resources.getString(R.string.dialog_note_conflict_state_updated));
            cloudDownloadButton.set(true);
        }
        cloudName.set(note.getNote());
        cloudTags.set(note.getTagsAsString());
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
        cloudStatus.set(resources.getString(R.string.dialog_note_conflict_status_loading));
    }

    @Override
    public boolean isStateDuplicated() {
        return resources.getString(
                R.string.dialog_note_conflict_state_duplicated).equals(cloudState.get());
    }

    @Override
    public String getLocalName() {
        return localName.get();
    }

    @Override
    public void activateButtons() {
        buttonsActive.set(true);

    }

    @Override
    public void deactivateButtons() {
        buttonsActive.set(false);
    }
}
