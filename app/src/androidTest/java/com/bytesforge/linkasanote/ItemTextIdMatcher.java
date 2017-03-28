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
