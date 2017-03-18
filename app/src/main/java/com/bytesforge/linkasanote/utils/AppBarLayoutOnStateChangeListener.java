package com.bytesforge.linkasanote.utils;

import android.support.design.widget.AppBarLayout;

public abstract class AppBarLayoutOnStateChangeListener implements AppBarLayout.OnOffsetChangedListener {

    public enum State {EXPANDED, COLLAPSED}

    private State currentState = State.EXPANDED;

    public abstract void onStateChanged(AppBarLayout appBarLayout, State state);

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (verticalOffset == 0
                && currentState != State.EXPANDED) {
            currentState = State.EXPANDED;
            onStateChanged(appBarLayout, currentState);
        } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()
                && currentState != State.COLLAPSED) {
            currentState = State.COLLAPSED;
            onStateChanged(appBarLayout, currentState);
        }
    } // onOffsetChanged
}
