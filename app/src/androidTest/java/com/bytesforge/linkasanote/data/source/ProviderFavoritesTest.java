package com.bytesforge.linkasanote.data.source;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.bytesforge.linkasanote.AndroidTestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.local.LocalContract;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class ProviderFavoritesTest extends ProviderTestCase2<Provider> {

    private final List<Favorite> FAVORITES;

    private ContentResolver contentResolver;
    private Provider provider;

    public ProviderFavoritesTest() {
        super(Provider.class, LocalContract.CONTENT_AUTHORITY);

        FAVORITES = AndroidTestUtils.buildFavorites();
    }

    @Before
    @Override
    public void setUp() throws Exception {
        setContext(InstrumentationRegistry.getTargetContext());
        super.setUp();

        contentResolver = getMockContentResolver();
        provider = getProvider();
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

        Cursor cursor = provider.query(favoriteUri, null, null, new String[]{}, null);
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

        Cursor cursor = provider.query(favoriteUri, null, null, new String[]{}, null);
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

        Cursor cursor = provider.query(favoriteTagsUri, null, null, new String[]{}, null);
        assertNotNull(cursor);

        List<Tag> tags = new ArrayList<>();
        while (cursor.moveToNext()) tags.add(Tag.from(cursor));
        cursor.close();

        return tags;
    }

    @NonNull
    private List<Tag> queryAllTags() {
        final Uri tagsUri = LocalContract.TagEntry.buildUri();
        Cursor cursor = provider.query(tagsUri, null, null, new String[]{}, null);
        assertNotNull(cursor);

        List<Tag> tags = new ArrayList<>();
        while (cursor.moveToNext()) tags.add(Tag.from(cursor));
        cursor.close();

        return tags;
    }
}
