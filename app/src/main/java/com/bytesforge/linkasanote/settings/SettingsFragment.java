package com.bytesforge.linkasanote.settings;

import android.accounts.Account;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.format.Formatter;
import android.view.View;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.owncloud.android.lib.resources.files.FileUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String ARGUMENT_SETTINGS_ACCOUNT = "ACCOUNT";

    private Context context;
    private Resources resources;
    private ListPreference prefSyncInterval;
    private Account account;

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
        context = getContext();
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
        boolean expandLinks = settings.isExpandLinks();
        prefExpandLinks.setChecked(expandLinks);

        CheckBoxPreference prefExpandNotes = (CheckBoxPreference) findPreference(
                resources.getString(R.string.pref_key_expand_notes));
        boolean expandNotes = settings.isExpandNotes();
        prefExpandNotes.setChecked(expandNotes);

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
                    View view = getView();
                    if (view != null) {
                        Snackbar.make(view, R.string.settings_fragment_snackbar_normalized,
                                Snackbar.LENGTH_LONG).show();
                    }
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
            prefSyncInterval.setSummary(names[index] + " " +
                    resources.getString(R.string.pref_sync_interval_notice));
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
