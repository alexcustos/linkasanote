package com.bytesforge.linkasanote;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public abstract class BaseFragment extends Fragment {

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
