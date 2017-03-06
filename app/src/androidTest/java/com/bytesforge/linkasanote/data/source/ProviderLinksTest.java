package com.bytesforge.linkasanote.data.source;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;

import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.utils.CommonUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class ProviderLinksTest extends ProviderTestCase2<Provider> {

    private final String ENTRY_KEY = CommonUtils.charRepeat('A', 22);
    private final String LINK_VALUE = "http://laano.net/link";
    private final String LINK_TITLE = "Title of the Link";

    private ContentResolver contentResolver;
    private Provider provider;

    public ProviderLinksTest() {
        super(Provider.class, LocalContract.CONTENT_AUTHORITY);
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
}