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
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.utils.CommonUtils;

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
public class ProviderLinksTest {

    private final String KEY_PREFIX = CommonUtils.charRepeat('A', 21);
    private final String[] ENTRY_KEYS;
    private final List<Tag> FAVORITE_TAGS;
    private final String[] FAVORITE_LINKS;
    private final String[] FAVORITE_NAMES;

    private ContentResolver contentResolver;

    @Rule
    public ProviderTestRule providerRule =
            new ProviderTestRule.Builder(Provider.class, LocalContract.CONTENT_AUTHORITY).build();

    public ProviderLinksTest() {
        ENTRY_KEYS = new String[]{KEY_PREFIX + 'A', KEY_PREFIX + 'B'};
        FAVORITE_LINKS = new String[]{"http://laano.net/link", "http://laano.net/link2"};
        FAVORITE_NAMES = new String[]{"Link", "Link #2"};
        FAVORITE_TAGS = new ArrayList<Tag>() {{
            add(new Tag("first"));
            add(new Tag("second"));
            add(new Tag("third"));
        }};
    }

    @Before
    public void setUp() throws Exception {
        contentResolver = providerRule.getResolver();
        AndroidTestUtils.cleanUpProvider(contentResolver);
    }

    @Test
    public void provider_insertLinkEntry() {
        final String linkId = ENTRY_KEYS[0];
        final Link link = new Link(linkId, FAVORITE_LINKS[0], FAVORITE_NAMES[0], false, FAVORITE_TAGS);

        insertLinkOnly(link);
        Link savedLink = queryLinkOnly(linkId, FAVORITE_TAGS);
        assertEquals(link, savedLink);
    }

    @Test
    public void provider_insertLinkEntryWithTags() {
        final String linkId = ENTRY_KEYS[0];
        final Link link = new Link(linkId, FAVORITE_LINKS[0], FAVORITE_NAMES[0], false, FAVORITE_TAGS);

        insertLinkWithTags(link);
        Link savedLink = queryLinkWithTags(linkId);
        assertEquals(link, savedLink);
    }

    @Test
    public void provider_deleteLinkButLeaveTags() {
        final String linkId = ENTRY_KEYS[0];
        final Link link = new Link(linkId, FAVORITE_LINKS[0], FAVORITE_NAMES[0], false, FAVORITE_TAGS);

        insertLinkWithTags(link);
        List<Tag> tags = queryAllTags();
        assertEquals(FAVORITE_TAGS, tags);

        int numRows = deleteLink(linkId);
        assertThat(numRows, equalTo(1));

        tags = queryAllTags();
        assertEquals(FAVORITE_TAGS, tags);
    }

    @Test
    public void provider_updateLinkEntry() {
        final String linkId = ENTRY_KEYS[0];
        final Link link = new Link(linkId, FAVORITE_LINKS[0], FAVORITE_NAMES[0], false, FAVORITE_TAGS);
        insertLinkWithTags(link);

        FAVORITE_TAGS.add(new Tag("four"));
        final Link updatedLink = new Link(linkId, FAVORITE_LINKS[1], FAVORITE_NAMES[1], false, FAVORITE_TAGS);
        insertLinkWithTags(updatedLink);

        Link savedLink = queryLinkWithTags(linkId);
        assertEquals(updatedLink, savedLink);

        List<Tag> tags = queryAllTags();
        assertEquals(FAVORITE_TAGS, tags);
    }

    private int deleteLink(String linkId) {
        final Uri linkUri = LocalContract.LinkEntry.buildUriWith(linkId);

        String selection = LocalContract.LinkEntry.COLUMN_NAME_ENTRY_ID + " = ?";
        String[] selectionArgs = new String[]{linkId};
        return contentResolver.delete(linkUri, selection, selectionArgs);
    }

    @NonNull
    private String insertLinkOnly(Link link) {
        final Uri linksUri = LocalContract.LinkEntry.buildUri();

        Uri newLinkUri = contentResolver.insert(
                linksUri, link.getContentValues());
        assertNotNull(newLinkUri);

        String newLinkRowId = LocalContract.LinkEntry.getIdFrom(newLinkUri);
        assertNotNull(newLinkRowId);
        assertTrue(Long.parseLong(newLinkRowId) > 0);
        return newLinkRowId;
    }

    @NonNull
    private String insertLinkTag(String linkRowId, Tag tag) {
        final Uri linkTagsUri = LocalContract.LinkEntry.buildTagsDirUriWith(linkRowId);

        ContentValues values = tag.getContentValues();
        Uri newTagUri = contentResolver.insert(linkTagsUri, values);
        assertNotNull(newTagUri);

        String newTagRowId = LocalContract.TagEntry.getNameFrom(newTagUri);
        assertNotNull(newTagRowId);
        assertTrue(Long.parseLong(newTagRowId) > 0);
        return newTagRowId;
    }

    @NonNull
    private String insertLinkWithTags(Link link) {
        String linkRowId = insertLinkOnly(link);
        List<Tag> tags = link.getTags();
        assertNotNull(tags);
        for (Tag tag : tags) insertLinkTag(linkRowId, tag);

        List<Tag> savedTags = queryLinkTags(linkRowId);
        assertEquals(tags, savedTags);

        return linkRowId;
    }


    @NonNull
    private Link queryLinkOnly(String linkId, List<Tag> tags) {
        assertNotNull(linkId);
        assertNotNull(tags);
        final Uri linkUri = LocalContract.LinkEntry.buildUriWith(linkId);

        Cursor cursor = contentResolver.query(linkUri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertThat(cursor.getCount(), equalTo(1));
        try {
            cursor.moveToLast();
            return new Link(Link.from(cursor), tags, null);
        } finally {
            cursor.close();
        }
    }

    @NonNull
    private Link queryLinkWithTags(String linkId) {
        assertNotNull(linkId);
        final Uri linkUri = LocalContract.LinkEntry.buildUriWith(linkId);

        Cursor cursor = contentResolver.query(linkUri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertThat(cursor.getCount(), equalTo(1));
        try {
            cursor.moveToLast();
            String rowId = LocalContract.rowIdFrom(cursor);
            return new Link(Link.from(cursor), queryLinkTags(rowId), null);
        } finally {
            cursor.close();
        }
    }

    @NonNull
    private List<Tag> queryLinkTags(String linkRowId) {
        final Uri linkTagsUri = LocalContract.LinkEntry.buildTagsDirUriWith(linkRowId);

        Cursor cursor = contentResolver.query(linkTagsUri, null, null, new String[]{}, null);
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
