package com.bytesforge.linkasanote.links;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;

public interface LinksContract {

    interface View extends BaseView<Presenter> {

        boolean isActive();
    }

    interface Presenter extends BasePresenter {

    }
}
