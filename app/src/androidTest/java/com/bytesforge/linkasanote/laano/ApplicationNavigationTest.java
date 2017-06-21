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

package com.bytesforge.linkasanote.laano;

import android.content.res.Resources;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.Gravity;

import com.bytesforge.linkasanote.AndroidTestUtils;
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
import static com.bytesforge.linkasanote.AndroidTestUtils.getToolbarNavigationContentDescription;
import static com.bytesforge.linkasanote.EspressoMatchers.withItemTextId;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApplicationNavigationTest {

    private LaanoActivity activity;

    private String LINKS_TITLE;
    private String FAVORITES_TITLE;
    private String NOTES_TITLE;

    @Rule
    public ActivityTestRule<LaanoActivity> laanoActivityTestRule =
            new ActivityTestRule<>(LaanoActivity.class);

    public ApplicationNavigationTest() {
    }

    @Before
    public void setupActivity() {
        activity = laanoActivityTestRule.getActivity();
        assertNotNull(activity);

        Resources resources = activity.getResources();
        assertNotNull(resources);

        LINKS_TITLE = resources.getString(R.string.laano_tab_links_title);
        FAVORITES_TITLE = resources.getString(R.string.laano_tab_favorites_title);
        NOTES_TITLE = resources.getString(R.string.laano_tab_notes_title);
    }

    @Test
    public void clickOnAndroidHomeIcon_opensNavigation() {
        onView(withId(R.id.drawer_layout)).check(matches(isClosed(Gravity.START)));

        onView(withContentDescription(getToolbarNavigationContentDescription(
                laanoActivityTestRule.getActivity(), R.id.toolbar))).perform(click());

        onView(withId(R.id.drawer_layout)).check(matches(isOpen(Gravity.START)));
    }

    @Test
    public void swipeLeftViewPager_switchesTab() {
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
    public void clickOnTab_switchesTab() {
        // Links
        onView(withItemTextId(LINKS_TITLE, R.id.tab_layout))
            .perform(click())
            .check(matches(isDisplayed()));
        assertThat((activity.getCurrentFragment()).getTitle(), Matchers.equalTo(LINKS_TITLE));
        assertThat(activity.getCurrentFragment(), instanceOf(LinksFragment.class));
        // Favorites
        onView(withItemTextId(FAVORITES_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        assertThat((activity.getCurrentFragment()).getTitle(), Matchers.equalTo(FAVORITES_TITLE));
        assertThat(activity.getCurrentFragment(), instanceOf(FavoritesFragment.class));
        // Notes
        onView(withItemTextId(NOTES_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        assertThat((activity.getCurrentFragment()).getTitle(), Matchers.equalTo(NOTES_TITLE));
        assertThat(activity.getCurrentFragment(), instanceOf(NotesFragment.class));
    }

    @Test
    public void clickOnSettingsNavigationItem_showsSettingsScreenAndClickOnHomeIcon_closesIt() {
        // Open
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)))
                .perform(DrawerActions.open());
        onView(withId(R.id.nav_view)).perform(navigateTo(R.id.settings_menu_item));
        onView(allOf(
                withText(R.string.actionbar_title_settings),
                isDescendantOfA(withResourceName("action_bar"))))
            .check(matches(isDisplayed()));
        // Close
        onView(withContentDescription("Navigate up")).perform(click());
        onView(withId(R.id.laano_view_pager)).check(matches(isDisplayed()));
    }

    @Test
    public void clickOnAddAccountNavigationItem_showsAddAccountScreen() {
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)))
                .perform(DrawerActions.open());
        onView(withId(R.id.nav_view)).perform(navigateTo(R.id.add_account_menu_item));
        AndroidTestUtils.sleep(150);
        onView(withId(R.id.application_logo)).check(matches(withText(R.string.app_name_full)));

    }

    @Test
    public void clickOnManageAccounts_showsManageAccountsScreenAndClickOnHomeIconClosesIt() {
        // Open
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)))
                .perform(DrawerActions.open());
        onView(withId(R.id.nav_view)).perform(navigateTo(R.id.manage_accounts_menu_item));
        onView(allOf(
                withText(R.string.actionbar_title_manage_accounts),
                isDescendantOfA(withResourceName("toolbar"))))
                .check(matches(isDisplayed()));
        // Close
        onView(withContentDescription("Navigate up")).perform(click());
        onView(withId(R.id.laano_view_pager)).check(matches(isDisplayed()));
    }
}
