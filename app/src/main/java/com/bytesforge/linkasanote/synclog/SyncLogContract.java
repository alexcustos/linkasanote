package com.bytesforge.linkasanote.synclog;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.SyncResult;

import java.util.List;

public interface SyncLogContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull SyncLogContract.ViewModel viewModel);
        boolean isActive();

        void showSyncResults(@NonNull List<SyncResult> syncResults);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(Bundle outState);
        void applyInstanceState(@NonNull Bundle state);
        Bundle getDefaultInstanceState();

        int getListSize();
        boolean setListSize(int listSize);

        void showProgressOverlay();
        void hideProgressOverlay();

        void showDatabaseErrorSnackbar();
    }

    interface Presenter extends BasePresenter {
    }
}
