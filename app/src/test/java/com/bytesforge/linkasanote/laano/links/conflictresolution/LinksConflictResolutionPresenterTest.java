package com.bytesforge.linkasanote.laano.links.conflictresolution;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalLinks;
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

public class LinksConflictResolutionPresenterTest {

    @Mock
    Repository repository;

    @Mock
    LocalLinks<Link> localLinks;

    @Mock
    CloudItem<Link> cloudLinks;

    @Mock
    LinksConflictResolutionContract.View view;

    @Mock
    LinksConflictResolutionContract.ViewModel viewModel;

    private static final String E_TAGL = "abcdefghigklmnopqrstuvwxwz";
    private static final String E_TAGC = "zwxwvutsrqponmlkgihgfedcba";

    private BaseSchedulerProvider schedulerProvider;
    private LinksConflictResolutionPresenter presenter;
    private Link defaultLink;
    private String linkId;

    @Before
    public void setupLinksConflictResolutionPresenter() {
        MockitoAnnotations.initMocks(this);
        schedulerProvider = new ImmediateSchedulerProvider();
        linkId = TestUtils.KEY_PREFIX + 'A';
        defaultLink = new Link(linkId, "http://laano.net/link", "Link", false, TestUtils.TAGS);
        presenter = new LinksConflictResolutionPresenter(
                repository, localLinks, cloudLinks,
                view, viewModel, schedulerProvider, defaultLink.getId());
    }

    @Test
    public void notConflictedLink_finishesActivityWithSuccess() {
        SyncState state = new SyncState(SyncState.State.SYNCED);
        Link link = new Link(defaultLink, state);
        when(localLinks.get(eq(linkId)))
                .thenReturn(Single.fromCallable(() -> link));
        presenter.subscribe();
        verify(repository).refreshLinks();
        verify(view).finishActivity();
    }

    @Test
    public void wrongId_finishesActivityWithSuccess() {
        when(localLinks.get(eq(linkId)))
                .thenReturn(Single.error(new NoSuchElementException()));
        presenter.subscribe();
        verify(repository).refreshLinks();
        verify(view).finishActivity();
    }

    @Test
    public void databaseError_showsErrorThenTriesToLoadCloudLink() {
        when(localLinks.get(eq(linkId)))
                .thenReturn(Single.error(new NullPointerException()));
        when(cloudLinks.download(eq(linkId)))
                .thenReturn(Single.fromCallable(() -> defaultLink));
        presenter.subscribe();
        verify(viewModel).showDatabaseError();
        verify(viewModel).populateCloudLink(eq(defaultLink));
    }

    @Test
    public void duplicatedLink_populatesToCloudThenLoadsMainToLocal() {
        SyncState state = new SyncState(E_TAGL, 1); // duplicated
        Link link = new Link(defaultLink, state);
        Link mainLink = new Link(
                TestUtils.KEY_PREFIX + 'B', "http://laano.net/link", "Link", false, TestUtils.TAGS);
        when(localLinks.get(eq(linkId)))
                .thenReturn(Single.fromCallable(() -> link));
        when(localLinks.getMain(eq(link.getName())))
                .thenReturn(Single.fromCallable(() -> mainLink));
        when(viewModel.isCloudPopulated()).thenReturn(true);
        presenter.subscribe();
        verify(viewModel).populateCloudLink(eq(link));
        verify(viewModel).populateLocalLink(eq(mainLink));
    }

    @Test
    public void duplicatedLinkWithNoMainRecord_resolvesConflictAutomatically() {
        SyncState state = new SyncState(E_TAGL, 1); // duplicated
        Link link = new Link(defaultLink, state);
        when(localLinks.get(eq(linkId)))
                .thenReturn(Single.fromCallable(() -> link));
        when(localLinks.getMain(eq(link.getName())))
                .thenReturn(Single.error(new NoSuchElementException()));
        when(viewModel.isCloudPopulated()).thenReturn(true);
        when(localLinks.update(eq(linkId), any(SyncState.class)))
                .thenReturn(Single.fromCallable(() -> 1));
        presenter.subscribe();
        verify(viewModel).populateCloudLink(eq(link));
        verify(repository).refreshLinks();
        verify(view).finishActivity();
    }
}