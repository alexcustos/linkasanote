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
package com.bytesforge.linkasanote.settings

import android.Manifest
import android.accounts.Account
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.preference.*
import com.bytesforge.linkasanote.ApplicationBackup
import com.bytesforge.linkasanote.LaanoApplication
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.data.source.Repository
import com.bytesforge.linkasanote.data.source.local.DatabaseHelper
import com.bytesforge.linkasanote.utils.CommonUtils
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider
import com.google.android.material.snackbar.Snackbar
import com.google.common.base.Joiner
import com.google.common.base.Preconditions
import com.google.common.base.Strings
import io.reactivex.disposables.CompositeDisposable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class SettingsFragment : PreferenceFragmentCompat() {
    private var account: Account? = null
    private var prefBackup: Preference? = null
    private var prefRestore: ListPreference? = null
    private var prefSyncInterval: ListPreference? = null

    @JvmField
    @Inject
    var repository: Repository? = null

    @JvmField
    @Inject
    var settings: Settings? = null

    @JvmField
    @Inject
    var schedulerProvider: BaseSchedulerProvider? = null
    private val isActive: Boolean
        get() = isAdded

    override fun onCreate(savedInstanceState: Bundle?) {
        val args = arguments
        account = args?.getParcelable(ARGUMENT_SETTINGS_ACCOUNT)
        val fragmentActivity = activity
        if (fragmentActivity != null) {
            val application = fragmentActivity.application as LaanoApplication
            application.applicationComponent.inject(this)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    // TODO: expand links/notes immediately when the options are changed
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val prefExpandLinks = findPreference<Preference>(
            resources.getString(R.string.pref_key_expand_links)
        ) as CheckBoxPreference?
        val expandLinks = settings!!.isExpandLinks
        prefExpandLinks!!.isChecked = expandLinks

        val prefExpandNotes = findPreference<Preference>(
            resources.getString(R.string.pref_key_expand_notes)
        ) as CheckBoxPreference?
        val expandNotes = settings!!.isExpandNotes
        prefExpandNotes!!.isChecked = expandNotes

        prefBackup = findPreference(
            resources.getString(R.string.pref_key_backup)
        )
        prefRestore = findPreference<Preference>(
            resources.getString(R.string.pref_key_restore)
        ) as ListPreference?
        prefRestore!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                val backupFile = newValue as String?
                val success = ApplicationBackup.restoreDB(requireContext(), backupFile!!)
                if (success) {
                    settings!!.resetSyncState()
                    compositeDisposable.clear()
                    val disposable = repository!!.resetSyncState()
                        .subscribeOn(schedulerProvider!!.computation())
                        .observeOn(schedulerProvider!!.ui())
                        .subscribe(
                            { count: Long? ->
                                if (isActive) {
                                    Toast.makeText(
                                        context, R.string.toast_restore_success,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ) { throwable: Throwable? ->
                            CommonUtils.logStackTrace(TAG_E!!, throwable!!)
                            if (isActive) {
                                refreshBackupEntries()
                                showSnackbar(
                                    resources.getString(
                                        R.string.pref_snackbar_restore_failed,
                                        backupFile
                                    ), Snackbar.LENGTH_LONG
                                )
                            }
                        }
                    compositeDisposable.add(disposable)
                } else {
                    refreshBackupEntries()
                    showSnackbar(
                        resources.getString(
                            R.string.pref_snackbar_restore_failed,
                            backupFile
                        ), Snackbar.LENGTH_LONG
                    )
                }
                false
            }
        refreshBackupEntries()

        val prefSyncUploadToEmpty = findPreference<Preference>(
            resources.getString(R.string.pref_key_sync_upload_to_empty)
        ) as CheckBoxPreference?
        val syncUploadToEmpty = settings!!.isSyncUploadToEmpty
        prefSyncUploadToEmpty!!.isChecked = syncUploadToEmpty

        val prefSyncProtectLocal = findPreference<Preference>(
            resources.getString(R.string.pref_key_sync_protect_local)
        ) as CheckBoxPreference?
        val syncProtectLocal = settings!!.isSyncProtectLocal
        prefSyncProtectLocal!!.isChecked = syncProtectLocal

        val prefClipboardLinkGetMetadata = findPreference<Preference>(
            resources.getString(R.string.pref_key_clipboard_link_get_metadata)
        ) as CheckBoxPreference?
        val clipboardLinkGetMetadata = settings!!.isClipboardLinkGetMetadata
        prefClipboardLinkGetMetadata!!.isChecked = clipboardLinkGetMetadata
        prefClipboardLinkGetMetadata.summary = resources.getString(
            R.string.pref_summary_clipboard_link_get_metadata,
            Formatter.formatShortFileSize(
                context,
                Settings.GLOBAL_LINK_MAX_BODY_SIZE_BYTES.toLong()
            )
        )

        val prefClipboardLinkFollow = findPreference<Preference>(
            resources.getString(R.string.pref_key_clipboard_link_follow)
        ) as CheckBoxPreference?
        val clipboardLinkFollow = settings!!.isClipboardLinkFollow
        prefClipboardLinkFollow!!.isChecked = clipboardLinkFollow

        val prefClipboardMonitor = findPreference<Preference>(
            resources.getString(R.string.pref_key_clipboard_fill_in_forms)
        ) as CheckBoxPreference?
        val clipboardFillInForms = settings!!.isClipboardFillInForms
        prefClipboardMonitor!!.isChecked = clipboardFillInForms

        val prefClipboardParameterWhiteList = findPreference<Preference>(
            resources.getString(R.string.pref_key_clipboard_parameter_white_list)
        ) as EditTextPreference?
        val clipboardParameterWhiteList = settings!!.clipboardParameterWhiteList
        prefClipboardParameterWhiteList!!.text = clipboardParameterWhiteList
        if (Strings.isNullOrEmpty(clipboardParameterWhiteList)) {
            prefClipboardParameterWhiteList.summary =
                resources.getString(R.string.pref_summary_clipboard_parameter_white_list)
        } else {
            prefClipboardParameterWhiteList.summary = clipboardParameterWhiteList
        }
        prefClipboardParameterWhiteList.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                val newWhiteList = newValue as String
                if (Strings.isNullOrEmpty(newWhiteList)) {
                    prefClipboardParameterWhiteList.summary =
                        resources.getString(R.string.pref_summary_clipboard_parameter_white_list)
                } else {
                    val joiner: Joiner =
                        Joiner.on(Settings.GLOBAL_PARAMETER_WHITE_LIST_DELIMITER)
                    val newWhiteListArray =
                        newWhiteList.trim { it <= ' ' }.split("\\W+").toTypedArray()
                    val newWhiteListNormalized: String =
                        if (newWhiteList.isNotEmpty() && Strings.isNullOrEmpty(
                            newWhiteListArray[0]
                        )
                    ) {
                        joiner.join(
                            newWhiteListArray.copyOfRange(1, newWhiteListArray.size)
                        )
                    } else {
                        joiner.join(newWhiteListArray)
                    }
                    prefClipboardParameterWhiteList.summary = newWhiteListNormalized
                    if (newWhiteList != newWhiteListNormalized) {
                        prefClipboardParameterWhiteList.text = newWhiteListNormalized
                        showSnackbar(
                            R.string.settings_fragment_snackbar_normalized,
                            Snackbar.LENGTH_LONG
                        )
                        return@OnPreferenceChangeListener false
                    }
                }
                true
            }

        val prefSyncDirectory = findPreference<Preference>(
            resources.getString(R.string.pref_key_sync_directory)
        ) as EditTextPreference?
        val syncDirectory = settings!!.syncDirectory
        prefSyncDirectory!!.text = syncDirectory
        prefSyncDirectory.summary = syncDirectory
        prefSyncDirectory.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                val newSyncDirectory = newValue as String
                // NOTE: since Nextcloud library v1.1.0 all paths are valid
                settings!!.syncDirectory = newSyncDirectory
                val normalizedSyncDirectory = settings!!.syncDirectory
                prefSyncDirectory.summary = normalizedSyncDirectory
                prefSyncDirectory.text = normalizedSyncDirectory
                false
            }

        prefSyncInterval = findPreference<Preference>(
            resources.getString(R.string.pref_key_sync_interval)
        ) as ListPreference?
        prefSyncInterval!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                val newSyncInterval = newValue as String
                val seconds = resources.getStringArray(R.array.pref_sync_interval_seconds)
                val newIndex = getSyncIntervalIndex(seconds, newSyncInterval)
                val oldSyncInterval = prefSyncInterval!!.value
                if (newSyncInterval != oldSyncInterval) {
                    settings!!.setSyncInterval(account, seconds[newIndex].toLong())
                    populateSyncInterval(account, true)
                }
                false
            }
        populateSyncInterval(account)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference === prefBackup) {
            checkWriteExternalStoragePermission()
        } else {
            return false
        }
        return true
    }

    // Backup
    private fun refreshBackupEntries() {
        if (prefRestore == null) return
        val backupEntries = backupEntries
        val size = backupEntries.size
        if (size <= 0) {
            prefRestore!!.isEnabled = false
            prefRestore!!.summary = getString(R.string.settings_fragment_restore_not_available)
        } else {
            val entries = backupEntries.keys
            prefRestore!!.entries = entries.toTypedArray()
            val entryValues = backupEntries.values
            prefRestore!!.entryValues = entryValues.toTypedArray()
            prefRestore!!.isEnabled = true
            prefRestore!!.summary = getResources().getQuantityString(
                R.plurals.settings_fragment_restore_backup_found, size, size
            )
        }
    }

    private val backupEntries: Map<String, String>
        get() {
            val backupEntries: MutableMap<String, String> = LinkedHashMap(0)
            val fileNames = ApplicationBackup.getBackupFileNames(
                requireContext()
            )
            if (fileNames != null) {
                val size = fileNames.size
                for (i in 0 until size) {
                    val fileName = fileNames[i]
                    val fileExtension = fileName.replace(DatabaseHelper.DATABASE_NAME, "")
                    val dateFormat = SimpleDateFormat(
                        ApplicationBackup.BACKUP_EXTENSION_FORMAT, currentLocaleCompat
                    )
                    val backupDate: Date? = try {
                        dateFormat.parse(fileExtension)
                    } catch (e: ParseException) {
                        CommonUtils.logStackTrace(TAG_E!!, e)
                        continue
                    }
                    backupEntries[CommonUtils.formatDateTime(
                        requireContext(), backupDate!!)] = fileName
                }
                return backupEntries
            }
            return backupEntries
        }
    private val currentLocaleCompat: Locale
        get() {
            checkNotNull(context) { "Context is needed at this point" }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requireContext().resources.configuration.locales[0]
            } else {
                requireContext().resources.configuration.locale
            }
        }

    private fun backup() {
        checkNotNull(context) { "Context is needed at this point" }
        val backupFile = ApplicationBackup.backupDB(requireContext())
        if (backupFile != null) {
            refreshBackupEntries()
            Toast.makeText(context, R.string.toast_backup_success, Toast.LENGTH_SHORT).show()
        } else {
            showSnackbar(R.string.pref_snackbar_backup_failed, Snackbar.LENGTH_LONG)
        }
    }

    // Sync

    private fun populateSyncInterval(account: Account?, isDelay: Boolean = false) {
        compositeDisposable.clear()
        val disposable = settings!!.getSyncInterval(account, isDelay)
            .subscribeOn(schedulerProvider!!.computation())
            .observeOn(schedulerProvider!!.ui())
            .subscribe({ syncInterval: Long? ->
                val seconds = resources.getStringArray(R.array.pref_sync_interval_seconds)
                val names = resources.getStringArray(R.array.pref_sync_interval_names)
                val index = getSyncIntervalIndex(seconds, syncInterval.toString())
                val validatedSyncInterval = seconds[index].toLong()
                if (syncInterval != validatedSyncInterval) {
                    settings!!.setSyncInterval(account, validatedSyncInterval)
                    if (isActive) {
                        showSnackbar(
                            R.string.settings_fragment_snackbar_interval_error,
                            Snackbar.LENGTH_LONG
                        )
                    }
                }
                if (isActive) {
                    prefSyncInterval!!.value = seconds[index]
                    prefSyncInterval!!.summary = names[index].toString() + " " +
                            resources.getString(R.string.pref_sync_interval_notice)
                }
            }) { throwable: Throwable? ->
                if (isActive) {
                    prefSyncInterval!!.isEnabled = false
                    prefSyncInterval!!.summary =
                        getString(R.string.settings_fragment_sync_interval_not_available)
                }
            }
        compositeDisposable.add(disposable)
    }

    private fun getSyncIntervalIndex(seconds: Array<String>, syncInterval: String): Int {
        Preconditions.checkNotNull(seconds)
        Preconditions.checkNotNull(syncInterval)
        val manualInterval = resources.getString(R.string.pref_sync_interval_manual_mode)
        val secondList = Arrays.asList(*seconds)
        var index = secondList.indexOf(syncInterval)
        if (index < 0) {
            index = secondList.indexOf(manualInterval)
        }
        return index
    }

    // Permissions

    private fun checkWriteExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), PERMISSION_WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestWriteExternalStoragePermission()
            } else {
                showSnackbar(
                    R.string.snackbar_no_permission,
                    Snackbar.LENGTH_LONG
                )
            }
        } else {
            backup()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                backup()
            } else {
                showSnackbar(
                    R.string.snackbar_no_permission,
                    Snackbar.LENGTH_LONG
                )
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun requestWriteExternalStoragePermission() {
        if (shouldShowRequestPermissionRationale(PERMISSION_WRITE_EXTERNAL_STORAGE)) {
            val view = view
            if (view != null) {
                Snackbar.make(
                    view, R.string.pref_snackbar_permission_write_external_storage,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.snackbar_button_ok) { v: View? ->
                        requestPermissions(
                            PERMISSIONS_WRITE_EXTERNAL_STORAGE,
                            REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
                        )
                    }
                    .show()
            }
        } else {
            requestPermissions(
                PERMISSIONS_WRITE_EXTERNAL_STORAGE,
                REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
            )
        }
    }

    // Snackbar

    fun showSnackbar(@StringRes messageId: Int, duration: Int) {
        val view = view
        if (view != null) {
            Snackbar.make(view, messageId, duration).show()
        }
    }

    fun showSnackbar(message: CharSequence, duration: Int) {
        Preconditions.checkNotNull(message)
        val view = view
        if (view != null) {
            Snackbar.make(view, message, duration).show()
        }
    }

    companion object {
        private val TAG = SettingsFragment::class.java.simpleName
        private val TAG_E = SettingsFragment::class.java.canonicalName

        private val compositeDisposable: CompositeDisposable = CompositeDisposable()

        private const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0
        private const val PERMISSION_WRITE_EXTERNAL_STORAGE =
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        private val PERMISSIONS_WRITE_EXTERNAL_STORAGE = arrayOf(PERMISSION_WRITE_EXTERNAL_STORAGE)
        private const val ARGUMENT_SETTINGS_ACCOUNT = "ACCOUNT"
        fun newInstance(account: Account?): SettingsFragment {
            val args = Bundle()
            args.putParcelable(ARGUMENT_SETTINGS_ACCOUNT, account)
            val fragment = SettingsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}