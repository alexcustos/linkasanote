package com.bytesforge.linkasanote.laano.notes;

import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.laano.LaanoTabPresenter;

public interface NotesContract {

    interface View extends BaseView<Presenter> {

        boolean isActive();
    }

    interface Presenter extends LaanoTabPresenter {

        void addNote();
    }
}
