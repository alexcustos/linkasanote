package com.bytesforge.linkasanote.laano;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.laano.favorites.FavoritesFragment;
import com.bytesforge.linkasanote.laano.links.LinksFragment;
import com.bytesforge.linkasanote.laano.notes.NotesFragment;

import static com.google.common.base.Preconditions.checkNotNull;

public class LaanoFragmentPagerAdapter extends FragmentPagerAdapter {

    public static final int NUM_TABS = 3;

    public static final int LINKS_TAB = 0;
    public static final int FAVORITES_TAB = 1;
    public static final int NOTES_TAB = 2;

    public static final int STATE_DEFAULT = 0; // NOTE: default if no mapping in SparseIntArray
    public static final int STATE_SYNC = 1;
    public static final int STATE_PROBLEM = 2;

    private final Context context;
    private final LayoutInflater inflater;
    private SparseArray<BaseItemFragment> tabFragments = new SparseArray<>();
    private SparseIntArray tabStates = new SparseIntArray();

    public LaanoFragmentPagerAdapter(FragmentManager fm, @NonNull Context context) {
        super(fm);
        this.context = checkNotNull(context);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case LINKS_TAB:
                LinksFragment linksFragment = LinksFragment.newInstance();
                linksFragment.attachTitle(getPageTitle(position).toString());
                return linksFragment;
            case FAVORITES_TAB:
                FavoritesFragment favoritesFragment = FavoritesFragment.newInstance();
                favoritesFragment.attachTitle(getPageTitle(position).toString());
                return favoritesFragment;
            case NOTES_TAB:
                NotesFragment notesFragment = NotesFragment.newInstance();
                notesFragment.attachTitle(getPageTitle(position).toString());
                return notesFragment;
            default:
                throw new IllegalArgumentException(
                        "Unexpected position in the LaanoFragmentPagerAdapter [" + position + "]");
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        BaseItemFragment fragment = (BaseItemFragment) super.instantiateItem(container, position);
        tabFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        tabFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    @Override
    public int getCount() {
        return NUM_TABS;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Resources resources = context.getResources();
        switch (position) {
            case LINKS_TAB:
                return resources.getString(R.string.laano_tab_links_title);
            case FAVORITES_TAB:
                return resources.getString(R.string.laano_tab_favorites_title);
            case NOTES_TAB:
                return resources.getString(R.string.laano_tab_notes_title);
            default:
                throw new IllegalArgumentException(
                        "Unexpected position in the LaanoFragmentPagerAdapter [" + position + "]");
        }
    }

    @DrawableRes
    public int getPageIcon(int position, int state) {
        switch (state) {
            case STATE_SYNC:
                return R.drawable.ic_sync_white_18dp;
            case STATE_PROBLEM:
                return R.drawable.ic_sync_problem_white_18dp;
        }
        // STATE_DEFAULT
        switch (position) {
            case LINKS_TAB:
                return R.drawable.ic_link_white_18dp;
            case FAVORITES_TAB:
                return R.drawable.ic_favorite_white_18dp;
            case NOTES_TAB:
                return R.drawable.ic_note_white_18dp;
            default:
                throw new IllegalArgumentException(
                        "Unexpected position in the LaanoFragmentPagerAdapter [" + position + "]");
        }
    }

    public synchronized void updateTab(@NonNull TabLayout.Tab tab, int position, int state) {
        View tabView = tab.getCustomView();
        if (tabStates.get(position) == state && tabView != null) return;

        if (tabView == null) {
            tabView = inflater.inflate(R.layout.tab_laano, (ViewGroup) null);
            tab.setCustomView(tabView);
        }
        TextView tabTitle = (TextView) tabView.findViewById(android.R.id.text1);
        tabTitle.setCompoundDrawablesWithIntrinsicBounds(getPageIcon(position, state), 0, 0, 0);
        tabStates.put(position, state);
    }

    public BaseItemFragment getFragment(int position) {
        return tabFragments.get(position);
    }

    public LinksFragment getLinksFragment() {
        BaseItemFragment fragment = getFragment(LINKS_TAB);
        if (fragment != null && fragment instanceof LinksFragment) {
            return (LinksFragment) fragment;
        }
        throw new IllegalStateException(
                "LinksFragment was not found in the right position [" + LINKS_TAB + "]");
    }

    public FavoritesFragment getFavoritesFragment() {
        BaseItemFragment fragment = getFragment(FAVORITES_TAB);
        if (fragment != null && fragment instanceof FavoritesFragment) {
            return (FavoritesFragment) fragment;
        }
        throw new IllegalStateException(
                "FavoriteFragment was not found in the right position [" + FAVORITES_TAB + "]");
    }

    public NotesFragment getNotesFragment() {
        BaseItemFragment fragment = getFragment(NOTES_TAB);
        if (fragment != null && fragment instanceof NotesFragment) {
            return (NotesFragment) fragment;
        }
        throw new IllegalStateException(
                "NotesFragment was not found in the right position [" + NOTES_TAB + "]");
    }

    public int getState(int position) {
        return tabStates.get(position);
    }
}
