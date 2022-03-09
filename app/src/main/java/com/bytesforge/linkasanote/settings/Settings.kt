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

import android.accounts.Account
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.data.Favorite
import com.bytesforge.linkasanote.data.Link
import com.bytesforge.linkasanote.data.Note
import com.bytesforge.linkasanote.data.source.local.LocalContract
import com.bytesforge.linkasanote.laano.FilterType
import com.bytesforge.linkasanote.laano.favorites.FavoritesPresenter
import com.bytesforge.linkasanote.laano.links.LinksPresenter
import com.bytesforge.linkasanote.laano.notes.NotesPresenter
import com.bytesforge.linkasanote.sync.SyncAdapter
import com.bytesforge.linkasanote.sync.files.JsonFile
import com.google.common.base.Strings
import io.reactivex.Single
import java.io.File

class Settings(context: Context, private val sharedPreferences: SharedPreferences) {
    private val resources: Resources = context.resources

    // Runtime settings
    var isSyncable = false
    var isOnline = false
    var linkFilter: Link? = null // OPTIMIZATION: just linkTitle is needed
    var favoriteFilter: Favorite? = null
    var noteFilter: Note? = null // OPTIMIZATION: just noteTitle is needed
    val isExpandLinks: Boolean
        get() = sharedPreferences.getBoolean(
            resources.getString(R.string.pref_key_expand_links), DEFAULT_EXPAND_LINKS
        )
    val isExpandNotes: Boolean
        get() = sharedPreferences.getBoolean(
            resources.getString(R.string.pref_key_expand_notes), DEFAULT_EXPAND_NOTES
        )
    val isSyncUploadToEmpty: Boolean
        get() = sharedPreferences.getBoolean(
            resources.getString(R.string.pref_key_sync_upload_to_empty),
            DEFAULT_SYNC_UPLOAD_TO_EMPTY
        )
    val isSyncProtectLocal: Boolean
        get() = sharedPreferences.getBoolean(
            resources.getString(R.string.pref_key_sync_protect_local),
            DEFAULT_SYNC_PROTECT_LOCAL
        )
    val isClipboardLinkGetMetadata: Boolean
        get() = sharedPreferences.getBoolean(
            resources.getString(
                R.string.pref_key_clipboard_link_get_metadata
            ), DEFAULT_CLIPBOARD_LINK_GET_METADATA
        )
    val isClipboardLinkFollow: Boolean
        get() {
            val clipboardLinkFollow = sharedPreferences.getBoolean(
                resources.getString(
                    R.string.pref_key_clipboard_link_follow
                ), DEFAULT_CLIPBOARD_LINK_FOLLOW
            )
            return isClipboardLinkGetMetadata && clipboardLinkFollow
        }
    val isClipboardFillInForms: Boolean
        get() = sharedPreferences.getBoolean(
            resources.getString(
                R.string.pref_key_clipboard_fill_in_forms
            ), DEFAULT_CLIPBOARD_FILL_IN_FORMS
        )
    val clipboardParameterWhiteList: String?
        get() = sharedPreferences.getString(
            resources.getString(
                R.string.pref_key_clipboard_parameter_white_list
            ), DEFAULT_CLIPBOARD_PARAMETER_WHITE_LIST
        )
    val clipboardParameterWhiteListArray: Array<String>
        get() {
            val clipboardParameterWhiteList = sharedPreferences.getString(
                resources.getString(
                    R.string.pref_key_clipboard_parameter_white_list
                ), DEFAULT_CLIPBOARD_PARAMETER_WHITE_LIST
            )
            return if (Strings.isNullOrEmpty(clipboardParameterWhiteList)) {
                EMPTY_STRING_ARRAY
            } else clipboardParameterWhiteList!!.split(GLOBAL_PARAMETER_WHITE_LIST_DELIMITER)
                .toTypedArray()
        }
    var syncDirectory: String?
        get() {
            val prefKey = resources.getString(R.string.pref_key_sync_directory)
            return sharedPreferences.getString(prefKey, DEFAULT_SYNC_DIRECTORY)
        }
        @Synchronized set(syncDirectory) {
            val oldValue = syncDirectory
            val prefKey = resources.getString(R.string.pref_key_sync_directory)
            val normalizedSyncDirectory = normalizeSyncDirectory(syncDirectory)
            if (oldValue != normalizedSyncDirectory) {
                putStringSetting(prefKey, normalizedSyncDirectory)
                resetSyncState()
            }
        }

