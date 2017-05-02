package com.bytesforge.linkasanote.laano.notes;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoTabPresenter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;

import java.util.List;

public interface NotesContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull NotesContract.ViewModel viewModel);
        boolean isActive();

        void showAddNote(String noteId);
        void showEditNote(@NonNull String noteId);
        void showNotes(@NonNull List<Note> notes);
        void enableActionMode();
        void finishActionMode();
        void selectionChanged(int position);
        void noteVisibilityChanged(int position);
        String removeNote(int position);
        int getPosition(String noteId);
        void scrollToPosition(int position);
        void confirmNotesRemoval(int[] selectedIds);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setLaanoUiManager(@NonNull LaanoUiManager laanoUiManager);

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(@NonNull Bundle outState);
        void applyInstanceState(@NonNull Bundle state);

        void setNoteListSize(int noteListSize);
        boolean isActionMode();
        void enableActionMode();
        void disableActionMode();

        void toggleSelection();
        void toggleSelection(int position);
        boolean toggleSingleSelection(int position);
        void setSingleSelection(int position, boolean selected);
        void removeSelection();
        void removeSelection(int position);
        int getSelectedCount();
        int[] getSelectedIds();
        void showDatabaseErrorSnackbar();
        void showProgressOverlay();
        void hideProgressOverlay();

        String getSearchText();
        void setSearchText(String searchText);
        void toggleNoteVisibility(int position);
        void expandAllNotes();
        void collapseAllNotes();
    }

    interface Presenter extends LaanoTabPresenter {

        void addNote();
        void loadNotes(boolean forceUpdate);

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
        void deleteNotes(int[] selectedIds);
        boolean isFavoriteFilter();
        boolean isLinkFilter();
        boolean isExpandNotes();
        boolean isNotesLayoutModeReading();
        boolean toggleNotesLayoutModeReading();
    }
}
