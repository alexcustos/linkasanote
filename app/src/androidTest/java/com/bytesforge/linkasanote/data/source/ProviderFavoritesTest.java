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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.rule.provider.ProviderTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.bytesforge.linkasanote.AndroidTestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.local.LocalContract;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ProviderFavoritesTest {

    private final List<Favorite> FAVORITES;

    private ContentResolver contentResolver;

    @Rule
    public ProviderTestRule providerRule =
            new ProviderTestRule.Builder(Provider.class, LocalContract.CONTENT_AUTHORITY).build();

    public ProviderFavoritesTest() {
        FAVORITES = AndroidTestUtils.buildFavorites();
    }

    @Before
    public void setUp() throws Exception {
        contentResolver = providerRule.getResolver();
        AndroidTestUtils.cleanUpProvider(contentResolver);
    }

    @Test
    public void provider_insertFavoriteEntry() {
        final Favorite favorite = FAVORITES.get(0);
        final String favoriteId = favorite.getId();

        insertFavoriteOnly(favorite);
        Favorite savedFavorite = queryFavoriteOnly(favoriteId, favorite.getTags());
        assertEquals(favorite, savedFavorite);
    }

    @Test
    public void provider_insertFavoriteEntryWithTags() {
        final Favorite favorite = FAVORITES.get(0);
        final String favoriteId = favorite.getId();

        insertFavoriteWithTags(favorite);
        Favorite savedFavorite = queryFavoriteWithTags(favoriteId);
        assertEquals(favorite, savedFavorite);
    }

    @Test
    public void provider_deleteFavoriteButLeaveTags() {
        final Favorite favorite = FAVORITES.get(0);
        final String favoriteId = favorite.getId();
        final List<Tag> favoriteTags = favorite.getTags();

        insertFavoriteWithTags(favorite);
        List<Tag> tags = queryAllTags();
        assertEquals(favoriteTags, tags);

        int numRows = deleteFavorite(favoriteId);
        assertThat(numRows, equalTo(1));

        tags = queryAllTags();
        assertEquals(favoriteTags, tags);
    }

    @Test
    public void provider_updateFavoriteEntry() {
        final Favorite favorite = FAVORITES.get(0);
        final String favoriteId = favorite.getId();
        assert favorite.getTags() != null;
        final List<Tag> favoriteTags = new ArrayList<>(favorite.getTags());
        insertFavoriteWithTags(favorite);

        favoriteTags.add(new Tag("third"));
        final Favorite updatedFavorite = new Favorite(favorite, favoriteTags);
        insertFavoriteWithTags(updatedFavorite);

        Favorite savedFavorite = queryFavoriteWithTags(favoriteId);
        assertEquals(updatedFavorite, savedFavorite);

        List<Tag> tags = queryAllTags();
        assertEquals(favoriteTags, tags);
    }

    private int deleteFavorite(String favoriteId) {
        final Uri favoriteUri = LocalContract.FavoriteEntry.buildUriWith(favoriteId);

        String selection = LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID + " = ?";
        String[] selectionArgs = new String[]{favoriteId};
        return contentResolver.delete(favoriteUri, selection, selectionArgs);
    }

    @NonNull
    private String insertFavoriteOnly(Favorite favorite) {
        final Uri favoritesUri = LocalContract.FavoriteEntry.buildUri();

        Uri newFavoriteUri = contentResolver.insert(
                favoritesUri, favorite.getContentValues());
        assertNotNull(newFavoriteUri);

        String newFavoriteRowId = LocalContract.FavoriteEntry.getIdFrom(newFavoriteUri);
        assertNotNull(newFavoriteRowId);
        assertTrue(Long.parseLong(newFavoriteRowId) > 0);
        return newFavoriteRowId;
    }

    @NonNull
    private String insertFavoriteTag(String favoriteRowId, Tag tag) {
        final Uri favoriteTagsUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(favoriteRowId);

        ContentValues values = tag.getContentValues();
        Uri newTagUri = contentResolver.insert(favoriteTagsUri, values);
        assertNotNull(newTagUri);

        String newTagRowId = LocalContract.TagEntry.getNameFrom(newTagUri);
        assertNotNull(newTagRowId);
        assertTrue(Long.parseLong(newTagRowId) > 0);
        return newTagRowId;
    }

    @NonNull
    private String insertFavoriteWithTags(Favorite favorite) {
        String favoriteRowId = insertFavoriteOnly(favorite);
        List<Tag> tags = favorite.getTags();
        assertNotNull(tags);
        for (Tag tag : tags) insertFavoriteTag(favoriteRowId, tag);

        List<Tag> savedTags = queryFavoriteTags(favoriteRowId);
        assertEquals(tags, savedTags);

        return favoriteRowId;
    }


    @NonNull
    private Favorite queryFavoriteOnly(String favoriteId, List<Tag> tags) {
        assertNotNull(favoriteId);
        assertNotNull(tags);
        final Uri favoriteUri = LocalContract.FavoriteEntry.buildUriWith(favoriteId);

        Cursor cursor = contentResolver.query(favoriteUri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertThat(cursor.getCount(), equalTo(1));
        try {
            cursor.moveToLast();
            return new Favorite(Favorite.from(cursor), tags);
        } finally {
            cursor.close();
        }
    }

    @NonNull
    private Favorite queryFavoriteWithTags(String favoriteId) {
        assertNotNull(favoriteId);
        final Uri favoriteUri = LocalContract.FavoriteEntry.buildUriWith(favoriteId);

        Cursor cursor = contentResolver.query(favoriteUri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertThat(cursor.getCount(), equalTo(1));
        try {
            cursor.moveToLast();
            String rowId = LocalContract.rowIdFrom(cursor);
            return new Favorite(Favorite.from(cursor), queryFavoriteTags(rowId));
        } finally {
            cursor.close();
        }
    }

    @NonNull
    private List<Tag> queryFavoriteTags(String favoriteRowId) {
        final Uri favoriteTagsUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(favoriteRowId);

        Cursor cursor = contentResolver.query(favoriteTagsUri, null, null, new String[]{}, null);
        assertNotNull(cursor);

        List<Tag> tags = new ArrayList<>();
        while (cursor.moveToNext()) tags.add(Tag.from(cursor));
        cursor.close();

        return tags;
    }

    @NonNull
    private List<Tag> queryAllTags() {
        final Uri tagsUri = LocalContract.TagEntry.buildUri();
        Cursor cursor = contentResolver.query(tagsUri, null, null, new String[]{}, null);
        assertNotNull(cursor);

        List<Tag> tags = new ArrayList<>();
        while (cursor.moveToNext()) tags.add(Tag.from(cursor));
        cursor.close();

        return tags;
    }
}