    private fun normalizeSyncDirectory(syncDirectory: String?): String {
        if (Strings.isNullOrEmpty(syncDirectory)) {
            return DEFAULT_SYNC_DIRECTORY
        }
        return if (syncDirectory!!.startsWith(JsonFile.PATH_SEPARATOR)) syncDirectory
        else JsonFile.PATH_SEPARATOR + syncDirectory
    }

    fun getSyncInterval(account: Account?, isDelay: Boolean): Single<Long?> {
        return Single.fromCallable {
            if (account == null) return@fromCallable null
            if (ContentResolver.getIsSyncable(account, LocalContract.CONTENT_AUTHORITY) <= 0) {
                return@fromCallable null
            }
            // NOTE: getPeriodSyncs returns old value if is called immediately after addPeriodSync
            if (isDelay) {
                Thread.sleep(25) // TODO: find a better way, it is not good at all
            }
            val manualInterval =
                resources.getString(R.string.pref_sync_interval_manual_mode).toLong()
            if (ContentResolver.getSyncAutomatically(account, LocalContract.CONTENT_AUTHORITY)) {
                val periodicSyncs = ContentResolver
                    .getPeriodicSyncs(account, LocalContract.CONTENT_AUTHORITY)

                if (periodicSyncs.isEmpty()) return@fromCallable manualInterval
                else return@fromCallable periodicSyncs[0].period
            } else {
                return@fromCallable manualInterval
            }
        }
    }

    fun setSyncInterval(account: Account?, seconds: Long) {
        if (account == null) return

        val manualInterval = resources.getString(
            R.string.pref_sync_interval_manual_mode
        ).toLong()
        if (seconds == manualInterval) {
            ContentResolver.setSyncAutomatically(account, LocalContract.CONTENT_AUTHORITY, false)
        } else {
            ContentResolver.setSyncAutomatically(account, LocalContract.CONTENT_AUTHORITY, true)
            ContentResolver.addPeriodicSync(
                account, LocalContract.CONTENT_AUTHORITY, Bundle(), seconds
            )
        }
    }

    @set:Synchronized
    var syncStatus: Int
        get() = sharedPreferences.getInt(SETTING_SYNC_STATUS, DEFAULT_SYNC_STATUS)
        set(syncStatus) {
            if (syncStatus != SyncAdapter.SYNC_STATUS_UNSYNCED
                && syncStatus != SyncAdapter.SYNC_STATUS_UNKNOWN
            ) {
                updateLastSyncTime()
            }
            putIntSetting(SETTING_SYNC_STATUS, syncStatus)
        }
    val lastSyncTime: Long
        get() = sharedPreferences.getLong(SETTING_LAST_SYNC_TIME, DEFAULT_LAST_SYNC_TIME)

    @Synchronized
    private fun updateLastSyncTime() {
        putLongSetting(SETTING_LAST_SYNC_TIME, System.currentTimeMillis())
    }

    val lastLinksSyncTime: Long
        get() = sharedPreferences.getLong(SETTING_LAST_LINKS_SYNC_TIME, DEFAULT_LAST_SYNC_TIME)

    @Synchronized
    fun updateLastLinksSyncTime() {
        putLongSetting(SETTING_LAST_LINKS_SYNC_TIME, System.currentTimeMillis())
    }

    val lastFavoritesSyncTime: Long
        get() = sharedPreferences.getLong(SETTING_LAST_FAVORITES_SYNC_TIME, DEFAULT_LAST_SYNC_TIME)

