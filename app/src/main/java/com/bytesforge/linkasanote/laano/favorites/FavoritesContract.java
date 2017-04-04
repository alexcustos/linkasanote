package com.bytesforge.linkasanote.laano.favorites;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.laano.LaanoTabPresenter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;

import java.util.List;

public interface FavoritesContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull FavoritesContract.ViewModel viewModel);
        boolean isActive();

        void showAddFavorite();
        void showEditFavorite(@NonNull String favoriteId);
        void showFavorites(@NonNull List<Favorite> favorites);
        void enableActionMode();
        void disableActionMode();
        void selectionChanged(int position);
        String removeFavorite(int position);
        int getPosition(String favoriteId);
        void confirmFavoritesRemoval(int[] selectedIds);
        void showConflictResolution(@NonNull String favoriteId);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setLaanoUiManager(@NonNull LaanoUiManager laanoUiManager);

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(@NonNull Bundle outState);
        void applyInstanceState(@NonNull Bundle state);

        void setFavoriteListSize(int favoriteListSize);
        boolean isActionMode();
        void enableActionMode();
        void disableActionMode();

        boolean isSelected(String favoriteId);
        void toggleSelection(int position);
        void removeSelection();
        void removeSelection(int position);
        int getSelectedCount();
        int[] getSelectedIds();
        void showDatabaseErrorSnackbar();
        void showConflictResolutionSuccessfulSnackbar();
        void showConflictResolutionErrorSnackbar();
        void showProgressOverlay();
        void hideProgressOverlay();

        FavoritesFilterType getFilterType();
        void setFilterType(FavoritesFilterType filterType);
        String getSearchText();
        void setSearchText(String searchText);
    }

    interface Presenter extends LaanoTabPresenter {

        void addFavorite();
        void loadFavorites(boolean forceUpdate);

        void onFavoriteClick(String favoriteId, boolean isConflicted);
        boolean onFavoriteLongClick(String favoriteId);
        void onCheckboxClick(String favoriteId);

        void onEditClick(@NonNull String favoriteId);
        void onToLinksClick(@NonNull String favoriteId);
        void onToNotesClick(@NonNull String favoriteId);
        void onDeleteClick();
        int getPosition(String favoriteId);
        void setFilterType(@NonNull FavoritesFilterType filtering);
        void deleteFavorites(int[] selectedIds);
    }
}
