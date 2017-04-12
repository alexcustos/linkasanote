package com.bytesforge.linkasanote.laano.favorites.conflictresolution;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.cloud.CloudFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.NoSuchElementException;

import io.reactivex.Single;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FavoritesConflictResolutionPresenterTest {

    @Mock
    Repository repository;

    @Mock
    LocalFavorites localFavorites;

    @Mock
    CloudFavorites cloudFavorites;

    @Mock
    FavoritesConflictResolutionContract.View view;

    @Mock
    FavoritesConflictResolutionContract.ViewModel viewModel;

    private static final String E_TAGL = "abcdefghigklmnopqrstuvwxwz";
    private static final String E_TAGC = "zwxwvutsrqponmlkgihgfedcba";

    private BaseSchedulerProvider schedulerProvider;
    private FavoritesConflictResolutionPresenter presenter;
    private Favorite defaultFavorite;
    private String favoriteId;

    @Before
    public void setupFavoritesConflictResolutionPresenter() {
        MockitoAnnotations.initMocks(this);
        schedulerProvider = new ImmediateSchedulerProvider();
        favoriteId = TestUtils.KEY_PREFIX + 'A';
        defaultFavorite = new Favorite(favoriteId, "Favorite", TestUtils.TAGS);
        presenter = new FavoritesConflictResolutionPresenter(
                repository, localFavorites, cloudFavorites,
                view, viewModel, schedulerProvider, defaultFavorite.getId());
    }

    @Test
    public void notConflictedFavorite_finishesActivityWithSuccess() {
        SyncState state = new SyncState(SyncState.State.SYNCED);
        Favorite favorite = new Favorite(defaultFavorite, state);
        when(localFavorites.getFavorite(eq(favoriteId)))
                .thenReturn(Single.fromCallable(() -> favorite));
        presenter.subscribe();
        verify(repository).refreshFavorites();
        verify(view).finishActivity();
    }

    @Test
    public void wrongId_finishesActivityWithSuccess() {
        when(localFavorites.getFavorite(eq(favoriteId)))
                .thenReturn(Single.error(new NoSuchElementException()));
        presenter.subscribe();
        verify(repository).refreshFavorites();
        verify(view).finishActivity();
    }

    @Test
    public void databaseError_showsErrorThenTriesToLoadCloudFavorite() {
        when(localFavorites.getFavorite(eq(favoriteId)))
                .thenReturn(Single.error(new NullPointerException()));
        when(cloudFavorites.downloadFavorite(eq(favoriteId)))
                .thenReturn(Single.fromCallable(() -> defaultFavorite));
        presenter.subscribe();
        verify(viewModel).showDatabaseError();
        verify(viewModel).populateCloudFavorite(eq(defaultFavorite));
    }

    @Test
    public void duplicatedFavorite_populatesToCloudThenLoadsMainToLocal() {
        SyncState state = new SyncState(E_TAGL, 1); // duplicated
        Favorite favorite = new Favorite(defaultFavorite, state);
        Favorite mainFavorite = new Favorite(
                TestUtils.KEY_PREFIX + 'B', "Favorite", TestUtils.TAGS);
        when(localFavorites.getFavorite(eq(favoriteId)))
                .thenReturn(Single.fromCallable(() -> favorite));
        when(localFavorites.getMainFavorite(eq(favorite.getName())))
                .thenReturn(Single.fromCallable(() -> mainFavorite));
        when(viewModel.isCloudPopulated()).thenReturn(true);
        presenter.subscribe();
        verify(viewModel).populateCloudFavorite(eq(favorite));
        verify(viewModel).populateLocalFavorite(eq(mainFavorite));
    }

    @Test
    public void duplicatedFavoriteWithNoMainRecord_resolvesConflictAutomatically() {
        SyncState state = new SyncState(E_TAGL, 1); // duplicated
        Favorite favorite = new Favorite(defaultFavorite, state);
        when(localFavorites.getFavorite(eq(favoriteId)))
                .thenReturn(Single.fromCallable(() -> favorite));
        when(localFavorites.getMainFavorite(eq(favorite.getName())))
                .thenReturn(Single.error(new NoSuchElementException()));
        when(viewModel.isCloudPopulated()).thenReturn(true);
        when(localFavorites.updateFavorite(eq(favoriteId), any(SyncState.class)))
                .thenReturn(Single.fromCallable(() -> 1));
        presenter.subscribe();
        verify(viewModel).populateCloudFavorite(eq(favorite));
        verify(repository).refreshFavorites();
        verify(view).finishActivity();
    }
}