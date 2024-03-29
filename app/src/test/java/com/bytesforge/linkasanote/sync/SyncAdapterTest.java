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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalLinks;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.data.source.local.LocalSyncResults;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;

@RunWith(MockitoJUnitRunner.class)
public class SyncAdapterTest {

    @Mock
    Context context;

    @Mock
    Settings settings;

    @Mock
    Account account;

    @Mock
    ContentProviderClient provider;

    @Mock
    SyncResult syncResult;

    @Mock
    AccountManager accountManager;

    @Mock
    SyncNotifications syncNotifications;

    @Mock
    LocalSyncResults localSyncResults;

    @Mock
    LocalLinks<Link> localLinks;

    @Mock
    CloudItem<Link> cloudLinks;

    @Mock
    LocalFavorites<Favorite> localFavorites;

    @Mock
    CloudItem<Favorite> cloudFavorites;

    @Mock
    LocalNotes<Note> localNotes;

    @Mock
    CloudItem<Note> cloudNotes;

    @Mock
    OwnCloudClient ownCloudClient;

    @Mock
    Resources resources;

    @Mock
    Bundle extras;

    @Captor
    ArgumentCaptor<SyncState> syncStateCaptor;

    private static MockedStatic<Log> mockedLog;
    private static MockedStatic<CloudUtils> mockedCloudUtils;
    private static MockedStatic<Uri> mockedUri;

    private SyncAdapter syncAdapter;

    private static final String E_TAGL = "abcdefghigklmnopqrstuvwxwz";
    private static final String E_TAGC = "zwxwvutsrqponmlkgihgfedcba";
    private static final String SERVER_URL = "https://demo.nextcloud.com";
    private static final String USERNAME = "demo";
    private static final String ACCOUNT_NAME = USERNAME + "@" + SERVER_URL;
    private static final String REMOTE_PATH = "/cloud/path";

    @BeforeClass
    public static void init() {
        mockedLog = Mockito.mockStatic(Log.class);
        mockedCloudUtils = Mockito.mockStatic(CloudUtils.class);
        mockedUri = Mockito.mockStatic(Uri.class);
    }

    @AfterClass
    public static void close() {
        mockedLog.close();
        mockedCloudUtils.close();
        mockedUri.close();
    }

    @Before
    public void setupSyncAdapter() {
        MockitoAnnotations.openMocks(this);

        when(context.getResources()).thenReturn(resources);
        when(resources.getString(any(int.class)))
                .thenAnswer((Answer<String>) invocation -> "Mocked");

        mockedCloudUtils.when(() -> CloudUtils.getOwnCloudClient(eq(account), any(Context.class)))
                .thenAnswer((Answer<OwnCloudClient>) invocation -> ownCloudClient);
        mockedCloudUtils.when(() -> CloudUtils.getAccountName(eq(account)))
                .thenAnswer((Answer<String>) invocation -> ACCOUNT_NAME);
        mockedCloudUtils.when(() -> CloudUtils.updateUserProfile(
                eq(account), any(OwnCloudClient.class), any(AccountManager.class)))
                .thenAnswer((Answer<Boolean>) invocation -> true);

        when(cloudFavorites.getDataSourceETag(ownCloudClient))
                .thenAnswer((Answer<String>) invocation -> E_TAGL);
        when(cloudFavorites.isCloudDataSourceChanged(E_TAGL))
                .thenAnswer((Answer<Boolean>) invocation -> true);
        // TODO: check why that's unnecessary
        //when(localFavorites.resetSyncState()).thenReturn(Single.just(0));
        //when(localFavorites.isConflicted()).thenReturn(Single.just(false));

        syncAdapter = new SyncAdapter(context, settings, true, accountManager, syncNotifications,
                localSyncResults, localLinks, cloudLinks, localFavorites, cloudFavorites,
                localNotes, cloudNotes);
    }

