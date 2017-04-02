package com.bytesforge.linkasanote.settings;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.utils.ActivityUtils;


public class SettingsActivity extends AppCompatActivity {

    public final static String EXTRA_ACCOUNT = "ACCOUNT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent startIntent = getIntent();
        Account account = startIntent.getParcelableExtra(EXTRA_ACCOUNT);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.actionbar_title_settings);
        }

        SettingsFragment fragment = SettingsFragment.newInstance(account);
        ActivityUtils.replaceFragmentInActivity(
                getSupportFragmentManager(), fragment, android.R.id.content);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
