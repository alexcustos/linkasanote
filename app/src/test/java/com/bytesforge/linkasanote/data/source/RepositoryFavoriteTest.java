package com.bytesforge.linkasanote.data.source;

import android.util.Log;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class RepositoryFavoriteTest {

    private final List<Favorite> FAVORITES;

    private Repository repository;

    @Mock
    private LocalDataSource localDataSource;

    @Mock
    private CloudDataSource cloudDataSource;

    private TestObserver<List<Favorite>> testFavoritesObserver;
    private TestObserver<Favorite> testFavoriteObserver;

    public RepositoryFavoriteTest() {
        FAVORITES = TestUtils.buildFavorites();
    }

    @Before
    public void setupRepository() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Log.class);
        BaseSchedulerProvider schedulerProvider = new ImmediateSchedulerProvider();
        repository = new Repository(localDataSource, cloudDataSource, schedulerProvider);
    }

    @Test
    public void getFavorites_requestsAllFavoritesFromLocalSource() {
        repository.favoriteCacheIsDirty = true;
        when(localDataSource.getFavorites(isNull()))
                .thenReturn(Observable.fromIterable(FAVORITES));
        when(localDataSource.markFavoritesSyncResultsAsApplied()).thenReturn(Single.just(0));
        testFavoritesObserver = repository.getFavorites().toList().test();
        testFavoritesObserver.assertValue(FAVORITES);
        assert repository.cachedFavorites != null;
        assertThat(repository.cachedFavorites.size(), is(FAVORITES.size()));
        Collection<Favorite> cachedFavorites = repository.cachedFavorites.values();
        Iterator iterator;
        int i;
        for (i = 0, iterator = cachedFavorites.iterator(); iterator.hasNext(); i++) {
            Favorite cachedFavorite = (Favorite) iterator.next();
            assertThat(cachedFavorite.getId(), is(FAVORITES.get(i).getId()));
        }
    }

    @Test
    public void getFavorite_requestsSingleFavoriteFromLocalSource() {
        repository.favoriteCacheIsDirty = true;
        Favorite favorite = FAVORITES.get(0);
        String favoriteId = favorite.getId();
        when(localDataSource.getFavorite(eq(favoriteId))).thenReturn(Single.just(favorite));

        testFavoriteObserver = repository.getFavorite(favoriteId).test();
        testFavoriteObserver.assertValue(favorite);
        assertThat(repository.favoriteCacheIsDirty, is(true));
    }

    @Test
    public void saveFavorite_savesFavoriteToLocalAndCloudStorage() {
        Favorite favorite = FAVORITES.get(0);
        String favoriteId = favorite.getId();
        when(localDataSource.saveFavorite(eq(favorite)))
                .thenReturn(Single.just(DataSource.ItemState.DEFERRED));
        when(cloudDataSource.saveFavorite(eq(favoriteId)))
                .thenReturn(Single.just(DataSource.ItemState.SAVED));

        TestObserver<DataSource.ItemState> saveFavoriteObserver =
                repository.saveFavorite(favorite, true).test();
        saveFavoriteObserver.assertValues(
                DataSource.ItemState.DEFERRED, DataSource.ItemState.SAVED);
        assertThat(repository.favoriteCacheIsDirty, is(true));
        assert repository.dirtyFavorites != null;
        assertThat(repository.dirtyFavorites.contains(favoriteId), is(true));
    }

    @Test
    public void deleteFavorite_deleteFavoriteFromLocalAndCloudStorage() {
        int size = FAVORITES.size();
        Favorite favorite = FAVORITES.get(0);
        String favoriteId = favorite.getId();
        // Cache
        when(localDataSource.getFavorites(isNull()))
                .thenReturn(Observable.fromIterable(FAVORITES));
        when(localDataSource.markFavoritesSyncResultsAsApplied()).thenReturn(Single.just(0));
        testFavoritesObserver = repository.getFavorites().toList().test();
        testFavoritesObserver.assertValue(FAVORITES);
        assert repository.cachedFavorites != null;
        assertThat(repository.cachedFavorites.size(), is(size));
        // Preconditions
        when(localDataSource.deleteFavorite(eq(favoriteId)))
                .thenReturn(Single.just(DataSource.ItemState.DEFERRED));
        when(cloudDataSource.deleteFavorite(eq(favoriteId), any(long.class)))
                .thenReturn(Single.just(DataSource.ItemState.DELETED));
        // Test
        TestObserver<DataSource.ItemState> deleteFavoriteObserver =
                repository.deleteFavorite(favoriteId, true, 0).test();
        deleteFavoriteObserver.assertValues(
                DataSource.ItemState.DEFERRED, DataSource.ItemState.DELETED);
        assertThat(repository.favoriteCacheIsDirty, is(false));
        assertThat(repository.cachedFavorites.size(), is(size - 1));
        Collection<Favorite> cachedFavorites = repository.cachedFavorites.values();
        Iterator iterator;
        int i;
        for (i = 0, iterator = cachedFavorites.iterator(); iterator.hasNext(); i++) {
            if (favoriteId.equals(FAVORITES.get(i).getId())) continue;

            Favorite cachedFavorite = (Favorite) iterator.next();
            assertThat(cachedFavorite.getId(), is(FAVORITES.get(i).getId()));
        }
    }
}