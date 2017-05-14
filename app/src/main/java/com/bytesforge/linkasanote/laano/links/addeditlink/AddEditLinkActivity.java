package com.bytesforge.linkasanote.laano.links.addeditlink;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.ActivityAddEditLinkBinding;
import com.bytesforge.linkasanote.utils.ActivityUtils;

import javax.inject.Inject;

public class AddEditLinkActivity extends AppCompatActivity {

    @Inject
    AddEditLinkPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAddEditLinkBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_add_edit_link);

        String linkId = getIntent().getStringExtra(AddEditLinkFragment.ARGUMENT_LINK_ID);
        // Toolbar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            if (linkId == null) {
                actionBar.setTitle(R.string.actionbar_title_new_link);
            } else {
                actionBar.setTitle(R.string.actionbar_title_edit_link);
            }
        }
        // Fragment
        AddEditLinkFragment fragment = (AddEditLinkFragment) getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        if (fragment == null) {
            fragment = AddEditLinkFragment.newInstance();

            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), fragment, R.id.content_frame);
        }
        // Presenter
        LaanoApplication application = (LaanoApplication) getApplication();
        application.getApplicationComponent()
                .getAddEditLinkComponent(
                        new AddEditLinkPresenterModule(this, fragment, linkId))
                .inject(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
