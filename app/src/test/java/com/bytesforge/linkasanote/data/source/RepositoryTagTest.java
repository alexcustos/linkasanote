package com.bytesforge.linkasanote.data.source;

import com.bytesforge.linkasanote.data.Tag;

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

public class RepositoryTagTest {

    private final List<String> TAG_NAMES;
    private final List<Tag> TAGS;

    private TestSubscriber<List<Tag>> testTagsSubscriber;
    private TestSubscriber<Tag> testTagSubscriber;

    private Repository repository;

    @Mock
    private DataSource localDataSource;

    @Mock
    private DataSource cloudDataSource;

    public RepositoryTagTest() {
        TAG_NAMES = new ArrayList<>();
        TAG_NAMES.add("first");
        TAG_NAMES.add("second");
        TAG_NAMES.add("third");

        TAGS = new ArrayList<>();
        TAGS.add(new Tag(TAG_NAMES.get(0)));
        TAGS.add(new Tag(TAG_NAMES.get(1)));
        TAGS.add(new Tag(TAG_NAMES.get(2)));

        testTagsSubscriber = new TestSubscriber<>();
        testTagSubscriber = new TestSubscriber<>();
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

        repository.getTags().subscribe(testTagsSubscriber);

        verify(localDataSource).getTags();
        testTagsSubscriber.assertValue(TAGS);
    }

    @Test
    public void getTag_requestsSingleTagFromLocalSource() {
        Tag tag = TAGS.get(0);

        setTagAvailable(localDataSource, tag);

        repository.getTag(tag.getName()).subscribe(testTagSubscriber);
        verify(localDataSource).getTag(eq(tag.getName()));
        testTagSubscriber.assertValue(tag);
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
        when(dataSource.getTags()).
                thenReturn(Observable.just(tags).concatWith(Observable.<List<Tag>>never()));
    }

    private void setTagsNotAvailable(DataSource dataSource) {
        when(dataSource.getTags()).thenReturn(Observable.just(Collections.<Tag>emptyList()));
    }

    private void setTagAvailable(DataSource dataSource, Tag tag) {
        when(dataSource.getTag(eq(tag.getName())))
                .thenReturn(Observable.just(tag).concatWith(Observable.<Tag>never()));
    }

    private void setTagNotAvailable(DataSource dataSource, String tagId) {
        when(dataSource.getTag(eq(tagId)))
                .thenReturn(Observable.<Tag>just(null).concatWith(Observable.<Tag>never()));
    }
}