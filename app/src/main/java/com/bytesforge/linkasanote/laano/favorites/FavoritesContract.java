package com.bytesforge.linkasanote.laano.favorites;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;

public interface FavoritesContract {

    interface View extends BaseView<Presenter> {

        boolean isActive();
    }

    interface Presenter extends BasePresenter {

    }
}
