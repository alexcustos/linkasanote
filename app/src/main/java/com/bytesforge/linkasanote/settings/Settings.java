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
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.sync.SyncAdapter;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.google.common.base.Strings;

import java.util.List;

import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

public class Settings {

    private static final String TAG = Settings.class.getSimpleName();

    public static final float GLOBAL_ICON_ALPHA_DISABLED = 0.4f;
    public static final float GLOBAL_PROGRESS_OVERLAY_ALPHA = 0.4f;
    public static final long GLOBAL_PROGRESS_OVERLAY_DURATION = 200; // ms
    public static final long GLOBAL_PROGRESS_OVERLAY_SHOW_DELAY = 100; // ms
    public static final boolean GLOBAL_ITEM_CLICK_SELECT_FILTER = false;
    public static final String GLOBAL_PARAMETER_WHITE_LIST_DELIMITER = ", ";
    public static final long GLOBAL_DOUBLE_BACK_TO_EXIT_MILLIS = 2000;
    public static final boolean GLOBAL_MULTIACCOUNT_SUPPORT = false;
    public static final int GLOBAL_TAGS_AUTOCOMPLETE_THRESHOLD = 1;
    public static final int GLOBAL_LINK_MAX_KEYWORDS = 5;
    public static final int GLOBAL_LINK_MAX_BODY_SIZE_BYTES = 10 * 1024;
    public static final boolean GLOBAL_CLIPBOARD_LINK_UPDATED_TOAST = true;
    // TODO: this settings should be 3 way switch: on app start; on addEdit start; off
    public static final boolean GLOBAL_CLIPBOARD_MONITOR_ON_START = true;
    public static final long GLOBAL_JSON_MAX_BODY_SIZE_BYTES = 10 * 1024;
    public static final String GLOBAL_APPLICATION_DIRECTORY = "LaaNo";

    private static final boolean DEFAULT_EXPAND_LINKS = false;
    private static final boolean DEFAULT_EXPAND_NOTES = true;
    private static final String DEFAULT_SYNC_DIRECTORY = "/.laano_sync";
    private static final boolean DEFAULT_SYNC_UPLOAD_TO_EMPTY = true;
    private static final boolean DEFAULT_SYNC_PROTECT_LOCAL = true;
    private static final boolean DEFAULT_CLIPBOARD_LINK_GET_METADATA = true;
    private static final boolean DEFAULT_CLIPBOARD_LINK_FOLLOW = false;
    private static final boolean DEFAULT_CLIPBOARD_FILL_IN_FORMS = true;
    private static final String DEFAULT_CLIPBOARD_PARAMETER_WHITE_LIST = "id, page";

    private static final String SETTING_LAST_SYNC_TIME = "LAST_SYNC_TIME";
    private static final String SETTING_SYNC_STATUS = "SYNC_STATUS";
    private static final String SETTING_LINK_FILTER = "LINK_FILTER";
    private static final String SETTING_FAVORITE_FILTER = "FAVORITE_FILTER";
    private static final String SETTING_NOTE_FILTER = "NOTE_FILTER";
    private static final String SETTING_SHOW_CONFLICT_RESOLUTION_WARNING =
            "SHOW_CONFLICT_RESOLUTION_WARNING";
    private static final String SETTING_SHOW_FILL_IN_FORM_INFO = "SHOW_FILL_IN_FORM_INFO";
    private static final String SETTING_NOTES_LAYOUT_MODE_READING = "NOTES_LAYOUT_MODE_READING";

    private static final long DEFAULT_LAST_SYNC_TIME = 0;
    private static final int DEFAULT_SYNC_STATUS = SyncAdapter.SYNC_STATUS_UNKNOWN;
    public static final FilterType DEFAULT_FILTER_TYPE = FilterType.ALL;
    private static final String DEFAULT_LAST_SYNCED_ETAG  = null;
    private static final String DEFAULT_LINK_FILTER = null;
    private static final String DEFAULT_FAVORITE_FILTER = null;
    private static final String DEFAULT_NOTE_FILTER = null;
    private static final boolean DEFAULT_SHOW_CONFLICT_RESOLUTION_WARNING = true;
    private static final boolean DEFAULT_SHOW_FILL_IN_FORM_INFO = true;
    private static final boolean DEFAULT_NOTES_LAYOUT_MODE_READING = false;

    private static final String[] EMPTY_STRING_ARRAY = {};

    private final Resources resources;
    private final SharedPreferences sharedPreferences;

