package com.bytesforge.linkasanote.data.source;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.utils.CommonUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class ProviderFavoritesTest extends ProviderTestCase2<Provider> {

    private final String KEY_PREFIX = CommonUtils.charRepeat('A', 21);
    private final String[] ENTRY_KEYS;
    private final List<Tag> FAVORITE_TAGS;
    private final String[] FAVORITE_NAMES;

    private ContentResolver contentResolver;
    private Provider provider;

    public ProviderFavoritesTest() {
        super(Provider.class, LocalContract.CONTENT_AUTHORITY);

        ENTRY_KEYS = new String[]{KEY_PREFIX + 'A', KEY_PREFIX + 'B'};
        FAVORITE_NAMES = new String[]{"Favorite", "Favorite #2"};
        FAVORITE_TAGS = new ArrayList<Tag>() {{
            add(new Tag("first"));
            add(new Tag("second"));
            add(new Tag("third"));
        }};
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
        final String favoriteId = ENTRY_KEYS[0];
        final Favorite favorite = new Favorite(favoriteId, FAVORITE_NAMES[0], FAVORITE_TAGS);

        insertFavoriteOnly(favorite);
        Favorite savedFavorite = queryFavoriteOnly(favoriteId, FAVORITE_TAGS);
        assertEquals(favorite, savedFavorite);
    }

    @Test
    public void provider_insertFavoriteEntryWithTags() {
        final String favoriteId = ENTRY_KEYS[0];
        final Favorite favorite = new Favorite(favoriteId, FAVORITE_NAMES[0], FAVORITE_TAGS);

        insertFavoriteWithTags(favorite);
        Favorite savedFavorite = queryFavoriteWithTags(favoriteId);
        assertEquals(favorite, savedFavorite);
    }

    @Test
    public void provider_deleteFavoriteButLeaveTags() {
        final String favoriteId = ENTRY_KEYS[0];
        final Favorite favorite = new Favorite(favoriteId, FAVORITE_NAMES[0], FAVORITE_TAGS);

        insertFavoriteWithTags(favorite);
        List<Tag> tags = queryAllTags();
        assertEquals(FAVORITE_TAGS, tags);

        int numRows = deleteFavorite(favoriteId);
        assertThat(numRows, equalTo(1));

        tags = queryAllTags();
        assertEquals(FAVORITE_TAGS, tags);
    }

    @Test
    public void provider_updateFavoriteEntry() {
        final String favoriteId = ENTRY_KEYS[0];
        final Favorite favorite = new Favorite(favoriteId, FAVORITE_NAMES[0], FAVORITE_TAGS);
        insertFavoriteWithTags(favorite);

        FAVORITE_TAGS.add(new Tag("four"));
        final Favorite updatedFavorite = new Favorite(favoriteId, FAVORITE_NAMES[1], FAVORITE_TAGS);
        insertFavoriteWithTags(updatedFavorite);

        Favorite savedFavorite = queryFavoriteWithTags(favoriteId);
        assertEquals(updatedFavorite, savedFavorite);

        List<Tag> tags = queryAllTags();
        assertEquals(FAVORITE_TAGS, tags);
    }

    private int deleteFavorite(String favoriteId) {
        final Uri favoriteUri = LocalContract.FavoriteEntry.buildFavoritesUriWith(favoriteId);

        String selection = LocalContract.FavoriteEntry.COLUMN_NAME_ENTRY_ID + " = ?";
        String[] selectionArgs = new String[]{favoriteId};
        return contentResolver.delete(favoriteUri, selection, selectionArgs);
    }

    @NonNull
    private String insertFavoriteOnly(Favorite favorite) {
        final Uri favoritesUri = LocalContract.FavoriteEntry.buildFavoritesUri();

        Uri newFavoriteUri = contentResolver.insert(
                favoritesUri, favorite.getContentValues());
        assertNotNull(newFavoriteUri);

        String newFavoriteRowId = LocalContract.FavoriteEntry.getFavoriteId(newFavoriteUri);
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

        String newTagRowId = LocalContract.TagEntry.getTagId(newTagUri);
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
        final Uri favoriteUri = LocalContract.FavoriteEntry.buildFavoritesUriWith(favoriteId);

        Cursor cursor = provider.query(favoriteUri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertThat(cursor.getCount(), equalTo(1));
        try {
            cursor.moveToLast();
            return Favorite.from(cursor, tags);
        } finally {
            cursor.close();
        }
    }

    @NonNull
    private Favorite queryFavoriteWithTags(String favoriteId) {
        assertNotNull(favoriteId);
        final Uri favoriteUri = LocalContract.FavoriteEntry.buildFavoritesUriWith(favoriteId);

        Cursor cursor = provider.query(favoriteUri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertThat(cursor.getCount(), equalTo(1));
        try {
            cursor.moveToLast();
            String rowId = LocalContract.rowIdFrom(cursor);
            return Favorite.from(cursor, queryFavoriteTags(rowId));
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
        final Uri tagsUri = LocalContract.TagEntry.buildTagsUri();
        Cursor cursor = provider.query(tagsUri, null, null, new String[]{}, null);
        assertNotNull(cursor);

        List<Tag> tags = new ArrayList<>();
        while (cursor.moveToNext()) tags.add(Tag.from(cursor));
        cursor.close();

        return tags;
    }
}
