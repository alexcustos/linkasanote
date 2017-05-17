package com.bytesforge.linkasanote.data.source;

import android.util.Log;

import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class RepositoryLinkTest {

    private final List<Link> LINKS;

    private Repository repository;

    @Mock
    private LocalDataSource localDataSource;

    @Mock
    private CloudDataSource cloudDataSource;

    private TestObserver<List<Link>> testLinksObserver;
    private TestObserver<Link> testLinkObserver;

    public RepositoryLinkTest() {
        LINKS = TestUtils.buildLinks();
    }

    @Before
    public void setupRepository() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Log.class);
        repository = new Repository(localDataSource, cloudDataSource);
    }

    @Test
    public void getLinks_requestsAllLinksFromLocalSource() {
        repository.linkCacheIsDirty = true;
        when(localDataSource.getLinks(isNull()))
                .thenReturn(Observable.fromIterable(LINKS));
        testLinksObserver = repository.getLinks().toList().test();
        testLinksObserver.assertValue(LINKS);
        assert repository.cachedLinks != null;
        assertThat(repository.cachedLinks.size(), is(LINKS.size()));
        Collection<Link> cachedLinks = repository.cachedLinks.values();
        Iterator iterator;
        int i;
        for (i = 0, iterator = cachedLinks.iterator(); iterator.hasNext(); i++) {
            Link cachedLink = (Link) iterator.next();
            assertThat(cachedLink.getId(), is(LINKS.get(i).getId()));
        }
    }

    @Test
    public void getLink_requestsSingleLinkFromLocalSource() {
        repository.linkCacheIsDirty = true;
        Link link = LINKS.get(0);
        String linkId = link.getId();
        when(localDataSource.getLink(eq(linkId))).thenReturn(Single.just(link));

        testLinkObserver = repository.getLink(linkId).test();
        testLinkObserver.assertValue(link);
        assertThat(repository.linkCacheIsDirty, is(true));
    }

    @Test
    public void saveLink_savesLinkToLocalAndCloudStorage() {
        Link link = LINKS.get(0);
        String linkId = link.getId();
        when(localDataSource.saveLink(eq(link)))
                .thenReturn(Single.just(DataSource.ItemState.DEFERRED));
        when(cloudDataSource.saveLink(eq(linkId)))
                .thenReturn(Single.just(DataSource.ItemState.SAVED));

        TestObserver<DataSource.ItemState> saveLinkObserver =
                repository.saveLink(link, true).test();
        saveLinkObserver.assertValues(
                DataSource.ItemState.DEFERRED, DataSource.ItemState.SAVED);
        assertThat(repository.linkCacheIsDirty, is(true));
        assert repository.dirtyLinks != null;
        assertThat(repository.dirtyLinks.contains(linkId), is(true));
    }

    @Test
    public void deleteLink_deleteLinkFromLocalAndCloudStorage() {
        int size = LINKS.size();
        Link link = LINKS.get(0);
        String linkId = link.getId();
        // Cache
        when(localDataSource.getLinks(isNull()))
                .thenReturn(Observable.fromIterable(LINKS));
        testLinksObserver = repository.getLinks().toList().test();
        testLinksObserver.assertValue(LINKS);
        assert repository.cachedLinks != null;
        assertThat(repository.cachedLinks.size(), is(size));
        // Preconditions
        when(localDataSource.deleteLink(eq(linkId)))
                .thenReturn(Single.just(DataSource.ItemState.DEFERRED));
        when(localDataSource.getNotes(eq(linkId)))
                .thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(cloudDataSource.deleteLink(eq(linkId)))
                .thenReturn(Single.just(DataSource.ItemState.DELETED));
        // Test
        TestObserver<DataSource.ItemState> deleteLinkObserver =
                repository.deleteLink(linkId, true, false).test();
        deleteLinkObserver.assertValues(
                DataSource.ItemState.DEFERRED, DataSource.ItemState.DELETED);
        assertThat(repository.linkCacheIsDirty, is(false));
        assertThat(repository.cachedLinks.size(), is(size - 1));
        Collection<Link> cachedLinks = repository.cachedLinks.values();
        Iterator iterator;
        int i;
        for (i = 0, iterator = cachedLinks.iterator(); iterator.hasNext(); i++) {
            if (linkId.equals(LINKS.get(i).getId())) continue;

            Link cachedLink = (Link) iterator.next();
            assertThat(cachedLink.getId(), is(LINKS.get(i).getId()));
        }
    }
}
