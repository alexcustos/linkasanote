package com.bytesforge.linkasanote.links;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.ActivityLinksBinding;
import com.bytesforge.linkasanote.utils.ActivityUtils;

import javax.inject.Inject;

public class LinksActivity extends AppCompatActivity {

    @Inject LinksPresenter linksPresenter;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLinksBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_links);

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

        // Fragment
        LinksFragment linksFragment =
                (LinksFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (linksFragment == null) {
            linksFragment = LinksFragment.newInstance();
            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), linksFragment, R.id.content_frame);
        }

        // Presenter
        DaggerLinksComponent.builder()
                .linksPresenterModule(new LinksPresenterModule(linksFragment))
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
}
