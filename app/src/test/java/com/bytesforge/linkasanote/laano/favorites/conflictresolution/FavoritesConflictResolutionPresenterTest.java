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

package com.bytesforge.linkasanote.laano.favorites.conflictresolution;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.util.Log;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.NoSuchElementException;

import io.reactivex.Single;

@RunWith(MockitoJUnitRunner.class)
public class FavoritesConflictResolutionPresenterTest {

    @Mock
    private Repository repository;

    @Mock
    private LocalFavorites<Favorite> localFavorites;

    @Mock
    private CloudItem<Favorite> cloudFavorites;

    @Mock
    private FavoritesConflictResolutionContract.View view;

    @Mock
    private FavoritesConflictResolutionContract.ViewModel viewModel;

    @Mock
    private Settings settings;

    private static MockedStatic<Log> mockedLog;

    private static final String E_TAGL = "abcdefghigklmnopqrstuvwxwz";
    private static final String E_TAGC = "zwxwvutsrqponmlkgihgfedcba";

    private BaseSchedulerProvider schedulerProvider;
    private FavoritesConflictResolutionPresenter presenter;
    private Favorite defaultFavorite;
    private String favoriteId;

    @BeforeClass
    public static void init() {
        mockedLog = Mockito.mockStatic(Log.class);
    }

    @AfterClass
    public static void close() {
        mockedLog.close();
    }

    @Before
    public void setupFavoritesConflictResolutionPresenter() {
        MockitoAnnotations.openMocks(this);
        schedulerProvider = new ImmediateSchedulerProvider();
        favoriteId = TestUtils.KEY_PREFIX + 'A';
        defaultFavorite = new Favorite(favoriteId, "Favorite", false, TestUtils.TAGS);
        presenter = new FavoritesConflictResolutionPresenter(
                repository, settings, localFavorites, cloudFavorites,
                view, viewModel, schedulerProvider, defaultFavorite.getId());
    }

    @Test
    public void notConflictedFavorite_finishesActivityWithSuccess() {
        SyncState state = new SyncState(SyncState.State.SYNCED);
        Favorite favorite = new Favorite(defaultFavorite, state);
        when(localFavorites.get(eq(favoriteId)))
                .thenReturn(Single.just(favorite));
        presenter.subscribe();
        verify(repository).refreshFavorites();
        verify(view).finishActivity();
    }

    @Test
    public void wrongId_finishesActivityWithSuccess() {
        when(localFavorites.get(eq(favoriteId)))
                .thenReturn(Single.error(new NoSuchElementException()));
        presenter.subscribe();
        verify(repository).refreshFavorites();
        verify(view).finishActivity();
    }

    @Test
    public void databaseError_showsErrorThenTriesToLoadCloudFavorite() {
        when(localFavorites.get(eq(favoriteId)))
                .thenReturn(Single.error(new NullPointerException()));
        when(cloudFavorites.download(eq(favoriteId)))
                .thenReturn(Single.just(defaultFavorite));
        presenter.subscribe();
        verify(viewModel).showDatabaseError();
        verify(viewModel).populateCloudFavorite(eq(defaultFavorite));
    }

    @Test
    public void duplicatedFavorite_populatesToCloudThenLoadsMainToLocal() {
        SyncState state = new SyncState(E_TAGL, 1); // duplicated
        Favorite favorite = new Favorite(defaultFavorite, state);
        Favorite mainFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'B', "Favorite", false, TestUtils.TAGS);
        when(localFavorites.get(eq(favoriteId)))
                .thenReturn(Single.just(favorite));
        when(localFavorites.getMain(eq(favorite.getDuplicatedKey())))
                .thenReturn(Single.just(mainFavorite));
        when(viewModel.isCloudPopulated()).thenReturn(true);
        presenter.subscribe();
        verify(viewModel).populateCloudFavorite(eq(favorite));
        verify(viewModel).populateLocalFavorite(eq(mainFavorite));
    }

    @Test
    public void duplicatedFavoriteWithNoMainRecord_resolvesConflictAutomatically() {
        SyncState state = new SyncState(E_TAGL, 1); // duplicated
        Favorite favorite = new Favorite(defaultFavorite, state);
        String favoriteId = favorite.getId();
        when(localFavorites.get(eq(favoriteId))).thenReturn(Single.just(favorite));
        when(localFavorites.getMain(eq(favorite.getDuplicatedKey())))
                .thenReturn(Single.error(new NoSuchElementException()));
        // TODO: check why that's unnecessary
        //when(viewModel.isCloudPopulated()).thenReturn(true);
        when(localFavorites.update(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.just(true));
        presenter.subscribe();
        verify(viewModel).populateCloudFavorite(eq(favorite));
        verify(repository).refreshFavorite(eq(favoriteId));
        verify(view).finishActivity();
    }
}