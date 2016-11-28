package com.bytesforge.linkasanote.laano;

import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.bytesforge.linkasanote.BaseFragment;
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

import javax.inject.Inject;

public class LaanoActivity extends AppCompatActivity {

    @Inject
    LinksPresenter linksPresenter;

    @Inject
    FavoritesPresenter favoritesPresenter;

    @Inject
    NotesPresenter notesPresenter;

    private DrawerLayout drawerLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLaanoBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_laano);

        // Toolbar
        setSupportActionBar(binding.toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeAsUpIndicator(R.drawable.ic_menu);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // Navigation Drawer
        drawerLayout = binding.drawerLayout;

        if (binding.navView != null) {
            setupDrawerContent(binding.navView);
        }

        // Fragments
        LinksFragment linksFragment = LinksFragment.newInstance();
        FavoritesFragment favoritesFragment = FavoritesFragment.newInstance();
        NotesFragment notesFragment = NotesFragment.newInstance();

        // Tabs
        viewPager = binding.viewPager;

        if (viewPager != null) {
            LaanoFragmentPagerAdapter adapter = new LaanoFragmentPagerAdapter(
                    getSupportFragmentManager(), LaanoActivity.this);

            Resources res = getResources();

            adapter.addTab(linksFragment, res.getString(R.string.laano_tab_links_title));
            adapter.addTab(favoritesFragment, res.getString(R.string.laano_tab_favorites_title));
            adapter.addTab(notesFragment, res.getString(R.string.laano_tab_notes_title));

            viewPager.setAdapter(adapter);

            if (binding.tabLayout != null) {
                setupTabsContent(binding.tabLayout);
            }
        }

        // Presenter
        DaggerLaanoComponent.builder()
                .linksPresenterModule(new LinksPresenterModule(linksFragment))
                .favoritesPresenterModule(new FavoritesPresenterModule(favoritesFragment))
                .notesPresenterModule(new NotesPresenterModule(notesFragment))
                .build().inject(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                (menuItem) -> {
                    switch (menuItem.getItemId()) {
                        case R.id.preferences_menu_item:
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

    public BaseFragment getCurrentFragment() {
        int position = viewPager.getCurrentItem();
        LaanoFragmentPagerAdapter adapter = (LaanoFragmentPagerAdapter) viewPager.getAdapter();

        return adapter.getFragment(position);
    }
}
