package com.bytesforge.linkasanote;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
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

        Resources resources = target.getContext().getResources();
        Drawable expectedDrawable = resources.getDrawable(expectedId, null);
        for (Drawable item : drawables) {
            if (item.getConstantState().equals(expectedDrawable.getConstantState())) {
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
