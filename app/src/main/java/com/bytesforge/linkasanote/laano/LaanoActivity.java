package com.bytesforge.linkasanote.laano;

import android.Manifest;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.bytesforge.linkasanote.BaseFragment;
import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.ActivityLaanoBinding;
import com.bytesforge.linkasanote.laano.favorites.FavoritesPresenter;
import com.bytesforge.linkasanote.laano.favorites.FavoritesPresenterModule;
import com.bytesforge.linkasanote.laano.links.LinksPresenter;
import com.bytesforge.linkasanote.laano.links.LinksPresenterModule;
import com.bytesforge.linkasanote.laano.notes.NotesPresenter;
import com.bytesforge.linkasanote.laano.notes.NotesPresenterModule;
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsActivity;
import com.bytesforge.linkasanote.settings.SettingsActivity;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;

import java.io.IOException;

import javax.inject.Inject;

import static com.bytesforge.linkasanote.utils.CloudUtils.getAccountType;
import static com.google.common.base.Preconditions.checkNotNull;

public class LaanoActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = LaanoActivity.class.getSimpleName();

    private static final String STATE_CURRENT_TAB = "CURRENT_TAB";

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

    private int viewPagerCurrentTab;
    private ActivityLaanoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            applyInstanceState(getDefaultInstanceState());
        } else {
            applyInstanceState(savedInstanceState);
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_laano);

        // Toolbar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // Navigation Drawer
        if (binding.drawerLayout != null) {
            setupDrawerLayout(binding.drawerLayout);
        }
        if (binding.navView != null) {
            setupDrawerContent(binding.navView);
        }
        // ViewPager
        LaanoFragmentPagerAdapter adapter = new LaanoFragmentPagerAdapter(
                getSupportFragmentManager(), getApplicationContext());
        if (binding.laanoViewPager != null) {
            ViewPager viewPager = binding.laanoViewPager;
            setupViewPager(viewPager, adapter);
            // TabLayout
            if (binding.tabLayout != null) {
                setupTabLayout(binding.tabLayout, viewPager);
            }
        }
        // Presenters
        LaanoApplication application = (LaanoApplication) getApplication();
        application.getApplicationComponent()
                .getLaanoComponent(
                        new LinksPresenterModule(adapter.getLinksFragment()),
                        new FavoritesPresenterModule(this, adapter.getFavoritesFragment()),
                        new NotesPresenterModule(adapter.getNotesFragment()))
                .inject(this);
        // FAB
        if (binding.fabAdd != null) {
            setupFabAdd(binding.fabAdd);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_TAB, viewPagerCurrentTab);
    }

    private void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        viewPagerCurrentTab = state.getInt(STATE_CURRENT_TAB);
    }

    private Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();
        defaultState.putInt(STATE_CURRENT_TAB, 0);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (ACTION_MANAGE_ACCOUNTS == requestCode && RESULT_OK == resultCode
                && data.getBooleanExtra(ManageAccountsActivity.KEY_ACCOUNT_LIST_CHANGED, false)) {
            updateAccountList();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_laano, menu);
        return true;
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

    private void notifyTabSelected(int position) {
        switch (position) {
            case LaanoFragmentPagerAdapter.LINKS_TAB:
                break;
            case LaanoFragmentPagerAdapter.FAVORITES_TAB:
                favoritesPresenter.onTabSelected();
                break;
            case LaanoFragmentPagerAdapter.NOTES_TAB:
                break;
            default:
                throw new IllegalStateException("Unexpected tab was selected");
        }
    }

    private void notifyTabDeselected(int position) {
        switch (position) {
            case LaanoFragmentPagerAdapter.LINKS_TAB:
                break;
            case LaanoFragmentPagerAdapter.FAVORITES_TAB:
                favoritesPresenter.onTabDeselected();
                break;
            case LaanoFragmentPagerAdapter.NOTES_TAB:
                break;
            default:
                throw new IllegalStateException("Unexpected tab was selected");
        }
    }

    // Setup

    private void setupViewPager(ViewPager viewPager, LaanoFragmentPagerAdapter adapter) {
        viewPager.setAdapter(adapter);
        // NOTE: Fragments are needed immediately to build Presenters
        adapter.instantiateItem(viewPager, LaanoFragmentPagerAdapter.LINKS_TAB);
        adapter.instantiateItem(viewPager, LaanoFragmentPagerAdapter.FAVORITES_TAB);
        adapter.instantiateItem(viewPager, LaanoFragmentPagerAdapter.NOTES_TAB);
        adapter.finishUpdate(viewPager);
        viewPager.setCurrentItem(viewPagerCurrentTab);
        // Listener
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (viewPagerCurrentTab != position) {
                    notifyTabDeselected(viewPagerCurrentTab);
                    notifyTabSelected(position);
                    viewPagerCurrentTab = position;
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

    private void setupDrawerContent(@NonNull NavigationView navigationView) {
        checkNotNull(navigationView);

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
                            Context context = getApplicationContext();
                            AccountManager accountManager = AccountManager.get(context);
                            accountManager.addAccount(getAccountType(context),
                                    null, null, null, this, addAccountCallback, new Handler());
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

    private void setupTabLayout(@NonNull TabLayout tabLayout, @NonNull ViewPager viewPager) {
        checkNotNull(tabLayout);
        checkNotNull(viewPager);

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

    private void setupFabAdd(@NonNull FloatingActionButton fab) {
        checkNotNull(fab);

        fab.setOnClickListener(v -> {
                    switch (viewPagerCurrentTab) {
                        case LaanoFragmentPagerAdapter.LINKS_TAB:
                            linksPresenter.addLink();
                            break;
                        case LaanoFragmentPagerAdapter.FAVORITES_TAB:
                            favoritesPresenter.addFavorite();
                            break;
                        case LaanoFragmentPagerAdapter.NOTES_TAB:
                            notesPresenter.addNote();
                            break;
                        default:
                            throw new IllegalStateException("Unexpected tab was selected");
                    }
                }
        );
    }

    // Callbacks

    private AccountManagerCallback<Bundle> addAccountCallback = future -> {
        if (future == null) return;
        try {
            future.getResult(); // NOTE: see exceptions
            showAccountSuccessfullyAddedSnackbar();
        } catch (OperationCanceledException e) {
            Log.d(TAG, "Account creation canceled");
        } catch (IOException | AuthenticatorException e) {
            Log.e(TAG, "Account creation finished with an exception", e);
        }
    };

    // SnackBars

    private void showPermissionDeniedSnackbar() {
        Snackbar.make(binding.laanoViewPager, R.string.snackbar_no_permission, Snackbar.LENGTH_LONG)
                .show();
    }

    private void showAccountSuccessfullyAddedSnackbar() {
        Snackbar.make(binding.laanoViewPager, R.string.laano_account_added, Snackbar.LENGTH_LONG)
                .show();
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
