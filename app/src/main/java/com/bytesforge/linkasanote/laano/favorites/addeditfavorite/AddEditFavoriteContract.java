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
