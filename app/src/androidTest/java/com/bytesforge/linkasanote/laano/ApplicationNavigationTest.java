package com.bytesforge.linkasanote.laano;

import android.content.res.Resources;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.Gravity;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.laano.favorites.FavoritesFragment;
import com.bytesforge.linkasanote.laano.links.LinksFragment;
import com.bytesforge.linkasanote.laano.notes.NotesFragment;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerMatchers.isClosed;
import static android.support.test.espresso.contrib.DrawerMatchers.isOpen;
import static android.support.test.espresso.contrib.NavigationViewActions.navigateTo;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withResourceName;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.bytesforge.linkasanote.TestUtils.getToolbarNavigationContentDescription;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApplicationNavigationTest {

    private LaanoActivity activity;
    private Resources resources;

    private String LINKS_TITLE;
    private String FAVORITES_TITLE;
    private String NOTES_TITLE;

    @Rule
    public ActivityTestRule<LaanoActivity> activityTestRule =
            new ActivityTestRule<>(LaanoActivity.class);

    @Before
    public void setupActivity() {
        activity = activityTestRule.getActivity();
        assertThat(activity, notNullValue());

        resources = activity.getResources();
        assertThat(resources, notNullValue());

        LINKS_TITLE = resources.getString(R.string.laano_tab_links_title);
        FAVORITES_TITLE = resources.getString(R.string.laano_tab_favorites_title);
        NOTES_TITLE = resources.getString(R.string.laano_tab_notes_title);
    }

    @Test
    public void clickOnAndroidHomeIcon_OpensNavigation() {
        onView(withId(R.id.drawer_layout)).check(matches(isClosed(Gravity.START)));

        onView(withContentDescription(getToolbarNavigationContentDescription(
                activityTestRule.getActivity(), R.id.toolbar))).perform(click());

        onView(withId(R.id.drawer_layout)).check(matches(isOpen(Gravity.START)));
    }

    @Test
    public void swipeLeftViewPager_SwitchesTab() {
        // Links
        onView(withId(R.id.laano_view_pager)).check(matches(isDisplayed()));
        assertThat((activity.getCurrentFragment()).getTitle(), Matchers.equalTo(LINKS_TITLE));
        assertThat(activity.getCurrentFragment(), instanceOf(LinksFragment.class));

        // Favorites
        onView(withId(R.id.laano_view_pager)).perform(swipeLeft());
        assertThat((activity.getCurrentFragment()).getTitle(), Matchers.equalTo(FAVORITES_TITLE));
        assertThat(activity.getCurrentFragment(), instanceOf(FavoritesFragment.class));

        // Notes
        onView(withId(R.id.laano_view_pager)).perform(swipeLeft());
        assertThat((activity.getCurrentFragment()).getTitle(), Matchers.equalTo(NOTES_TITLE));
        assertThat(activity.getCurrentFragment(), instanceOf(NotesFragment.class));
    }

    @Test
    public void clickOnTab_SwitchesTab() {
        // Links
        onView(allOf(withText(LINKS_TITLE), isDescendantOfA(withId(R.id.tab_layout))))
            .perform(click())
            .check(matches(isDisplayed()));
        assertThat((activity.getCurrentFragment()).getTitle(), Matchers.equalTo(LINKS_TITLE));
        assertThat(activity.getCurrentFragment(), instanceOf(LinksFragment.class));

        // Favorites
        onView(allOf(withText(FAVORITES_TITLE), isDescendantOfA(withId(R.id.tab_layout))))
                .perform(click())
                .check(matches(isDisplayed()));
        assertThat((activity.getCurrentFragment()).getTitle(), Matchers.equalTo(FAVORITES_TITLE));
        assertThat(activity.getCurrentFragment(), instanceOf(FavoritesFragment.class));

        // Notes
        onView(allOf(withText(NOTES_TITLE), isDescendantOfA(withId(R.id.tab_layout))))
                .perform(click())
                .check(matches(isDisplayed()));
        assertThat((activity.getCurrentFragment()).getTitle(), Matchers.equalTo(NOTES_TITLE));
        assertThat(activity.getCurrentFragment(), instanceOf(NotesFragment.class));
    }

    @Test
    public void clickOnSettingsNavigationItem_ShowsSettingsScreen_And_ClickOnHomeIcon_ClosesIt() {
        // Open
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)))
                .perform(DrawerActions.open());
        onView(withId(R.id.nav_view)).perform(navigateTo(R.id.settings_menu_item));
        onView(allOf(
                withText(R.string.actionbar_title_settings),
                isDescendantOfA(withResourceName("action_bar_container"))))
            .check(matches(isDisplayed()));

        // Close
        onView(withContentDescription("Navigate up")).perform(click());
        onView(withId(R.id.laano_view_pager)).check(matches(isDisplayed()));
    }
}
