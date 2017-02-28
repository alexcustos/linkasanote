package com.bytesforge.linkasanote.laano.favorites;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseBooleanArray;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Favorite;

import java.util.List;

public interface FavoritesContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull FavoritesContract.ViewModel viewModel);
        boolean isActive();

        void showAddFavorite();
        void showEditFavorite(@NonNull String favoriteId);
        void showFavorites(List<Favorite> favorites);
        void enableActionMode();
        void disableActionMode();
        void selectionChanged(int position);
        Favorite removeFavorite(int position);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void loadInstanceState(@NonNull Bundle outState);
        void applyInstanceState(@NonNull Bundle state);

        void setFavoriteListSize(int favoriteListSize);
        boolean isActionMode();
        void enableActionMode();
        void disableActionMode();

        boolean isSelected(int position);
        void toggleSelection(int position);
        void removeSelection();
        void removeSelection(int position);
        int getSelectedCount();
        SparseBooleanArray getSelectedIds();
    }

    interface Presenter extends BasePresenter {

        void addFavorite();
        void loadFavorites(boolean forceUpdate);

        void onFavoriteClick(int position);
        boolean onFavoriteLongClick(int position);
        void onCheckboxClick(int position);

        void onEditClick(@NonNull String favoriteId);
        void onToLinksClick(@NonNull String favoriteId);
        void onToNotesClick(@NonNull String favoriteId);
        void onDeleteClick();

        void onTabSelected();
        void onTabDeselected();
    }
}
