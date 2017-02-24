package com.bytesforge.linkasanote.laano.favorites;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
        void onFavoriteSelected(int position);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void loadInstanceState(@NonNull Bundle outState);
        void applyInstanceState(@NonNull Bundle state);

        void setFavoriteListSize(int favoriteListSize);
        boolean isActionMode();
        void setActionMode(boolean actionMode);
    }

    interface Presenter extends BasePresenter {

        void addFavorite();
        void loadFavorites(boolean forceUpdate);

        void onFavoriteClick(int position);
        boolean onFavoriteLongClick(int position);
    }
}
