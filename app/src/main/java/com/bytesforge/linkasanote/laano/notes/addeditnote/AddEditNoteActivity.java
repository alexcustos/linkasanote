package com.bytesforge.linkasanote.laano.notes.addeditnote;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.ActivityAddEditNoteBinding;
import com.bytesforge.linkasanote.utils.ActivityUtils;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;

import javax.inject.Inject;

public class AddEditNoteActivity extends AppCompatActivity {

    @Inject
    AddEditNotePresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAddEditNoteBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_add_edit_note);

        Intent startIntent = getIntent();
        String noteId = startIntent.getStringExtra(AddEditNoteFragment.ARGUMENT_NOTE_ID);
        String linkId = startIntent.getStringExtra(AddEditNoteFragment.ARGUMENT_RELATED_LINK_ID);
        if (noteId != null && linkId != null) {
            throw new UnsupportedOperationException("Link connection can only be set for the new Note");
        }
        // Toolbar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        // Fragment
        AddEditNoteFragment fragment = (AddEditNoteFragment) getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        if (fragment == null) {
            fragment = AddEditNoteFragment.newInstance();

            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), fragment, R.id.content_frame);
        }
        // Presenter
        LaanoApplication application = (LaanoApplication) getApplication();
        application.getApplicationComponent()
                .getAddEditNoteComponent(
                        new AddEditNotePresenterModule(this, fragment, noteId, linkId))
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
