package com.bytesforge.linkasanote.laano.favorites.addeditfavorite;

import android.database.sqlite.SQLiteConstraintException;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddEditFavoritePresenterTest {

    @Mock
    private Repository repository;

    @Mock
    private AddEditFavoriteContract.View view;

    @Mock
    private AddEditFavoriteContract.ViewModel viewModel;

    private BaseSchedulerProvider schedulerProvider;
    private AddEditFavoritePresenter presenter;

    @Captor
    ArgumentCaptor<List<Tag>> tagListCaptor;

    private final List<Favorite> FAVORITES;
    private Favorite defaultFavorite;

    public AddEditFavoritePresenterTest() {
        FAVORITES = TestUtils.buildFavorites();
        defaultFavorite = FAVORITES.get(0);
    }

    @Before
    public void setLinksPresenter() throws Exception {
        MockitoAnnotations.initMocks(this);
        schedulerProvider = new ImmediateSchedulerProvider();
        when(view.isActive()).thenReturn(true);
        presenter = new AddEditFavoritePresenter(
                repository, view, viewModel, schedulerProvider, null);
    }

    @Test
    public void loadAllTagsFromRepository_loadsItIntoView() {
        List<Tag> tags = FAVORITES.get(FAVORITES.size() - 1).getTags();
        assertNotNull(tags);
        when(repository.getTags()).thenReturn(Observable.fromIterable(tags));
        presenter.loadTags();
        verify(view).swapTagsCompletionViewItems(tags);
    }

    @Test
    public void loadEmptyListOfTags_loadsEmptyListIntoView() {
        when(repository.getTags()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        presenter.loadTags();
        verify(view).swapTagsCompletionViewItems(tagListCaptor.capture());
        assertEquals(tagListCaptor.getValue().size(), 0);
    }

    @Test
    public void saveNewFavoriteToRepository_finishesActivity() {
        presenter.saveFavorite(defaultFavorite.getName(), defaultFavorite.getTags());
        verify(repository).saveFavorite(any(Favorite.class));
        verify(view).finishActivity();
    }

    @Test
    public void saveEmptyFavorite_showsEmptyError() {
        presenter.saveFavorite(defaultFavorite.getName(), new ArrayList<>());
        presenter.saveFavorite("", new ArrayList<>());
        presenter.saveFavorite("", defaultFavorite.getTags());
        verify(viewModel, times(3)).showEmptyFavoriteSnackbar();
    }

    @Test
    public void saveExistingFavorite_finishesActivity() {
        // Edit Favorite Presenter
        AddEditFavoritePresenter presenter = new AddEditFavoritePresenter(
                repository, view, viewModel, schedulerProvider, defaultFavorite.getId());
        presenter.saveFavorite(defaultFavorite.getName(), defaultFavorite.getTags());
        verify(repository).saveFavorite(any(Favorite.class));
        verify(view).finishActivity();
    }

    @Test
    public void saveFavoriteWithExistedName_showsDuplicateError() {
        doThrow(new SQLiteConstraintException()).when(repository).saveFavorite(any(Favorite.class));
        presenter.saveFavorite(defaultFavorite.getName(), defaultFavorite.getTags());
        verify(view, never()).finishActivity();
        verify(viewModel).showDuplicateKeyError();
    }

    @Test
    public void populateFavorite_callsRepositoryAndUpdateViewOnSuccess() {
        final String favoriteId = defaultFavorite.getId();
        when(repository.getFavorite(favoriteId)).thenReturn(Single.just(defaultFavorite));
        // Edit Favorite Presenter
        AddEditFavoritePresenter presenter = new AddEditFavoritePresenter(
                repository, view, viewModel, schedulerProvider, favoriteId);
        presenter.populateFavorite();
        verify(repository).getFavorite(favoriteId);
        verify(view).setupFavoriteState(any(Favorite.class));
        verify(viewModel).checkAddButton();
    }

    @Test
    public void populateFavorite_callsRepositoryAndShowsWarningOnError() {
        final String favoriteId = defaultFavorite.getId();
        when(repository.getFavorite(favoriteId)).thenReturn(Single.error(new NoSuchElementException()));
        // Edit Favorite Presenter
        AddEditFavoritePresenter presenter = new AddEditFavoritePresenter(
                repository, view, viewModel, schedulerProvider, favoriteId);
        presenter.populateFavorite();
        verify(repository).getFavorite(favoriteId);
        verify(view, never()).setupFavoriteState(any(Favorite.class));
        verify(viewModel).showFavoriteNotFoundSnackbar();
    }
}