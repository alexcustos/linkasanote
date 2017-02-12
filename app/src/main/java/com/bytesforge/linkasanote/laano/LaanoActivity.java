package com.bytesforge.linkasanote.laano;

import android.Manifest;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.test.espresso.IdlingResource;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.bytesforge.linkasanote.BaseFragment;
import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.ActivityLaanoBinding;
import com.bytesforge.linkasanote.laano.favorites.FavoritesFragment;
import com.bytesforge.linkasanote.laano.favorites.FavoritesPresenter;
import com.bytesforge.linkasanote.laano.favorites.FavoritesPresenterModule;
import com.bytesforge.linkasanote.laano.links.LinksFragment;
import com.bytesforge.linkasanote.laano.links.LinksPresenter;
import com.bytesforge.linkasanote.laano.links.LinksPresenterModule;
import com.bytesforge.linkasanote.laano.notes.NotesFragment;
import com.bytesforge.linkasanote.laano.notes.NotesPresenter;
import com.bytesforge.linkasanote.laano.notes.NotesPresenterModule;
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsActivity;
import com.bytesforge.linkasanote.settings.SettingsActivity;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;

import javax.inject.Inject;

import static com.bytesforge.linkasanote.utils.CloudUtils.getAccountType;

public class LaanoActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int REQUEST_GET_ACCOUNTS = 0;
    private static final String PERMISSION_GET_ACCOUNTS = Manifest.permission.GET_ACCOUNTS;
    private static String[] PERMISSIONS_GET_ACCOUNTS = {PERMISSION_GET_ACCOUNTS};

    private static final int ACTION_MANAGE_ACCOUNTS = 1;

    @Inject
    LinksPresenter linksPresenter;

    @Inject
    FavoritesPresenter favoritesPresenter;

    @Inject
    NotesPresenter notesPresenter;

    /*@Inject
    SharedPreferences sharedPreferences;*/

    private ActivityLaanoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_laano);

        // Toolbar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // Navigation Drawer
        DrawerLayout drawerLayout = binding.drawerLayout;
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

        if (binding.navView != null) {
            setupDrawerContent(binding.navView);
        }
        // Fragments
        LinksFragment linksFragment = LinksFragment.newInstance();
        FavoritesFragment favoritesFragment = FavoritesFragment.newInstance();
        NotesFragment notesFragment = NotesFragment.newInstance();
        // Tabs
        ViewPager viewPager = binding.laanoViewPager;

        if (viewPager != null) {
            LaanoFragmentPagerAdapter adapter =
                    new LaanoFragmentPagerAdapter(getSupportFragmentManager());

            Resources res = getResources();

            adapter.addTab(linksFragment, res.getString(R.string.laano_tab_links_title));
            adapter.addTab(favoritesFragment, res.getString(R.string.laano_tab_favorites_title));
            adapter.addTab(notesFragment, res.getString(R.string.laano_tab_notes_title));

            viewPager.setAdapter(adapter);

            if (binding.tabLayout != null) {
                setupTabsContent(binding.tabLayout);
            }
        }
        // Presenters
        DaggerLaanoComponent.builder()
                .applicationComponent(((LaanoApplication) getApplication()).getApplicationComponent())
                .linksPresenterModule(new LinksPresenterModule(linksFragment))
                .favoritesPresenterModule(new FavoritesPresenterModule(favoritesFragment))
                .notesPresenterModule(new NotesPresenterModule(notesFragment))
                .build().inject(this);
        // FAB
        if (binding.fabAdd != null) {
            setupFabAdd(binding.fabAdd);
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (ACTION_MANAGE_ACCOUNTS == requestCode && RESULT_OK == resultCode
                && data.getBooleanExtra(ManageAccountsActivity.KEY_ACCOUNT_LIST_CHANGED, false)) {
            updateAccountList();
        }
    }

    private void updateAccountList() {
        // TODO: update status in Navigation Drawer
    }

    // Get Accounts Permission

    public void checkGetAccountsPermissionAndLaunchActivity() {
        if (ActivityCompat.checkSelfPermission(this, PERMISSION_GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            requestGetAccountsPermission();
        } else {
            startManageAccountsActivity();
        }
    }

    private void requestGetAccountsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_GET_ACCOUNTS)) {
            Snackbar.make(binding.laanoViewPager,
                    R.string.laano_permission_get_accounts,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_button_ok, view ->
                            requestPermissions(PERMISSIONS_GET_ACCOUNTS, REQUEST_GET_ACCOUNTS))
                    .show();
        } else {
            requestPermissions(PERMISSIONS_GET_ACCOUNTS, REQUEST_GET_ACCOUNTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_GET_ACCOUNTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startManageAccountsActivity();
            } else {
                showPermissionDeniedSnackbar();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showPermissionDeniedSnackbar() {
        Snackbar.make(binding.laanoViewPager, R.string.snackbar_no_permission, Snackbar.LENGTH_LONG)
                .show();
    }

    // Setup

    private void startManageAccountsActivity() {
        Intent manageAccountsIntent =
                new Intent(getApplicationContext(), ManageAccountsActivity.class);
        startActivityForResult(manageAccountsIntent, ACTION_MANAGE_ACCOUNTS);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        DrawerLayout drawerLayout = binding.drawerLayout;
        navigationView.setNavigationItemSelectedListener(
                (menuItem) -> {
                    switch (menuItem.getItemId()) {
                        case R.id.settings_menu_item:
                            Intent settingsIntent =
                                    new Intent(getApplicationContext(), SettingsActivity.class);
                            startActivity(settingsIntent);
                            break;
                        case R.id.add_account_menu_item:
                            AccountManager accountManager =
                                    AccountManager.get(getApplicationContext());
                            accountManager.addAccount(getAccountType(),
                                    null, null, null, this, null, new Handler());
                            break;
                        case R.id.manage_accounts_menu_item:
                            checkGetAccountsPermissionAndLaunchActivity();
                            break;
                        default:
                            break;
                    }
                    menuItem.setChecked(true);
                    drawerLayout.closeDrawers();

                    return true;
                }
        );
    }

    private void setupTabsContent(TabLayout tabLayout) {
        ViewPager viewPager = binding.laanoViewPager;
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    // TODO: refactor to get rid of instanceof checking
    private void setupFabAdd(FloatingActionButton fab) {
        fab.setOnClickListener(v -> {
                    BaseFragment fragment = getCurrentFragment();
                    if (fragment instanceof LinksFragment) {
                        linksPresenter.addLink();
                    } else if (fragment instanceof FavoritesFragment) {
                        favoritesPresenter.addFavorite();
                    } else if (fragment instanceof NotesFragment) {
                        notesPresenter.addNote();
                    }
                }
        );
    }

    // Testing

    @VisibleForTesting
    public BaseFragment getCurrentFragment() {
        ViewPager viewPager = binding.laanoViewPager;
        int position = viewPager.getCurrentItem();
        LaanoFragmentPagerAdapter adapter = (LaanoFragmentPagerAdapter) viewPager.getAdapter();

        return adapter.getFragment(position);
    }

    @VisibleForTesting
    public IdlingResource getCountingIdlingResource() {
        return EspressoIdlingResource.getIdlingResource();
    }
}
