package com.bytesforge.linkasanote.data.source;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.utils.CommonUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryLinkTest {

    private final String KEY_PREFIX = CommonUtils.charRepeat('A', 21);
    private final List<Link> LINKS;

    private Repository repository;

    @Mock
    private DataSource localDataSource;

    @Mock
    private DataSource cloudDataSource;

    public RepositoryLinkTest() {
        LINKS = TestUtils.buildLinks();
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

        TestObserver<List<Link>> testLinksObserver = repository.getLinks().test();
        verify(localDataSource).getLinks();
        testLinksObserver.assertValue(LINKS);
    }

    @Test
    public void getLink_requestsSingleLinkFromLocalDataSource() {
        Link link = LINKS.get(0);

        setLinkAvailable(localDataSource, link);
        setLinkNotAvailable(cloudDataSource, link.getId());

        TestObserver<Link> testLinkObserver = repository.getLink(link.getId()).test();
        verify(localDataSource).getLink(eq(link.getId()));
        testLinkObserver.assertValue(link);
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
        when(dataSource.getLinks()).thenReturn(Single.just(links));
    }

    private void setLinksNotAvailable(DataSource dataSource) {
        when(dataSource.getLinks()).thenReturn(Single.just(Collections.emptyList()));
    }

    private void setLinkAvailable(DataSource dataSource, Link link) {
        when(dataSource.getLink(eq(link.getId()))).thenReturn(Single.just(link));
    }

    private void setLinkNotAvailable(DataSource dataSource, String linkId) {
        when(dataSource.getLink(eq(linkId))).thenReturn(Single.error(new NoSuchElementException()));
    }
}