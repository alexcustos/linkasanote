package com.bytesforge.linkasanote.laano;

import android.content.Context;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.core.deps.guava.collect.Lists;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.bytesforge.linkasanote.AndroidTestUtils;
import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.favorites.FavoritesFragment;
import com.bytesforge.linkasanote.laano.links.LinksFragment;
import com.bytesforge.linkasanote.laano.notes.NotesFragment;

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
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.bytesforge.linkasanote.EspressoMatchers.clickChildViewWithId;
import static com.bytesforge.linkasanote.EspressoMatchers.withItemTextId;
import static com.bytesforge.linkasanote.EspressoMatchers.withItemTextRv;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.instanceOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LaanoActivityTest {

    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private Repository repository;

    private final List<Favorite> FAVORITES;
    private final List<Link> LINKS;
    private final List<Note> NOTES;

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
        FAVORITES = AndroidTestUtils.buildFavorites();
        LINKS = AndroidTestUtils.buildLinks();
        NOTES = AndroidTestUtils.buildNotes();
    }

    private void cleanupRepository(Repository repository) {
        // TODO: provide fake repository in the mock flavor to prevent data loss in main DB
        repository.deleteAllLinks();
        repository.deleteAllFavorites();
        repository.deleteAllNotes();
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
    public void fabButton_addsEntriesToSelectedRecyclerView() {
        fabButton_addsFavoritesToFavoritesRecyclerView();
        fabButton_addsLinksToLinksRecyclerView();
        fabButton_addsNotesToNotesRecyclerView();
    }

    private void fabButton_addsLinksToLinksRecyclerView() {
        repository.linkCacheIsDirty = true;
        setupLinksTab();
        for (Link link : Lists.reverse(LINKS)) {
            createLink(link);
            onView(withItemTextRv(link.getLink())).check(matches(isDisplayed()));
        }
    }

    private void fabButton_addsFavoritesToFavoritesRecyclerView() {
        repository.favoriteCacheIsDirty = true;
        setupFavoritesTab();
        for (Favorite favorite : FAVORITES) {
            createFavorite(favorite);
            onView(withItemTextRv(favorite.getName())).check(matches(isDisplayed()));
        }
    }

    private void fabButton_addsNotesToNotesRecyclerView() {
        repository.noteCacheIsDirty = true;
        int position;
        for (Note note : Lists.reverse(NOTES)) {
            String linkId = note.getLinkId();
            if (linkId == null) {
                setupNotesTab();
                onView(withId(R.id.fab_add)).perform(click());
                createNote(note);
            } else if (note.getId().equals(AndroidTestUtils.KEY_PREFIX + 'L')) {
                position = 1;
                setupLinksTab();
                onView(withId(R.id.rv_links)).perform(
                        scrollToPosition(position),
                        RecyclerViewActions.actionOnItemAtPosition(
                                position, clickChildViewWithId(R.id.add_note_button)));
                createNote(note);
                setupNotesTab();
            } else {
                position = 0;
                setupLinksTab();
                onView(withId(R.id.rv_links)).perform(
                        scrollToPosition(position),
                        RecyclerViewActions.actionOnItemAtPosition(
                                position, clickChildViewWithId(R.id.add_note_button)));
                createNote(note);
                setupNotesTab();
            }
            onView(withItemTextRv(note.getNote())).check(matches(isDisplayed()));
        }
    }

    // Links

    private void setupLinksTab() {
        // Activity
        LaanoActivity laanoActivity = laanoActivityTestRule.getActivity();
        assertNotNull(laanoActivity);

        Resources resources = laanoActivity.getResources();
        assertNotNull(resources);
        String LINKS_TITLE = resources.getString(R.string.laano_tab_links_title);

        // Tab
        onView(withItemTextId(LINKS_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        assertThat((laanoActivity.getCurrentFragment()).getTitle(), Matchers.equalTo(LINKS_TITLE));
        assertThat(laanoActivity.getCurrentFragment(), instanceOf(LinksFragment.class));
    }

    private void createLink(Link link) {
        assertNotNull(link);
        List<Tag> tags = link.getTags();
        String linkLink = link.getLink();
        assertNotNull(linkLink);
        String linkName = link.getName();
        if (linkName == null) linkName = "";
        boolean linkDisabled = link.isDisabled();
        // NOTE: last tag complete if there is a comma at the end
        String tagLine = "";
        if (tags != null) {
            tagLine = tags.stream().map(Tag::getName).collect(Collectors.joining(",")) + ",";
        }
        onView(withId(R.id.fab_add)).perform(click());
        onView(withId(R.id.link_link)).check(matches(isDisplayed()));
        onView(withId(R.id.link_link)).perform(scrollTo(), typeText(linkLink), closeSoftKeyboard());
        if (linkDisabled) {
            // .check(matches(isNotChecked()))
            onView(withId(R.id.checkbox_disabled)).perform(scrollTo(), click());
        }
        onView(withId(R.id.link_name)).check(matches(isDisplayed()));
        onView(withId(R.id.link_name)).perform(scrollTo(), typeText(linkName), closeSoftKeyboard());
        onView(withId(R.id.link_tags)).perform(scrollTo(), typeText(tagLine), closeSoftKeyboard());
        onView(withId(R.id.add_button)).perform(click());
    }

    // Favorites

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
        // NOTE: last tag complete if there is a comma at the end
        String tagLine = tags.stream().map(Tag::getName).collect(Collectors.joining(",")) + ",";
        //String tagLine = Arrays.stream(tags).collect(Collectors.joining(",")) + ",";
        onView(withId(R.id.fab_add)).perform(click());
        onView(withId(R.id.favorite_name)).check(matches(isDisplayed()));
        onView(withId(R.id.favorite_name)).perform(typeText(name), closeSoftKeyboard());
        onView(withId(R.id.favorite_tags)).perform(typeText(tagLine), closeSoftKeyboard());
        onView(withId(R.id.add_button)).perform(click());
    }

    // Notes

    private void setupNotesTab() {
        // Activity
        LaanoActivity laanoActivity = laanoActivityTestRule.getActivity();
        assertNotNull(laanoActivity);

        Resources resources = laanoActivity.getResources();
        assertNotNull(resources);
        String NOTES_TITLE = resources.getString(R.string.laano_tab_notes_title);

        // Tab
        onView(withItemTextId(NOTES_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        assertThat((laanoActivity.getCurrentFragment()).getTitle(), Matchers.equalTo(NOTES_TITLE));
        assertThat(laanoActivity.getCurrentFragment(), instanceOf(NotesFragment.class));
    }

    private void createNote(Note note) {
        assertNotNull(note);
        List<Tag> tags = note.getTags();
        assertNotNull(tags);
        String noteNote = note.getNote();
        assertNotNull(noteNote);
        // NOTE: last tag complete if there is a comma at the end
        String tagLine = tags.stream().map(Tag::getName).collect(Collectors.joining(",")) + ",";
        //String tagLine = Arrays.stream(tags).collect(Collectors.joining(",")) + ",";
        onView(withId(R.id.note_note)).check(matches(isDisplayed()));
        onView(withId(R.id.note_note)).perform(typeText(noteNote), closeSoftKeyboard());
        onView(withId(R.id.note_tags)).perform(typeText(tagLine), closeSoftKeyboard());
        onView(withId(R.id.add_button)).perform(click());
    }
}