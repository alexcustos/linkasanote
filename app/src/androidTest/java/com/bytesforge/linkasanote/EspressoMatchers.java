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

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;

import com.google.common.base.Strings;

import org.hamcrest.Matcher;

import static com.google.common.base.Preconditions.checkArgument;

public class EspressoMatchers {

    public static Matcher<View> withDrawable(final int resourceId) {
        return new DrawableMatcher(resourceId);
    }

    public static Matcher<View> noDrawable() {
        return new DrawableMatcher(-1);
    }

    public static Matcher<View> withItemTextRv(final String itemText) {
        checkArgument(!Strings.isNullOrEmpty(itemText), "itemText cannot be null or empty");
        return new ItemTextRVMatcher(itemText);
    }

    public static Matcher<View> withItemTextId(final String itemText, final int resourceId) {
        checkArgument(!Strings.isNullOrEmpty(itemText), "itemText cannot be null or empty");
        checkArgument(resourceId > 0, "resourceId should be positive integer");
        return new ItemTextIdMatcher(itemText, resourceId);
    }

    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {

            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "click on a child view with ID [" + id + "]";
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.findViewById(id).performClick();
            }
        };
    }
}
