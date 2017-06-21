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

package com.bytesforge.linkasanote.data.source.local;

import android.content.ContentResolver;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.bytesforge.linkasanote.AndroidTestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.FavoriteFactory;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.LinkFactory;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.NoteFactory;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.ImmediateSchedulerProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.observers.TestObserver;

import static junit.framework.TestCase.assertNotNull;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LocalDataSourceTest {

    private final BaseSchedulerProvider schedulerProvider;
    private final ContentResolver contentResolver;
    private LocalDataSource localDataSource;

    private List<Favorite> FAVORITES;
    private TestObserver<List<Favorite>> testFavoritesObserver;
    private TestObserver<Favorite> testFavoriteObserver;

    public LocalDataSourceTest() {
        schedulerProvider = new ImmediateSchedulerProvider();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        contentResolver = context.getContentResolver();
    }

    @Before
    public void setupLocalDataSource() {
        FAVORITES = AndroidTestUtils.buildFavorites();
        LocalTags localTags = new LocalTags(contentResolver);
        LocalSyncResults localSyncResults = new LocalSyncResults(contentResolver);

        NoteFactory<Note> noteFactory = Note.getFactory();
        LocalNotes<Note> localNotes = new LocalNotes<>(contentResolver,
                localSyncResults, localTags, noteFactory);

        LinkFactory<Link> linkFactory = Link.getFactory();
        LocalLinks<Link> localLinks = new LocalLinks<>(contentResolver,
                localSyncResults, localTags, localNotes, linkFactory);

        FavoriteFactory<Favorite> favoriteFactory = Favorite.getFactory();
        LocalFavorites<Favorite> localFavorites = new LocalFavorites<>(contentResolver,
                localSyncResults, localTags, favoriteFactory);

        localDataSource = new LocalDataSource(localSyncResults,
                localLinks, localFavorites, localNotes, localTags);
        cleanupLocalDataSource();
    }

    @After
    public void cleanupLocalDataSource() {
        localDataSource.deleteAllLinks();
        localDataSource.deleteAllFavorites();
        localDataSource.deleteAllNotes();
        localDataSource.deleteAllTags();
    }

    @Test
    public void saveFavorite_retrievesFavorite() {
        Favorite favorite = FAVORITES.get(0);
        // Preconditions
        testFavoriteObserver = localDataSource.getFavorite(favorite.getId()).test();
        testFavoriteObserver.assertError(NoSuchElementException.class);
        // Test
        TestObserver<DataSource.ItemState> testSaveObserver =
                localDataSource.saveFavorite(favorite).test();
        testSaveObserver.assertValue(DataSource.ItemState.DEFERRED);
        testFavoriteObserver = localDataSource.getFavorite(favorite.getId()).test();
        testFavoriteObserver.assertValue(favorite);

    }

    @Test
    public void getFavorites_retrievesSavedFavorites() {
        // Preconditions
        testFavoritesObserver = localDataSource.getFavorites().toList().test();
        testFavoritesObserver.assertValue(Collections.emptyList());
        for (Favorite favorite : FAVORITES) {
            TestObserver<DataSource.ItemState> testSaveObserver =
                    localDataSource.saveFavorite(favorite).test();
            testSaveObserver.assertValue(DataSource.ItemState.DEFERRED);
            List<Tag> tags = favorite.getTags();
            assertNotNull(tags);
        }
        // Test
        testFavoritesObserver = localDataSource.getFavorites().toList().test();
        testFavoritesObserver.assertValue(FAVORITES);
    }

    @Test
    public void deleteAllFavorites_emptyListOfRetrievedFavorites() {
        // Preconditions
        for (Favorite favorite : FAVORITES) {
            TestObserver<DataSource.ItemState> testSaveObserver =
                    localDataSource.saveFavorite(favorite).test();
            testSaveObserver.assertValue(DataSource.ItemState.DEFERRED);
            List<Tag> tags = favorite.getTags();
            assertNotNull(tags);
        }
        testFavoritesObserver = localDataSource.getFavorites().toList().test();
        testFavoritesObserver.assertValue(FAVORITES);
        // Test
        localDataSource.deleteAllFavorites();
        testFavoritesObserver = localDataSource.getFavorites().toList().test();
        testFavoritesObserver.assertValue(Collections.emptyList());
    }

    @Test
    public void deleteFavorite_remainsListOfOtherFavorites() {
        // Preconditions
        for (Favorite favorite : FAVORITES) {
            TestObserver<DataSource.ItemState> testSaveObserver =
                    localDataSource.saveFavorite(favorite).test();
            testSaveObserver.assertValue(DataSource.ItemState.DEFERRED);
            List<Tag> tags = favorite.getTags();
            assertNotNull(tags);
            //Collections.sort(tags);
        }
        testFavoritesObserver = localDataSource.getFavorites().toList().test();
        testFavoritesObserver.assertValue(FAVORITES);
        // Test
        Favorite favorite = FAVORITES.remove(0);
        TestObserver<DataSource.ItemState> testDeleteObserver =
                localDataSource.deleteFavorite(favorite.getId()).test();
        testDeleteObserver.assertValue(DataSource.ItemState.DELETED);
        testFavoritesObserver = localDataSource.getFavorites().toList().test();
        testFavoritesObserver.assertValue(FAVORITES);
    }
}