package com.bytesforge.linkasanote.settings;

import android.accounts.Account;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.owncloud.android.lib.resources.files.FileUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String ARGUMENT_SETTINGS_ACCOUNT = "ACCOUNT";

    private ListPreference prefSyncInterval;
    private Account account;
    private Resources resources;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        resources = getResources();
        account = getArguments().getParcelable(ARGUMENT_SETTINGS_ACCOUNT);
        LaanoApplication application = (LaanoApplication) getActivity().getApplication();
        application.getApplicationComponent().inject(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        CheckBoxPreference prefExpandLinks = (CheckBoxPreference) findPreference(
                resources.getString(R.string.pref_key_expand_links));
        boolean isExpandLinks = settings.isExpandLinks();
        prefExpandLinks.setChecked(isExpandLinks);

        CheckBoxPreference prefExpandNotes = (CheckBoxPreference) findPreference(
                resources.getString(R.string.pref_key_expand_notes));
        boolean isExpandNotes = settings.isExpandNotes();
        prefExpandNotes.setChecked(isExpandNotes);

        CheckBoxPreference prefClipboardMonitor = (CheckBoxPreference) findPreference(
                resources.getString(R.string.pref_key_clipboard_monitor));
        boolean isClipboardMonitor = settings.isClipboardMonitor();
        prefClipboardMonitor.setChecked(isClipboardMonitor);

        EditTextPreference prefSyncDirectory = (EditTextPreference) findPreference(
                resources.getString(R.string.pref_key_sync_directory));
        String syncDirectory = settings.getSyncDirectory();
        prefSyncDirectory.setText(syncDirectory);
        prefSyncDirectory.setSummary(syncDirectory);
        prefSyncDirectory.setOnPreferenceChangeListener((preference, newValue) -> {
            String newSyncDirectory = (String) newValue;
            // TODO: provide version to the client and use it to check if supportsForbiddenChars
            if (!FileUtils.isValidPath(newSyncDirectory, false)) {
                View view = getView();
                if (view != null) {
                    Snackbar.make(view, R.string.settings_fragment_snackbar_directory_error,
                            Snackbar.LENGTH_LONG).show();
                }
                return false;
            }
            prefSyncDirectory.setSummary(newSyncDirectory);
            return true;
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
                View view = getView();
                if (view != null) {
                    Snackbar.make(view, R.string.settings_fragment_snackbar_interval_error,
                            Snackbar.LENGTH_LONG).show();
                }
            }
            prefSyncInterval.setValue(seconds[index]);
            prefSyncInterval.setSummary(names[index]);
        }, throwable -> {
            prefSyncInterval.setEnabled(false);
            prefSyncInterval.setSummary(
                    getString(R.string.settings_fragment_sync_interval_not_available));
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
}