    @Synchronized
    fun updateLastFavoritesSyncTime() {
        putLongSetting(SETTING_LAST_FAVORITES_SYNC_TIME, System.currentTimeMillis())
    }

    val lastNotesSyncTime: Long
        get() = sharedPreferences.getLong(SETTING_LAST_NOTES_SYNC_TIME, DEFAULT_LAST_SYNC_TIME)

    @Synchronized
    fun updateLastNotesSyncTime() {
        putLongSetting(SETTING_LAST_NOTES_SYNC_TIME, System.currentTimeMillis())
    }

    fun getLastSyncedETag(key: String): String? {
        return sharedPreferences.getString(key, DEFAULT_LAST_SYNCED_ETAG)
    }

    @Synchronized
    fun setLastSyncedETag(key: String, lastSyncedETag: String?) {
        putStringSetting(key, lastSyncedETag)
    }

    @Synchronized
    fun resetSyncState() {
        setLastSyncedETag(Link.SETTING_LAST_SYNCED_ETAG, DEFAULT_LAST_SYNCED_ETAG)
        setLastSyncedETag(Favorite.SETTING_LAST_SYNCED_ETAG, DEFAULT_LAST_SYNCED_ETAG)
        setLastSyncedETag(Note.SETTING_LAST_SYNCED_ETAG, DEFAULT_LAST_SYNCED_ETAG)
        putLongSetting(SETTING_LAST_SYNC_TIME, DEFAULT_LAST_SYNC_TIME)
        syncStatus = SyncAdapter.SYNC_STATUS_UNSYNCED
    }

    private fun getFilterType(key: String): FilterType {
        val ordinal = sharedPreferences.getInt(key, DEFAULT_FILTER_TYPE.ordinal)
        return try {
            FilterType.values()[ordinal]
        } catch (e: ArrayIndexOutOfBoundsException) {
            DEFAULT_FILTER_TYPE
        }
    }

    @Synchronized
    private fun setFilterType(key: String, filterType: FilterType?) {
        if (filterType == null) return

        val filter = getFilterType(key)
        if (filterType != filter) {
            putIntSetting(key, filterType.ordinal)
        }
    }

    var linksFilterType: FilterType
        get() = getFilterType(LinksPresenter.SETTING_LINKS_FILTER_TYPE)
        set(filterType) {
            setFilterType(LinksPresenter.SETTING_LINKS_FILTER_TYPE, filterType)
        }

    @set:Synchronized
    var linkFilterId: String?
        get() = sharedPreferences.getString(SETTING_LINK_FILTER_ID, DEFAULT_LINK_FILTER_ID)
        set(linkId) {
            if (linkId == null) linkFilter = null
            val filterId = linkFilterId
            if (filterId == null && linkId == null) return

            if ((filterId == null) xor (linkId == null)) {
                putStringSetting(SETTING_LINK_FILTER_ID, linkId)
            } else if (linkId != filterId) {
                putStringSetting(SETTING_LINK_FILTER_ID, linkId)
            }
        }

    fun resetLinkFilterId(linkId: String?) {
        val filterId = linkFilterId
        if (filterId == null || linkId == null) return

        if (filterId == linkId) {
            linkFilterId = null
        }
    }

    var favoritesFilterType: FilterType
        get() = getFilterType(FavoritesPresenter.SETTING_FAVORITES_FILTER_TYPE)
        set(filterType) {
            setFilterType(FavoritesPresenter.SETTING_FAVORITES_FILTER_TYPE, filterType)
        }

    @set:Synchronized
    var favoriteFilterId: String?
        get() = sharedPreferences.getString(SETTING_FAVORITE_FILTER_ID, DEFAULT_FAVORITE_FILTER_ID)
        set(favoriteId) {
            if (favoriteId == null) favoriteFilter = null
            val filterId = favoriteFilterId
            if (filterId == null && favoriteId == null) return
            if ((filterId == null) xor (favoriteId == null)) {
                putStringSetting(SETTING_FAVORITE_FILTER_ID, favoriteId)
            } else if (favoriteId != filterId) {
                putStringSetting(SETTING_FAVORITE_FILTER_ID, favoriteId)
            }
        }

