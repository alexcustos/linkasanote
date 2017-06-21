/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    }
}
