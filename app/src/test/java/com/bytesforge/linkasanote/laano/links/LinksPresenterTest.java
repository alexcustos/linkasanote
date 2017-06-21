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

package com.bytesforge.linkasanote.laano.links;

import android.util.Log;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Link;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class LinksPresenterTest {

    @Mock
    private Repository repository;

    @Mock
    private LinksContract.View view;

    @Mock
    private LinksContract.ViewModel viewModel;

    @Mock
    private LaanoUiManager laanoUiManager;

    @Mock
    private Settings settings;

    private LinksPresenter presenter;

    @Captor
    ArgumentCaptor<List<Link>> linkListCaptor;

    private final List<Link> LINKS;

    public LinksPresenterTest() {
        LINKS = TestUtils.buildLinks();
    }

    @Before
    public void setupLinksPresenter() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Log.class);
        BaseSchedulerProvider schedulerProvider = new ImmediateSchedulerProvider();
        when(view.isActive()).thenReturn(true);
        when(settings.getLinksFilterType()).thenReturn(FilterType.ALL);
        presenter = new LinksPresenter(
                repository, view, viewModel, schedulerProvider, laanoUiManager, settings);
    }

    @Test
    public void loadAllLinksFromRepository_loadsItIntoView() {
        when(repository.getLinks()).thenReturn(Observable.fromIterable(LINKS));
        presenter.loadLinks(true);
        verify(view).showLinks(LINKS);
    }

    @Test
    public void loadEmptyListOfLinks_showsEmptyList() {
        when(repository.getLinks()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        presenter.loadLinks(true);
        verify(view).showLinks(linkListCaptor.capture());
        assertEquals(linkListCaptor.getValue().size(), 0);
    }

    @Test
    public void clickOnAddLink_showAddLinkUi() {
        presenter.showAddLink();
        verify(view).startAddLinkActivity();
    }

    @Test
    public void clickOnEditLink_showEditLinkUi() {
        final String linkId = LINKS.get(0).getId();
        presenter.onEditClick(linkId);
        verify(view).showEditLink(eq(linkId));
    }

    @Test
    public void clickOnDeleteLink_showsConfirmLinksRemoval() {
        ArrayList<String> selectedIds = new ArrayList<String>() {{
            add(LINKS.get(0).getId());
            add(LINKS.get(2).getId());
        }};
        when(viewModel.getSelectedIds()).thenReturn(selectedIds);
        presenter.onDeleteClick();
        verify(view).confirmLinksRemoval(eq(selectedIds));
    }
}