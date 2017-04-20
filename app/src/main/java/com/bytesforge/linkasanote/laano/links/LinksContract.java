package com.bytesforge.linkasanote.laano.links;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoTabPresenter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;

import java.util.List;

public interface LinksContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull LinksContract.ViewModel viewModel);
        boolean isActive();

        void showAddLink();
        void showAddNote(@NonNull String linkId);
        void showEditLink(@NonNull String linkId);
        void showLinks(@NonNull List<Link> links);
        void enableActionMode();
        void finishActionMode();
        void selectionChanged(int position);
        String removeLink(int position);
        int getPosition(String linkId);
        void scrollToPosition(int position);
        void openLink(@NonNull Uri uri);
        void confirmLinksRemoval(int[] selectedIds);
        void showConflictResolution(@NonNull String linkId);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setLaanoUiManager(@NonNull LaanoUiManager laanoUiManager);

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(@NonNull Bundle outState);
        void applyInstanceState(@NonNull Bundle state);

        void setLinkListSize(int linkListSize);
        boolean isActionMode();
        void enableActionMode();
        void disableActionMode();

        boolean isSelected(String linkId, boolean changed);
        void toggleSelection();
        void toggleSelection(int position);
        void toggleSingleSelection(int position);
        void setSingleSelection(int position, boolean selected);
        void removeSelection();
        void removeSelection(int position);
        int getSelectedCount();
        int[] getSelectedIds();
        void showDatabaseErrorSnackbar();
        void showConflictResolutionSuccessfulSnackbar();
        void showConflictResolutionErrorSnackbar();
        void showOpenLinkErrorSnackbar();
        void showProgressOverlay();
        void hideProgressOverlay();

        String getSearchText();
        void setSearchText(String searchText);
    }

    interface Presenter extends LaanoTabPresenter {

        void addLink();
        void loadLinks(boolean forceUpdate);

        void onLinkClick(String linkId, boolean isConflicted);
        boolean onLinkLongClick(String linkId);
        void onCheckboxClick(String linkId);
        void selectLinkFilter();

        void onEditClick(@NonNull String linkId);
        void onLinkOpenClick(@NonNull String linkId);
        void onToNotesClick(@NonNull String linkId);
        void onAddNoteClick(@NonNull String linkId);
        void onDeleteClick();
        void onSelectAllClick();
        int getPosition(String linkId);
        void setFilterType(@NonNull FilterType filtering);
        boolean isFavoriteFilter();
        boolean isNoteFilter();
        void deleteLinks(int[] selectedIds);
    }
}
