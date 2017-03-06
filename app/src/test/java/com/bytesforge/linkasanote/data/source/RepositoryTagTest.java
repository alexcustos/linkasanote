package com.bytesforge.linkasanote.data.source;

import com.bytesforge.linkasanote.data.Tag;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
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

public class RepositoryTagTest {

    private final List<Tag> TAGS;

    private Repository repository;

    @Mock
    private DataSource localDataSource;

    @Mock
    private DataSource cloudDataSource;

    public RepositoryTagTest() {
        TAGS = new ArrayList<Tag>() {{
            add(new Tag("first"));
            add(new Tag("second"));
            add(new Tag("third"));
        }};
    }

    @Before
    public void setupRepository() {
        MockitoAnnotations.initMocks(this);
        repository = new Repository(localDataSource, cloudDataSource);
    }

    @Test
    public void getTags_requestsAllTagsFromLocalSource() {
        setTagsAvailable(localDataSource, TAGS);
        setTagsNotAvailable(cloudDataSource);


        TestObserver<List<Tag>> testTagsObserver = repository.getTags().test();

        verify(localDataSource).getTags();
        testTagsObserver.assertValue(TAGS);
    }

    @Test
    public void getTag_requestsSingleTagFromLocalSource() {
        Tag tag = TAGS.get(0);

        setTagAvailable(localDataSource, tag);

        TestObserver<Tag> testTagObserver = repository.getTag(tag.getName()).test();
        verify(localDataSource).getTag(eq(tag.getName()));
        testTagObserver.assertValue(tag);
    }

    @Test
    public void saveTag_savesTagToLocalStorage() {
        Tag tag = TAGS.get(0);
        repository.saveTag(tag);

        verify(localDataSource).saveTag(tag);
        assertThat(repository.cachedTags.size(), is(1));
    }

    @Test
    public void deleteAllTags_deleteTagsFromLocalStorage() {
        for (Tag tag : TAGS) {
            repository.saveTag(tag);
        }
        assertThat(repository.cachedTags.size(), is(TAGS.size()));

        repository.deleteAllTags();
        verify(localDataSource).deleteAllTags();
        assertThat(repository.cachedTags.size(), is(0));
    }

    // Data setup

    private void setTagsAvailable(DataSource dataSource, List<Tag> tags) {
        when(dataSource.getTags()).thenReturn(Single.just(tags));
    }

    private void setTagsNotAvailable(DataSource dataSource) {
        when(dataSource.getTags()).thenReturn(Single.just(Collections.emptyList()));
    }

    private void setTagAvailable(DataSource dataSource, Tag tag) {
        when(dataSource.getTag(eq(tag.getName()))).thenReturn(Single.just(tag));
    }

    private void setTagNotAvailable(DataSource dataSource, String tagName) {
        when(dataSource.getTag(eq(tagName))).thenReturn(Single.error(new NoSuchElementException()));
    }
}