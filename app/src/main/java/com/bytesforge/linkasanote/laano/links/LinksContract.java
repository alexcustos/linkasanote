package com.bytesforge.linkasanote.laano.links;

import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.laano.LaanoTabPresenter;

public interface LinksContract {

    interface View extends BaseView<Presenter> {

        boolean isActive();
    }

    interface Presenter extends LaanoTabPresenter {

        void addLink();
    }
}
