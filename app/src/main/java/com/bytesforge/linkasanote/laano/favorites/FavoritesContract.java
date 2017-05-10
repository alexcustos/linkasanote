package com.bytesforge.linkasanote.laano.favorites;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.laano.BaseItemPresenterInterface;
import com.bytesforge.linkasanote.laano.BaseItemViewModelInterface;
import com.bytesforge.linkasanote.laano.FilterType;

import java.util.ArrayList;
import java.util.List;

public interface FavoritesContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull FavoritesContract.ViewModel viewModel);
        boolean isActive();
        void onActivityResult(int requestCode, int resultCode, Intent data);

        void startAddFavoriteActivity();
        void showEditFavorite(@NonNull String favoriteId);
        void showFavorites(@NonNull List<Favorite> favorites);
        void enableActionMode();
        void finishActionMode();
        void selectionChanged(@NonNull String id);
        void removeFavorite(@NonNull String id);
        int getPosition(String favoriteId);
        String[] getIds();
        void scrollToPosition(int position);
        void confirmFavoritesRemoval(ArrayList<String> selectedIds);
        void showConflictResolution(@NonNull String favoriteId);
    }

    interface ViewModel extends BaseItemViewModelInterface {

        void showDatabaseErrorSnackbar();
        void showConflictResolutionSuccessfulSnackbar();
        void showConflictResolutionErrorSnackbar();
        void showConflictedErrorSnackbar();
        void showCloudErrorSnackbar();
        void showSaveSuccessSnackbar();
        void showDeleteSuccessSnackbar();
    }

    interface Presenter extends BaseItemPresenterInterface {

        void showAddFavorite();
        void loadFavorites(boolean forceUpdate);

        void onFavoriteClick(String favoriteId, boolean isConflicted);
        boolean onFavoriteLongClick(String favoriteId);
        void onCheckboxClick(String favoriteId);
        void selectFavoriteFilter();

        void onEditClick(@NonNull String favoriteId);
        void onToLinksClick(@NonNull String favoriteId);
        void onToNotesClick(@NonNull String favoriteId);
        void onDeleteClick();
        void onSelectAllClick();
        int getPosition(String favoriteId);
        void setFilterType(@NonNull FilterType filtering);
        void syncSavedFavorite(@NonNull final String favoriteId);
        void deleteFavorites(ArrayList<String> selectedIds);
    }
}
