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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class DrawableMatcher extends TypeSafeMatcher<View> {

    private final int expectedId;
    private String resourceName;

    public DrawableMatcher(int resourceId) {
        super(View.class);
        this.expectedId = resourceId;
    }

    @Override
    protected boolean matchesSafely(View target) {
        if (!(target instanceof TextView)) return false;

        TextView textView = (TextView) target;
        Drawable[] drawables = textView.getCompoundDrawables();

        if (expectedId < 0) {
            for (Drawable item : drawables) {
                if (item != null) return false;
            }
            return true;
        }

        Context context = target.getContext();
        Resources resources = context.getResources();
        Drawable expectedDrawable = ContextCompat.getDrawable(context, expectedId);
        for (Drawable item : drawables) {
            Drawable.ConstantState itemConstantState = item.getConstantState();
            if (itemConstantState != null
                    && itemConstantState.equals(expectedDrawable.getConstantState())) {
                resourceName = resources.getResourceEntryName(expectedId);
                return true;
            }
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        if (expectedId >= 0) {
            description.appendText("with drawable from resource id " + expectedId);
            if (resourceName != null) {
                description.appendText("[" + resourceName + "]");
            }
        } else {
            description.appendText("without drawable");
        }
    }
}