    // Runtime settings

    private boolean syncable;
    private boolean online;

    public void setSyncable(boolean syncable) {
        this.syncable = syncable;
    }

    public boolean isSyncable() {
        return syncable;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }

    // Normal settings

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

    public boolean isSyncUploadToEmpty() {
        return sharedPreferences.getBoolean(
                resources.getString(R.string.pref_key_sync_upload_to_empty),
                DEFAULT_SYNC_UPLOAD_TO_EMPTY);
    }

    public boolean isSyncProtectLocal() {
        return sharedPreferences.getBoolean(
                resources.getString(R.string.pref_key_sync_protect_local),
                DEFAULT_SYNC_PROTECT_LOCAL);
    }

    public boolean isClipboardLinkGetMetadata() {
        return sharedPreferences.getBoolean(resources.getString(
                R.string.pref_key_clipboard_link_get_metadata), DEFAULT_CLIPBOARD_LINK_GET_METADATA);
    }

    public boolean isClipboardLinkFollow() {
        boolean clipboardLinkFollow = sharedPreferences.getBoolean(resources.getString(
                R.string.pref_key_clipboard_link_follow), DEFAULT_CLIPBOARD_LINK_FOLLOW);
        return isClipboardLinkGetMetadata() && clipboardLinkFollow;
    }

    public boolean isClipboardFillInForms() {
        return sharedPreferences.getBoolean(resources.getString(
                R.string.pref_key_clipboard_fill_in_forms), DEFAULT_CLIPBOARD_FILL_IN_FORMS);
    }

    public String getClipboardParameterWhiteList() {
        return sharedPreferences.getString(resources.getString(
                R.string.pref_key_clipboard_parameter_white_list), DEFAULT_CLIPBOARD_PARAMETER_WHITE_LIST);
    }

