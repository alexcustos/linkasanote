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