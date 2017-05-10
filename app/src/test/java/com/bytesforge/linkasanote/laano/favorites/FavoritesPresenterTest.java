package com.bytesforge.linkasanote.laano.favorites;

import android.util.Log;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

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

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class FavoritesPresenterTest {

    @Mock
    private Repository repository;

    @Mock
    private FavoritesContract.View view;

    @Mock
    private FavoritesContract.ViewModel viewModel;

    @Mock
    private LaanoUiManager laanoUiManager;

    @Mock
    private Settings settings;

    private FavoritesPresenter presenter;

    @Captor
    ArgumentCaptor<List<Favorite>> favoriteListCaptor;

    private final List<Favorite> FAVORITES;

    public FavoritesPresenterTest() {
        FAVORITES = TestUtils.buildFavorites();
    }

    @Before
    public void setupFavoritesPresenter() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Log.class);
        BaseSchedulerProvider schedulerProvider = new ImmediateSchedulerProvider();
        when(view.isActive()).thenReturn(true);

        presenter = new FavoritesPresenter(
                repository, view, viewModel, schedulerProvider, laanoUiManager, settings);
    }

    @Test
    public void loadAllFavoritesFromRepository_loadsItIntoView() {
        when(repository.getFavorites()).thenReturn(Observable.fromIterable(FAVORITES));
        when(viewModel.getFilterType()).thenReturn(FilterType.ALL);
        presenter.loadFavorites(true);
        verify(view).showFavorites(FAVORITES);
    }

    @Test
    public void loadEmptyListOfFavorites_showsEmptyList() {
        when(repository.getFavorites()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        presenter.loadFavorites(true);
        verify(view).showFavorites(favoriteListCaptor.capture());
        assertEquals(favoriteListCaptor.getValue().size(), 0);
    }

    @Test
    public void clickOnAddFavorite_showAddFavoriteUi() {
        presenter.showAddFavorite();
        verify(view).startAddFavoriteActivity();
    }

    @Test
    public void clickOnEditFavorite_showEditFavoriteUi() {
        final String favoriteId = FAVORITES.get(0).getId();
        presenter.onEditClick(favoriteId);
        verify(view).showEditFavorite(eq(favoriteId));
    }

    @Test
    public void clickOnDeleteFavorite_showsConfirmFavoritesRemoval() {
        int[] selectedIds = new int[]{0, 5, 10};
        String favoriteId = TestUtils.KEY_PREFIX + 'A';
        when(viewModel.getSelectedIds()).thenReturn(selectedIds);
        when(view.removeFavorite(anyInt())).thenReturn(favoriteId);
        presenter.onDeleteClick();
        verify(view).confirmFavoritesRemoval(eq(selectedIds));
    }
}