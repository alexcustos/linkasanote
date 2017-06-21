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

package com.bytesforge.linkasanote.laano.favorites.addeditfavorite;

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
public class AddEditFavoriteActivityTest {

    private final String FAVORITE_NAME = "Favorite";
    private final String[] FAVORITE_TAGS = new String[]{"first", "second",  "third"};

    @Rule
    public ActivityTestRule<AddEditFavoriteActivity> addEditFavoriteActivityTestRule =
            new ActivityTestRule<>(AddEditFavoriteActivity.class);

    @Test
    public void orientationChange_editTextFieldsPersists() throws InterruptedException {
        // NOTE: last tag is incomplete if there is no a space at the end
        Joiner joiner = Joiner.on(",");
        String tags = joiner.join(FAVORITE_TAGS);
        fillFavoriteFields(FAVORITE_NAME, tags);

        AndroidTestUtils.rotateOrientation(addEditFavoriteActivityTestRule);

        onView(withId(R.id.favorite_name)).check(matches(withText(FAVORITE_NAME)));
        String uncompletedTag = FAVORITE_TAGS[FAVORITE_TAGS.length - 1];
        onView(withId(R.id.favorite_tags)).check(matches(withText(containsString(uncompletedTag))));
    }

    private void fillFavoriteFields(String name, String tags) {
        onView(withId(R.id.favorite_name)).perform(typeText(name), closeSoftKeyboard());
        onView(withId(R.id.favorite_tags)).perform(typeText(tags), closeSoftKeyboard());
    }
}