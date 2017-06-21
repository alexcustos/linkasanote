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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

public interface BaseItemViewModelInterface {

    void setInstanceState(@Nullable Bundle savedInstanceState);
    void saveInstanceState(@NonNull Bundle outState);
    void applyInstanceState(@NonNull Bundle state);

    int getListSize();
    void setListSize(int listSize);
    boolean isActionMode();
    void enableActionMode();
    void disableActionMode();

    boolean isSelected(String id);
    void setSelection(String[] ids);
    void toggleSelection(@NonNull String id);
    boolean toggleSingleSelection(@NonNull String id);
    void setSingleSelection(@NonNull String id, boolean selected);
    void removeSelection();
    void removeSelection(@NonNull String id);
    int getSelectedCount();
    ArrayList<String> getSelectedIds();
    boolean toggleFilterId(@NonNull String filterId);
    void setFilterId(String filterId);

    String getSearchText();
    void setSearchText(String searchText);

    void showProgressOverlay();
    void hideProgressOverlay();
}