    @Test
    public void newLocalFavorite_goesToUploadThenChangesStateToSynced() {
        Favorite favorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", false, TestUtils.TAGS);
        assertNotNull(favorite);
        String favoriteId = favorite.getId();

        setLocalFavorites(localFavorites, singletonList(favorite));
        setCloudFavorites(cloudFavorites, Collections.emptyList());
        RemoteOperationResult result =
                new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
        ArrayList<Object> data = new ArrayList<>();
        JsonFile file = new JsonFile(REMOTE_PATH);
        file.setETag(E_TAGC);
        data.add(file);
        result.setData(data);
        // Upload
        when(cloudFavorites.upload(any(Favorite.class), eq(ownCloudClient)))
                .thenReturn(Single.just(result));
        when(localFavorites.update(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.just(true));
        when(localSyncResults.cleanup()).thenReturn(Single.just(0));
        when(localFavorites.logSyncResult(any(long.class), eq(favoriteId),
                any(LocalContract.SyncResultEntry.Result.class))).thenReturn(Single.just(true));

        syncAdapter.onPerformSync(account, extras, "", provider, syncResult);
        verify(cloudFavorites).upload(eq(favorite), eq(ownCloudClient));
        verify(localFavorites).update(eq(favoriteId), syncStateCaptor.capture());
        assertTrue(syncStateCaptor.getValue().isSynced());
        verify(syncNotifications).sendSyncBroadcast(
                any(String.class), eq(SyncNotifications.STATUS_UPLOADED),
                any(String.class), any(int.class));
        //assertEquals(syncAdapter.getFailsCount(), 0); TODO: capture syncItem and check
    }

    @Test
    public void newCloudFavorite_goesToLocalWithSyncedState() {
        Favorite favorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", false, TestUtils.TAGS);
        assertNotNull(favorite);
        String favoriteId = favorite.getId();
        SyncState state = new SyncState(E_TAGC, SyncState.State.SYNCED);
        Favorite cloudFavorite = new Favorite(favorite, state);

        setLocalFavorites(localFavorites, Collections.emptyList());
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        // Download
        when(cloudFavorites.download(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.just(cloudFavorite));
        when(localFavorites.save(eq(cloudFavorite))).thenReturn(Single.just(true));
        when(localSyncResults.cleanup()).thenReturn(Single.just(0));
        when(localFavorites.logSyncResult(any(long.class), eq(favoriteId),
                any(LocalContract.SyncResultEntry.Result.class))).thenReturn(Single.just(true));

        syncAdapter.onPerformSync(account, extras, "", provider, syncResult);
        verify(cloudFavorites).download(eq(favoriteId), eq(ownCloudClient));
        verify(localFavorites).save(eq(cloudFavorite));
        verify(syncNotifications).sendSyncBroadcast(
                eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_CREATED),
                eq(favoriteId), any(int.class));
        //assertEquals(syncAdapter.getFailsCount(), 0);
    }

    @Test
    public void localUnsyncedFavorite_ifEqualUpdateLocalETag() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.UNSYNCED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", false, TestUtils.TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();
        SyncState cloudState = new SyncState(E_TAGC, SyncState.State.SYNCED); // default
        Favorite cloudFavorite = new Favorite(
                favoriteId, "Favorite", false, TestUtils.TAGS, cloudState);

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        when(cloudFavorites.download(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.just(cloudFavorite));
        when(localFavorites.update(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.just(true));
        when(localSyncResults.cleanup()).thenReturn(Single.just(0));
        when(localFavorites.logSyncResult(any(long.class), eq(favoriteId),
                any(LocalContract.SyncResultEntry.Result.class))).thenReturn(Single.just(true));

        syncAdapter.onPerformSync(account, extras, "", provider, syncResult);
        verify(cloudFavorites).download(eq(favoriteId), eq(ownCloudClient));
        verify(localFavorites).update(eq(favoriteId), syncStateCaptor.capture());
        assertTrue(syncStateCaptor.getValue().isSynced());
        assertEquals(syncStateCaptor.getValue().getETag(), E_TAGC);
        verify(syncNotifications).sendSyncBroadcast(
                eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_UPDATED),
                eq(favoriteId), any(int.class));
        //assertEquals(syncAdapter.getFailsCount(), 0);
    }

    @Test
    public void localDeletedFavoriteAgainstUpdatedCloud_ifEqualDeleteCloudAndLocal() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.DELETED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", false, TestUtils.TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();
        SyncState cloudState = new SyncState(E_TAGC, SyncState.State.SYNCED); // default
        Favorite cloudFavorite = new Favorite(
                favoriteId, "Favorite", false, TestUtils.TAGS, cloudState);

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        when(cloudFavorites.download(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.just(cloudFavorite));
        RemoteOperationResult result = new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
        when(cloudFavorites.delete(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.just(result));
        when(localFavorites.delete(eq(favoriteId))).thenReturn(Single.just(true));
        when(localSyncResults.cleanup()).thenReturn(Single.just(0));
        when(localFavorites.logSyncResult(any(long.class), eq(favoriteId),
                any(LocalContract.SyncResultEntry.Result.class))).thenReturn(Single.just(true));

        syncAdapter.onPerformSync(account, extras, "", provider, syncResult);
        verify(cloudFavorites).download(eq(favoriteId), eq(ownCloudClient));
        verify(cloudFavorites).delete(eq(favoriteId), eq(ownCloudClient));
        verify(localFavorites).delete(eq(favoriteId));
        verify(syncNotifications).sendSyncBroadcast(
                eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_DELETED),
                eq(favoriteId), any(int.class));
        //assertEquals(syncAdapter.getFailsCount(), 0);
    }

    @Test
    public void localUnsyncedFavorite_ifNotEqualLocalGoesToConflictedState() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.UNSYNCED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", false, TestUtils.TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();
        SyncState cloudState = new SyncState(E_TAGC, SyncState.State.SYNCED); // default
        Favorite cloudFavorite = new Favorite(
                favoriteId, "Favorite #2", false, TestUtils.TAGS, cloudState);

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        when(cloudFavorites.download(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.just(cloudFavorite));
        when(localFavorites.update(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.just(true));
        when(localSyncResults.cleanup()).thenReturn(Single.just(0));
        when(localFavorites.logSyncResult(any(long.class), eq(favoriteId),
                any(LocalContract.SyncResultEntry.Result.class))).thenReturn(Single.just(true));

        syncAdapter.onPerformSync(account, extras, "", provider, syncResult);
        verify(cloudFavorites).download(eq(favoriteId), eq(ownCloudClient));
        verify(localFavorites).update(eq(favoriteId), syncStateCaptor.capture());
        assertTrue(syncStateCaptor.getValue().isConflicted());
        verify(syncNotifications).sendSyncBroadcast(
                eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_UPDATED),
                eq(favoriteId), any(int.class));
        //assertEquals(syncAdapter.getFailsCount(), 0);
    }

    @Test
    public void cloudFavoriteUpdatedWhenLocalSynced_downloadCloudAndUpdateLocal() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.SYNCED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", false, TestUtils.TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();
        SyncState cloudState = new SyncState(E_TAGC, SyncState.State.SYNCED); // default
        Favorite cloudFavorite = new Favorite(
                favoriteId, "Favorite #2", false, TestUtils.TAGS, cloudState);

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        when(cloudFavorites.download(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.just(cloudFavorite));
        when(localFavorites.save(eq(cloudFavorite))).thenReturn(Single.just(true));
        when(localSyncResults.cleanup()).thenReturn(Single.just(0));
        when(localFavorites.logSyncResult(any(long.class), eq(favoriteId),
                any(LocalContract.SyncResultEntry.Result.class))).thenReturn(Single.just(true));

        syncAdapter.onPerformSync(account, extras, "", provider, syncResult);
        verify(cloudFavorites).download(eq(favoriteId), eq(ownCloudClient));
        verify(localFavorites).save(eq(cloudFavorite));
        verify(syncNotifications).sendSyncBroadcast(
                eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_UPDATED),
                eq(favoriteId), any(int.class));
        //assertEquals(syncAdapter.getFailsCount(), 0);
    }

