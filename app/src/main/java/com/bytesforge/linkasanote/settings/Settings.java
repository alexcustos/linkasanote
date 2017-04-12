package com.bytesforge.linkasanote.settings;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.PeriodicSync;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.sync.SyncAdapter;
import com.bytesforge.linkasanote.sync.files.JsonFile;

import java.util.List;

import io.reactivex.Single;

import static java.lang.System.currentTimeMillis;

public class Settings {

    public static final float GLOBAL_IMAGE_BUTTON_ALPHA_DISABLED = 0.3f;

    private static final String DEFAULT_SYNC_DIRECTORY = "/.laano_sync";
    private static final boolean DEFAULT_EXPAND_LINKS = false;
    private static final boolean DEFAULT_EXPAND_NOTES = false;
    private static final boolean DEFAULT_CLIPBOARD_MONITOR = true;

    private static final String SETTING_LAST_SYNC_TIME = "LAST_SYNC_TIME";
    private static final String SETTING_LAST_SYNC_STATUS = "LAST_SYNC_STATUS";
    private static final String SETTING_LINKS_LAST_SYNCED_ETAG = "LINKS_LAST_SYNCED_ETAG";
    private static final String SETTING_FAVORITES_LAST_SYNCED_ETAG = "FAVORITES_LAST_SYNCED_ETAG";

    private static final long DEFAULT_LAST_SYNC_TIME = 0;
    private static final int DEFAULT_LAST_SYNC_STATUS = SyncAdapter.LAST_SYNC_STATUS_UNKNOWN;
    private static final String DEFAULT_LINKS_LAST_SYNCED_ETAG  = null;
    private static final String DEFAULT_FAVORITES_LAST_SYNCED_ETAG  = null;

    private final Resources resources;
    private final SharedPreferences sharedPreferences;

    public Settings(Context context, SharedPreferences sharedPreferences) {
        resources = context.getResources();
        this.sharedPreferences = sharedPreferences;
    }

    public boolean isExpandLinks() {
        return sharedPreferences.getBoolean(
                resources.getString(R.string.pref_key_expand_links), DEFAULT_EXPAND_LINKS);
    }

    public boolean isExpandNotes() {
        return sharedPreferences.getBoolean(
                resources.getString(R.string.pref_key_expand_notes), DEFAULT_EXPAND_NOTES);
    }

    public boolean isClipboardMonitor() {
        return sharedPreferences.getBoolean(
                resources.getString(R.string.pref_key_clipboard_monitor), DEFAULT_CLIPBOARD_MONITOR);
    }

    public String getSyncDirectory() {
        String syncDirectory = sharedPreferences.getString(
                resources.getString(R.string.pref_key_sync_directory), DEFAULT_SYNC_DIRECTORY);
        return syncDirectory.startsWith(JsonFile.PATH_SEPARATOR)
                ? syncDirectory
                : JsonFile.PATH_SEPARATOR + syncDirectory;
    }

    @NonNull
    Single<Long> getSyncInterval(Account account, boolean isDelay) {
        return Single.fromCallable(() -> {
            if (account == null) return null;

            if (ContentResolver.getIsSyncable(account, LocalContract.CONTENT_AUTHORITY) <= 0) {
                return null;
            }
            // NOTE: getPeriodSyncs returns old value if is called immediately after addPeriodSync
            if (isDelay) {
                Thread.sleep(25); // TODO: find a better way, it is not good at all
            }
            Long manualInterval = Long.parseLong(
                    resources.getString(R.string.pref_sync_interval_manual_mode));
            if (ContentResolver.getSyncAutomatically(account, LocalContract.CONTENT_AUTHORITY)) {
                List<PeriodicSync> periodicSyncs = ContentResolver
                        .getPeriodicSyncs(account, LocalContract.CONTENT_AUTHORITY);

                if (periodicSyncs.isEmpty()) return manualInterval;
                else return periodicSyncs.get(0).period;
            } else {
                return manualInterval;
            }
        });
    }

    void setSyncInterval(Account account, long seconds) {
        if (account == null) return;

        Long manualInterval = Long.parseLong(resources.getString(
                R.string.pref_sync_interval_manual_mode));
        if (seconds == manualInterval) {
            ContentResolver.setSyncAutomatically(account, LocalContract.CONTENT_AUTHORITY, false);
        } else {
            ContentResolver.setSyncAutomatically(account, LocalContract.CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(
                    account, LocalContract.CONTENT_AUTHORITY, new Bundle(), seconds);
        }
    }

    public int getLastSyncStatus() {
        return sharedPreferences.getInt(SETTING_LAST_SYNC_STATUS, DEFAULT_LAST_SYNC_STATUS);
    }

    public synchronized void setLastSyncStatus(int lastSyncStatus) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SETTING_LAST_SYNC_STATUS, lastSyncStatus);
        editor.apply();
    }

    public long getLastSyncTime() {
        return sharedPreferences.getLong(SETTING_LAST_SYNC_TIME, DEFAULT_LAST_SYNC_TIME);
    }

    public synchronized void updateLastSyncTime() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(SETTING_LAST_SYNC_TIME, currentTimeMillis());
        editor.apply();
    }

    public String getLinksLastSyncedETag() {
        return sharedPreferences.getString(
                SETTING_LINKS_LAST_SYNCED_ETAG, DEFAULT_LINKS_LAST_SYNCED_ETAG);
    }

    public void setLinksLastSyncedETag(String linksLastSyncedETag) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SETTING_LINKS_LAST_SYNCED_ETAG, linksLastSyncedETag);
        editor.apply();
    }

    public String getFavoritesLastSyncedETag() {
        return sharedPreferences.getString(
                SETTING_FAVORITES_LAST_SYNCED_ETAG, DEFAULT_FAVORITES_LAST_SYNCED_ETAG);
    }

    public void setFavoritesLastSyncedETag(String favoritesLastSyncedETag) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SETTING_FAVORITES_LAST_SYNCED_ETAG, favoritesLastSyncedETag);
        editor.apply();
    }
}
