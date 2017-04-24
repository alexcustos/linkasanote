package com.bytesforge.linkasanote.laano.links;

import android.util.Log;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.source.Repository;
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
        // TODO: check if it's needed at all
        when(view.isActive()).thenReturn(true);

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
        presenter.addLink();
        verify(view).showAddLink();
    }

    @Test
    public void clickOnEditLink_showEditLinkUi() {
        final String linkId = LINKS.get(0).getId();
        presenter.onEditClick(linkId);
        verify(view).showEditLink(eq(linkId));
    }

    @Test
    public void clickOnDeleteLink_showsConfirmLinksRemoval() {
        int[] selectedIds = new int[]{0, 5, 10};
        String linkId = TestUtils.KEY_PREFIX + 'A';
        when(viewModel.getSelectedIds()).thenReturn(selectedIds);
        when(view.removeLink(anyInt())).thenReturn(linkId);
        presenter.onDeleteClick();
        verify(view).confirmLinksRemoval(eq(selectedIds));
    }
}