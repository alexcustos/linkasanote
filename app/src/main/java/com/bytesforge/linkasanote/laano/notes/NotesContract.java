package com.bytesforge.linkasanote.laano.notes;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.laano.BaseItemPresenterInterface;
import com.bytesforge.linkasanote.laano.BaseItemViewModelInterface;
import com.bytesforge.linkasanote.laano.FilterType;

import java.util.ArrayList;
import java.util.List;

public interface NotesContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull NotesContract.ViewModel viewModel);
        boolean isActive();

        void startAddNoteActivity(String noteId);
        void showEditNote(@NonNull String noteId);
        void showNotes(@NonNull List<Note> notes);
        void enableActionMode();
        void finishActionMode();
        void selectionChanged(@NonNull String id);
        void visibilityChanged(@NonNull String id);
        void removeNote(@NonNull String noteId);
        int getPosition(String noteId);
        @NonNull String[] getIds();
        void scrollToPosition(int position);
        void confirmNotesRemoval(ArrayList<String> selectedIds);
        void showConflictResolution(@NonNull String noteId);
        void expandAllNotes();
        void collapseAllNotes();
    }

    interface ViewModel extends BaseItemViewModelInterface {

        void showDatabaseErrorSnackbar();
        void showConflictResolutionSuccessfulSnackbar();
        void showConflictResolutionErrorSnackbar();
        void showConflictedErrorSnackbar();
        void showCloudErrorSnackbar();
        void showSaveSuccessSnackbar();
        void showDeleteSuccessSnackbar();

        void setExpandByDefault(boolean expandByDefault);
        boolean isVisible(String id);
        void toggleVisibility(@NonNull String id);
        void setVisibility(@NonNull String[] ids, boolean expand);
    }

    interface Presenter extends BaseItemPresenterInterface {

        void showAddNote();
        void loadNotes(final boolean forceUpdate);

        void onNoteClick(String noteId, boolean isConflicted);
        boolean onNoteLongClick(String noteId);
        void onCheckboxClick(String noteId);
        void selectNoteFilter();

        void onEditClick(@NonNull String noteId);
        void onToLinksClick(@NonNull String noteId);
        void onToggleClick(@NonNull String noteId);
        void onDeleteClick();
        void onSelectAllClick();
        int getPosition(String noteId);
        void setFilterType(@NonNull FilterType filtering);
        @NonNull FilterType getFilterType();
        @Nullable Boolean isFavoriteAndGate();
        void syncSavedNote(final String linkId, @NonNull final String noteId);
        void deleteNotes(ArrayList<String> selectedIds);
        boolean isFavoriteFilter();
        boolean isLinkFilter();
        boolean isExpandNotes();
        boolean isNotesLayoutModeReading();
        boolean toggleNotesLayoutModeReading();
        void setFilterIsChanged(boolean filterIsChanged);
    }
}
