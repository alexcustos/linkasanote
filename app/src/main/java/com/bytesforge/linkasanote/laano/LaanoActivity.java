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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AuthenticatorException;
import android.accounts.OnAccountsUpdateListener;
import android.accounts.OperationCanceledException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.about.AboutActivity;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.databinding.ActivityLaanoBinding;
import com.bytesforge.linkasanote.laano.favorites.FavoritesPresenter;
import com.bytesforge.linkasanote.laano.favorites.FavoritesPresenterModule;
import com.bytesforge.linkasanote.laano.links.LinksPresenter;
import com.bytesforge.linkasanote.laano.links.LinksPresenterModule;
import com.bytesforge.linkasanote.laano.links.addeditlink.AddEditLinkActivity;
import com.bytesforge.linkasanote.laano.notes.NotesPresenter;
import com.bytesforge.linkasanote.laano.notes.NotesPresenterModule;
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsActivity;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.settings.SettingsActivity;
import com.bytesforge.linkasanote.sync.SyncAdapter;
import com.bytesforge.linkasanote.sync.SyncNotifications;
import com.bytesforge.linkasanote.synclog.SyncLogActivity;
import com.bytesforge.linkasanote.utils.AppBarLayoutOnStateChangeListener;
import com.bytesforge.linkasanote.utils.CloudUtils;

import java.io.IOException;

import javax.inject.Inject;

import static com.bytesforge.linkasanote.utils.CloudUtils.getAccountType;
import static com.google.common.base.Preconditions.checkNotNull;

