package com.bytesforge.linkasanote.laano;

import com.bytesforge.linkasanote.BasePresenter;

public interface LaanoTabPresenter extends BasePresenter {

    void onTabSelected();
    void onTabDeselected();
    boolean isConflicted();
    void updateTabNormalState();
}
