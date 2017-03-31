package com.bytesforge.linkasanote.laano;

import android.accounts.Account;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Menu;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.laano.favorites.FavoritesFilterType;
import com.bytesforge.linkasanote.manageaccounts.AccountItem;
import com.bytesforge.linkasanote.utils.CloudUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class LaanoUiManager {

    private final LaanoActivity laanoActivity;
    private final Resources resources;
    private final ActionBar actionBar;
    private final TabLayout tabLayout;
    private final Menu drawerMenu;
    private final LaanoDrawerHeaderViewModel headerViewModel;
    private final LaanoFragmentPagerAdapter pagerAdapter;

    private SparseArray<String> syncTitles = new SparseArray<>();
    private SparseArray<String> normalTitles = new SparseArray<>();

    private SparseIntArray syncUploaded = new SparseIntArray();
    private SparseIntArray syncDownloaded = new SparseIntArray();
    private SparseIntArray filterType = new SparseIntArray();
    private boolean isAccount = false;
    private boolean isSyncState = false;

    public LaanoUiManager(
            @NonNull LaanoActivity laanoActivity,
            @NonNull TabLayout tabLayout, @NonNull Menu drawerMenu,
            @NonNull LaanoDrawerHeaderViewModel headerViewModel,
            @NonNull LaanoFragmentPagerAdapter pagerAdapter) {
        this.laanoActivity = checkNotNull(laanoActivity);
        this.tabLayout = checkNotNull(tabLayout);
        this.drawerMenu = checkNotNull(drawerMenu);
        this.headerViewModel = checkNotNull(headerViewModel);
        this.pagerAdapter = checkNotNull(pagerAdapter);
        resources = checkNotNull(laanoActivity).getResources();
        this.actionBar = checkNotNull(laanoActivity.getSupportActionBar());
    }

    public void setTabSyncState(int position) {
        setTab(position, LaanoFragmentPagerAdapter.STATE_SYNC);
        syncUploaded.put(position, 0);
        syncDownloaded.put(position, 0);
        setSyncTitle(position);
    }

    public void setTabNormalState(int position, boolean isConflicted) {
        setTab(position, isConflicted ? LaanoFragmentPagerAdapter.STATE_PROBLEM
                : LaanoFragmentPagerAdapter.STATE_DEFAULT);
    }

    private void setTab(int position, int state) {
        TabLayout.Tab tab = tabLayout.getTabAt(position);
        if (tab == null) return;

        pagerAdapter.updateTab(tab, position, state);
    }

    private void setSyncTitle(int position) {
        String title = String.format(resources.getString(
                          R.string.laano_actionbar_manager_sync_title),
                syncUploaded.get(position), syncDownloaded.get(position));
        syncTitles.put(position, title);
    }

    public void setFavoriteFilterType(FavoritesFilterType filterType) {
        int position = LaanoFragmentPagerAdapter.FAVORITES_TAB;
        @StringRes int filterTitleId;
        switch (filterType) {
            case FAVORITES_ALL:
                filterTitleId = R.string.filter_favorites_all;
                break;
            case FAVORITES_CONFLICTED:
                filterTitleId = R.string.filter_favorites_conflicted;
                break;
            default:
                throw new IllegalArgumentException("Unexpected filtering type [" + filterType.name() + "]");
        }
        this.filterType.put(position, filterTitleId);
        setNormalTitle(position);
    }

    private void setNormalTitle(int position) {
        @StringRes int titleId = filterType.get(position);
        normalTitles.put(position, resources.getString(titleId));
    }

    public void updateTitle(int position) {
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
        setSyncTitle(position);
    }

    public void incDownloaded(int position) {
        syncDownloaded.put(position, syncDownloaded.get(position) + 1);
        setSyncTitle(position);
    }

    public void updateDefaultAccount(Account account) {
        isAccount = (account != null);
        if (isAccount) {
            AccountItem accountItem = CloudUtils.getAccountItem(account, laanoActivity);
            headerViewModel.showAccount(accountItem);
        } else {
            headerViewModel.showAppName();
        }
        updateDrawerMenu();
    }

    public void updateLastSyncStatus() {
        long lastSyncTime = CloudUtils.getLastSyncTime(laanoActivity);
        int lastSyncStatus = CloudUtils.getLastSyncStatus(laanoActivity);
        headerViewModel.showLastSyncStatus(lastSyncTime, lastSyncStatus);
    }

    private void updateDrawerMenu() {
        if (resources.getBoolean(R.bool.multiaccount_support)) {
            drawerMenu.findItem(R.id.add_account_menu_item).setVisible(true);
            drawerMenu.findItem(R.id.manage_accounts_menu_item).setVisible(true);
        } else if (isAccount) {
            drawerMenu.findItem(R.id.add_account_menu_item).setVisible(false);
            drawerMenu.findItem(R.id.manage_accounts_menu_item).setVisible(true);
        } else {
            drawerMenu.findItem(R.id.add_account_menu_item).setVisible(true);
            drawerMenu.findItem(R.id.manage_accounts_menu_item).setVisible(false);
        }
        setNormalDrawerMenu();
    }

    public void setSyncDrawerMenu() {
        if (!isSyncState) {
            isSyncState = true;
            drawerMenu.findItem(R.id.sync_menu_item).setTitle(R.string.drawer_actions_sync_in_progress);
            drawerMenu.findItem(R.id.sync_menu_item).setEnabled(false);
        }
    }

    public void setNormalDrawerMenu() {
        isSyncState = false;
        drawerMenu.findItem(R.id.sync_menu_item).setTitle(R.string.drawer_actions_sync_start);
        if (isAccount) {
            drawerMenu.findItem(R.id.sync_menu_item).setEnabled(true);
        } else {
            drawerMenu.findItem(R.id.sync_menu_item).setEnabled(false);
        }
    }
}
