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
        void addFavorites(@NonNull List<Favorite> favorites);
        void clearFavorites();
        void updateView();
        void enableActionMode();
        void finishActionMode();
        void selectionChanged(@NonNull String id);
        void removeFavorite(@NonNull String id);
        int getPosition(String favoriteId);
        @NonNull String[] getIds();
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
        void loadFavorites(final boolean forceUpdate);

        void onFavoriteClick(String favoriteId, boolean isConflicted);
        boolean onFavoriteLongClick(String favoriteId);
        void onCheckboxClick(String favoriteId);
        void selectFavoriteFilter();

        void onEditClick(@NonNull String favoriteId);
        void onToLinksClick(@NonNull Favorite favoriteFilter);
        void onToNotesClick(@NonNull Favorite favoriteFilter);
        void onDeleteClick();
        void onSelectAllClick();
        int getPosition(String favoriteId);
        void setFilterType(@NonNull FilterType filtering);
        @NonNull FilterType getFilterType();
        void syncSavedFavorite(@NonNull final String favoriteId);
        void deleteFavorites(@NonNull ArrayList<String> selectedIds);
        void setFilterIsChanged(boolean filterIsChanged);
    }
}
