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

package com.bytesforge.linkasanote.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.StringRes;
import android.util.Log;
import android.widget.Toast;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalLinks;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.data.source.local.LocalSyncResults;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.google.common.base.Joiner;
import com.owncloud.android.lib.common.OwnCloudClient;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;

import static java.lang.System.currentTimeMillis;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = SyncAdapter.class.getSimpleName();

    public static final int SYNC_STATUS_UNKNOWN = 0;
    public static final int SYNC_STATUS_SYNCED = 1;
    public static final int SYNC_STATUS_UNSYNCED = 2;
    public static final int SYNC_STATUS_ERROR = 3;
    public static final int SYNC_STATUS_CONFLICT = 4;

    public static final String SYNC_MANUAL_MODE = "MANUAL_MODE";

    private final Context context;
    private final Settings settings;
    private final SyncNotifications syncNotifications;
    private final LocalSyncResults localSyncResults;
    private final LocalLinks<Link> localLinks;
    private final CloudItem<Link> cloudLinks;
    private final LocalFavorites<Favorite> localFavorites;
    private final CloudItem<Favorite> cloudFavorites;
    private final LocalNotes<Note> localNotes;
    private final CloudItem<Note> cloudNotes;
    private final AccountManager accountManager;
    private final Resources resources;
    private boolean manualMode;

    // NOTE: Note should contain linkId to notify related Link
    public SyncAdapter(
            Context context, Settings settings, boolean autoInitialize,
            AccountManager accountManager, SyncNotifications syncNotifications,
            LocalSyncResults localSyncResults,
            LocalLinks<Link> localLinks, CloudItem<Link> cloudLinks,
            LocalFavorites<Favorite> localFavorites, CloudItem<Favorite> cloudFavorites,
            LocalNotes<Note> localNotes, CloudItem<Note> cloudNotes) {
        super(context, autoInitialize);
        this.context = context;
        this.settings = settings;
        this.accountManager = accountManager;
        this.syncNotifications = syncNotifications;
        this.localSyncResults = localSyncResults;
        this.localLinks = localLinks;
        this.cloudLinks = cloudLinks;
        this.localFavorites = localFavorites;
        this.cloudFavorites = cloudFavorites;
        this.localNotes = localNotes;
        this.cloudNotes = cloudNotes;
        resources = context.getResources();
    }

    @Override
    public void onPerformSync(
            Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        manualMode = extras.getBoolean(SYNC_MANUAL_MODE, false);
        long started = currentTimeMillis();
        syncNotifications.setAccountName(CloudUtils.getAccountName(account));

        OwnCloudClient ocClient = CloudUtils.getOwnCloudClient(account, context);
        if (ocClient == null) {
            syncNotifications.notifyFailedSynchronization(
                    resources.getString(R.string.sync_adapter_title_failed_login),
                    resources.getString(R.string.sync_adapter_text_failed_login));
            return;
        }

        //Start
        syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC, SyncNotifications.STATUS_SYNC_START);
        boolean updated = CloudUtils.updateUserProfile(account, ocClient, accountManager);
        if (!updated) {
            syncNotifications.notifyFailedSynchronization(
                    resources.getString(R.string.sync_adapter_title_failed_cloud),
                    resources.getString(R.string.sync_adapter_text_failed_cloud_profile));
            return;
        }
        int numRows = localSyncResults.cleanup().blockingGet();
        Log.d(TAG, "onPerformSync(): cleanupSyncResults() [" + numRows + "]");

        boolean fatalError;
        SyncItemResult favoritesSyncResult, linksSyncResult = null, notesSyncResult = null;

        // Favorites
        syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC_FAVORITES, SyncNotifications.STATUS_SYNC_START);
        SyncItem<Favorite> syncFavorites = new SyncItem<>(ocClient, localFavorites, cloudFavorites,
                syncNotifications, SyncNotifications.ACTION_SYNC_FAVORITES,
                settings.isSyncUploadToEmpty(), settings.isSyncProtectLocal(), started);
        favoritesSyncResult = syncFavorites.sync();
        settings.updateLastFavoritesSyncTime();
        syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC_FAVORITES, SyncNotifications.STATUS_SYNC_STOP);
        fatalError = favoritesSyncResult.isFatal();

        // Links
        if (!fatalError) {
            syncNotifications.sendSyncBroadcast(
                    SyncNotifications.ACTION_SYNC_LINKS, SyncNotifications.STATUS_SYNC_START);
            SyncItem<Link> syncLinks = new SyncItem<>(ocClient, localLinks, cloudLinks,
                    syncNotifications, SyncNotifications.ACTION_SYNC_LINKS,
                    settings.isSyncUploadToEmpty(), settings.isSyncProtectLocal(), started);
            linksSyncResult = syncLinks.sync();
            settings.updateLastLinksSyncTime();
            syncNotifications.sendSyncBroadcast(
                    SyncNotifications.ACTION_SYNC_LINKS, SyncNotifications.STATUS_SYNC_STOP);
            fatalError = linksSyncResult.isFatal();
        }

        // Notes
        if (!fatalError) {
            syncNotifications.sendSyncBroadcast(
                    SyncNotifications.ACTION_SYNC_NOTES, SyncNotifications.STATUS_SYNC_START);
            SyncItem<Note> syncNotes = new SyncItem<>(ocClient, localNotes, cloudNotes,
                    syncNotifications, SyncNotifications.ACTION_SYNC_NOTES,
                    settings.isSyncUploadToEmpty(), settings.isSyncProtectLocal(), started);
            notesSyncResult = syncNotes.sync();
            settings.updateLastNotesSyncTime();
            settings.updateLastLinksSyncTime(); // NOTE: because there are related links
            syncNotifications.sendSyncBroadcast(
                    SyncNotifications.ACTION_SYNC_NOTES, SyncNotifications.STATUS_SYNC_STOP);
            fatalError = notesSyncResult.isFatal();
        }

        // Stop
        boolean success = !fatalError && favoritesSyncResult.isSuccess()
                && linksSyncResult.isSuccess() && notesSyncResult.isSuccess();
        saveLastSyncStatus(success);
        syncNotifications.sendSyncBroadcast(
                SyncNotifications.ACTION_SYNC, SyncNotifications.STATUS_SYNC_STOP);

        // Error notifications
        if (fatalError) {
            SyncItemResult fatalResult = favoritesSyncResult;
            if (linksSyncResult != null) fatalResult = linksSyncResult;
            if (notesSyncResult != null) fatalResult = notesSyncResult;

            if (fatalResult.isDbAccessError()) {
                syncNotifications.notifyFailedSynchronization(
                        resources.getString(R.string.sync_adapter_title_failed_database),
                        resources.getString(R.string.sync_adapter_text_failed_database));
            } else if (fatalResult.isSourceNotReady()) {
                syncNotifications.notifyFailedSynchronization(
                        resources.getString(R.string.sync_adapter_title_failed_cloud),
                        resources.getString(R.string.sync_adapter_text_failed_cloud_access));
            }
        } else {
            // Fail
            List<String> failSources = new ArrayList<>();
            int linkFailsCount = linksSyncResult.getFailsCount();
            if (linkFailsCount > 0) {
                failSources.add(resources.getQuantityString(
                        R.plurals.count_links,
                        linkFailsCount, linkFailsCount));
            }
            int favoriteFailsCount = favoritesSyncResult.getFailsCount();
            if (favoriteFailsCount > 0) {
                failSources.add(resources.getQuantityString(
                        R.plurals.count_favorites,
                        favoriteFailsCount, favoriteFailsCount));
            }
            int noteFailsCount = notesSyncResult.getFailsCount();
            if (noteFailsCount > 0) {
                failSources.add(resources.getQuantityString(
                        R.plurals.count_notes,
                        noteFailsCount, noteFailsCount));
            }
            if (!failSources.isEmpty()) {
                syncNotifications.notifyFailedSynchronization(resources.getString(
                        R.string.sync_adapter_text_failed, Joiner.on(", ").join(failSources)));
            }
        }
    }

    private void saveLastSyncStatus(boolean success) {
        int syncStatus;
        if (success) {
            boolean conflictedStatus = localLinks.isConflicted()
                    .flatMap(conflicted -> conflicted ? Single.just(true) : localFavorites.isConflicted())
                    .flatMap(conflicted -> conflicted ? Single.just(true) : localNotes.isConflicted())
                    .blockingGet();
            boolean unsyncedStatus = localLinks.isUnsynced()
                    .flatMap(unsynced -> unsynced ? Single.just(true) : localFavorites.isUnsynced())
                    .flatMap(unsynced -> unsynced ? Single.just(true) : localNotes.isUnsynced())
                    .blockingGet();
            if (conflictedStatus) {
                syncStatus = SYNC_STATUS_CONFLICT;
                if (manualMode) {
                    showToast(R.string.toast_sync_conflict, Toast.LENGTH_LONG);
                }
            } else if (unsyncedStatus) {
                // NOTE: normally it should not be happened, but the chance is not zero
                syncStatus = SYNC_STATUS_UNSYNCED;
                if (manualMode) {
                    showToast(R.string.toast_sync_unsynced, Toast.LENGTH_LONG);
                }
            } else {
                syncStatus = SYNC_STATUS_SYNCED;
                if (manualMode) {
                    showToast(R.string.toast_sync_success, Toast.LENGTH_SHORT);
                }
            }
        } else {
            syncStatus = SYNC_STATUS_ERROR;
            if (manualMode) {
                showToast(R.string.toast_sync_error, Toast.LENGTH_LONG);
            }
        }
        settings.setSyncStatus(syncStatus);
    }

    private void showToast(@StringRes final int toastId, final int duration) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(context, toastId, duration).show());
    }
}
