package com.bytesforge.linkasanote.data.source;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.source.local.PersistenceContract;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class ProviderTest extends ProviderTestCase2<Provider> {

    private final static String LINK_VALUE = "http://laano.net/link";
    private final static String LINK_KEY = StringUtils.repeat('A', 22);

    private ContentResolver contentResolver;

    public ProviderTest() {
        super(Provider.class, PersistenceContract.CONTENT_AUTHORITY);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        setContext(InstrumentationRegistry.getTargetContext());
        super.setUp();

        contentResolver = getMockContentResolver();
    }

    @Test
    public void provider_insertsValidLinkEntry() {
        final Uri uri = PersistenceContract.LinkEntry.buildLinksUri();
        Link link = new Link(LINK_KEY, LINK_VALUE, "Title for Link");

        final Uri newLinkUri = contentResolver.insert(uri, link.getContentValues());
        assertNotNull(newLinkUri);
        assertThat(newLinkUri.getLastPathSegment(), equalTo(LINK_KEY));

        Provider provider = getProvider();
        Cursor cursor = provider.query(newLinkUri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertThat(cursor.getCount(), equalTo(1));
        cursor.close();
    }
}