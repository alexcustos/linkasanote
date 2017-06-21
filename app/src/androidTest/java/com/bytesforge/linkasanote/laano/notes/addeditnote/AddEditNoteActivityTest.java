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

package com.bytesforge.linkasanote.laano.notes.addeditnote;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.bytesforge.linkasanote.AndroidTestUtils;
import com.bytesforge.linkasanote.R;
import com.google.common.base.Joiner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddEditNoteActivityTest {

    private final String NOTE_NAME = "Note";
    private final String[] NOTE_TAGS = new String[]{"first", "second",  "third"};

    @Rule
    public ActivityTestRule<AddEditNoteActivity> addEditNoteActivityTestRule =
            new ActivityTestRule<>(AddEditNoteActivity.class);

    @Test
    public void orientationChange_editTextFieldsPersists() throws InterruptedException {
        // NOTE: last tag is incomplete if there is no a space at the end
        Joiner joiner = Joiner.on(",");
        String tags = joiner.join(NOTE_TAGS);
        fillNoteFields(NOTE_NAME, tags);

        AndroidTestUtils.rotateOrientation(addEditNoteActivityTestRule);

        onView(withId(R.id.note_note)).check(matches(withText(NOTE_NAME)));
        String uncompletedTag = NOTE_TAGS[NOTE_TAGS.length - 1];
        onView(withId(R.id.note_tags)).check(matches(withText(containsString(uncompletedTag))));
    }

    private void fillNoteFields(String name, String tags) {
        onView(withId(R.id.note_note)).perform(typeText(name), closeSoftKeyboard());
        onView(withId(R.id.note_tags)).perform(typeText(tags), closeSoftKeyboard());
    }
}