    @Test
    public void localNeverSyncedFavorite_deleteLocalOnly() {
        SyncState localState = new SyncState(SyncState.State.DELETED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", false, TestUtils.TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, Collections.emptyList());
        when(localFavorites.delete(eq(favoriteId))).thenReturn(Single.just(true));
        when(localSyncResults.cleanup()).thenReturn(Single.just(0));
        when(localFavorites.logSyncResult(any(long.class), eq(favoriteId),
                any(LocalContract.SyncResultEntry.Result.class))).thenReturn(Single.just(true));

        syncAdapter.onPerformSync(account, extras, "", provider, syncResult);
        verify(localFavorites).delete(eq(favoriteId));
        verify(syncNotifications).sendSyncBroadcast(
                eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_DELETED),
                eq(favoriteId), any(int.class));
        //assertEquals(syncAdapter.getFailsCount(), 0);
    }

    @Test
    public void localDeletedFavoriteWithCloudETag_deleteCloudAndLocal() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.DELETED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", false, TestUtils.TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();
        SyncState cloudState = new SyncState(E_TAGL, SyncState.State.SYNCED); // default
        Favorite cloudFavorite = new Favorite(
                favoriteId, "Favorite", false, TestUtils.TAGS, cloudState);

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        RemoteOperationResult result = new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
        when(cloudFavorites.delete(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.just(result));
        when(localFavorites.delete(eq(favoriteId))).thenReturn(Single.just(true));
        when(localSyncResults.cleanup()).thenReturn(Single.just(0));
        when(localFavorites.logSyncResult(any(long.class), eq(favoriteId),
                any(LocalContract.SyncResultEntry.Result.class))).thenReturn(Single.just(true));

        syncAdapter.onPerformSync(account, extras, "", provider, syncResult);
        verify(cloudFavorites).delete(eq(favoriteId), eq(ownCloudClient));
        verify(localFavorites).delete(eq(favoriteId));
        verify(syncNotifications).sendSyncBroadcast(
                eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_DELETED),
                eq(favoriteId), any(int.class));
        //assertEquals(syncAdapter.getFailsCount(), 0);
    }

    @Test
    public void cloudDeletedFavoriteAgainstLocalSynced_deleteLocal() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.SYNCED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", false, TestUtils.TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, Collections.emptyList());
        when(localFavorites.delete(eq(favoriteId))).thenReturn(Single.just(true));
        when(localSyncResults.cleanup()).thenReturn(Single.just(0));
        when(localFavorites.logSyncResult(any(long.class), eq(favoriteId),
                any(LocalContract.SyncResultEntry.Result.class))).thenReturn(Single.just(true));

        syncAdapter.onPerformSync(account, extras, "", provider, syncResult);
        verify(localFavorites).delete(eq(favoriteId));
        verify(syncNotifications).sendSyncBroadcast(
                eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_DELETED),
                eq(favoriteId), any(int.class));
        //assertEquals(syncAdapter.getFailsCount(), 0);
    }

    @Test
    public void cloudDeletedFavoriteAgainstLocalUnsynced_localGoesToConflictedState() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.UNSYNCED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", false, TestUtils.TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, Collections.emptyList());
        when(localFavorites.update(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.just(true));
        when(localSyncResults.cleanup()).thenReturn(Single.just(0));
        when(localFavorites.logSyncResult(any(long.class), eq(favoriteId),
                any(LocalContract.SyncResultEntry.Result.class))).thenReturn(Single.just(true));

        syncAdapter.onPerformSync(account, extras, "", provider, syncResult);
        verify(localFavorites).update(eq(favoriteId), syncStateCaptor.capture());
        assertFalse(syncStateCaptor.getValue().isDeleted());
        assertTrue(syncStateCaptor.getValue().isConflicted());
        verify(syncNotifications).sendSyncBroadcast(
                eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_UPDATED),
                eq(favoriteId), any(int.class));
        //assertEquals(syncAdapter.getFailsCount(), 0);
    }

    @Test
    public void cloudDeletedFavoriteAgainstLocalDeleted_deleteLocal() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.DELETED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", false, TestUtils.TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, Collections.emptyList());
        when(localFavorites.delete(eq(favoriteId))).thenReturn(Single.just(true));
        when(localSyncResults.cleanup()).thenReturn(Single.just(0));
        when(localFavorites.logSyncResult(any(long.class), eq(favoriteId),
                any(LocalContract.SyncResultEntry.Result.class))).thenReturn(Single.just(true));

        syncAdapter.onPerformSync(account, extras, "", provider, syncResult);
        verify(localFavorites).delete(eq(favoriteId));
        verify(syncNotifications).sendSyncBroadcast(
                eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_DELETED),
                eq(favoriteId), any(int.class));
        //assertEquals(syncAdapter.getFailsCount(), 0);
    }

    @Test
    public void localUnsyncedFavoriteWithCloudETag_uploadIfNotDuplicated() {
        SyncState localState = new SyncState(E_TAGC, SyncState.State.UNSYNCED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite #2", false, TestUtils.TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();
        SyncState cloudState = new SyncState(E_TAGC, SyncState.State.SYNCED); // default
        Favorite cloudFavorite = new Favorite(
                favoriteId, "Favorite", false, TestUtils.TAGS, cloudState);

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        // Upload
        RemoteOperationResult result =
                new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
        ArrayList<Object> data = new ArrayList<>();
        JsonFile file = new JsonFile(REMOTE_PATH);
        file.setETag(E_TAGC);
        data.add(file);
        result.setData(data);
        when(cloudFavorites.upload(any(Favorite.class), eq(ownCloudClient)))
                .thenReturn(Single.just(result));
        when(localFavorites.update(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.just(true));
        when(localSyncResults.cleanup()).thenReturn(Single.just(0));
        when(localFavorites.logSyncResult(any(long.class), eq(favoriteId),
                any(LocalContract.SyncResultEntry.Result.class))).thenReturn(Single.just(true));

        syncAdapter.onPerformSync(account, extras, "", provider, syncResult);
        verify(cloudFavorites).upload(eq(localFavorite), eq(ownCloudClient));
        verify(localFavorites).update(eq(favoriteId), syncStateCaptor.capture());
        assertTrue(syncStateCaptor.getValue().isSynced());
        verify(syncNotifications).sendSyncBroadcast(
                any(String.class), eq(SyncNotifications.STATUS_UPLOADED),
                any(String.class), any(int.class));
        //assertEquals(syncAdapter.getFailsCount(), 0);
    }

    // Helpers

    private void setLocalFavorites(LocalFavorites localFavorites, List<Favorite> favorites) {
        when(localFavorites.getAll()).thenReturn(Observable.fromIterable(favorites));
        List<String> localFavoriteIds = new ArrayList<>(favorites.size());
        for (Favorite favorite : favorites) {
            localFavoriteIds.add(favorite.getId());
        }
        when(localFavorites.getIds()).thenReturn(Observable.fromIterable(localFavoriteIds));
    }

    private void setCloudFavorites(CloudItem<Favorite> cloudFavorites, List<Favorite> favorites) {
        Map<String, String> cloudDataSourceMap = new HashMap<>(favorites.size());
        for (Favorite favorite : favorites) {
            cloudDataSourceMap.put(favorite.getId(), favorite.getETag());
        }
        when(cloudFavorites.getDataSourceMap(eq(ownCloudClient))).thenReturn(cloudDataSourceMap);
    }

    private List<Favorite> singletonList(Favorite favorite) {
        return new ArrayList<>(Collections.singletonList(favorite));
    }
}