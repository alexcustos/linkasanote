package com.bytesforge.linkasanote.addeditfavorite;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.ActivityAddEditFavoriteBinding;
import com.bytesforge.linkasanote.utils.ActivityUtils;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;

import javax.inject.Inject;

public class AddEditFavoriteActivity extends AppCompatActivity {

    @Inject
    AddEditFavoritePresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAddEditFavoriteBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_add_edit_favorite);

        String favoriteId = getIntent().getStringExtra(
                AddEditFavoriteFragment.ARGUMENT_EDIT_FAVORITE_ID);
        // Toolbar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            if (favoriteId == null) {
                actionBar.setTitle(R.string.actionbar_title_new_favorite);
            } else {
                actionBar.setTitle(R.string.actionbar_title_edit_favorite);
            }
        }
        // Fragment
        AddEditFavoriteFragment fragment = (AddEditFavoriteFragment) getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        if (fragment == null) {
            fragment = AddEditFavoriteFragment.newInstance();

            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), fragment, R.id.content_frame);
        }
        // Presenter
        LaanoApplication application = (LaanoApplication) getApplication();
        application.getApplicationComponent()
                .getAddEditFavoriteComponent(
                        new AddEditFavoritePresenterModule(this, fragment, favoriteId))
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
