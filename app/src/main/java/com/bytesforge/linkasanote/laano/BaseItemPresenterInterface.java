package com.bytesforge.linkasanote.laano;

import com.bytesforge.linkasanote.BasePresenter;

public interface BaseItemPresenterInterface extends BasePresenter {

    void onTabSelected();
    void onTabDeselected();
    void updateTabNormalState();
    void updateSyncStatus();
}
