package com.bytesforge.linkasanote.laano.favorites;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FavoritesPresenterTest {

    @Mock
    private Repository repository;

    @Mock
    private FavoritesContract.View view;

    @Mock
    private FavoritesContract.ViewModel viewModel;

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
        BaseSchedulerProvider schedulerProvider = new ImmediateSchedulerProvider();
        // TODO: check if it's needed at all
        when(view.isActive()).thenReturn(true);

        presenter = new FavoritesPresenter(repository, view, viewModel, schedulerProvider);
    }

    @Test
    public void loadAllFavoritesFromRepository_loadsItIntoView() {
        when(repository.getFavorites()).thenReturn(Observable.fromIterable(FAVORITES));
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
        presenter.addFavorite();
        verify(view).showAddFavorite();
    }

    @Test
    public void clickOnEditFavorite_showEditFavoriteUi() {
        final String favoriteId = FAVORITES.get(0).getId();
        presenter.onEditClick(favoriteId);
        verify(view).showEditFavorite(favoriteId);
    }

    @Test
    public void clickOnDeleteFavorite_removesSelectedFavorites() {
        int[] selectedIds = new int[]{0, 5, 10};
        String favoriteId = TestUtils.KEY_PREFIX + 'A';
        when(viewModel.getSelectedIds()).thenReturn(selectedIds);
        when(view.removeFavorite(anyInt())).thenReturn(favoriteId);
        presenter.onDeleteClick();
        for (int selectedId : selectedIds) {
            verify(viewModel).removeSelection(selectedId);
            verify(view).removeFavorite(selectedId);
        }
        verify(repository, times(selectedIds.length)).deleteFavorite(favoriteId);
    }
}