package com.bytesforge.linkasanote.addeditfavorite;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Tag;

import java.util.List;

public interface AddEditFavoriteContract {

    interface View extends BaseView<Presenter> {

        void swapTagsCompletionViewItems(List<Tag> tags);

        boolean isActive();
        void finishActivity();
    }

    interface ViewModel extends BaseView<Presenter> {

        void setTagsCompletionView(@NonNull FavoriteTagsCompletionView completionView);
        void showEmptyFavoriteSnackbar();
        void onSaveInstanceState(Bundle outState);
    }

    interface Presenter extends BasePresenter {

        boolean isNewFavorite();
        void saveFavorite(String name, List<Tag> tags);
    }
}
