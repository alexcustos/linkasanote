package com.bytesforge.linkasanote.manageaccounts;

import android.accounts.Account;
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
import com.bytesforge.linkasanote.databinding.ActivityManageAccountsBinding;
import com.bytesforge.linkasanote.utils.ActivityUtils;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;

import java.util.Arrays;

import javax.inject.Inject;

public class ManageAccountsActivity extends AppCompatActivity {

    public static final String KEY_ACCOUNT_LIST_CHANGED = "ACCOUNT_LIST_CHANGED";
    public static final String STATE_ACCOUNT_NAMES = "ACCOUNT_NAMES";

    private String[] accountNames;
    private ManageAccountsContract.View view;

    @Inject
    ManageAccountsPresenter presenter;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityManageAccountsBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_manage_accounts);
        // Toolbar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.action_bar_title_manage_accounts);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        // Fragment
        ManageAccountsFragment fragment = (ManageAccountsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        if (fragment == null) {
            fragment = ManageAccountsFragment.newInstance();
            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), fragment, R.id.content_frame);
        }
        view = fragment;
        // Presenter
        LaanoApplication application = (LaanoApplication) getApplication();
        application.getApplicationComponent()
                .getManageAccountsComponent(new ManageAccountsPresenterModule(this, fragment))
                .inject(this);
        // Accounts
        if (savedInstanceState != null) {
            accountNames = savedInstanceState.getStringArray(STATE_ACCOUNT_NAMES);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray(STATE_ACCOUNT_NAMES, accountNames);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // NOTE: when fragment (view) is completely initialized
        if (accountNames == null) {
            accountNames = accountsToStringArray(view.getAccountsWithPermissionCheck());
        }
        if (accountNames == null) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(KEY_ACCOUNT_LIST_CHANGED, hasAccountListChanged());
        setResult(RESULT_OK, resultIntent);

        super.onBackPressed();
    }

    private boolean hasAccountListChanged() {
        Account[] accounts = view.getAccountsWithPermissionCheck();
        if (accounts == null) return true; // NOTE: non-normal, indeed something was changed

        String[] actualAccountNames = accountsToStringArray(accounts);
        return !Arrays.equals(accountNames, actualAccountNames);
    }

    @Nullable
    private String[] accountsToStringArray(@Nullable Account[] accounts) {
        if (accounts == null) return null;

        String[] accountNames = new String[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
            accountNames[i] = accounts[i].name;
        }
        return accountNames;
    }

    @VisibleForTesting
    public IdlingResource getCountingIdlingResource() {
        return EspressoIdlingResource.getIdlingResource();
    }
}
