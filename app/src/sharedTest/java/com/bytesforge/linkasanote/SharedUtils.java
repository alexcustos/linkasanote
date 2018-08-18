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

package com.bytesforge.linkasanote;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class SharedUtils {

    public static String KEY_PREFIX = CommonUtils.charRepeat('A', 21);
    public static final List<Tag> TAGS = new ArrayList<Tag>() {{
        add(new Tag("first"));
        add(new Tag("second tag"));
    }};
    public static final List<Tag> TAGS2 = new ArrayList<Tag>() {{
        add(new Tag("second tag"));
        add(new Tag("third"));
    }};
    public static final List<Tag> TAGS3 = new ArrayList<Tag>() {{
        add(new Tag("third"));
        add(new Tag("fourth"));
        add(new Tag("fifth"));
    }};

    private static final List<Favorite> FAVORITES = new ArrayList<Favorite>() {{
        add(new Favorite(KEY_PREFIX + 'A', "Favorite title", false, TAGS));
        add(new Favorite(KEY_PREFIX + 'B', "Second Favorite", true, TAGS2));
        add(new Favorite(KEY_PREFIX + 'C', "Third Favorite with very long title which end up with ellipsis", false, TAGS3));
    }};

    private static final List<Link> LINKS = new ArrayList<Link>() {{
        add(new Link(KEY_PREFIX + 'D', "http://laano.net/link", "Laano Link title", false, TAGS));
        add(new Link(KEY_PREFIX + 'E', "http://laano.net/link2", "Disabled Link with no tags", true, null));
        add(new Link(KEY_PREFIX + 'F', "http://laano.net/link3", "Link with unique tags", false, TAGS3));
        add(new Link(KEY_PREFIX + 'G', "http://laano.net/link4", null, false, TAGS2));
        add(new Link(KEY_PREFIX + 'H', "http://laano.net/link5", null, false, null));
    }};

    private static final List<Note> NOTES = new ArrayList<Note>() {{
        add(new Note(KEY_PREFIX + 'I', "Simple Note which is not bound to any Link", null, TAGS));
        add(new Note(KEY_PREFIX + 'G', "Multiline Note\n" +
                "Line number two of this note confirms binding to the first Link\n" +
                "\n" +
                "Forth line which is being followed the last one.\n" +
                "The end.", LINKS.get(0).getId(), TAGS2));
        add(new Note(KEY_PREFIX + 'K', "Another Note which is bound to first Link", LINKS.get(0).getId(), TAGS3));
        add(new Note(KEY_PREFIX + 'L', "This note is bound to the disabled Link.\n" +
                "It has the second line.", LINKS.get(1).getId(), TAGS2));
    }};

    public static List<Favorite> buildFavorites() {
        return FAVORITES;
    }

    public static List<Link> buildLinks() {
        return LINKS;
    }

    public static List<Note> buildNotes() {
        return NOTES;
    }
}