    fun resetFavoriteFilterId(favoriteId: String?) {
        val filterId = favoriteFilterId
        if (filterId == null || favoriteId == null) return
        if (filterId == favoriteId) {
            favoriteFilterId = null
        }
    }

    var notesFilterType: FilterType
        get() = getFilterType(NotesPresenter.SETTING_NOTES_FILTER_TYPE)
        set(filterType) {
            setFilterType(NotesPresenter.SETTING_NOTES_FILTER_TYPE, filterType)
        }

    @set:Synchronized
    var noteFilterId: String?
        get() = sharedPreferences.getString(SETTING_NOTE_FILTER_ID, DEFAULT_NOTE_FILTER_ID)
        set(noteId) {
            if (noteId == null) noteFilter = null
            val filterId = noteFilterId
            if (filterId == null && noteId == null) return
            if ((filterId == null) xor (noteId == null)) {
                putStringSetting(SETTING_NOTE_FILTER_ID, noteId)
            } else if (noteId != filterId) {
                putStringSetting(SETTING_NOTE_FILTER_ID, noteId)
            }
        }

    fun resetNoteFilterId(noteId: String?) {
        val filterId = noteFilterId
        if (filterId == null || noteId == null) return
        if (filterId == noteId) {
            noteFilterId = null
        }
    }

    @set:Synchronized
    var isShowConflictResolutionWarning: Boolean
        get() = sharedPreferences.getBoolean(
            SETTING_SHOW_CONFLICT_RESOLUTION_WARNING, DEFAULT_SHOW_CONFLICT_RESOLUTION_WARNING
        )
        set(show) {
            val oldValue = isShowConflictResolutionWarning
            if (show != oldValue) {
                putBooleanSetting(SETTING_SHOW_CONFLICT_RESOLUTION_WARNING, show)
            }
        }

    @set:Synchronized
    var isShowFillInFormInfo: Boolean
        get() = sharedPreferences.getBoolean(
            SETTING_SHOW_FILL_IN_FORM_INFO, DEFAULT_SHOW_FILL_IN_FORM_INFO
        )
        set(show) {
            val oldValue = isShowFillInFormInfo
            if (show != oldValue) {
                putBooleanSetting(SETTING_SHOW_FILL_IN_FORM_INFO, show)
            }
        }

    @set:Synchronized
    var isNotesLayoutModeReading: Boolean
        get() = sharedPreferences.getBoolean(
            SETTING_NOTES_LAYOUT_MODE_READING, DEFAULT_NOTES_LAYOUT_MODE_READING
        )
        set(readingMode) {
            val oldValue = isNotesLayoutModeReading
            if (readingMode != oldValue) {
                putBooleanSetting(SETTING_NOTES_LAYOUT_MODE_READING, readingMode)
            }
        }

