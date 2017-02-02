package com.bytesforge.linkasanote.addeditfavorite;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Tag;

import java.util.List;

public interface AddEditFavoriteContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull AddEditFavoriteContract.ViewModel viewModel);
        void swapTagsCompletionViewItems(List<Tag> tags);

        boolean isActive();
        void finishActivity();
    }

    interface ViewModel extends BaseView<Presenter> {

        void setTagsCompletionView(@NonNull FavoriteTagsCompletionView completionView);
        void showEmptyFavoriteSnackbar();
    }

    interface Presenter extends BasePresenter {

        void saveFavorite(String name, List<Tag> tags);
    }
}
