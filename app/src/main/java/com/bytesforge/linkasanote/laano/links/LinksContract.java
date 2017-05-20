package com.bytesforge.linkasanote.laano.links;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.laano.BaseItemPresenterInterface;
import com.bytesforge.linkasanote.laano.BaseItemViewModelInterface;
import com.bytesforge.linkasanote.laano.FilterType;

import java.util.ArrayList;
import java.util.List;

public interface LinksContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull LinksContract.ViewModel viewModel);
        boolean isActive();
        void onActivityResult(int requestCode, int resultCode, Intent data);

        void startAddLinkActivity();
        void showAddNote(@NonNull String linkId);
        void showEditLink(@NonNull String linkId);
        void showLinks(@NonNull List<Link> links);
        void enableActionMode();
        void finishActionMode();
        void selectionChanged(@NonNull String id);
        void visibilityChanged(@NonNull String id);
        void removeLink(@NonNull String id);
        int getPosition(String linkId);
        @NonNull String[] getIds();
        void scrollToPosition(int position);
        void openLink(@NonNull Uri uri);
        void confirmLinksRemoval(ArrayList<String> selectedIds);
        void showConflictResolution(@NonNull String linkId);
        void showConflictResolutionWarning(@NonNull String linkId);
        void expandAllLinks();
        void collapseAllLinks();
    }

    interface ViewModel extends BaseItemViewModelInterface {

        void showDatabaseErrorSnackbar();
        void showConflictResolutionSuccessfulSnackbar();
        void showConflictResolutionErrorSnackbar();
        void showOpenLinkErrorSnackbar();
        void showConflictedErrorSnackbar();
        void showCloudErrorSnackbar();
        void showSaveSuccessSnackbar();
        void showDeleteExtraErrorSnackbar();
        void showDeleteSuccessSnackbar();

        void setExpandByDefault(boolean expandByDefault);
        boolean isVisible(String id, int numNotes);
        void toggleVisibility(@NonNull String id);
        void setVisibility(@NonNull String[] ids, boolean expand);
    }

    interface Presenter extends BaseItemPresenterInterface {

        void showAddLink();
        void loadLinks(boolean forceUpdate);

        void onLinkClick(String linkId, boolean isConflicted, int numNotes);
        boolean onLinkLongClick(String linkId);
        void onCheckboxClick(String linkId);
        void selectLinkFilter();

        void onEditClick(@NonNull String linkId);
        void onLinkOpenClick(@NonNull String linkId);
        void onToNotesClick(@NonNull String linkId);
        void onAddNoteClick(@NonNull String linkId);
        void onToggleClick(@NonNull String linkId);
        void onDeleteClick();
        void onSelectAllClick();
        void updateSyncStatus();
        int getPosition(String linkId);
        void setFilterType(@NonNull FilterType filtering);
        @NonNull FilterType getFilterType();
        void syncSavedLink(@NonNull final String linkId);
        void syncSavedNote(@NonNull final String linkId, @NonNull final String noteId);
        void deleteLinks(@NonNull ArrayList<String> selectedIds, boolean deleteNotes);
        boolean isFavoriteFilter();
        boolean isNoteFilter();
        boolean isExpandLinks();
        void setShowConflictResolutionWarning(boolean show);
    }
}