    private fun putStringSetting(key: String, value: String?) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun putIntSetting(key: String, value: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    private fun putLongSetting(key: String, value: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    private fun putBooleanSetting(key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    companion object {
        private val TAG = Settings::class.java.simpleName

        const val GLOBAL_ICON_ALPHA_DISABLED = 0.4f
        const val GLOBAL_PROGRESS_OVERLAY_ALPHA = 0.4f
        const val GLOBAL_PROGRESS_OVERLAY_DURATION: Long = 200 // ms
        const val GLOBAL_PROGRESS_OVERLAY_SHOW_DELAY: Long = 100 // ms
        const val GLOBAL_ITEM_CLICK_SELECT_FILTER = false
        const val GLOBAL_PARAMETER_WHITE_LIST_DELIMITER = ", "
        const val GLOBAL_DOUBLE_BACK_TO_EXIT_MILLIS: Long = 2000
        const val GLOBAL_MULTIACCOUNT_SUPPORT = false
        const val GLOBAL_TAGS_AUTOCOMPLETE_THRESHOLD = 1
        const val GLOBAL_LINK_MAX_KEYWORDS = 10
        const val GLOBAL_LINK_MAX_BODY_SIZE_BYTES = 10 * 1024
        const val GLOBAL_CLIPBOARD_LINK_UPDATED_TOAST = true
        const val GLOBAL_CLIPBOARD_MONITOR_ON_START = true
        const val GLOBAL_JSON_MAX_BODY_SIZE_BYTES = (10 * 1024).toLong()
        @JvmField
        val GLOBAL_APPLICATION_DIRECTORY = File.separator + "LaaNo"
        const val GLOBAL_RETRY_ON_NETWORK_ERROR = 2
        const val GLOBAL_DELAY_ON_NETWORK_ERROR_MILLIS = 2000
        const val GLOBAL_SYNC_LOG_KEEPING_PERIOD_DAYS = 7
        const val GLOBAL_DEFER_RELOAD_DELAY_MILLIS = 100
        const val GLOBAL_QUERY_CHUNK_SIZE = 20

        private const val DEFAULT_EXPAND_LINKS = false
        private const val DEFAULT_EXPAND_NOTES = true
        private const val DEFAULT_SYNC_DIRECTORY = "/.laano_sync"
        private const val DEFAULT_SYNC_UPLOAD_TO_EMPTY = true
        private const val DEFAULT_SYNC_PROTECT_LOCAL = true
        private const val DEFAULT_CLIPBOARD_LINK_GET_METADATA = true
        private const val DEFAULT_CLIPBOARD_LINK_FOLLOW = false
        private const val DEFAULT_CLIPBOARD_FILL_IN_FORMS = true
        private const val DEFAULT_CLIPBOARD_PARAMETER_WHITE_LIST = "id, page"

        private const val SETTING_LAST_SYNC_TIME = "LAST_SYNC_TIME"
        private const val SETTING_LAST_LINKS_SYNC_TIME = "LAST_LINKS_SYNC_TIME"
        private const val SETTING_LAST_FAVORITES_SYNC_TIME = "LAST_FAVORITES_SYNC_TIME"
        private const val SETTING_LAST_NOTES_SYNC_TIME = "LAST_NOTES_SYNC_TIME"
        private const val SETTING_SYNC_STATUS = "SYNC_STATUS"
        private const val SETTING_LINK_FILTER_ID = "LINK_FILTER"
        private const val SETTING_FAVORITE_FILTER_ID = "FAVORITE_FILTER"
        private const val SETTING_NOTE_FILTER_ID = "NOTE_FILTER"
        private const val SETTING_SHOW_CONFLICT_RESOLUTION_WARNING =
            "SHOW_CONFLICT_RESOLUTION_WARNING"
        private const val SETTING_SHOW_FILL_IN_FORM_INFO = "SHOW_FILL_IN_FORM_INFO"
        private const val SETTING_NOTES_LAYOUT_MODE_READING = "NOTES_LAYOUT_MODE_READING"

        private const val DEFAULT_LAST_SYNC_TIME: Long = 0
        private const val DEFAULT_SYNC_STATUS = SyncAdapter.SYNC_STATUS_UNKNOWN
        @JvmField
        val DEFAULT_FILTER_TYPE = FilterType.ALL
        private val DEFAULT_LAST_SYNCED_ETAG: String? = null
        private val DEFAULT_LINK_FILTER_ID: String? = null
        private val DEFAULT_FAVORITE_FILTER_ID: String? = null
        private val DEFAULT_NOTE_FILTER_ID: String? = null
        private const val DEFAULT_SHOW_CONFLICT_RESOLUTION_WARNING = true
        private const val DEFAULT_SHOW_FILL_IN_FORM_INFO = true
        private const val DEFAULT_NOTES_LAYOUT_MODE_READING = false

        private val EMPTY_STRING_ARRAY = arrayOf<String>()
    }
}