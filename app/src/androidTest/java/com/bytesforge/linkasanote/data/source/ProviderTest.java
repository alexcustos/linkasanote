package com.bytesforge.linkasanote.data.source;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
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
public class ProviderTest extends ProviderTestCase2<Provider> {

    private final String ENTRY_KEY = CommonUtils.charRepeat('A', 22);
    private final List<Tag> TAGS;
    private final String LINK_VALUE = "http://laano.net/link";
    private final String LINK_TITLE = "Title of the Link";
    private final String FAVORITE_NAME = "Favorite";

    private ContentResolver contentResolver;
    private Provider provider;

    public ProviderTest() {
        super(Provider.class, LocalContract.CONTENT_AUTHORITY);

        TAGS = new ArrayList<>();
        TAGS.add(new Tag("first"));
        TAGS.add(new Tag("second"));
        TAGS.add(new Tag("third"));
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
    public void provider_insertsValidLinkEntry() {
        final Uri insertUri = LocalContract.LinkEntry.buildLinksUri();
        final Link link = new Link(ENTRY_KEY, LINK_VALUE, LINK_TITLE);

        Uri linkUri = contentResolver.insert(insertUri, link.getContentValues());
        assertNotNull(linkUri);
        assertTrue(Long.parseLong(LocalContract.LinkEntry.getLinkId(linkUri)) > 0);

        linkUri = LocalContract.LinkEntry.buildLinksUriWith(link.getId());
        Cursor cursor = provider.query(linkUri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertThat(cursor.getCount(), equalTo(1));
        cursor.close();

        // TODO: complete the test
    }

    @Test
    public void provider_insertsValidFavoriteEntryWithTags() {
        final Uri insertUri = LocalContract.FavoriteEntry.buildFavoritesUri();
        final Favorite favorite = new Favorite(ENTRY_KEY, FAVORITE_NAME, TAGS);

        // Favorite
        Uri newFavoriteUri = contentResolver.insert(insertUri, favorite.getContentValues());
        assertNotNull(newFavoriteUri);
        assertTrue(Long.parseLong(LocalContract.FavoriteEntry.getFavoriteId(newFavoriteUri)) > 0);

        Uri favoriteUri = LocalContract.FavoriteEntry.buildFavoritesUriWith(favorite.getId());
        Cursor cursor = provider.query(favoriteUri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertThat(cursor.getCount(), equalTo(1));
        cursor.close();

        // Tags
        Uri tagsUri = LocalContract.FavoriteEntry.buildTagsDirUriWith(
                LocalContract.FavoriteEntry.getFavoriteId(newFavoriteUri));
        for (Tag tag : favorite.getTags()) {
            ContentValues values = tag.getContentValues();
            Uri newTagUri = contentResolver.insert(tagsUri, values);
            assertNotNull(newTagUri);
            assertTrue(Long.parseLong(LocalContract.TagEntry.getTagId(newTagUri)) > 0);
        }

        cursor = provider.query(tagsUri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertThat(cursor.getCount(), equalTo(TAGS.size()));
        cursor.close();
    }
}