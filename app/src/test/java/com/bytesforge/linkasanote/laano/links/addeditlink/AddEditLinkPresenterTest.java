package com.bytesforge.linkasanote.laano.links.addeditlink;

import android.database.sqlite.SQLiteConstraintException;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Link;
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

public class AddEditLinkPresenterTest {

    @Mock
    private Repository repository;

    @Mock
    private AddEditLinkContract.View view;

    @Mock
    private AddEditLinkContract.ViewModel viewModel;

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
                repository, view, viewModel, schedulerProvider, null);
    }

    @Test
    public void loadAllTagsFromRepository_loadsItIntoView() {
        List<Tag> tags = LINKS.get(LINKS.size() - 1).getTags();
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
    public void saveNewLinkToRepository_finishesActivity() {
        presenter.saveLink(defaultLink.getLink(), defaultLink.getName(),
                defaultLink.isDisabled(), defaultLink.getTags());
        verify(repository).saveLink(any(Link.class));
        verify(view).finishActivity();
    }

    @Test
    public void saveEmptyLink_showsEmptyError() {
        presenter.saveLink(null, "", false, new ArrayList<>());
        presenter.saveLink("", defaultLink.getName(), true, defaultLink.getTags());
        verify(viewModel, times(2)).showEmptyLinkSnackbar();
    }

    @Test
    public void saveExistingLink_finishesActivity() {
        // Edit Link Presenter
        AddEditLinkPresenter presenter = new AddEditLinkPresenter(
                repository, view, viewModel, schedulerProvider, defaultLink.getId());
        presenter.saveLink(defaultLink.getLink(), defaultLink.getName(), false, defaultLink.getTags());
        verify(repository).saveLink(any(Link.class));
        verify(view).finishActivity();
    }

    @Test
    public void saveLinkWithExistedName_showsDuplicateError() {
        doThrow(new SQLiteConstraintException()).when(repository).saveLink(any(Link.class));
        presenter.saveLink(defaultLink.getLink(), defaultLink.getName(), false, defaultLink.getTags());
        verify(view, never()).finishActivity();
        verify(viewModel).showDuplicateKeyError();
    }

    @Test
    public void populateLink_callsRepositoryAndUpdateViewOnSuccess() {
        final String linkId = defaultLink.getId();
        when(repository.getLink(linkId)).thenReturn(Single.just(defaultLink));
        // Edit Link Presenter
        AddEditLinkPresenter presenter = new AddEditLinkPresenter(
                repository, view, viewModel, schedulerProvider, linkId);
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
                repository, view, viewModel, schedulerProvider, linkId);
        presenter.populateLink();
        verify(repository).getLink(linkId);
        verify(viewModel, never()).populateLink(any(Link.class));
        verify(viewModel).showLinkNotFoundSnackbar();
    }
}