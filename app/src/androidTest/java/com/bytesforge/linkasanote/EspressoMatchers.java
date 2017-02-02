package com.bytesforge.linkasanote;

import android.text.TextUtils;
import android.view.View;

import org.hamcrest.Matcher;

import static com.google.common.base.Preconditions.checkArgument;

public class EspressoMatchers {

    public static Matcher<View> withDrawable(final int resourceId) {
        return new DrawableMatcher(resourceId);
    }

    public static Matcher<View> noDrawable() {
        return new DrawableMatcher(-1);
    }

    public static Matcher<View> withItemTextRV(final String itemText) {
        checkArgument(!TextUtils.isEmpty(itemText), "itemText cannot be null or empty");
        return new ItemTextRVMatcher(itemText);
    }

    public static Matcher<View> withItemTextId(final String itemText, final int resourceId) {
        checkArgument(!TextUtils.isEmpty(itemText), "itemText cannot be null or empty");
        checkArgument(resourceId > 0, "resourceId should be positive integer");
        return new ItemTextIdMatcher(itemText, resourceId);
    }
}
