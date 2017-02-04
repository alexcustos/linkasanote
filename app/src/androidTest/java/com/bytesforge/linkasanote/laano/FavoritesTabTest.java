package com.bytesforge.linkasanote.laano;

import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.favorites.FavoritesFragment;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.bytesforge.linkasanote.EspressoMatchers.withItemTextId;
import static com.bytesforge.linkasanote.EspressoMatchers.withItemTextRV;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FavoritesTabTest {

    private final List<String> FAVORITE_NAMES;
    // last space to complete tag and close suggestions
    private final String TAGS = "first second third ";

    private Repository repository;

    @Rule
    public ActivityTestRule<LaanoActivity> laanoActivityTestRule =
            new ActivityTestRule<LaanoActivity>(LaanoActivity.class) {

                @Override
                protected void beforeActivityLaunched() {
                    super.beforeActivityLaunched();
                    // TODO: fix data loss on non-test DB; testApplicationId is not enough
                    repository = ((LaanoApplication) InstrumentationRegistry
                            .getTargetContext().getApplicationContext())
                            .getApplicationComponent().getRepository();
                    repository.deleteAllFavorites();
                    repository.deleteAllTags();
                }
            };

    public FavoritesTabTest() {
        FAVORITE_NAMES = new ArrayList<>();
        FAVORITE_NAMES.add("Favorite");
        FAVORITE_NAMES.add("Favorite #2");
        FAVORITE_NAMES.add("Favorite #3");
    }

    @Before
    public void setupTab() {
        // Activity
        LaanoActivity laanoActivity = laanoActivityTestRule.getActivity();
        assertThat(laanoActivity, notNullValue());

        Resources resources = laanoActivity.getResources();
        assertThat(resources, notNullValue());
        String FAVORITES_TITLE = resources.getString(R.string.laano_tab_favorites_title);

        // Tab
        onView(withItemTextId(FAVORITES_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        assertThat((laanoActivity.getCurrentFragment()).getTitle(), Matchers.equalTo(FAVORITES_TITLE));
        assertThat(laanoActivity.getCurrentFragment(), instanceOf(FavoritesFragment.class));
    }

    @Before
    public void registerIdlingResource() {
        Espresso.registerIdlingResources(
                laanoActivityTestRule.getActivity().getCountingIdlingResource());
    }

    @After
    public void unregisterIdlingResource() {
        Espresso.unregisterIdlingResources(
                laanoActivityTestRule.getActivity().getCountingIdlingResource());
    }

    @Test
    public void addFavoritesToFavoritesRecyclerView() {
        repository.cacheIsDirty = true;
        for (String name : FAVORITE_NAMES) {
            createFavorite(name, TAGS);
        }
        onView(withItemTextRV(FAVORITE_NAMES.get(0))).check(matches(isDisplayed()));
    }

    private void createFavorite(String name, String tags) {
        onView(withId(R.id.fab_add)).perform(click());
        onView(withId(R.id.favorite_name)).check(matches(isDisplayed()));

        onView(withId(R.id.favorite_name)).perform(typeText(name), closeSoftKeyboard());
        onView(withId(R.id.favorite_tags)).perform(typeText(tags), closeSoftKeyboard());

        onView(withId(R.id.add_button)).perform(click());
    }
}