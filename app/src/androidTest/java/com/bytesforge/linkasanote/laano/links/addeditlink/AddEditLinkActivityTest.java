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

package com.bytesforge.linkasanote.laano.links.addeditlink;

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
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddEditLinkActivityTest {

    private final String LINK_LINK = "http://laano.net/link";
    private final String LINK_NAME = "Title for Link";
    private final boolean LINK_DISABLED = true;
    private final String[] LINK_TAGS = new String[]{"first", "second",  "third"};

    @Rule
    public ActivityTestRule<AddEditLinkActivity> addEditLinkActivityTestRule =
            new ActivityTestRule<>(AddEditLinkActivity.class);

    @Test
    public void orientationChange_editTextFieldsPersists() throws InterruptedException {
        // NOTE: last tag is incomplete if there is no a space at the end
        Joiner joiner = Joiner.on(",");
        String tags = joiner.join(LINK_TAGS);
        fillLinkFields(LINK_LINK, LINK_NAME, LINK_DISABLED, tags);

        AndroidTestUtils.rotateOrientation(addEditLinkActivityTestRule);

        onView(withId(R.id.link_link)).check(matches(withText(LINK_LINK)));
        onView(withId(R.id.link_name)).check(matches(withText(LINK_NAME)));
        if (LINK_DISABLED) {
            onView(withId(R.id.checkbox_disabled)).check(matches(isChecked()));
        } else {
            onView(withId(R.id.checkbox_disabled)).check(matches(isNotChecked()));
        }
        String uncompletedTag = LINK_TAGS[LINK_TAGS.length - 1];
        onView(withId(R.id.link_tags)).check(matches(withText(containsString(uncompletedTag))));
    }

    private void fillLinkFields(String link, String name, boolean disabled, String tags) {
        onView(withId(R.id.link_link)).perform(scrollTo(), typeText(link));
        onView(withId(R.id.link_name)).perform(scrollTo(), typeText(name));
        if (disabled) {
            onView(withId(R.id.checkbox_disabled)).perform(scrollTo(), click());
        }
        onView(withId(R.id.link_tags)).perform(scrollTo(), typeText(tags));
    }
}
