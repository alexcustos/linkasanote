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

package com.bytesforge.linkasanote.laano.favorites.addeditfavorite;

import android.database.sqlite.SQLiteConstraintException;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.settings.Settings;
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
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private Settings settings;

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
    public void setFavoritePresenter() throws Exception {
        MockitoAnnotations.initMocks(this);
        schedulerProvider = new ImmediateSchedulerProvider();
        when(view.isActive()).thenReturn(true);
        presenter = new AddEditFavoritePresenter(
                repository, view, viewModel, schedulerProvider, settings, null);
    }

    @Test
    public void loadAllTagsFromRepository_loadsItIntoView() {
        List<Tag> tags = defaultFavorite.getTags();
        assertNotNull(tags);
        when(repository.getTags()).thenReturn(Observable.fromIterable(tags));
        presenter.loadTags();
        verify(view).swapTagsCompletionViewItems(eq(tags));
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
        String favoriteName = defaultFavorite.getName();
        boolean favoriteAndGate = defaultFavorite.isAndGate();
        List<Tag> favoriteTags = defaultFavorite.getTags();
        when(repository.saveFavorite(any(Favorite.class), eq(false)))
                .thenReturn(Observable.just(DataSource.ItemState.DEFERRED));

        presenter.saveFavorite(favoriteName, favoriteAndGate, favoriteTags);
        verify(repository, never()).refreshFavorites();
        verify(view).finishActivity(any(String.class));
    }

    @Test
    public void saveEmptyFavorite_showsEmptyError() {
        presenter.saveFavorite(defaultFavorite.getName(), false, new ArrayList<>());
        presenter.saveFavorite(defaultFavorite.getName(), true, new ArrayList<>());
        presenter.saveFavorite("", false, new ArrayList<>());
        presenter.saveFavorite("", true, new ArrayList<>());
        presenter.saveFavorite("", false, defaultFavorite.getTags());
        presenter.saveFavorite("", true, defaultFavorite.getTags());
        verify(viewModel, times(6)).showEmptyFavoriteSnackbar();
    }

    @Test
    public void saveExistingFavorite_finishesActivity() {
        String favoriteId = defaultFavorite.getId();
        boolean favoriteAndGate = defaultFavorite.isAndGate();
        String favoriteName = defaultFavorite.getName();
        List<Tag> favoriteTags = defaultFavorite.getTags();
        when(repository.saveFavorite(any(Favorite.class), eq(false)))
                .thenReturn(Observable.just(DataSource.ItemState.DEFERRED));
        // Edit Favorite Presenter
        AddEditFavoritePresenter presenter = new AddEditFavoritePresenter(
                repository, view, viewModel, schedulerProvider, settings, favoriteId);
        presenter.saveFavorite(favoriteName, favoriteAndGate, favoriteTags);
        verify(repository, never()).refreshFavorites();
        verify(view).finishActivity(eq(favoriteId));
    }

    @Test
    public void saveFavoriteWithExistedName_showsDuplicateError() {
        String favoriteId = defaultFavorite.getId();
        boolean favoriteAndGate = defaultFavorite.isAndGate();
        String favoriteName = defaultFavorite.getName();
        List<Tag> favoriteTags = defaultFavorite.getTags();
        when(repository.saveFavorite(any(Favorite.class), eq(false)))
                .thenReturn(Observable.error(new SQLiteConstraintException()));

        presenter.saveFavorite(favoriteName, favoriteAndGate, favoriteTags);
        verify(view, never()).finishActivity(eq(favoriteId));
        verify(viewModel).showDuplicateKeyError();
    }

    @Test
    public void populateFavorite_callsRepositoryAndUpdateViewOnSuccess() {
        final String favoriteId = defaultFavorite.getId();
        when(repository.getFavorite(favoriteId)).thenReturn(Single.just(defaultFavorite));
        // Edit Favorite Presenter
        AddEditFavoritePresenter presenter = new AddEditFavoritePresenter(
                repository, view, viewModel, schedulerProvider, settings, favoriteId);
        presenter.populateFavorite();
        verify(repository).getFavorite(favoriteId);
        verify(viewModel).populateFavorite(any(Favorite.class));
    }

    @Test
    public void populateFavorite_callsRepositoryAndShowsWarningOnError() {
        final String favoriteId = defaultFavorite.getId();
        when(repository.getFavorite(favoriteId)).thenReturn(Single.error(new NoSuchElementException()));
        // Edit Favorite Presenter
        AddEditFavoritePresenter presenter = new AddEditFavoritePresenter(
                repository, view, viewModel, schedulerProvider, settings, favoriteId);
        presenter.populateFavorite();
        verify(repository).getFavorite(favoriteId);
        verify(viewModel, never()).populateFavorite(any(Favorite.class));
        verify(viewModel).showFavoriteNotFoundSnackbar();
    }
}