package com.bytesforge.linkasanote.addeditfavorite;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Tag;

import java.util.List;

public interface AddEditFavoriteContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull AddEditFavoriteContract.ViewModel viewModel);
        boolean isActive();
        void finishActivity();

        void swapTagsCompletionViewItems(List<Tag> tags);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void loadInstanceState(Bundle outState);
        void applyInstanceState(@NonNull Bundle state);

        void setTagsCompletionView(@NonNull FavoriteTagsCompletionView completionView);
        void showEmptyFavoriteSnackbar();
        void showDuplicateKeyError();
    }

    interface Presenter extends BasePresenter {

        boolean isNewFavorite();
        void saveFavorite(String name, List<Tag> tags);
    }
}