public class LaanoActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = LaanoActivity.class.getSimpleName();

    private static final String STATE_ACTIVE_TAB = "ACTIVE_TAB";

    private static final int REQUEST_GET_ACCOUNTS = 0;
    private static final String PERMISSION_GET_ACCOUNTS = Manifest.permission.GET_ACCOUNTS;
    private static String[] PERMISSIONS_GET_ACCOUNTS = new String[]{PERMISSION_GET_ACCOUNTS};

    private static final int ACTION_MANAGE_ACCOUNTS = 1;

    @Inject
    LinksPresenter linksPresenter;

    @Inject
    FavoritesPresenter favoritesPresenter;

    @Inject
    NotesPresenter notesPresenter;

    @Inject
    LaanoUiManager laanoUiManager;

    @Inject
    AccountManager accountManager;

    @Inject
    Settings settings;

    private boolean doubleBackPressed = false;
    private int activeTab;
    private long lastSyncTime = 0;
    private IntentFilter connectivityIntentFilter;
    private ConnectivityBroadcastReceiver connectivityBroadcastReceiver;
    private IntentFilter syncIntentFilter;
    private SyncBroadcastReceiver syncBroadcastReceiver;
    private ActivityLaanoBinding binding;
    private LaanoViewModel viewModel;

    @Override
    protected void onStart() {
        if (Settings.GLOBAL_CLIPBOARD_MONITOR_ON_START) {
            // NOTE: application context
            startClipboardService();
        } // NOTE: else it will be started with the first launch of the addEdit... activity
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        long synced = settings.getLastSyncTime();
        if (lastSyncTime != synced) {
            lastSyncTime = synced;
            laanoUiManager.updateSyncStatus();
            laanoUiManager.setNormalDrawerMenu();
        }
        linksPresenter.updateTabNormalState();
        favoritesPresenter.updateTabNormalState();
        notesPresenter.updateTabNormalState();

        notifyTabSelected(activeTab);
        laanoUiManager.updateTitle(activeTab);

        registerReceiver(connectivityBroadcastReceiver, connectivityIntentFilter);
        registerReceiver(syncBroadcastReceiver, syncIntentFilter);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(syncBroadcastReceiver);
        unregisterReceiver(connectivityBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        accountManager.removeOnAccountsUpdatedListener(accountsUpdateListener);
        if (!isChangingConfigurations()) {
            stopClipboardService();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            applyInstanceState(getDefaultInstanceState());
        } else {
            applyInstanceState(savedInstanceState);
        }
        TagsBindingAdapter.invalidateTagsViewWidths();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_laano);
        viewModel = new LaanoViewModel(this);
        viewModel.setInstanceState(savedInstanceState);
        binding.setViewModel(viewModel);

        // Toolbar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // Navigation Drawer
        setupDrawerLayout(binding.drawerLayout);
        setupDrawerContent(binding.navView);
        // ViewPager
        LaanoFragmentPagerAdapter pagerAdapter = new LaanoFragmentPagerAdapter(
                getSupportFragmentManager(), getApplicationContext());
        ViewPager viewPager = binding.laanoViewPager;
        setupViewPager(viewPager, pagerAdapter);
        setCurrentTab(activeTab);
        viewPager.setOffscreenPageLimit(2);
        // TabLayout
        setupTabLayout(binding.tabLayout, viewPager);
        // Presenters
        LaanoApplication application = (LaanoApplication) getApplication();
        application.getApplicationComponent()
                .getLaanoComponent(
                        new LinksPresenterModule(this, pagerAdapter.getLinksFragment()),
                        new FavoritesPresenterModule(this, pagerAdapter.getFavoritesFragment()),
                        new NotesPresenterModule(this, pagerAdapter.getNotesFragment()),
                        new LaanoUiManagerModule(this, binding.tabLayout,
                                binding.navView.getMenu(), viewModel.headerViewModel, pagerAdapter))
                .inject(this);
        // FAB
        setupFabAdd(binding.fabAdd);
        // AppBar
        setupAppBarLayout(binding.appBarLayout, binding.fabAdd);
        // Connectivity receiver
        connectivityIntentFilter = new IntentFilter();
        connectivityIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        connectivityBroadcastReceiver = new ConnectivityBroadcastReceiver();
        // Sync receiver
        syncIntentFilter = new IntentFilter();
        syncIntentFilter.addAction(SyncNotifications.ACTION_SYNC);
        syncIntentFilter.addAction(SyncNotifications.ACTION_SYNC_LINKS);
        syncIntentFilter.addAction(SyncNotifications.ACTION_SYNC_FAVORITES);
        syncIntentFilter.addAction(SyncNotifications.ACTION_SYNC_NOTES);
        syncBroadcastReceiver = new SyncBroadcastReceiver();
        // AccountManager
        accountManager.addOnAccountsUpdatedListener(accountsUpdateListener, null, true);

        handleStartIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        handleStartIntent();
    }

    private void handleStartIntent() {
        Intent startIntent = getIntent();
        String action = startIntent.getAction();
        String type = startIntent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = startIntent.getStringExtra(Intent.EXTRA_TEXT);
                startAddEditLinkActivity(sharedText);
            }
        }
    }

    private void startClipboardService() {
        Intent intent = new Intent(getApplicationContext(), ClipboardService.class);
        startService(intent);
    }

    private void stopClipboardService() {
        Intent intent = new Intent(getApplicationContext(), ClipboardService.class);
        stopService(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ACTIVE_TAB, activeTab);
        viewModel.saveInstanceState(outState);
    }

    private void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        activeTab = state.getInt(STATE_ACTIVE_TAB);
    }

    private Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();
        defaultState.putInt(STATE_ACTIVE_TAB, 0);
        return defaultState;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        DrawerLayout drawerLayout = binding.drawerLayout;
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void triggerSync() {
        Account account = CloudUtils.getDefaultAccount(this, accountManager);
        if (account == null) { // NOTE: should not be happened
            updateDefaultAccount();
            laanoUiManager.showApplicationNotSyncableSnackbar();
            return;
        }
        laanoUiManager.resetCounters(LaanoFragmentPagerAdapter.LINKS_TAB);
        laanoUiManager.resetCounters(LaanoFragmentPagerAdapter.FAVORITES_TAB);
        laanoUiManager.resetCounters(LaanoFragmentPagerAdapter.NOTES_TAB);
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        extras.putBoolean(SyncAdapter.SYNC_MANUAL_MODE, true);
        ContentResolver.requestSync(account, LocalContract.CONTENT_AUTHORITY, extras);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*if (ACTION_MANAGE_ACCOUNTS == requestCode && RESULT_OK == resultCode) {
        }*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_laano, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (doubleBackPressed) {
            super.onBackPressed();
            return;
        }
        doubleBackPressed = true;
        laanoUiManager.showShortToast(R.string.toast_double_back_press);
        new Handler().postDelayed(
                () -> doubleBackPressed = false,
                Settings.GLOBAL_DOUBLE_BACK_TO_EXIT_MILLIS);
    }

    private void updateDefaultAccount() {
        Log.d(TAG, "updateDefaultAccount()");
        Account account = CloudUtils.getDefaultAccount(this, accountManager);
        laanoUiManager.updateDefaultAccount(account);
        laanoUiManager.updateSyncStatus();
    }

    // Public

    public int getActiveTab() {
        return activeTab;
    }

    public void setCurrentTab(int tab) {
        ViewPager viewPager = binding.laanoViewPager;
        viewPager.setCurrentItem(tab);
    }

    // Get Accounts Permission

    public void checkGetAccountsPermissionAndLaunchActivity() {
        if (ContextCompat.checkSelfPermission(this, PERMISSION_GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestGetAccountsPermission();
            } else {
                showPermissionDeniedSnackbar();
            }
        } else {
            startManageAccountsActivity();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestGetAccountsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_GET_ACCOUNTS)) {
            Snackbar.make(binding.laanoViewPager,
                    R.string.laano_permission_get_accounts,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_button_ok, view ->
                            ActivityCompat.requestPermissions(
                                    this, PERMISSIONS_GET_ACCOUNTS, REQUEST_GET_ACCOUNTS))
                    .show();
        } else {
            ActivityCompat.requestPermissions(
                    this, PERMISSIONS_GET_ACCOUNTS, REQUEST_GET_ACCOUNTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_GET_ACCOUNTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startManageAccountsActivity();
            } else {
                showPermissionDeniedSnackbar();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void notifyTabSelected(int position) {
        switch (position) {
            case LaanoFragmentPagerAdapter.LINKS_TAB:
                linksPresenter.onTabSelected();
                break;
            case LaanoFragmentPagerAdapter.FAVORITES_TAB:
                favoritesPresenter.onTabSelected();
                break;
            case LaanoFragmentPagerAdapter.NOTES_TAB:
                notesPresenter.onTabSelected();
                break;
            default:
                throw new IllegalStateException("Unexpected tab was selected");
        }
    }

    private void notifyTabDeselected(int position) {
        switch (position) {
            case LaanoFragmentPagerAdapter.LINKS_TAB:
                linksPresenter.onTabDeselected();
                break;
            case LaanoFragmentPagerAdapter.FAVORITES_TAB:
                favoritesPresenter.onTabDeselected();
                break;
            case LaanoFragmentPagerAdapter.NOTES_TAB:
                notesPresenter.onTabDeselected();
                break;
            default:
                throw new IllegalStateException("Unexpected tab was selected");
        }
    }

    // Broadcast Receivers

    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            settings.setOnline(CloudUtils.isApplicationConnected(context));
        }
    }

    private class SyncBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int status = intent.getIntExtra(SyncNotifications.EXTRA_STATUS, -1);
            //String id = intent.getStringExtra(SyncNotifications.EXTRA_ID);
            int count = intent.getIntExtra(SyncNotifications.EXTRA_COUNT, -1);

            if (action.equals(SyncNotifications.ACTION_SYNC)) {
                switch (status) {
                    case SyncNotifications.STATUS_SYNC_START:
                        laanoUiManager.setSyncDrawerMenu();
                        break;
                    case SyncNotifications.STATUS_SYNC_STOP:
                        laanoUiManager.updateSyncStatus();
                        laanoUiManager.setNormalDrawerMenu();
                        break;
                }
                return;
            }
            int tabPosition;
            switch (action) {
                case SyncNotifications.ACTION_SYNC_LINKS:
                    tabPosition = LaanoFragmentPagerAdapter.LINKS_TAB;
                    if (status == SyncNotifications.STATUS_SYNC_STOP) {
                        linksPresenter.loadLinks(false);
                    }
                    break;
                case SyncNotifications.ACTION_SYNC_FAVORITES:
                    tabPosition = LaanoFragmentPagerAdapter.FAVORITES_TAB;
                    if (status == SyncNotifications.STATUS_SYNC_STOP) {
                        favoritesPresenter.loadFavorites(false);
                    }
                    break;
                case SyncNotifications.ACTION_SYNC_NOTES:
                    tabPosition = LaanoFragmentPagerAdapter.NOTES_TAB;
                    if (status == SyncNotifications.STATUS_SYNC_STOP) {
                        notesPresenter.loadNotes(false);
                        linksPresenter.loadLinks(false);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unexpected action has been received in SyncBroadcastReceiver [" + action + "]");
            }
            if (status == SyncNotifications.STATUS_UPLOADED) {
                if (count >= 0) laanoUiManager.setUploaded(tabPosition, count);
                else laanoUiManager.incUploaded(tabPosition);
            } else if (status == SyncNotifications.STATUS_DOWNLOADED) {
                if (count >= 0) laanoUiManager.setDownloaded(tabPosition, count);
                else laanoUiManager.incDownloaded(tabPosition);
            }
            if (status != SyncNotifications.STATUS_SYNC_STOP) {
                laanoUiManager.setTabSyncState(tabPosition);
                laanoUiManager.setSyncDrawerMenu();
                laanoUiManager.updateTitle(tabPosition);
            }
        }
    }

    // Setup

    private void setupAppBarLayout(
            @NonNull AppBarLayout appBarLayout, @NonNull FloatingActionButton fab) {
        checkNotNull(appBarLayout);
        checkNotNull(fab);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayoutOnStateChangeListener() {

            @Override
            public void onStateChanged(AppBarLayout appBarLayout, State state) {
                if (state == State.EXPANDED && !fab.isShown()) {
                    fab.show();
                } else if (state == State.COLLAPSED && fab.isShown()) {
                    fab.hide();
                }
            }
        });
    }

    private void setupViewPager(
            @NonNull ViewPager viewPager, @NonNull LaanoFragmentPagerAdapter pagerAdapter) {
        checkNotNull(viewPager);
        checkNotNull(pagerAdapter);
        viewPager.setAdapter(pagerAdapter);
        // NOTE: Fragments are needed immediately to build Presenters
        pagerAdapter.instantiateItem(viewPager, LaanoFragmentPagerAdapter.LINKS_TAB);
        pagerAdapter.instantiateItem(viewPager, LaanoFragmentPagerAdapter.FAVORITES_TAB);
        pagerAdapter.instantiateItem(viewPager, LaanoFragmentPagerAdapter.NOTES_TAB);
        pagerAdapter.finishUpdate(viewPager);
        // Listener
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (activeTab != position) {
                    notifyTabDeselected(activeTab);
                    notifyTabSelected(position);
                    activeTab = position;
                    laanoUiManager.updateTitle(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void setupDrawerLayout(@NonNull final DrawerLayout drawerLayout) {
        checkNotNull(drawerLayout);
        // NOTE: untestable behavior (swipeRight() doesn't open Drawer)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);
    }

    private void startManageAccountsActivity() {
        Intent manageAccountsIntent =
                new Intent(getApplicationContext(), ManageAccountsActivity.class);
        startActivityForResult(manageAccountsIntent, ACTION_MANAGE_ACCOUNTS);
    }

    private void startSyncLogActivity() {
        Intent syncLogIntent = new Intent(this, SyncLogActivity.class);
        startActivity(syncLogIntent);
    }

    private void startSettingsActivity() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        settingsIntent.putExtra(SettingsActivity.EXTRA_ACCOUNT,
                CloudUtils.getDefaultAccount(this, accountManager));
        startActivity(settingsIntent);
    }

    private void startAboutActivity() {
        Intent settingsIntent = new Intent(this, AboutActivity.class);
        startActivity(settingsIntent);
    }

    private void startAddEditLinkActivity(@Nullable String sharedText) {
        Intent addEditLinkIntent = new Intent(this, AddEditLinkActivity.class);
        addEditLinkIntent.putExtra(AddEditLinkActivity.EXTRA_SHARED_TEXT, sharedText);
        startActivity(addEditLinkIntent);
    }

    private void setupDrawerContent(@NonNull NavigationView navigationView) {
        checkNotNull(navigationView);
        DrawerLayout drawerLayout = binding.drawerLayout;
        navigationView.setNavigationItemSelectedListener(
                (menuItem) -> {
                    switch (menuItem.getItemId()) {
                        case R.id.add_account_menu_item:
                            accountManager.addAccount(getAccountType(this),
                                    null, null, null, this, addAccountCallback, new Handler());
                            break;
                        case R.id.manage_accounts_menu_item:
                            checkGetAccountsPermissionAndLaunchActivity();
                            break;
                        case R.id.sync_menu_item:
                            if (!settings.isOnline()) {
                                laanoUiManager.showApplicationOfflineSnackbar();
                            } else {
                                triggerSync();
                            }
                            break;
                        case R.id.sync_log_menu_item:
                            startSyncLogActivity();
                            break;
                        case R.id.settings_menu_item:
                            startSettingsActivity();
                            break;
                        case R.id.about_menu_item:
                            startAboutActivity();
                            break;
                    }
                    drawerLayout.closeDrawers();
                    return true;
                }
        );
    }

    private void setupTabLayout(@NonNull TabLayout tabLayout, @NonNull ViewPager viewPager) {
        checkNotNull(tabLayout);
        checkNotNull(viewPager);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                setCurrentTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupFabAdd(@NonNull FloatingActionButton fab) {
        checkNotNull(fab);
        fab.setOnClickListener(v -> {
                    switch (activeTab) {
                        case LaanoFragmentPagerAdapter.LINKS_TAB:
                            linksPresenter.showAddLink();
                            break;
                        case LaanoFragmentPagerAdapter.FAVORITES_TAB:
                            favoritesPresenter.showAddFavorite();
                            break;
                        case LaanoFragmentPagerAdapter.NOTES_TAB:
                            notesPresenter.showAddNote();
                            break;
                        default:
                            throw new IllegalStateException("Unexpected tab was selected");
                    }
                }
        );
    }

    // Callbacks

    private AccountManagerCallback<Bundle> addAccountCallback = future -> {
        try {
            future.getResult(); // NOTE: see exceptions
            //updateDefaultAccount(); // NOTE: see accountsUpdateListener
            showAccountSuccessfullyAddedSnackbar();
        } catch (OperationCanceledException e) {
            Log.d(TAG, "Account creation was canceled by user");
        } catch (IOException | AuthenticatorException e) {
            Log.e(TAG, "Account creation was finished with an exception");
            updateDefaultAccount();
            Throwable throwable = e.getCause();
            if (throwable != null) {
                Snackbar.make(binding.laanoViewPager, throwable.getMessage(), Snackbar.LENGTH_LONG)
                        .show();
            }
        }
    };

    private OnAccountsUpdateListener accountsUpdateListener = accounts -> updateDefaultAccount();

    // SnackBars

    private void showPermissionDeniedSnackbar() {
        Snackbar.make(binding.laanoViewPager, R.string.snackbar_no_permission,
                Snackbar.LENGTH_LONG).show();
    }

    private void showAccountSuccessfullyAddedSnackbar() {
        Snackbar.make(binding.laanoViewPager, R.string.laano_account_added,
                Snackbar.LENGTH_LONG).show();
    }

    // Testing

    @VisibleForTesting
    public BaseItemFragment getCurrentFragment() {
        ViewPager viewPager = binding.laanoViewPager;
        int position = viewPager.getCurrentItem();
        LaanoFragmentPagerAdapter adapter = (LaanoFragmentPagerAdapter) viewPager.getAdapter();

        return adapter.getFragment(position);
    }
}
