package com.bytesforge.linkasanote;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class TestUtils {

    public static final String KEY_PREFIX = CommonUtils.charRepeat('A', 21);
    public static final List<Tag> FAVORITE_TAGS = new ArrayList<Tag>() {{
        add(new Tag("first"));
        add(new Tag("second"));
    }};
    public static final List<Tag> FAVORITE_TAGS2 = new ArrayList<Tag>() {{
        addAll(FAVORITE_TAGS);
        add(new Tag("third"));
    }};
    public static final List<Tag> FAVORITE_TAGS3 = new ArrayList<Tag>() {{
        addAll(FAVORITE_TAGS2);
        add(new Tag("fourth"));
    }};

    public static List<Favorite> buildFavorites() {
        return new ArrayList<Favorite>() {{
            add(new Favorite(KEY_PREFIX + 'A', "Favorite", FAVORITE_TAGS));
            add(new Favorite(KEY_PREFIX + 'B', "Favorite #2", FAVORITE_TAGS2));
            add(new Favorite(KEY_PREFIX + 'C', "Favorite #3", FAVORITE_TAGS3));
        }};
    }

    public static List<Link> buildLinks() {
        return new ArrayList<Link>() {{
            add(new Link(KEY_PREFIX + 'A', "http://laano.net/link", "Title for Link"));
            add(new Link(KEY_PREFIX + 'B', "http://laano.net/link2", "Title for Link #2"));
            add(new Link(KEY_PREFIX + 'C', "http://laano.net/link3", "Title for Link #3"));
        }};
    }
}
