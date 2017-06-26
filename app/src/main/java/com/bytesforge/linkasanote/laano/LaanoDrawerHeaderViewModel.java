/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bytesforge.linkasanote.laano;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.bytesforge.linkasanote.BR;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.manageaccounts.AccountItem;
import com.bytesforge.linkasanote.sync.SyncAdapter;
import com.bytesforge.linkasanote.utils.CommonUtils;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

public class LaanoDrawerHeaderViewModel extends BaseObservable {

    private static final String STATE_STATUS_ICON_TINT = "STATUS_ICON_TINT";
    private static final String STATE_LAST_SYNCED_TEXT = "LAST_SYNCED_TEXT";
    private static final String STATE_STATUS_TEXT = "STATUS_TEXT";
    private static final String STATE_USERNAME_TEXT = "USERNAME_TEXT";
    private static final String STATE_ACCOUNT_NAME_TEXT = "ACCOUNT_NAME_TEXT";
    private static final String STATE_APP_NAME = "APP_NAME";
    private static final String STATE_USERNAME = "USERNAME";
    private static final String STATE_ACCOUNT_NAME = "ACCOUNT_NAME";


    public final ObservableField<String> lastSyncedText = new ObservableField<>();
    public final ObservableField<String> statusText = new ObservableField<>();
    public final ObservableField<String> usernameText = new ObservableField<>();
    public final ObservableField<String> accountNameText = new ObservableField<>();
    public final ObservableBoolean appName = new ObservableBoolean();
    public final ObservableBoolean username = new ObservableBoolean();
    public final ObservableBoolean accountName = new ObservableBoolean();

    private final Context context;
    private final Resources resources;

    @Bindable
    public int statusIconTint;

    public LaanoDrawerHeaderViewModel(Context context) {
        this.context = checkNotNull(context);
        resources = context.getResources();
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

    public void showAppName() {
        usernameText.set(null);
        accountNameText.set(null);
        appName.set(true);
        username.set(false);
        accountName.set(false);
        statusText.set(resources.getString(R.string.drawer_header_status_no_account));
    }

    public void showAccount(@NonNull AccountItem accountItem) {
        checkNotNull(accountItem);
        usernameText.set(accountItem.getDisplayName());
        accountNameText.set(accountItem.getAccountName());
        appName.set(false);
        username.set(true);
        accountName.set(true);
        statusText.set(resources.getString(R.string.drawer_header_status_ready));
    }

    public void showSyncStatus(long lastSyncTime, int syncStatus) {
        if (lastSyncTime == 0) {
            lastSyncedText.set(resources.getString(R.string.drawer_header_last_synced_label,
                    resources.getString(R.string.drawer_header_last_synced_never)));
            statusIconTint = ContextCompat.getColor(context, R.color.sync_state_neutral);
            notifyPropertyChanged(BR.statusIconTint);
            return;
        }
        Date date = new Date(lastSyncTime);
        String dateTime = CommonUtils.formatDateTime(context, date);
        lastSyncedText.set(resources.getString(R.string.drawer_header_last_synced_label, dateTime));
        switch (syncStatus) {
            case SyncAdapter.SYNC_STATUS_SYNCED:
                statusText.set(resources.getString(R.string.drawer_header_status_synced));
                statusIconTint = ContextCompat.getColor(context, R.color.sync_state_success);
                break;
            case SyncAdapter.SYNC_STATUS_UNSYNCED:
                statusText.set(resources.getString(R.string.drawer_header_status_unsynced));
                statusIconTint = ContextCompat.getColor(context, R.color.sync_state_neutral);
                break;
            case SyncAdapter.SYNC_STATUS_ERROR:
                statusText.set(resources.getString(R.string.drawer_header_status_error));
                statusIconTint = ContextCompat.getColor(context, R.color.sync_state_error);
                break;
            case SyncAdapter.SYNC_STATUS_CONFLICT:
                statusText.set(resources.getString(R.string.drawer_header_status_conflict));
                statusIconTint = ContextCompat.getColor(context, R.color.sync_state_conflict);
                break;
            default:
                statusText.set(resources.getString(R.string.drawer_header_status_unknown));
                statusIconTint = ContextCompat.getColor(context, R.color.sync_state_neutral);
        }
        notifyPropertyChanged(BR.statusIconTint);
    }
}
