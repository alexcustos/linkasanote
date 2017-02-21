package com.bytesforge.linkasanote.addeditfavorite;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Tag;

import java.util.List;

public interface AddEditFavoriteContract {

    interface View extends BaseView<Presenter> {

        boolean isActive();
        void finishActivity();

        void swapTagsCompletionViewItems(List<Tag> tags);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setTagsCompletionView(@NonNull FavoriteTagsCompletionView completionView);
        void showEmptyFavoriteSnackbar();
        void loadInstanceState(Bundle outState);
        void showDuplicateKeyError();
    }

    interface Presenter extends BasePresenter {

        void setViewModel(@NonNull AddEditFavoriteContract.ViewModel viewModel);

        boolean isNewFavorite();
        void saveFavorite(String name, List<Tag> tags);
    }
}
