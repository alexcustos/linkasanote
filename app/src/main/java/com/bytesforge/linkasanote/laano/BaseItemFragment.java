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

package com.bytesforge.linkasanote.laano;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public abstract class BaseItemFragment extends Fragment {

    public static final String ARGUMENT_TITLE = "ARGUMENT_TITLE";

    private String title;

    public void attachTitle(String tabTitle) {
        Bundle args = new Bundle();
        args.putString(ARGUMENT_TITLE, tabTitle);
        setArguments(args);
    }

    public String getTitle() {
        if (title == null) {
            title = getArguments().getString(ARGUMENT_TITLE);
        }
        return title;
    }
}
