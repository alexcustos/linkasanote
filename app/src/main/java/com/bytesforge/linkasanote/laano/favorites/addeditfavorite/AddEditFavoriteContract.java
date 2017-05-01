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

import java.util.List;

public interface AddEditFavoriteContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull AddEditFavoriteContract.ViewModel viewModel);
        boolean isActive();
        void finishActivity(String favoriteId);

        void swapTagsCompletionViewItems(List<Tag> tags);
        void setFavoritePaste(int clipboardState);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(Bundle outState);
        void applyInstanceState(@NonNull Bundle state);
        Bundle getDefaultInstanceState();

        void setTagsCompletionView(@NonNull TagsCompletionView completionView);
        void showEmptyFavoriteSnackbar();
        void showFavoriteNotFoundSnackbar();
        void showDuplicateKeyError();

        boolean isValid();
        void checkAddButton();
        void enableAddButton();
        void disableAddButton();
        void hideNameError();
        void afterNameChanged();
        void afterTagsChanged();

        void populateFavorite(@NonNull Favorite favorite);
        void setFavoriteName(String favoriteName);
        void setFavoriteTags(String[] tags);
    }

    interface Presenter extends BasePresenter, ClipboardService.Callback {

        boolean isNewFavorite();
        void loadTags();
        void saveFavorite(String name, List<Tag> tags);
        void populateFavorite();

        void setShowFillInFormInfo(boolean show);
        boolean isShowFillInFormInfo();
    }
}
