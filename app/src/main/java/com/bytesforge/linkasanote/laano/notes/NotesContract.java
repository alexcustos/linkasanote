package com.bytesforge.linkasanote.laano.notes;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;

public interface NotesContract {

    interface View extends BaseView<Presenter> {

        boolean isActive();
    }

    interface Presenter extends BasePresenter {

    }
}
