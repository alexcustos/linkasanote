package com.bytesforge.linkasanote.synclog;

import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.ActivitySyncLogBinding;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.ActivityUtils;

import javax.inject.Inject;

public class SyncLogActivity extends AppCompatActivity {

    @Inject
    SyncLogPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySyncLogBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_sync_log);
        // Toolbar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            Resources resources = getResources();
            actionBar.setTitle(resources.getQuantityString(
                    R.plurals.actionbar_title_sync_log,
                    Settings.GLOBAL_SYNC_LOG_KEEPING_PERIOD_DAYS,
                    Settings.GLOBAL_SYNC_LOG_KEEPING_PERIOD_DAYS));
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        // Fragment
        SyncLogFragment fragment = (SyncLogFragment) getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        if (fragment == null) {
            fragment = SyncLogFragment.newInstance();
            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), fragment, R.id.content_frame);
        }
        // Presenter
        LaanoApplication application = (LaanoApplication) getApplication();
        application.getApplicationComponent()
                .getSyncLogComponent(new SyncLogPresenterModule(this, fragment))
                .inject(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
