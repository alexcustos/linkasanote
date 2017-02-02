package com.bytesforge.linkasanote.data.source;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.observers.TestSubscriber;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryFavoriteTest {

    private final List<Tag> TAGS;
    private final List<String> FAVORITE_NAMES;
    private final List<Favorite> FAVORITES;

    private final TestSubscriber<List<Favorite>> testFavoritesSubscriber;
    private final TestSubscriber<Favorite> testFavoriteSubscriber;

    private Repository repository;

    @Mock
    private DataSource localDataSource;

    @Mock
    private DataSource cloudDataSource;

    public RepositoryFavoriteTest() {
        String keyPrefix = StringUtils.repeat('A', 21);

        TAGS = new ArrayList<>();
        TAGS.add(new Tag("first"));
        TAGS.add(new Tag("second"));
        TAGS.add(new Tag("third"));

        FAVORITE_NAMES = new ArrayList<>();
        FAVORITE_NAMES.add("Favorite");
        FAVORITE_NAMES.add("Favorite #2");
        FAVORITE_NAMES.add("Favorite #3");

        FAVORITES = new ArrayList<>();
        FAVORITES.add(new Favorite(keyPrefix + 'A', FAVORITE_NAMES.get(0), TAGS));
        FAVORITES.add(new Favorite(keyPrefix + 'B', FAVORITE_NAMES.get(1), TAGS));
        FAVORITES.add(new Favorite(keyPrefix + 'C', FAVORITE_NAMES.get(2), TAGS));

        testFavoritesSubscriber = new TestSubscriber<>();
        testFavoriteSubscriber = new TestSubscriber<>();
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

        repository.getFavorites().subscribe(testFavoritesSubscriber);

        verify(localDataSource).getFavorites();
        testFavoritesSubscriber.assertValue(FAVORITES);
    }

    @Test
    public void getFavorite_requestsSingleFavoriteFromLocalSource() {
        Favorite favorite = FAVORITES.get(0);

        setFavoriteAvailable(localDataSource, favorite);
        setFavoriteNotAvailable(cloudDataSource, favorite.getId());

        repository.getFavorite(favorite.getId()).subscribe(testFavoriteSubscriber);
        verify(localDataSource).getFavorite(eq(favorite.getId()));
        testFavoriteSubscriber.assertValue(favorite);
    }

    @Test
    public void saveFavorite_savesFavoriteToLocalAndCloudStorage() {
        Favorite favorite = FAVORITES.get(0);

        repository.saveFavorite(favorite);

        verify(localDataSource).saveFavorite(favorite);
        verify(cloudDataSource).saveFavorite(favorite);
        assertThat(repository.cachedFavorites.size(), is(1));
    }

    @Test
    public void deleteAllFavorites_deleteFavoritesFromLocalAndCloudStorage() {
        for (Favorite favorite : FAVORITES) {
            repository.saveFavorite(favorite);
        }
        assertThat(repository.cachedFavorites.size(), is(FAVORITES.size()));

        repository.deleteAllFavorites();
        verify(localDataSource).deleteAllFavorites();
        verify(cloudDataSource).deleteAllFavorites();
        assertThat(repository.cachedFavorites.size(), is(0));
    }

    // Data setup

    private void setFavoritesAvailable(DataSource dataSource, List<Favorite> favorites) {
        when(dataSource.getFavorites()).
                thenReturn(Observable.just(favorites).concatWith(Observable.<List<Favorite>>never()));
    }

    private void setFavoritesNotAvailable(DataSource dataSource) {
        when(dataSource.getFavorites()).thenReturn(Observable.just(Collections.<Favorite>emptyList()));
    }

    private void setFavoriteAvailable(DataSource dataSource, Favorite favorite) {
        when(dataSource.getFavorite(eq(favorite.getId())))
                .thenReturn(Observable.just(favorite).concatWith(Observable.<Favorite>never()));
    }

    private void setFavoriteNotAvailable(DataSource dataSource, String favoriteId) {
        when(dataSource.getFavorite(eq(favoriteId)))
                .thenReturn(Observable.<Favorite>just(null).concatWith(Observable.<Favorite>never()));
    }
}