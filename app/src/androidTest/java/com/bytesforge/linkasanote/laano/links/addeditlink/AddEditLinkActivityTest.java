package com.bytesforge.linkasanote.laano.links.addeditlink;

import android.support.test.espresso.Espresso;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.bytesforge.linkasanote.AndroidTestUtils;
import com.bytesforge.linkasanote.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.stream.Collectors;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
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

    @Before
    public void registerIdlingResource() {
        Espresso.registerIdlingResources(
                addEditLinkActivityTestRule.getActivity().getCountingIdlingResource());
    }

    @After
    public void unregisterIdlingResource() {
        Espresso.unregisterIdlingResources(
                addEditLinkActivityTestRule.getActivity().getCountingIdlingResource());
    }

    @Test
    public void orientationChange_editTextFieldsPersists() throws InterruptedException {
        // NOTE: last tag is incomplete if there is no a space at the end
        String tags = Arrays.stream(LINK_TAGS).collect(Collectors.joining(","));
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
        onView(withId(R.id.link_link)).perform(typeText(link), closeSoftKeyboard());
        onView(withId(R.id.link_name)).perform(typeText(name), closeSoftKeyboard());
        if (disabled) {
            onView(withId(R.id.checkbox_disabled)).perform(click());
        }
        onView(withId(R.id.link_tags)).perform(typeText(tags), closeSoftKeyboard());
    }
}
