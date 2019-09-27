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

package com.bytesforge.linkasanote.settings;

import android.Manifest;
import android.accounts.Account;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Toast;

import com.bytesforge.linkasanote.ApplicationBackup;
import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.local.DatabaseHelper;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = SettingsFragment.class.getSimpleName();
    private static final String TAG_E = SettingsFragment.class.getCanonicalName();

    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0;
    private static final String PERMISSION_WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static String[] PERMISSIONS_WRITE_EXTERNAL_STORAGE =
            new String[]{PERMISSION_WRITE_EXTERNAL_STORAGE};

    private static final String ARGUMENT_SETTINGS_ACCOUNT = "ACCOUNT";

    private Context context;
    private Resources resources;
    private Account account;

    private Preference prefBackup;
    private ListPreference prefRestore;
    private ListPreference prefSyncInterval;

    @Inject
    Repository repository;

    @Inject
    Settings settings;

    @Inject
    BaseSchedulerProvider schedulerProvider;

    public static SettingsFragment newInstance(@Nullable Account account) {
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_SETTINGS_ACCOUNT, account);
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private boolean isActive() {
        return isAdded();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        context = getContext();
        resources = getResources();
        Bundle args = getArguments();
        if (args != null)
            account = args.getParcelable(ARGUMENT_SETTINGS_ACCOUNT);
        else
            account = null;
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            LaanoApplication application = (LaanoApplication) fragmentActivity.getApplication();
            application.getApplicationComponent().inject(this);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        CheckBoxPreference prefExpandLinks = (CheckBoxPreference) findPreference(
                resources.getString(R.string.pref_key_expand_links));
        boolean expandLinks = settings.isExpandLinks();
        prefExpandLinks.setChecked(expandLinks);

        CheckBoxPreference prefExpandNotes = (CheckBoxPreference) findPreference(
                resources.getString(R.string.pref_key_expand_notes));
        boolean expandNotes = settings.isExpandNotes();
        prefExpandNotes.setChecked(expandNotes);

        prefBackup = findPreference(resources.getString(R.string.pref_key_backup));
        prefRestore = (ListPreference) findPreference(
                resources.getString(R.string.pref_key_restore));
        prefRestore.setOnPreferenceChangeListener((preference, newValue) -> {
            String backupFile = (String) newValue;
            boolean success = ApplicationBackup.restoreDB(context, backupFile);
            if (success) {
                settings.resetSyncState();
                repository.resetSyncState()
                        .subscribeOn(schedulerProvider.computation())
                        .observeOn(schedulerProvider.ui())
                        .subscribe(
                                count -> {
                                    if (isActive()) {
                                        Toast.makeText(
                                                context, R.string.toast_restore_success,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                },
                                throwable -> {
                                    CommonUtils.logStackTrace(TAG_E, throwable);
                                    if (isActive()) {
                                        refreshBackupEntries();
                                        showSnackbar(resources.getString(
                                                R.string.pref_snackbar_restore_failed,
                                                backupFile), Snackbar.LENGTH_LONG);
                                    }
                                });
            } else {
                refreshBackupEntries();
                showSnackbar(resources.getString(R.string.pref_snackbar_restore_failed,
                        backupFile), Snackbar.LENGTH_LONG);
            }
            return false;
        });
        refreshBackupEntries();

        CheckBoxPreference prefSyncUploadToEmpty = (CheckBoxPreference) findPreference(
                resources.getString(R.string.pref_key_sync_upload_to_empty));
        boolean syncUploadToEmpty = settings.isSyncUploadToEmpty();
        prefSyncUploadToEmpty.setChecked(syncUploadToEmpty);

        CheckBoxPreference prefSyncProtectLocal = (CheckBoxPreference) findPreference(
                resources.getString(R.string.pref_key_sync_protect_local));
        boolean syncProtectLocal = settings.isSyncProtectLocal();
        prefSyncProtectLocal.setChecked(syncProtectLocal);

        CheckBoxPreference prefClipboardLinkGetMetadata = (CheckBoxPreference) findPreference(
                resources.getString(R.string.pref_key_clipboard_link_get_metadata));
        boolean clipboardLinkGetMetadata = settings.isClipboardLinkGetMetadata();
        prefClipboardLinkGetMetadata.setChecked(clipboardLinkGetMetadata);
        prefClipboardLinkGetMetadata.setSummary(resources.getString(
                R.string.pref_summary_clipboard_link_get_metadata,
                Formatter.formatShortFileSize(context, Settings.GLOBAL_LINK_MAX_BODY_SIZE_BYTES)));

        CheckBoxPreference prefClipboardLinkFollow = (CheckBoxPreference) findPreference(
                resources.getString(R.string.pref_key_clipboard_link_follow));
        boolean clipboardLinkFollow = settings.isClipboardLinkFollow();
        prefClipboardLinkFollow.setChecked(clipboardLinkFollow);

        CheckBoxPreference prefClipboardMonitor = (CheckBoxPreference) findPreference(
                resources.getString(R.string.pref_key_clipboard_fill_in_forms));
        boolean clipboardFillInForms = settings.isClipboardFillInForms();
        prefClipboardMonitor.setChecked(clipboardFillInForms);

        EditTextPreference prefClipboardParameterWhiteList = (EditTextPreference) findPreference(
                resources.getString(R.string.pref_key_clipboard_parameter_white_list));
        String clipboardParameterWhiteList = settings.getClipboardParameterWhiteList();
        prefClipboardParameterWhiteList.setText(clipboardParameterWhiteList);
        if (Strings.isNullOrEmpty(clipboardParameterWhiteList)) {
            prefClipboardParameterWhiteList.setSummary(
                    resources.getString(R.string.pref_summary_clipboard_parameter_white_list));
        } else {
            prefClipboardParameterWhiteList.setSummary(clipboardParameterWhiteList);
        }
        prefClipboardParameterWhiteList.setOnPreferenceChangeListener((preference, newValue) -> {
            String newWhiteList = (String) newValue;
            if (Strings.isNullOrEmpty(newWhiteList)) {
                prefClipboardParameterWhiteList.setSummary(
                        resources.getString(R.string.pref_summary_clipboard_parameter_white_list));
            } else {
                Joiner joiner = Joiner.on(Settings.GLOBAL_PARAMETER_WHITE_LIST_DELIMITER);
                String[] newWhiteListArray = newWhiteList.trim().split("\\W+");
                String newWhiteListNormalized;
                if (!newWhiteList.isEmpty() && Strings.isNullOrEmpty(newWhiteListArray[0])) {
                    newWhiteListNormalized = joiner.join(
                            Arrays.copyOfRange(newWhiteListArray, 1, newWhiteListArray.length));
                } else {
                    newWhiteListNormalized = joiner.join(newWhiteListArray);
                }
                prefClipboardParameterWhiteList.setSummary(newWhiteListNormalized);
                if (!newWhiteList.equals(newWhiteListNormalized)) {
                    prefClipboardParameterWhiteList.setText(newWhiteListNormalized);
                    showSnackbar(R.string.settings_fragment_snackbar_normalized,
                            Snackbar.LENGTH_LONG);
                    return false;
                }
            }
            return true;
        });

        EditTextPreference prefSyncDirectory = (EditTextPreference) findPreference(
                resources.getString(R.string.pref_key_sync_directory));
        String syncDirectory = settings.getSyncDirectory();
        prefSyncDirectory.setText(syncDirectory);
        prefSyncDirectory.setSummary(syncDirectory);
        prefSyncDirectory.setOnPreferenceChangeListener((preference, newValue) -> {
            String newSyncDirectory = (String) newValue;
            // NOTE: since Nextcloud library v1.1.0 all paths are valid
            settings.setSyncDirectory(newSyncDirectory);
            String normalizedSyncDirectory = settings.getSyncDirectory();
            prefSyncDirectory.setSummary(normalizedSyncDirectory);
            prefSyncDirectory.setText(normalizedSyncDirectory);
            return false;
        });

        prefSyncInterval = (ListPreference) findPreference(
                resources.getString(R.string.pref_key_sync_interval));
        prefSyncInterval.setOnPreferenceChangeListener((preference, newValue) -> {
            String newSyncInterval = (String) newValue;
            String[] seconds = resources.getStringArray(R.array.pref_sync_interval_seconds);
            int newIndex = getSyncIntervalIndex(seconds, newSyncInterval);
            String oldSyncInterval = prefSyncInterval.getValue();
            if (!newSyncInterval.equals(oldSyncInterval)) {
                settings.setSyncInterval(account, Long.parseLong(seconds[newIndex]));
                populateSyncInterval(account, true);
            }
            return false;
        });
        populateSyncInterval(account);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == prefBackup) {
            checkWriteExternalStoragePermission();
        } else {
            return false;
        }
        return true;
    }

    // Backup

    private void refreshBackupEntries() {
        if (prefRestore == null) return;

        Map<String, String> backupEntries = getBackupEntries();
        int size = backupEntries.size();
        if (size <= 0) {
            prefRestore.setEnabled(false);
            prefRestore.setSummary(getString(R.string.settings_fragment_restore_not_available));
        } else {
            Set<String> entries = backupEntries.keySet();
            prefRestore.setEntries(entries.toArray(new String[size]));
            Collection<String> entryValues = backupEntries.values();
            prefRestore.setEntryValues(entryValues.toArray(new String[size]));
            prefRestore.setEnabled(true);
            prefRestore.setSummary(getResources().getQuantityString(
                            R.plurals.settings_fragment_restore_backup_found, size, size));
        }
    }

    @NonNull
    private Map<String, String> getBackupEntries() {
        Map<String, String> backupEntries = new LinkedHashMap<>(0);
        List<String> fileNames = ApplicationBackup.getBackupFileNames(context);
        if (fileNames != null) {
            int size = fileNames.size();
            for (int i = 0; i < size; i++) {
                String fileName = fileNames.get(i);
                String fileExtension = fileName.replace(DatabaseHelper.DATABASE_NAME, "");
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        ApplicationBackup.BACKUP_EXTENSION_FORMAT, getCurrentLocaleCompat());
                Date backupDate;
                try {
                    backupDate = dateFormat.parse(fileExtension);
                } catch (ParseException e) {
                    CommonUtils.logStackTrace(TAG_E, e);
                    continue;
                }
                backupEntries.put(CommonUtils.formatDateTime(context, backupDate), fileName);
            }
            return backupEntries;
        }
        return backupEntries;
    }

    @NonNull
    private Locale getCurrentLocaleCompat() {
        if (context == null) throw new IllegalStateException("Context is needed at this point");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return context.getResources().getConfiguration().locale;
        }
    }

    private void backup() {
        if (context == null) throw new IllegalStateException("Context is needed at this point");

        String backupFile = ApplicationBackup.backupDB(context);
        if (backupFile != null) {
            refreshBackupEntries();
            Toast.makeText(context, R.string.toast_backup_success, Toast.LENGTH_SHORT).show();
        } else {
            showSnackbar(R.string.pref_snackbar_backup_failed, Snackbar.LENGTH_LONG);
        }
    }

    // Sync

    private void populateSyncInterval(@Nullable Account account) {
        populateSyncInterval(account, false);
    }

    private void populateSyncInterval(@Nullable Account account, boolean isDelay) {
        settings.getSyncInterval(account, isDelay)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(syncInterval -> {
            String[] seconds = resources.getStringArray(R.array.pref_sync_interval_seconds);
            String[] names = resources.getStringArray(R.array.pref_sync_interval_names);
            int index = getSyncIntervalIndex(seconds, syncInterval.toString());
            Long validatedSyncInterval = Long.parseLong(seconds[index]);
            if (!Objects.equals(syncInterval, validatedSyncInterval)) {
                settings.setSyncInterval(account, validatedSyncInterval);
                if (isActive()) {
                    showSnackbar(R.string.settings_fragment_snackbar_interval_error,
                            Snackbar.LENGTH_LONG);
                }
            }
            if (isActive()) {
                prefSyncInterval.setValue(seconds[index]);
                prefSyncInterval.setSummary(names[index] + " " +
                        resources.getString(R.string.pref_sync_interval_notice));
            }
        }, throwable -> {
            if (isActive()) {
                prefSyncInterval.setEnabled(false);
                prefSyncInterval.setSummary(
                        getString(R.string.settings_fragment_sync_interval_not_available));
            }
        });
    }

    private int getSyncIntervalIndex(@NonNull String[] seconds, @NonNull String syncInterval) {
        checkNotNull(seconds);
        checkNotNull(syncInterval);
        String manualInterval = resources.getString(R.string.pref_sync_interval_manual_mode);
        List<String> secondList = Arrays.asList(seconds);
        int index = secondList.indexOf(syncInterval);
        if (index < 0) {
            index = secondList.indexOf(manualInterval);
        }
        return index;
    }

    // Permissions

    public void checkWriteExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(context, PERMISSION_WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestWriteExternalStoragePermission();
            } else {
                showSnackbar(R.string.snackbar_no_permission, Snackbar.LENGTH_LONG);
            }
        } else {
            backup();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                backup();
            } else {
                showSnackbar(R.string.snackbar_no_permission, Snackbar.LENGTH_LONG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestWriteExternalStoragePermission() {
        if (shouldShowRequestPermissionRationale(PERMISSION_WRITE_EXTERNAL_STORAGE)) {
            View view = getView();
            if (view != null) {
                Snackbar.make(view, R.string.pref_snackbar_permission_write_external_storage,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.snackbar_button_ok, v ->
                                requestPermissions(PERMISSIONS_WRITE_EXTERNAL_STORAGE,
                                        REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE))
                        .show();
            }
        } else {
            requestPermissions(PERMISSIONS_WRITE_EXTERNAL_STORAGE,
                    REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
    }

    // Snackbar

    public void showSnackbar(@StringRes int messageId, int duration) {
        View view = getView();
        if (view != null) {
            Snackbar.make(view, messageId, duration).show();
        }
    }

    public void showSnackbar(@NonNull CharSequence message, int duration) {
        checkNotNull(message);
        View view = getView();
        if (view != null) {
            Snackbar.make(view, message, duration).show();
        }
    }
}
