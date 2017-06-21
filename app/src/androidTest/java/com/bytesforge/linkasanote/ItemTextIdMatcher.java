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

import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class ItemTextIdMatcher extends TypeSafeMatcher<View> {

    private final int expectedId;
    private final String itemText;

    public ItemTextIdMatcher(String itemText, int resourceId) {
        super(View.class);
        this.itemText = itemText;
        this.expectedId = resourceId;
    }

    @Override
    protected boolean matchesSafely(View item) {
        return allOf(
                isDescendantOfA(withId(expectedId)),
                withText(itemText), isDisplayed()).matches(item);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(
                "is isDescendantOfA ID [" + expectedId + "] with text [" + itemText + "]");
    }
}
