package com.bytesforge.linkasanote.data.source;

import com.bytesforge.linkasanote.data.Link;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.observers.TestSubscriber;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryTest {

    private final static String LINK_VALUE = "http://laano.net/link";
    private final static String LINK_VALUE2 = "http://laano.net/link2";
    private final static String LINK_VALUE3 = "http://laano.net/link3";
    private static List<Link> LINKS;

    private Repository repository;
    private TestSubscriber<List<Link>> testLinksSubscriber;
    private TestSubscriber<Link> testLinkSubscriber;

    @Mock
    private DataSource localDataSource;

    @Mock
    private DataSource cloudDataSource;

    @Before
    public void setupRepository() {
        MockitoAnnotations.initMocks(this);

        String keyPrefix = StringUtils.repeat('A', 21);

        LINKS = Lists.newArrayList(
                new Link(keyPrefix + 'A', LINK_VALUE, "Title for Link"),
                new Link(keyPrefix + 'B', LINK_VALUE2, "Title for link #2"),
                new Link(keyPrefix + 'C', LINK_VALUE3, "Title for link #3"));

        repository = new Repository(localDataSource, cloudDataSource);
        testLinksSubscriber = new TestSubscriber<>();
        testLinkSubscriber = new TestSubscriber<>();
    }

    @Test
    public void getLinks_requestsAllLinksFromLocalDataSource() {
        setLinksAvailable(localDataSource, LINKS);
        setLinksNotAvailable(cloudDataSource);

        repository.getLinks().subscribe(testLinksSubscriber);

        verify(localDataSource).getLinks();
        testLinksSubscriber.assertValue(LINKS);
    }

    @Test
    public void getLink_requestsSingleLinkFromLocalDataSource() {
        Link link = LINKS.get(0);

        setLinkAvailable(localDataSource, link);
        setLinkNotAvailable(cloudDataSource, link.getId());

        repository.getLink(link.getId()).subscribe(testLinkSubscriber);
        verify(localDataSource).getLink(eq(link.getId()));
        testLinkSubscriber.assertValue(link);
    }

    @Test
    public void saveLink_savesLinkToLocalAndCloudStorage() {
        Link link = LINKS.get(0);

        repository.saveLink(link);

        verify(localDataSource).saveLink(link);
        verify(cloudDataSource).saveLink(link);
    }

    private void setLinksAvailable(DataSource dataSource, List<Link> links) {
        when(dataSource.getLinks()).
                thenReturn(Observable.just(links).concatWith(Observable.<List<Link>>never()));
    }

    private void setLinksNotAvailable(DataSource dataSource) {
        when(dataSource.getLinks()).thenReturn(Observable.just(Collections.<Link>emptyList()));
    }

    private void setLinkAvailable(DataSource dataSource, Link link) {
        when(dataSource.getLink(eq(link.getId())))
                .thenReturn(Observable.just(link).concatWith(Observable.<Link>never()));
    }

    private void setLinkNotAvailable(DataSource dataSource, String linkId) {
        when(dataSource.getLink(eq(linkId)))
                .thenReturn(Observable.<Link>just(null).concatWith(Observable.<Link>never()));
    }
}