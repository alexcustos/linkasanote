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

package com.bytesforge.linkasanote.laano.links.addeditlink;

import android.database.sqlite.SQLiteConstraintException;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Link;
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

public class AddEditLinkPresenterTest {

    @Mock
    private Repository repository;

    @Mock
    private AddEditLinkContract.View view;

    @Mock
    private AddEditLinkContract.ViewModel viewModel;

    @Mock
    private Settings settings;

    private BaseSchedulerProvider schedulerProvider;
    private AddEditLinkPresenter presenter;

    @Captor
    ArgumentCaptor<List<Tag>> tagListCaptor;

    private final List<Link> LINKS;
    private Link defaultLink;

    public AddEditLinkPresenterTest() {
        LINKS = TestUtils.buildLinks();
        defaultLink = LINKS.get(0);
    }

    @Before
    public void setLinksPresenter() throws Exception {
        MockitoAnnotations.initMocks(this);
        schedulerProvider = new ImmediateSchedulerProvider();
        when(view.isActive()).thenReturn(true);
        presenter = new AddEditLinkPresenter(
                repository, view, viewModel, schedulerProvider, settings, null);
    }

    @Test
    public void loadAllTagsFromRepository_loadsItIntoView() {
        List<Tag> tags = defaultLink.getTags();
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
    public void saveNewLinkToRepository_finishesActivity() {
        String linkLink = defaultLink.getLink();
        String linkName = defaultLink.getName();
        boolean linkDisabled = defaultLink.isDisabled();
        List<Tag> linkTags = defaultLink.getTags();
        when(repository.saveLink(any(Link.class), eq(false)))
                .thenReturn(Observable.just(DataSource.ItemState.DEFERRED));

        presenter.saveLink(linkLink, linkName, linkDisabled, linkTags);
        verify(repository, never()).refreshLinks();
        verify(view).finishActivity(any(String.class));
    }

    @Test
    public void saveEmptyLink_showsEmptyError() {
        presenter.saveLink(null, "", false, new ArrayList<>());
        presenter.saveLink("", defaultLink.getName(), true, defaultLink.getTags());
        verify(viewModel, times(2)).showEmptyLinkSnackbar();
    }

    @Test
    public void saveExistingLink_finishesActivity() {
        String linkId = defaultLink.getId();
        String linkLink = defaultLink.getLink();
        String linkName = defaultLink.getName();
        boolean linkDisabled = defaultLink.isDisabled();
        List<Tag> linkTags = defaultLink.getTags();
        when(repository.saveLink(any(Link.class), eq(false)))
                .thenReturn(Observable.just(DataSource.ItemState.DEFERRED));
        // Edit Link Presenter
        AddEditLinkPresenter presenter = new AddEditLinkPresenter(
                repository, view, viewModel, schedulerProvider, settings, linkId);
        presenter.saveLink(linkLink, linkName, linkDisabled, linkTags);
        verify(repository, never()).refreshLinks();
        verify(view).finishActivity(eq(linkId));
    }

    @Test
    public void saveLinkWithExistedName_showsDuplicateError() {
        String linkId = defaultLink.getId();
        String linkLink = defaultLink.getLink();
        String linkName = defaultLink.getName();
        boolean linkDisabled = defaultLink.isDisabled();
        List<Tag> linkTags = defaultLink.getTags();
        when(repository.saveLink(any(Link.class), eq(false)))
                .thenReturn(Observable.error(new SQLiteConstraintException()));

        presenter.saveLink(linkLink, linkName, linkDisabled, linkTags);
        verify(view, never()).finishActivity(eq(linkId));
        verify(viewModel).showDuplicateKeyError();
    }

    @Test
    public void populateLink_callsRepositoryAndUpdateViewOnSuccess() {
        final String linkId = defaultLink.getId();
        when(repository.getLink(linkId)).thenReturn(Single.just(defaultLink));
        // Edit Link Presenter
        AddEditLinkPresenter presenter = new AddEditLinkPresenter(
                repository, view, viewModel, schedulerProvider, settings, linkId);
        presenter.populateLink();
        verify(repository).getLink(linkId);
        verify(viewModel).populateLink(any(Link.class));
    }

    @Test
    public void populateLink_callsRepositoryAndShowsWarningOnError() {
        final String linkId = defaultLink.getId();
        when(repository.getLink(linkId)).thenReturn(Single.error(new NoSuchElementException()));
        // Edit Link Presenter
        AddEditLinkPresenter presenter = new AddEditLinkPresenter(
                repository, view, viewModel, schedulerProvider, settings, linkId);
        presenter.populateLink();
        verify(repository).getLink(linkId);
        verify(viewModel, never()).populateLink(any(Link.class));
        verify(viewModel).showLinkNotFoundSnackbar();
    }
}