package com.bytesforge.linkasanote.sync;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.cloud.CloudFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.Observable;
import io.reactivex.Single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class, CloudUtils.class})
public class SyncAdapterTest {

    @Mock
    Context context;

    @Mock
    Settings settings;

    @Mock
    AccountManager accountManager;

    @Mock
    SyncNotifications syncNotifications;

    @Mock
    LocalFavorites localFavorites;

    @Mock
    CloudFavorites cloudFavorites;

    @Mock
    OwnCloudClient ownCloudClient;

    @Mock
    Resources resources;

    @Captor
    ArgumentCaptor<SyncState> syncStateCaptor;

    private SyncAdapter syncAdapter;

    private static final String E_TAGL = "abcdefghigklmnopqrstuvwxwz";
    private static final String E_TAGC = "zwxwvutsrqponmlkgihgfedcba";
    private static final String SERVER_URL = "https://demo.nextcloud.com";
    private static final String USERNAME = "demo";
    private static final String ACCOUNT_NAME = USERNAME + "@" + SERVER_URL;
    private static final String REMOTE_PATH = "/cloud/path";

    @Before
    public void setupSyncAdapter() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Log.class);
        PowerMockito.mockStatic(CloudUtils.class);

        when(context.getResources()).thenReturn(resources);
        when(CloudUtils.getOwnCloudClient(isNull(), any(Context.class))).thenReturn(ownCloudClient);
        when(CloudUtils.getAccountName(isNull())).thenReturn(ACCOUNT_NAME);
        when(cloudFavorites.getDataSourceETag(ownCloudClient)).thenReturn(E_TAGL);
        when(cloudFavorites.isCloudDataSourceChanged(E_TAGL)).thenReturn(true);
        when(localFavorites.resetFavoritesSyncState()).thenReturn(Single.fromCallable(() -> 0));
        when(localFavorites.isConflictedFavorites()).thenReturn(Single.fromCallable(() -> false));

        syncAdapter = new SyncAdapter(context, settings, true,
                accountManager, syncNotifications, localFavorites, cloudFavorites);
    }

    @Test
    public void newLocalFavorite_goesToUploadThenChangesStateToSynced() {
        Favorite favorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", TestUtils.FAVORITE_TAGS);
        assertNotNull(favorite);
        String favoriteId = favorite.getId();

        setLocalFavorites(localFavorites, singletonList(favorite));
        setCloudFavorites(cloudFavorites, Collections.emptyList());
        // Upload
        RemoteOperationResult result =
                new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
        ArrayList<Object> data = new ArrayList<>();
        JsonFile file = new JsonFile(REMOTE_PATH);
        file.setETag(E_TAGC);
        data.add(file);
        result.setData(data);
        when(cloudFavorites.uploadFavorite(any(Favorite.class), eq(ownCloudClient)))
                .thenReturn(Single.fromCallable(() -> result));
        when(localFavorites.updateFavorite(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.fromCallable(() -> 1));

        syncAdapter.onPerformSync(null, null, null, null, null);
        verify(cloudFavorites).uploadFavorite(eq(favorite), eq(ownCloudClient));
        verify(localFavorites).updateFavorite(eq(favoriteId), syncStateCaptor.capture());
        assertEquals(syncStateCaptor.getValue().isSynced(), true);
        verify(syncNotifications).sendSyncBroadcast(
                any(String.class), eq(SyncNotifications.STATUS_UPLOADED), any(String.class));
        assertEquals(syncAdapter.getFavoriteFailsCount(), 0);
    }

    @Test
    public void newCloudFavorite_goesToLocalWithSyncedState() {
        Favorite favorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", TestUtils.FAVORITE_TAGS);
        assertNotNull(favorite);
        String favoriteId = favorite.getId();
        SyncState state = new SyncState(E_TAGC, SyncState.State.SYNCED);
        Favorite cloudFavorite = new Favorite(favorite, state);

        setLocalFavorites(localFavorites, Collections.emptyList());
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        // Download
        when(cloudFavorites.downloadFavorite(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.fromCallable(() -> cloudFavorite));
        when(localFavorites.saveFavorite(eq(cloudFavorite)))
                .thenReturn(Single.fromCallable(() -> 1L));

        syncAdapter.onPerformSync(null, null, null, null, null);
        verify(cloudFavorites).downloadFavorite(eq(favoriteId), eq(ownCloudClient));
        verify(localFavorites).saveFavorite(eq(cloudFavorite));
        verify(syncNotifications).sendSyncBroadcast(eq(SyncNotifications.ACTION_SYNC_FAVORITES),
               eq(SyncNotifications.STATUS_CREATED), eq(favoriteId));
        assertEquals(syncAdapter.getFavoriteFailsCount(), 0);
    }

    @Test
    public void localUnsyncedFavorite_ifEqualUpdateLocalETag() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.UNSYNCED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", TestUtils.FAVORITE_TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();
        SyncState cloudState = new SyncState(E_TAGC, SyncState.State.SYNCED); // default
        Favorite cloudFavorite = new Favorite(
                favoriteId, "Favorite", TestUtils.FAVORITE_TAGS, cloudState);

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        when(cloudFavorites.downloadFavorite(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.fromCallable(() -> cloudFavorite));
        when(localFavorites.updateFavorite(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.fromCallable(() -> 1));

        syncAdapter.onPerformSync(null, null, null, null, null);
        verify(cloudFavorites).downloadFavorite(eq(favoriteId), eq(ownCloudClient));
        verify(localFavorites).updateFavorite(eq(favoriteId), syncStateCaptor.capture());
        assertEquals(syncStateCaptor.getValue().isSynced(), true);
        assertEquals(syncStateCaptor.getValue().getETag(), E_TAGC);
        verify(syncNotifications).sendSyncBroadcast(eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_UPDATED), eq(favoriteId));
        assertEquals(syncAdapter.getFavoriteFailsCount(), 0);
    }

    @Test
    public void localDeletedFavoriteAgainstUpdatedCloud_ifEqualDeleteCloudAndLocal() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.DELETED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", TestUtils.FAVORITE_TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();
        SyncState cloudState = new SyncState(E_TAGC, SyncState.State.SYNCED); // default
        Favorite cloudFavorite = new Favorite(
                favoriteId, "Favorite", TestUtils.FAVORITE_TAGS, cloudState);

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        when(cloudFavorites.downloadFavorite(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.fromCallable(() -> cloudFavorite));
        RemoteOperationResult result = new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
        when(cloudFavorites.deleteFavorite(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.fromCallable(() -> result));
        when(localFavorites.deleteFavorite(eq(favoriteId))).thenReturn(Single.fromCallable(() -> 1));

        syncAdapter.onPerformSync(null, null, null, null, null);
        verify(cloudFavorites).downloadFavorite(eq(favoriteId), eq(ownCloudClient));
        verify(cloudFavorites).deleteFavorite(eq(favoriteId), eq(ownCloudClient));
        verify(localFavorites).deleteFavorite(eq(favoriteId));
        verify(syncNotifications).sendSyncBroadcast(eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_DELETED), eq(favoriteId));
        assertEquals(syncAdapter.getFavoriteFailsCount(), 0);
    }

    @Test
    public void localUnsyncedFavorite_ifNotEqualLocalGoesToConflictedState() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.UNSYNCED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", TestUtils.FAVORITE_TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();
        SyncState cloudState = new SyncState(E_TAGC, SyncState.State.SYNCED); // default
        Favorite cloudFavorite = new Favorite(
                favoriteId, "Favorite #2", TestUtils.FAVORITE_TAGS, cloudState);

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        when(cloudFavorites.downloadFavorite(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.fromCallable(() -> cloudFavorite));
        when(localFavorites.updateFavorite(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.fromCallable(() -> 1));

        syncAdapter.onPerformSync(null, null, null, null, null);
        verify(cloudFavorites).downloadFavorite(eq(favoriteId), eq(ownCloudClient));
        verify(localFavorites).updateFavorite(eq(favoriteId), syncStateCaptor.capture());
        assertEquals(syncStateCaptor.getValue().isConflicted(), true);
        verify(syncNotifications).sendSyncBroadcast(eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_UPDATED), eq(favoriteId));
        assertEquals(syncAdapter.getFavoriteFailsCount(), 0);
    }

    @Test
    public void cloudFavoriteUpdatedWhenLocalSynced_downloadCloudAndUpdateLocal() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.SYNCED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", TestUtils.FAVORITE_TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();
        SyncState cloudState = new SyncState(E_TAGC, SyncState.State.SYNCED); // default
        Favorite cloudFavorite = new Favorite(
                favoriteId, "Favorite #2", TestUtils.FAVORITE_TAGS, cloudState);

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        when(cloudFavorites.downloadFavorite(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.fromCallable(() -> cloudFavorite));
        when(localFavorites.saveFavorite(eq(cloudFavorite))).thenReturn(Single.fromCallable(() -> 1L));
        syncAdapter.onPerformSync(null, null, null, null, null);
        verify(cloudFavorites).downloadFavorite(eq(favoriteId), eq(ownCloudClient));
        verify(localFavorites).saveFavorite(eq(cloudFavorite));
        verify(syncNotifications).sendSyncBroadcast(eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_UPDATED), eq(favoriteId));
        assertEquals(syncAdapter.getFavoriteFailsCount(), 0);
    }

    @Test
    public void localNeverSyncedFavorite_deleteLocalOnly() {
        SyncState localState = new SyncState(SyncState.State.DELETED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", TestUtils.FAVORITE_TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, Collections.emptyList());
        when(localFavorites.deleteFavorite(eq(favoriteId))).thenReturn(Single.fromCallable(() -> 1));

        syncAdapter.onPerformSync(null, null, null, null, null);
        verify(localFavorites).deleteFavorite(eq(favoriteId));
        verify(syncNotifications).sendSyncBroadcast(eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_DELETED), eq(favoriteId));
        assertEquals(syncAdapter.getFavoriteFailsCount(), 0);
    }

    @Test
    public void localDeletedFavoriteWithCloudETag_deleteCloudAndLocal() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.DELETED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", TestUtils.FAVORITE_TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();
        SyncState cloudState = new SyncState(E_TAGL, SyncState.State.SYNCED); // default
        Favorite cloudFavorite = new Favorite(
                favoriteId, "Favorite", TestUtils.FAVORITE_TAGS, cloudState);

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, singletonList(cloudFavorite));
        RemoteOperationResult result = new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
        when(cloudFavorites.deleteFavorite(eq(favoriteId), eq(ownCloudClient)))
                .thenReturn(Single.fromCallable(() -> result));
        when(localFavorites.deleteFavorite(eq(favoriteId))).thenReturn(Single.fromCallable(() -> 1));

        syncAdapter.onPerformSync(null, null, null, null, null);
        verify(cloudFavorites).deleteFavorite(eq(favoriteId), eq(ownCloudClient));
        verify(localFavorites).deleteFavorite(eq(favoriteId));
        verify(syncNotifications).sendSyncBroadcast(eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_DELETED), eq(favoriteId));
        assertEquals(syncAdapter.getFavoriteFailsCount(), 0);
    }

    @Test
    public void cloudDeletedFavoriteAgainstLocalSynced_deleteLocal() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.SYNCED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", TestUtils.FAVORITE_TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, Collections.emptyList());
        when(localFavorites.deleteFavorite(eq(favoriteId))).thenReturn(Single.fromCallable(() -> 1));

        syncAdapter.onPerformSync(null, null, null, null, null);
        verify(localFavorites).deleteFavorite(eq(favoriteId));
        verify(syncNotifications).sendSyncBroadcast(eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_DELETED), eq(favoriteId));
        assertEquals(syncAdapter.getFavoriteFailsCount(), 0);
    }

    @Test
    public void cloudDeletedFavoriteAgainstLocalUnsynced_localGoesToConflictedState() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.UNSYNCED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", TestUtils.FAVORITE_TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, Collections.emptyList());
        when(localFavorites.updateFavorite(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.fromCallable(() -> 1));

        syncAdapter.onPerformSync(null, null, null, null, null);
        verify(localFavorites).updateFavorite(eq(favoriteId), syncStateCaptor.capture());
        assertEquals(syncStateCaptor.getValue().isDeleted(), false);
        assertEquals(syncStateCaptor.getValue().isConflicted(), true);
        verify(syncNotifications).sendSyncBroadcast(eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_UPDATED), eq(favoriteId));
        assertEquals(syncAdapter.getFavoriteFailsCount(), 0);
    }

    @Test
    public void cloudDeletedFavoriteAgainstLocalDeleted_localGoesToConflictedState() {
        SyncState localState = new SyncState(E_TAGL, SyncState.State.DELETED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite", TestUtils.FAVORITE_TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();

        setLocalFavorites(localFavorites, singletonList(localFavorite));
        setCloudFavorites(cloudFavorites, Collections.emptyList());
        when(localFavorites.updateFavorite(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.fromCallable(() -> 1));

        syncAdapter.onPerformSync(null, null, null, null, null);
        verify(localFavorites).updateFavorite(eq(favoriteId), syncStateCaptor.capture());
        assertEquals(syncStateCaptor.getValue().isDeleted(), true);
        assertEquals(syncStateCaptor.getValue().isConflicted(), true);
        verify(syncNotifications).sendSyncBroadcast(eq(SyncNotifications.ACTION_SYNC_FAVORITES),
                eq(SyncNotifications.STATUS_UPDATED), eq(favoriteId));
        assertEquals(syncAdapter.getFavoriteFailsCount(), 0);
    }

    @Test
    public void localUnsyncedFavoriteWithCloudETag_uploadIfNotDuplicated() {
        SyncState localState = new SyncState(E_TAGC, SyncState.State.UNSYNCED);
        Favorite localFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'A', "Favorite #2", TestUtils.FAVORITE_TAGS, localState);
        assertNotNull(localFavorite);
        String favoriteId = localFavorite.getId();
        SyncState cloudState = new SyncState(E_TAGC, SyncState.State.SYNCED); // default
        Favorite cloudFavorite = new Favorite(
                favoriteId, "Favorite", TestUtils.FAVORITE_TAGS, cloudState);

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
        when(cloudFavorites.uploadFavorite(any(Favorite.class), eq(ownCloudClient)))
                .thenReturn(Single.fromCallable(() -> result));
        when(localFavorites.updateFavorite(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.fromCallable(() -> 1));

        syncAdapter.onPerformSync(null, null, null, null, null);
        verify(cloudFavorites).uploadFavorite(eq(localFavorite), eq(ownCloudClient));
        verify(localFavorites).updateFavorite(eq(favoriteId), syncStateCaptor.capture());
        assertEquals(syncStateCaptor.getValue().isSynced(), true);
        verify(syncNotifications).sendSyncBroadcast(
                any(String.class), eq(SyncNotifications.STATUS_UPLOADED), any(String.class));
        assertEquals(syncAdapter.getFavoriteFailsCount(), 0);
    }

    // Helpers

    private void setLocalFavorites(LocalFavorites localFavorites, List<Favorite> favorites) {
        when(localFavorites.getFavorites()).thenReturn(Observable.fromIterable(favorites));
        Set<String> localFavoriteIds = favorites.stream()
                .map(Favorite::getId).collect(Collectors.toSet());
        when(localFavorites.getFavoriteIds()).thenReturn(Observable.fromIterable(localFavoriteIds));
    }

    private void setCloudFavorites(CloudFavorites cloudFavorites, List<Favorite> favorites) {
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