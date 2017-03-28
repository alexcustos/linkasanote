package com.bytesforge.linkasanote.laano.favorites.conflictresolution;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.ActivityFavoritesConflictResolutionBinding;
import com.bytesforge.linkasanote.utils.ActivityUtils;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;

import javax.inject.Inject;

public class FavoritesConflictResolutionActivity extends AppCompatActivity {

    public static final int RESULT_FAILED = RESULT_FIRST_USER;

    @Inject
    FavoritesConflictResolutionPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityFavoritesConflictResolutionBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_favorites_conflict_resolution);

        String favoriteId = getIntent().getStringExtra(
                FavoritesConflictResolutionFragment.ARGUMENT_FAVORITE_ID);
        // Toolbar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle("Favorite: conflict resolution");
        }
        // Fragment
        FavoritesConflictResolutionFragment fragment =
                (FavoritesConflictResolutionFragment ) getSupportFragmentManager()
                        .findFragmentById(R.id.content_frame);
        if (fragment == null) {
            fragment = FavoritesConflictResolutionFragment.newInstance();

            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), fragment, R.id.content_frame);
        }
        // Presenter
        LaanoApplication application = (LaanoApplication) getApplication();
        application.getApplicationComponent()
                .getFavoritesConflictResolutionComponent(
                        new FavoritesConflictResolutionPresenterModule(this, fragment, favoriteId))
                .inject(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @VisibleForTesting
    public IdlingResource getCountingIdlingResource() {
        return EspressoIdlingResource.getIdlingResource();
    }
}
