package com.bytesforge.linkasanote.laano.favorites;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Favorite;

import java.util.List;

public interface FavoritesContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull FavoritesContract.ViewModel viewModel);
        boolean isActive();
        void showAddFavorite();
        void showFavorites(List<Favorite> favorites);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setFavoriteListSize(int favoriteListSize);
    }

    interface Presenter extends BasePresenter {

        void addFavorite();
        void loadFavorites(boolean forceUpdate);
    }
}
