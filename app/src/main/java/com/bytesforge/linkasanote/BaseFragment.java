package com.bytesforge.linkasanote;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public abstract class BaseFragment extends Fragment {

    public static final String ARG_TITLE = "ARG_TITLE";

    private String title = null;

    public static void attachTitle(String tabTitle, Fragment fragment) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, tabTitle);
        fragment.setArguments(args);
    }

    public String getTitle() {
        if (title == null) {
            title = getArguments().getString(ARG_TITLE);
        }
        return title;
    }
}