    public String[] getClipboardParameterWhiteListArray() {
        String clipboardParameterWhiteList = sharedPreferences.getString(resources.getString(
                R.string.pref_key_clipboard_parameter_white_list), DEFAULT_CLIPBOARD_PARAMETER_WHITE_LIST);
        if (Strings.isNullOrEmpty(clipboardParameterWhiteList)) {
            return EMPTY_STRING_ARRAY;
        }
        return clipboardParameterWhiteList.split(GLOBAL_PARAMETER_WHITE_LIST_DELIMITER);
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

    public int getSyncStatus() {
        return sharedPreferences.getInt(SETTING_SYNC_STATUS, DEFAULT_SYNC_STATUS);
    }

    public synchronized void setSyncStatus(int syncStatus) {
        if (syncStatus != SyncAdapter.SYNC_STATUS_UNSYNCED
                && syncStatus != SyncAdapter.SYNC_STATUS_UNKNOWN) {
            updateLastSyncTime();
        }
        putIntSetting(SETTING_SYNC_STATUS, syncStatus);
    }

    public long getLastSyncTime() {
        return sharedPreferences.getLong(SETTING_LAST_SYNC_TIME, DEFAULT_LAST_SYNC_TIME);
    }

    private synchronized void updateLastSyncTime() {
        putLongSetting(SETTING_LAST_SYNC_TIME, currentTimeMillis());
    }

    public String getLastSyncedETag(@NonNull String key) {
        return sharedPreferences.getString(checkNotNull(key), DEFAULT_LAST_SYNCED_ETAG);
    }

    public synchronized void setLastSyncedETag(@NonNull String key, String lastSyncedETag) {
        putStringSetting(checkNotNull(key), lastSyncedETag);
    }

    public synchronized void resetSyncState() {
        setLastSyncedETag(Link.SETTING_LAST_SYNCED_ETAG, DEFAULT_LAST_SYNCED_ETAG);
        setLastSyncedETag(Favorite.SETTING_LAST_SYNCED_ETAG, DEFAULT_LAST_SYNCED_ETAG);
        setLastSyncedETag(Note.SETTING_LAST_SYNCED_ETAG, DEFAULT_LAST_SYNCED_ETAG);
        putLongSetting(SETTING_LAST_SYNC_TIME, DEFAULT_LAST_SYNC_TIME);
        setSyncStatus(SyncAdapter.SYNC_STATUS_UNSYNCED);
    }

    @NonNull
    public FilterType getFilterType(@NonNull String key) {
        checkNotNull(key);
        int ordinal = sharedPreferences.getInt(key, DEFAULT_FILTER_TYPE.ordinal());
        try {
            return FilterType.values()[ordinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            return DEFAULT_FILTER_TYPE;
        }
    }

    public synchronized void setFilterType(@NonNull String key, FilterType filterType) {
        checkNotNull(key);
        if (filterType == null) return;

        FilterType filter = getFilterType(key);
        if (filterType != filter) {
            putIntSetting(key, filterType.ordinal());
        }
    }

    public String getLinkFilter() {
        return sharedPreferences.getString(SETTING_LINK_FILTER, DEFAULT_LINK_FILTER);
    }

    public synchronized void setLinkFilter(String linkId) {
        String filter = getLinkFilter();
        if (filter == null && linkId == null) return;

        if (filter == null ^ linkId == null) {
            putStringSetting(SETTING_LINK_FILTER, linkId);
        } else if (!linkId.equals(filter)) {
            putStringSetting(SETTING_LINK_FILTER, linkId);
        }
    }

    public void resetLinkFilter(String linkId) {
        String filter = getLinkFilter();
        if (filter == null || linkId == null) return;

        if (filter.equals(linkId)) {
            setLinkFilter(null);
        }
    }

    public String getFavoriteFilter() {
        return sharedPreferences.getString(SETTING_FAVORITE_FILTER, DEFAULT_FAVORITE_FILTER);
    }

    public synchronized void setFavoriteFilter(String favoriteId) {
        String filter = getFavoriteFilter();
        if (filter == null && favoriteId == null) return;

        if (filter == null ^ favoriteId == null) {
            putStringSetting(SETTING_FAVORITE_FILTER, favoriteId);
        } else if (!favoriteId.equals(filter)) {
            putStringSetting(SETTING_FAVORITE_FILTER, favoriteId);
        }
    }

    public void resetFavoriteFilter(String favoriteId) {
        String filter = getFavoriteFilter();
        if (filter == null || favoriteId == null) return;

        if (filter.equals(favoriteId)) {
            setFavoriteFilter(null);
        }
    }

    public String getNoteFilter() {
        return sharedPreferences.getString(SETTING_NOTE_FILTER, DEFAULT_NOTE_FILTER);
    }

    public synchronized void setNoteFilter(String noteId) {
        String filter = getNoteFilter();
        if (filter == null && noteId == null) return;

        if (filter == null ^ noteId == null) {
            putStringSetting(SETTING_NOTE_FILTER, noteId);
        } else if (!noteId.equals(filter)) {
            putStringSetting(SETTING_NOTE_FILTER, noteId);
        }
    }

    public void resetNoteFilter(String noteId) {
        String filter = getNoteFilter();
        if (filter == null || noteId == null) return;

        if (filter.equals(noteId)) {
            setNoteFilter(null);
        }
    }

    public boolean isShowConflictResolutionWarning() {
        return sharedPreferences.getBoolean(
                SETTING_SHOW_CONFLICT_RESOLUTION_WARNING, DEFAULT_SHOW_CONFLICT_RESOLUTION_WARNING);
    }

    public synchronized void setShowConflictResolutionWarning(boolean show) {
        boolean oldValue = isShowConflictResolutionWarning();
        if (show != oldValue) {
            putBooleanSetting(SETTING_SHOW_CONFLICT_RESOLUTION_WARNING, show);
        }
    }

    public boolean isShowFillInFormInfo() {
        return sharedPreferences.getBoolean(
                SETTING_SHOW_FILL_IN_FORM_INFO, DEFAULT_SHOW_FILL_IN_FORM_INFO);
    }

    public synchronized void setShowFillInFormInfo(boolean show) {
        boolean oldValue = isShowFillInFormInfo();
        if (show != oldValue) {
            putBooleanSetting(SETTING_SHOW_FILL_IN_FORM_INFO, show);
        }
    }

    public boolean isNotesLayoutModeReading() {
        return sharedPreferences.getBoolean(
                SETTING_NOTES_LAYOUT_MODE_READING, DEFAULT_NOTES_LAYOUT_MODE_READING);
    }

    public synchronized void setNotesLayoutModeReading(boolean readingMode) {
        boolean oldValue = isNotesLayoutModeReading();
        if (readingMode != oldValue) {
            putBooleanSetting(SETTING_NOTES_LAYOUT_MODE_READING, readingMode);
        }
    }

    private void putStringSetting(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void putIntSetting(String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private void putLongSetting(String key, long value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    private void putBooleanSetting(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }
}
