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

import android.accounts.Account;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Menu;
import android.widget.Toast;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.laano.favorites.FavoritesViewModel;
import com.bytesforge.linkasanote.laano.links.LinksViewModel;
import com.bytesforge.linkasanote.laano.notes.NotesViewModel;
import com.bytesforge.linkasanote.manageaccounts.AccountItem;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncAdapter;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.bytesforge.linkasanote.utils.CommonUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class LaanoUiManager {

    private static final String TAG = LaanoUiManager.class.getSimpleName();

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
        if (pagerAdapter.getState(position) != LaanoFragmentPagerAdapter.STATE_SYNC) {
            setTab(position, LaanoFragmentPagerAdapter.STATE_SYNC);
            setSyncTitle(position);
        }
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

    public void setFilterType(int position, @NonNull FilterType filterType) {
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
                Link linkFilter = settings.getLinkFilter();
                if (linkFilter == null) {
                    throw new IllegalStateException("setFilterType(): Link filter must not be null");
                }
                String linkTitle = linkFilter.getName() == null
                        ? linkFilter.getLink()
                        : linkFilter.getName();
                normalTitle = LinksViewModel.FILTER_PREFIX + " " + linkTitle;
                break;
            case FAVORITE:
                Favorite favoriteFilter = settings.getFavoriteFilter();
                if (favoriteFilter == null) {
                    throw new IllegalStateException("setFilterType(): Favorite filter must not be null");
                }
                String filterPrefix = favoriteFilter.isAndGate()
                            ? FavoritesViewModel.FILTER_AND_GATE_PREFIX
                            : FavoritesViewModel.FILTER_OR_GATE_PREFIX;
                normalTitle = filterPrefix + " " + favoriteFilter.getName();
                break;
            case NOTE:
                Note noteFilter = settings.getNoteFilter();
                if (noteFilter == null) {
                    throw new IllegalStateException("setFilterType(): Note filter must not be null");
                }
                normalTitle = NotesViewModel.FILTER_PREFIX + " " + noteFilter.getNote();
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
        String title = CommonUtils.strFirstLine(normalTitle);
        normalTitles.put(position, title);
    }

    public void updateTitle(int position) {
        if (position != laanoActivity.getActiveTab()) return;

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

    public void resetCounters(int position) {
        syncUploaded.put(position, 0);
        syncDownloaded.put(position, 0);
    }

    public void setUploaded(int position, int count) {
        syncUploaded.put(position, count);
        setSyncTitle(position);
    }

    public void incUploaded(int position) {
        syncUploaded.put(position, syncUploaded.get(position) + 1);
        setSyncTitle(position);
    }

    public void setDownloaded(int position, int count) {
        syncDownloaded.put(position, count);
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
        updateNormalDrawerMenu();
    }

    /**
     * Update Navigation Drawer Header
     */
    public void updateSyncStatus() {
        long lastSyncTime = settings.getLastSyncTime();
        int syncStatus = settings.getSyncStatus();
        headerViewModel.showSyncStatus(lastSyncTime, syncStatus);
    }

    public void notifySyncStatus() {
        int syncStatus = settings.getSyncStatus();
        switch (syncStatus) {
            case SyncAdapter.SYNC_STATUS_SYNCED:
                showShortToast(R.string.toast_sync_success);
                break;
            case SyncAdapter.SYNC_STATUS_ERROR:
                showLongToast(R.string.toast_sync_error);
                break;
            case SyncAdapter.SYNC_STATUS_CONFLICT:
                showLongToast(R.string.toast_sync_conflict);
                break;
        }
    }

    public void showShortToast(@StringRes int toastId) {
        Toast.makeText(laanoActivity, toastId, Toast.LENGTH_SHORT).show();
    }

    public void showLongToast(@StringRes int toastId) {
        Toast.makeText(laanoActivity, toastId, Toast.LENGTH_LONG).show();
    }

    private void updateNormalDrawerMenu() {
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

    /**
     * Set Navigation Drawer Menu to Normal state
     */
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

    public void showApplicationOfflineSnackbar() {
        Snackbar.make(tabLayout, R.string.laano_offline, Snackbar.LENGTH_LONG).show();
    }

    public void showApplicationNotSyncableSnackbar() {
        Snackbar.make(tabLayout, R.string.laano_not_syncable, Snackbar.LENGTH_LONG).show();
    }
}
