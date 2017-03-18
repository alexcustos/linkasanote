package com.bytesforge.linkasanote.data.source;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Favorite;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryFavoriteTest {

    private final List<Favorite> FAVORITES;

    private Repository repository;

    @Mock
    private DataSource localDataSource;

    @Mock
    private DataSource cloudDataSource;

    private TestObserver<List<Favorite>> testFavoritesObserver;
    private TestObserver<Favorite> testFavoriteObserver;

    public RepositoryFavoriteTest() {
        FAVORITES = TestUtils.buildFavorites();
    }

    @Before
    public void setupRepository() {
        MockitoAnnotations.initMocks(this);
        repository = new Repository(localDataSource, cloudDataSource);
    }

    @Test
    public void getFavorites_requestsAllFavoritesFromLocalSource() {
        setFavoritesAvailable(localDataSource, FAVORITES);
        setFavoritesNotAvailable(cloudDataSource);

        testFavoritesObserver = repository.getFavorites().toList().test();
        verify(localDataSource).getFavorites();
        testFavoritesObserver.assertValue(FAVORITES);
    }

    @Test
    public void getFavorite_requestsSingleFavoriteFromLocalSource() {
        Favorite favorite = FAVORITES.get(0);

        setFavoriteAvailable(localDataSource, favorite);
        setFavoriteNotAvailable(cloudDataSource, favorite.getId());

        testFavoriteObserver = repository.getFavorite(favorite.getId()).test();
        verify(localDataSource).getFavorite(eq(favorite.getId()));
        testFavoriteObserver.assertValue(favorite);
    }

    @Test
    public void saveFavorite_savesFavoriteToLocalAndCloudStorage() {
        Favorite favorite = FAVORITES.get(0);

        repository.saveFavorite(favorite);
        verify(localDataSource).saveFavorite(favorite);
        verify(cloudDataSource).saveFavorite(favorite);
        assertThat(repository.favoriteCacheIsDirty, is(true));
    }

    @Test
    public void deleteAllFavorites_deleteFavoritesFromLocalAndCloudStorage() {
        setFavoritesAvailable(localDataSource, FAVORITES);
        testFavoritesObserver = repository.getFavorites().toList().test();
        assertThat(repository.cachedFavorites.size(), is(FAVORITES.size()));

        repository.deleteAllFavorites();
        verify(localDataSource).deleteAllFavorites();
        verify(cloudDataSource, never()).deleteAllFavorites();
        assertThat(repository.cachedFavorites.size(), is(0));
    }

    // Data setup

    private void setFavoritesAvailable(DataSource dataSource, List<Favorite> favorites) {
        when(dataSource.getFavorites()).thenReturn(Observable.fromIterable(favorites));
    }

    private void setFavoritesNotAvailable(DataSource dataSource) {
        when(dataSource.getFavorites()).thenReturn(Observable.fromIterable(Collections.emptyList()));
    }

    private void setFavoriteAvailable(DataSource dataSource, Favorite favorite) {
        when(dataSource.getFavorite(eq(favorite.getId()))).thenReturn(Single.just(favorite));
    }

    private void setFavoriteNotAvailable(DataSource dataSource, String favoriteId) {
        when(dataSource.getFavorite(eq(favoriteId))).thenReturn(
                Single.error(new NoSuchElementException()));
    }
}