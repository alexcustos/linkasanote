package com.bytesforge.linkasanote;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class ItemTextRVMatcher extends TypeSafeMatcher<View> {

    private final String itemText;

    public ItemTextRVMatcher(String itemText) {
        super(View.class);
        this.itemText = itemText;
    }

    @Override
    protected boolean matchesSafely(View item) {
        return allOf(
                isDescendantOfA(isAssignableFrom(RecyclerView.class)),
                withText(itemText)).matches(item);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is isDescendantOfA RV with text [" + itemText + "]");
    }
}
