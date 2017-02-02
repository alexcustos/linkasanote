package com.bytesforge.linkasanote.laano.favorites;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.annotation.NonNull;

import com.android.databinding.library.baseAdapters.BR;

import static com.google.common.base.Preconditions.checkNotNull;

public class FavoritesViewModel extends BaseObservable implements FavoritesContract.ViewModel {

    private FavoritesContract.Presenter presenter;
    private Context context;
    private int favoriteListSize = 0;

    public FavoritesViewModel(Context context) {
        this.context = context;
    }

    @Override
    public void setPresenter(@NonNull FavoritesContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Bindable
    public boolean isFavoritesEmpty() {
        return favoriteListSize <= 0;
    }

    @Override
    public void setFavoriteListSize(int favoriteListSize) {
        this.favoriteListSize = favoriteListSize;
        notifyPropertyChanged(BR.favoritesEmpty);
    }
}
