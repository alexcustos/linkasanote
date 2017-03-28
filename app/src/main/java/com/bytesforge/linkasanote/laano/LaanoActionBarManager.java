package com.bytesforge.linkasanote.laano;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.laano.favorites.FavoritesFilterType;

import static com.google.common.base.Preconditions.checkNotNull;

public class LaanoActionBarManager {

    private final Resources resources;
    private final ActionBar actionBar;
    private final TabLayout tabLayout;
    private final LaanoFragmentPagerAdapter pagerAdapter;

    private SparseArray<String> syncTitles = new SparseArray<>();
    private SparseArray<String> normalTitles = new SparseArray<>();

    private SparseIntArray syncUploaded = new SparseIntArray();
    private SparseIntArray syncDownloaded = new SparseIntArray();
    private SparseIntArray normalFilterType = new SparseIntArray();

    public LaanoActionBarManager(
            @NonNull Context context, @NonNull ActionBar actionBar, @NonNull TabLayout tabLayout,
            @NonNull LaanoFragmentPagerAdapter pagerAdapter) {
        resources = checkNotNull(context).getResources();
        this.actionBar = checkNotNull(actionBar);
        this.tabLayout = checkNotNull(tabLayout);
        this.pagerAdapter = checkNotNull(pagerAdapter);
    }

    public void setTabSyncState(int position) {
        updateTab(position, LaanoFragmentPagerAdapter.STATE_SYNC);
        syncUploaded.put(position, 0);
        syncDownloaded.put(position, 0);
        updateSyncTitle(position);
    }

    public void setTabNormalState(int position, boolean isConflicted) {
        updateTab(position, isConflicted ? LaanoFragmentPagerAdapter.STATE_PROBLEM
                : LaanoFragmentPagerAdapter.STATE_DEFAULT);
    }

    private void updateTab(int position, int state) {
        TabLayout.Tab tab = tabLayout.getTabAt(position);
        if (tab == null) return;

        pagerAdapter.updateTab(tab, position, state);
    }

    private void updateSyncTitle(int position) {
        String title = String.format(resources.getString(
                          R.string.laano_actionbar_manager_sync_title),
                syncUploaded.get(position), syncDownloaded.get(position));
        syncTitles.put(position, title);
    }

    public void setFavoriteFilterType(FavoritesFilterType filterType) {
        int position = LaanoFragmentPagerAdapter.FAVORITES_TAB;
        switch (filterType) {
            case FAVORITES_ALL:
                normalFilterType.put(position, R.string.filter_favorites_all);
                break;
            case FAVORITES_CONFLICTED:
                normalFilterType.put(position, R.string.filter_favorites_conflicted);
                break;
            default:
                throw new IllegalArgumentException("Unexpected filtering type [" + filterType.name() + "]");
        }
        updateNormalTitle(position);
        showTitle(position);
    }

    private void updateNormalTitle(int position) {
        @StringRes int titleId = normalFilterType.get(position);
        normalTitles.put(position, resources.getString(titleId));
    }

    public void showTitle(int position) {
        String title;
        if (pagerAdapter.getState(position) == LaanoFragmentPagerAdapter.STATE_SYNC) {
            title = syncTitles.get(position);
        } else {
            title = normalTitles.get(position);
        }
        if (title == null) { // Fallback
            title = pagerAdapter.getPageTitle(position).toString();
        }
        actionBar.setTitle(title);
    }

    public void incUploaded(int position) {
        syncUploaded.put(position, syncUploaded.get(position) + 1);
        updateSyncTitle(position);
    }

    public void incDownloaded(int position) {
        syncDownloaded.put(position, syncDownloaded.get(position) + 1);
        updateSyncTitle(position);
    }
}
