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
    boolean setListSize(int listSize);
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
