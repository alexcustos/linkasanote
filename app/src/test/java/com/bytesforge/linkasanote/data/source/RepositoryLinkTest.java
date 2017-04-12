package com.bytesforge.linkasanote.data.source;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Link;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryLinkTest {

    private final List<Link> LINKS;

    private Repository repository;

    @Mock
    private DataSource localDataSource;

    @Mock
    private DataSource cloudDataSource;

    private TestObserver<List<Link>> testLinksObserver;
    private TestObserver<Link> testLinkObserver;

    public RepositoryLinkTest() {
        LINKS = TestUtils.buildLinks();
    }

    @Before
    public void setupRepository() {
        MockitoAnnotations.initMocks(this);
        repository = new Repository(localDataSource, cloudDataSource);
    }

    @Test
    public void getLinks_requestsAllLinksFromLocalSource() {
        setLinksAvailable(localDataSource, LINKS);
        setLinksNotAvailable(cloudDataSource);

        testLinksObserver = repository.getLinks().toList().test();
        verify(localDataSource).getLinks();
        testLinksObserver.assertValue(LINKS);
    }

    @Test
    public void getLink_requestsSingleLinkFromLocalSource() {
        Link link = LINKS.get(0);

        setLinkAvailable(localDataSource, link);
        setLinkNotAvailable(cloudDataSource, link.getId());

        testLinkObserver = repository.getLink(link.getId()).test();
        verify(localDataSource).getLink(eq(link.getId()));
        testLinkObserver.assertValue(link);
    }

    @Test
    public void saveLink_savesLinkToLocalAndCloudStorage() {
        Link link = LINKS.get(0);

        repository.saveLink(link);
        verify(localDataSource).saveLink(link);
        verify(cloudDataSource).saveLink(link);
        assertThat(repository.linkCacheIsDirty, is(true));
    }

    @Test
    public void deleteAllLinks_deleteLinksFromLocalAndCloudStorage() {
        setLinksAvailable(localDataSource, LINKS);
        testLinksObserver = repository.getLinks().toList().test();
        assertThat(repository.cachedLinks.size(), is(LINKS.size()));

        repository.deleteAllLinks();
        verify(localDataSource).deleteAllLinks();
        verify(cloudDataSource, never()).deleteAllLinks();
        assertThat(repository.cachedLinks.size(), is(0));
    }

    // Data setup

    private void setLinksAvailable(DataSource dataSource, List<Link> links) {
        when(dataSource.getLinks()).thenReturn(Observable.fromIterable(links));
    }

    private void setLinksNotAvailable(DataSource dataSource) {
        when(dataSource.getLinks()).thenReturn(Observable.fromIterable(Collections.emptyList()));
    }

    private void setLinkAvailable(DataSource dataSource, Link link) {
        when(dataSource.getLink(eq(link.getId()))).thenReturn(Single.just(link));
    }

    private void setLinkNotAvailable(DataSource dataSource, String linkId) {
        when(dataSource.getLink(eq(linkId))).thenReturn(
                Single.error(new NoSuchElementException()));
    }
}
