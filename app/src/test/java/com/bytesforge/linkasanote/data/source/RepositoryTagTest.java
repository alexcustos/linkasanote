/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bytesforge.linkasanote.data.source;

import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryTagTest {

    private final List<Tag> TAGS;

    private Repository repository;

    @Mock
    private LocalDataSource localDataSource;

    @Mock
    private CloudDataSource cloudDataSource;

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
        BaseSchedulerProvider schedulerProvider = new ImmediateSchedulerProvider();
        repository = new Repository(localDataSource, cloudDataSource, schedulerProvider);
    }

    @Test
    public void getTags_requestsAllTagsFromLocalSource() {
        setTagsAvailable(localDataSource, TAGS);

        TestObserver<List<Tag>> testTagsObserver = repository.getTags().toList().test();

        verify(localDataSource).getTags();
        testTagsObserver.assertValue(TAGS);
    }

    @Test
    public void getTag_requestsSingleTagFromLocalSource() {
        Tag tag = TAGS.get(0);
        String tagName = tag.getName();
        assertNotNull(tagName);

        setTagAvailable(localDataSource, tag);

        TestObserver<Tag> testTagObserver = repository.getTag(tagName).test();
        verify(localDataSource).getTag(eq(tag.getName()));
        testTagObserver.assertValue(tag);
    }

    @Test
    public void saveTag_savesTagToLocalStorage() {
        Tag tag = TAGS.get(0);
        repository.saveTag(tag);

        verify(localDataSource).saveTag(tag);
        assertNotNull(repository.cachedTags);
        assertThat(repository.cachedTags.size(), is(1));
    }

    @Test
    public void deleteAllTags_deleteTagsFromLocalStorage() {
        for (Tag tag : TAGS) {
            repository.saveTag(tag);
        }
        assertNotNull(repository.cachedTags);
        assertThat(repository.cachedTags.size(), is(TAGS.size()));

        repository.deleteAllTags();
        verify(localDataSource).deleteAllTags();
        assertThat(repository.cachedTags.size(), is(0));
    }

    // Data setup

    private void setTagsAvailable(LocalDataSource localDataSource, List<Tag> tags) {
        when(localDataSource.getTags()).thenReturn(Observable.fromIterable(tags));
    }

    private void setTagsNotAvailable(LocalDataSource localDataSource) {
        when(localDataSource.getTags()).thenReturn(
                Observable.fromIterable(Collections.emptyList()));
    }

    private void setTagAvailable(LocalDataSource localDataSource, Tag tag) {
        String tagName = tag.getName();
        assert tagName != null;
        when(localDataSource.getTag(eq(tagName))).thenReturn(Single.just(tag));
    }

    private void setTagNotAvailable(LocalDataSource localDataSource, String tagName) {
        when(localDataSource.getTag(eq(tagName))).thenReturn(
                Single.error(new NoSuchElementException()));
    }
}