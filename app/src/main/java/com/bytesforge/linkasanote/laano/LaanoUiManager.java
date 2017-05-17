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
import android.widget.Toast;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.laano.favorites.FavoritesViewModel;
import com.bytesforge.linkasanote.laano.links.LinksViewModel;
import com.bytesforge.linkasanote.laano.notes.NotesViewModel;
import com.bytesforge.linkasanote.manageaccounts.AccountItem;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.CloudUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class LaanoUiManager {

    private final LaanoActivity laanoActivity;
    private final Settings settings;
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
    private boolean syncState = false;

    public LaanoUiManager(
            @NonNull LaanoActivity laanoActivity,
            @NonNull Settings settings,
            @NonNull TabLayout tabLayout, @NonNull Menu drawerMenu,
            @NonNull LaanoDrawerHeaderViewModel headerViewModel,
            @NonNull LaanoFragmentPagerAdapter pagerAdapter) {
        this.laanoActivity = checkNotNull(laanoActivity);
        this.settings = settings;
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
        setTab(position, isConflicted
                ? LaanoFragmentPagerAdapter.STATE_PROBLEM
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

    public void setFilterType(int position, FilterType filterType) {
        switch (filterType) {
            case ALL:
            case CONFLICTED:
                setFilterType(position, filterType, null);
                break;
            default:
                throw new IllegalArgumentException(
                        "This filter type must be set with the title [" + filterType.name() + "]");
        }
    }

    public void setFilterType(
            int position, @NonNull FilterType filterType, String filterTitle) {
        checkNotNull(filterType);

        String normalTitle;
        switch (filterType) {
            case ALL:
                normalTitle = resources.getString(R.string.filter_all);
                break;
            case CONFLICTED:
                normalTitle = resources.getString(R.string.filter_conflicted);
                break;
            case LINK:
                if (filterTitle == null) {
                    throw new IllegalStateException("Link filter title must not be null or empty");
                }
                normalTitle = LinksViewModel.FILTER_PREFIX + " " + filterTitle;
                break;
            case FAVORITE:
                if (filterTitle == null) {
                    throw new IllegalStateException("Favorite filter title must not be null or empty");
                }
                normalTitle = FavoritesViewModel.FILTER_PREFIX + " " + filterTitle;
                break;
            case NOTE:
                if (filterTitle == null) {
                    throw new IllegalStateException("Note filter title must not be null or empty");
                }
                normalTitle = NotesViewModel.FILTER_PREFIX + " " + filterTitle;
                break;
            case NO_TAGS:
                normalTitle = resources.getString(R.string.filter_no_tags);
                break;
            case UNBOUND:
                normalTitle = resources.getString(R.string.filter_unbound);
                break;
            default:
                throw new IllegalArgumentException("Unexpected filtering type [" + filterType.name() + "]");
        }
        setNormalTitle(position, normalTitle);
    }

    private void setNormalTitle(int position, String normalTitle) {
        normalTitles.put(position, normalTitle);
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
        settings.setSyncable(account != null);
        if (settings.isSyncable()) {
            assert account != null;
            AccountItem accountItem = CloudUtils.getAccountItem(account, laanoActivity);
            headerViewModel.showAccount(accountItem);
        } else {
            headerViewModel.showAppName();
        }
        updateDrawerMenu();
    }

    public void updateSyncStatus() {
        long lastSyncTime = settings.getLastSyncTime();
        int syncStatus = settings.getSyncStatus();
        headerViewModel.showSyncStatus(lastSyncTime, syncStatus);
    }

    public void showShortToast(@StringRes int toastId) {
        Toast.makeText(laanoActivity, toastId, Toast.LENGTH_SHORT).show();
    }

    public void showLongToast(@StringRes int toastId) {
        Toast.makeText(laanoActivity, toastId, Toast.LENGTH_LONG).show();
    }

    private void updateDrawerMenu() {
        if (Settings.GLOBAL_MULTIACCOUNT_SUPPORT) {
            drawerMenu.findItem(R.id.add_account_menu_item).setVisible(true);
            drawerMenu.findItem(R.id.manage_accounts_menu_item).setVisible(true);
        } else if (settings.isSyncable()) {
            drawerMenu.findItem(R.id.add_account_menu_item).setVisible(false);
            drawerMenu.findItem(R.id.manage_accounts_menu_item).setVisible(true);
        } else {
            drawerMenu.findItem(R.id.add_account_menu_item).setVisible(true);
            drawerMenu.findItem(R.id.manage_accounts_menu_item).setVisible(false);
        }
        setNormalDrawerMenu();
    }

    public void setSyncDrawerMenu() {
        if (!syncState) {
            syncState = true;
            drawerMenu.findItem(R.id.sync_menu_item).setTitle(R.string.drawer_actions_sync_in_progress);
            drawerMenu.findItem(R.id.sync_menu_item).setEnabled(false);
        }
    }

    public void setNormalDrawerMenu() {
        syncState = false;
        drawerMenu.findItem(R.id.sync_menu_item).setTitle(R.string.drawer_actions_sync_start);
        if (settings.isSyncable()) {
            drawerMenu.findItem(R.id.sync_menu_item).setEnabled(true);
        } else {
            drawerMenu.findItem(R.id.sync_menu_item).setEnabled(false);
        }
    }

    public void setCurrentTab(int tab) {
        laanoActivity.setCurrentTab(tab);
    }
}
