package com.bytesforge.linkasanote.data.source;

import com.bytesforge.linkasanote.data.Link;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.observers.TestSubscriber;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryLinkTest {

    private final List<String> LINK_VALUES;
    private final List<String> LINK_TITLES;
    private final List<Link> LINKS;

    private TestSubscriber<List<Link>> testLinksSubscriber;
    private TestSubscriber<Link> testLinkSubscriber;

    private Repository repository;

    @Mock
    private DataSource localDataSource;

    @Mock
    private DataSource cloudDataSource;

    public RepositoryLinkTest() {
        LINK_VALUES = new ArrayList<>();
        LINK_VALUES.add("http://laano.net/link");
        LINK_VALUES.add("http://laano.net/link2");
        LINK_VALUES.add("http://laano.net/link3");

        LINK_TITLES = new ArrayList<>();
        LINK_TITLES.add("Title for Link");
        LINK_TITLES.add("Title for Link #2");
        LINK_TITLES.add("Title for Link #3");

        String keyPrefix = StringUtils.repeat('A', 21);
        LINKS = new ArrayList<>();
        LINKS.add(new Link(keyPrefix + 'A', LINK_VALUES.get(0), LINK_TITLES.get(0)));
        LINKS.add(new Link(keyPrefix + 'B', LINK_VALUES.get(1), LINK_TITLES.get(1)));
        LINKS.add(new Link(keyPrefix + 'C', LINK_VALUES.get(2), LINK_TITLES.get(2)));

        testLinksSubscriber = new TestSubscriber<>();
        testLinkSubscriber = new TestSubscriber<>();
    }

    @Before
    public void setupRepository() {
        MockitoAnnotations.initMocks(this);
        repository = new Repository(localDataSource, cloudDataSource);
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
        assertThat(repository.cachedLinks.size(), is(1));
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