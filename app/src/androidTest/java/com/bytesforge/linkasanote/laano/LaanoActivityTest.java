package com.bytesforge.linkasanote.laano;

import android.content.Context;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.TestUtils;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.favorites.FavoritesFragment;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.Collectors;

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
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.instanceOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LaanoActivityTest {

    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private Repository repository;

    private final List<Favorite> FAVORITES;

    @Rule
    public ActivityTestRule<LaanoActivity> laanoActivityTestRule =
            new ActivityTestRule<LaanoActivity>(LaanoActivity.class) {

                @Override
                protected void beforeActivityLaunched() {
                    super.beforeActivityLaunched();
                    repository = ((LaanoApplication) context.getApplicationContext())
                            .getApplicationComponent().getRepository();
                    cleanupRepository(repository);
                }
            };

    public LaanoActivityTest() {
        FAVORITES = TestUtils.buildFavorites();
    }

    private void cleanupRepository(Repository repository) {
        // TODO: fix data loss on non-test DB
        repository.deleteAllFavorites();
        repository.deleteAllTags();
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
    public void fabButton_addFavoritesToFavoritesRecyclerView() {
        repository.cacheIsDirty = true;
        setupFavoritesTab();
        FAVORITES.forEach(this::createFavorite);
        for (Favorite favorite : FAVORITES) {
            onView(withItemTextRV(favorite.getName())).check(matches(isDisplayed()));
        }
    }

    private void setupFavoritesTab() {
        // Activity
        LaanoActivity laanoActivity = laanoActivityTestRule.getActivity();
        assertNotNull(laanoActivity);

        Resources resources = laanoActivity.getResources();
        assertNotNull(resources);
        String FAVORITES_TITLE = resources.getString(R.string.laano_tab_favorites_title);

        // Tab
        onView(withItemTextId(FAVORITES_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        assertThat((laanoActivity.getCurrentFragment()).getTitle(), Matchers.equalTo(FAVORITES_TITLE));
        assertThat(laanoActivity.getCurrentFragment(), instanceOf(FavoritesFragment.class));
    }

    private void createFavorite(Favorite favorite) {
        assertNotNull(favorite);
        List<Tag> tags = favorite.getTags();
        assertNotNull(tags);
        String name = favorite.getName();
        assertNotNull(name);
        // NOTE: last tag complete if there is a space at the end
        String tagLine = tags.stream().map(Tag::getName).collect(Collectors.joining(" ")) + " ";
        //String tagLine = Arrays.stream(tags).collect(Collectors.joining(" ")) + " ";
        onView(withId(R.id.fab_add)).perform(click());
        onView(withId(R.id.favorite_name)).check(matches(isDisplayed()));
        onView(withId(R.id.favorite_name)).perform(typeText(name), closeSoftKeyboard());
        onView(withId(R.id.favorite_tags)).perform(typeText(tagLine), closeSoftKeyboard());
        onView(withId(R.id.add_button)).perform(click());
    }
}