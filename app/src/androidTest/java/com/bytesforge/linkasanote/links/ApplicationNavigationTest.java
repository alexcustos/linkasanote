package com.bytesforge.linkasanote.links;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.Gravity;

import com.bytesforge.linkasanote.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerMatchers.isClosed;
import static android.support.test.espresso.contrib.DrawerMatchers.isOpen;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static com.bytesforge.linkasanote.TestUtils.getToolbarNavigationContentDescription;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApplicationNavigationTest {

    @Rule
    public ActivityTestRule<LinksActivity> activityTestRule =
            new ActivityTestRule<>(LinksActivity.class);

    @Test
    public void clickOnAndroidHomeIcon_OpensNavigation() {
        onView(withId(R.id.drawer_layout)).check(matches(isClosed(Gravity.START)));

        onView(withContentDescription(getToolbarNavigationContentDescription(
                activityTestRule.getActivity(), R.id.toolbar))).perform(click());

        onView(withId(R.id.drawer_layout)).check(matches(isOpen(Gravity.START)));
    }
}