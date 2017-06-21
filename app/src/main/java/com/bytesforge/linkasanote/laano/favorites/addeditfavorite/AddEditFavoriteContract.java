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

package com.bytesforge.linkasanote.laano.favorites.addeditfavorite;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.laano.ClipboardService;
import com.bytesforge.linkasanote.laano.TagsCompletionView;
import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;

public interface AddEditFavoriteContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull AddEditFavoriteContract.ViewModel viewModel);
        boolean isActive();
        void finishActivity(String favoriteId);

        void swapTagsCompletionViewItems(List<Tag> tags);
        void setFavoritePaste(int clipboardState);
        void fillInForm();
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(Bundle outState);
        void applyInstanceState(@NonNull Bundle state);
        Bundle getDefaultInstanceState();

        void setTagsCompletionView(@NonNull TagsCompletionView completionView);
        void showDatabaseErrorSnackbar();
        void showEmptyFavoriteSnackbar();
        void showFavoriteNotFoundSnackbar();
        void showDuplicateKeyError();
        void showTagsDuplicateRemovedToast();

        boolean isValid();
        boolean isEmpty();
        void checkAddButton();
        void enableAddButton();
        void disableAddButton();
        void hideNameError();
        void afterTagsChanged();

        void populateFavorite(@NonNull Favorite favorite);
        void setFavoriteName(String favoriteName);
        void setFavoriteTags(String[] tags);
        SyncState getFavoriteSyncState();
    }

    interface Presenter extends BasePresenter, ClipboardService.Callback {

        boolean isNewFavorite();
        void loadTags();
        void saveFavorite(String name, boolean andGate, List<Tag> tags);
        void populateFavorite();

        void setShowFillInFormInfo(boolean show);
        boolean isShowFillInFormInfo();
    }
}
