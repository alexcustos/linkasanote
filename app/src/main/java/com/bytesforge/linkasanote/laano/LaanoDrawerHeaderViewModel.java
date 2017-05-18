package com.bytesforge.linkasanote.laano;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.manageaccounts.AccountItem;
import com.bytesforge.linkasanote.sync.SyncAdapter;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

public class LaanoDrawerHeaderViewModel extends BaseObservable {

    public static final String STATE_STATUS_ICON_TINT = "STATUS_ICON_TINT";
    public static final String STATE_LAST_SYNCED_TEXT = "LAST_SYNCED_TEXT";
    public static final String STATE_STATUS_TEXT = "STATUS_TEXT";
    public static final String STATE_USERNAME_TEXT = "USERNAME_TEXT";
    public static final String STATE_ACCOUNT_NAME_TEXT = "ACCOUNT_NAME_TEXT";
    public static final String STATE_APP_NAME = "APP_NAME";
    public static final String STATE_USERNAME = "USERNAME";
    public static final String STATE_ACCOUNT_NAME = "ACCOUNT_NAME";


    public final ObservableField<String> lastSyncedText = new ObservableField<>();
    public final ObservableField<String> statusText = new ObservableField<>();
    public final ObservableField<String> usernameText = new ObservableField<>();
    public final ObservableField<String> accountNameText = new ObservableField<>();
    public final ObservableBoolean appName = new ObservableBoolean();
    public final ObservableBoolean username = new ObservableBoolean();
    public final ObservableBoolean accountName = new ObservableBoolean();

    private final Context context;

    @Bindable
    public int statusIconTint;

    public LaanoDrawerHeaderViewModel(Context context) {
        this.context = checkNotNull(context);
    }

    public void setInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            applyInstanceState(getDefaultInstanceState());
        } else {
            applyInstanceState(savedInstanceState);
        }
    }

    public void saveInstanceState(@NonNull Bundle outState) {
        checkNotNull(outState);
        outState.putInt(STATE_STATUS_ICON_TINT, statusIconTint);
        outState.putString(STATE_LAST_SYNCED_TEXT, lastSyncedText.get());
        outState.putString(STATE_STATUS_TEXT, statusText.get());
        outState.putString(STATE_USERNAME_TEXT, usernameText.get());
        outState.putString(STATE_ACCOUNT_NAME_TEXT, accountNameText.get());
        outState.putBoolean(STATE_APP_NAME, appName.get());
        outState.putBoolean(STATE_USERNAME, username.get());
        outState.putBoolean(STATE_ACCOUNT_NAME, accountName.get());
    }

    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        statusIconTint = state.getInt(STATE_STATUS_ICON_TINT);
        lastSyncedText.set(state.getString(STATE_LAST_SYNCED_TEXT));
        statusText.set(state.getString(STATE_STATUS_TEXT));
        usernameText.set(state.getString(STATE_USERNAME_TEXT));
        accountNameText.set(state.getString(STATE_ACCOUNT_NAME_TEXT));
        appName.set(state.getBoolean(STATE_APP_NAME));
        username.set(state.getBoolean(STATE_USERNAME));
        accountName.set(state.getBoolean(STATE_ACCOUNT_NAME));

        notifyChange();
    }

    public Bundle getDefaultInstanceState() {
        Bundle defaultState = new Bundle();

        defaultState.putInt(STATE_STATUS_ICON_TINT,
                ContextCompat.getColor(context, R.color.sync_state_neutral));
        defaultState.putString(STATE_LAST_SYNCED_TEXT, null);
        defaultState.putString(STATE_STATUS_TEXT, null);
        defaultState.putString(STATE_USERNAME_TEXT, null);
        defaultState.putString(STATE_ACCOUNT_NAME_TEXT, null);
        defaultState.putBoolean(STATE_APP_NAME, true);
        defaultState.putBoolean(STATE_USERNAME, false);
        defaultState.putBoolean(STATE_ACCOUNT_NAME, false);

        return defaultState;
    }

    private String getString(@StringRes int string) {
        return context.getResources().getString(string);
    }

    public void showAppName() {
        usernameText.set(null);
        accountNameText.set(null);
        appName.set(true);
        username.set(false);
        accountName.set(false);
        statusText.set(getString(R.string.drawer_header_status_no_account));
    }

    public void showAccount(@NonNull AccountItem accountItem) {
        checkNotNull(accountItem);
        usernameText.set(accountItem.getDisplayName());
        accountNameText.set(accountItem.getAccountName());
        appName.set(false);
        username.set(true);
        accountName.set(true);
        statusText.set(getString(R.string.drawer_header_status_ready));
    }

    public void showSyncStatus(long lastSyncTime, int syncStatus) {
        if (lastSyncTime == 0) {
            lastSyncedText.set(getString(R.string.drawer_header_last_synced_never));
            statusIconTint = ContextCompat.getColor(context, R.color.sync_state_neutral);
            notifyPropertyChanged(BR.statusIconTint);
            return;
        }
        Date date = new Date(lastSyncTime);
        String datePart = DateFormat.getMediumDateFormat(context).format(date);
        String timePart;
        if (DateFormat.is24HourFormat(context)) {
            timePart = DateFormat.format("HH:mm", date).toString();
        } else {
            timePart = DateFormat.getTimeFormat(context).format(date);
        }
        lastSyncedText.set(datePart + " " + timePart);
        switch (syncStatus) {
            case SyncAdapter.SYNC_STATUS_SYNCED:
                statusText.set(getString(R.string.drawer_header_status_synced));
                statusIconTint = ContextCompat.getColor(context, R.color.sync_state_success);
                break;
            case SyncAdapter.SYNC_STATUS_UNSYNCED:
                statusText.set(getString(R.string.drawer_header_status_unsynced));
                statusIconTint = ContextCompat.getColor(context, R.color.sync_state_neutral);
                break;
            case SyncAdapter.SYNC_STATUS_ERROR:
                statusText.set(getString(R.string.drawer_header_status_error));
                statusIconTint = ContextCompat.getColor(context, R.color.sync_state_error);
                break;
            case SyncAdapter.SYNC_STATUS_CONFLICT:
                statusText.set(getString(R.string.drawer_header_status_conflict));
                statusIconTint = ContextCompat.getColor(context, R.color.sync_state_conflict);
                break;
            default:
                statusText.set(getString(R.string.drawer_header_status_unknown));
                statusIconTint = ContextCompat.getColor(context, R.color.sync_state_neutral);
        }
        notifyPropertyChanged(BR.statusIconTint);
    }
}
