package com.bytesforge.linkasanote.laano;

import android.content.Context;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.bytesforge.linkasanote.AndroidTestUtils;
import com.bytesforge.linkasanote.ApplicationComponent;
import com.bytesforge.linkasanote.ApplicationModule;
import com.bytesforge.linkasanote.DaggerApplicationComponent;
import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.source.Cloud;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.data.source.Local;
import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.RepositoryModule;
import com.bytesforge.linkasanote.laano.links.LinksFragment;
import com.bytesforge.linkasanote.settings.SettingsModule;
import com.bytesforge.linkasanote.utils.schedulers.SchedulerProviderModule;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.bytesforge.linkasanote.EspressoMatchers.withItemTextId;
import static com.bytesforge.linkasanote.EspressoMatchers.withItemTextRv;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LinksTabTest {

    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private LaanoApplication laanoApplication = (LaanoApplication) context.getApplicationContext();

    private String LINKS_TITLE;
    private String FAVORITES_TITLE;

    @Mock
    Repository mockRepository;

    @Rule
    public ActivityTestRule<LaanoActivity> laanoActivityTestRule =
            new ActivityTestRule<LaanoActivity>(LaanoActivity.class, false, false) {
                private ApplicationComponent applicationComponent;

                @Override
                protected void beforeActivityLaunched() {
                    super.beforeActivityLaunched();
                    applicationComponent = setupMockApplicationComponent(mockRepository);
                }

                @Override
                protected void afterActivityLaunched() {
                    super.afterActivityLaunched();
                    setupTab();
                    registerIdlingResource();
                }

                @Override
                protected void afterActivityFinished() {
                    super.afterActivityFinished();
                    unregisterIdlingResource();
                    restoreApplicationComponent(applicationComponent);
                }
            };

    private final List<Link> LINKS;

    public LinksTabTest() {
        MockitoAnnotations.initMocks(this);
        LINKS = AndroidTestUtils.buildLinks();
    }

    private ApplicationComponent setupMockApplicationComponent(Repository repository) {
        ApplicationComponent oldApplicationComponent = laanoApplication.getApplicationComponent();
        ApplicationComponent applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(laanoApplication))
                .settingsModule(new SettingsModule())
                .repositoryModule(new RepositoryModule() {

                    @Override
                    public Repository provideRepository(
                            @Local DataSource localDataSource,
                            @Cloud DataSource cloudDataSource) {
                        return repository;
                    }
                })
                .providerModule(new ProviderModule())
                .schedulerProviderModule(new SchedulerProviderModule())
                .build();
        laanoApplication.setApplicationComponent(applicationComponent);
        return oldApplicationComponent;
    }

    private void restoreApplicationComponent(ApplicationComponent applicationComponent) {
        laanoApplication.setApplicationComponent(applicationComponent);
    }

    private void setupTab() { // @Before
        // Activity
        LaanoActivity laanoActivity = laanoActivityTestRule.getActivity();
        assertNotNull(laanoActivity);

        Resources resources = laanoActivity.getResources();
        assertNotNull(resources);
        LINKS_TITLE = resources.getString(R.string.laano_tab_links_title);
        FAVORITES_TITLE = resources.getString(R.string.laano_tab_favorites_title);

        // Tab
        onView(withItemTextId(LINKS_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        assertThat((laanoActivity.getCurrentFragment()).getTitle(), Matchers.equalTo(LINKS_TITLE));
        assertThat(laanoActivity.getCurrentFragment(), instanceOf(LinksFragment.class));
    }

    private void registerIdlingResource() { // @Before
        Espresso.registerIdlingResources(
                laanoActivityTestRule.getActivity().getCountingIdlingResource());
    }

    private void unregisterIdlingResource() { // @After
        Espresso.unregisterIdlingResources(
                laanoActivityTestRule.getActivity().getCountingIdlingResource());
    }

    @Test
    public void addLinksToLinksRecyclerView_CheckIfPersistOnOrientationChange() {
        List<Link> links = new ArrayList<>();
        links.add(LINKS.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(links));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.fromCallable(() -> false));
        laanoActivityTestRule.launchActivity(null);

        for (Link link : links) {
            onView(withItemTextRv(link.getName())).check(matches(isDisplayed()));
        }
        AndroidTestUtils.rotateOrientation(laanoActivityTestRule);
        for (Link link : links) {
            onView(withItemTextRv(link.getName())).check(matches(isDisplayed()));
        }
    }

    @Test
    public void clickOnActionModeMenuItem_switchesToActionMode() {
        List<Link> links = new ArrayList<>();
        links.add(LINKS.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(links));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.fromCallable(() -> false));
        laanoActivityTestRule.launchActivity(null);

        openActionBarOverflowOrOptionsMenu(context);
        // NOTE: R.id.toolbar_link_action_mode does not work
        onView(withText(R.string.toolbar_links_item_action_mode)).perform(click());
        onView(withText(context.getResources()
                .getString(R.string.laano_links_action_mode_selected, 0, links.size())))
                .check(matches(isDisplayed()));
        onView(withId(R.id.links_delete)).check(matches(isDisplayed()));
        onView(withId(R.id.link_checkbox)).check(matches(isNotChecked()));
        onView(withId(R.id.link_edit)).check(matches(isDisplayed()));
    }

    @Test
    public void longClickOnRecyclerViewItem_switchesToActionModeAndSelectCurrentOne() {
        List<Link> links = new ArrayList<>();
        links.add(LINKS.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(links));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.fromCallable(() -> false));
        laanoActivityTestRule.launchActivity(null);

        onView(withId(R.id.rv_links))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
        onView(withText(context.getResources()
                .getString(R.string.laano_links_action_mode_selected, 1, links.size())))
                .check(matches(isDisplayed()));
        onView(withId(R.id.links_delete)).check(matches(isDisplayed()));
        onView(withId(R.id.link_checkbox)).check(matches(isChecked()));
        onView(withId(R.id.link_edit)).check(matches(isDisplayed()));
    }

    @Test
    public void actionMode_persistsOnOrientationChange() {
        List<Link> links = new ArrayList<>();
        links.add(LINKS.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(links));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.fromCallable(() -> false));
        laanoActivityTestRule.launchActivity(null);

        onView(withId(R.id.rv_links))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
        AndroidTestUtils.rotateOrientation(laanoActivityTestRule);
        onView(withText(context.getResources()
                .getString(R.string.laano_links_action_mode_selected, 1, links.size())))
                .check(matches(isDisplayed()));
        onView(withId(R.id.links_delete)).check(matches(isDisplayed()));
        onView(withId(R.id.link_checkbox)).check(matches(isChecked()));
        onView(withId(R.id.link_edit)).check(matches(isDisplayed()));
    }

    @Test
    public void tagChange_disablesActionMode() {
        List<Link> links = new ArrayList<>();
        links.add(LINKS.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(links));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.fromCallable(() -> false));
        laanoActivityTestRule.launchActivity(null);

        onView(withId(R.id.rv_links))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
        onView(withItemTextId(FAVORITES_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        //onView(withId(laano_view_pager)).perform(swipeRight());
        onView(withId(R.id.links_delete)).check(doesNotExist());

        onView(withItemTextId(LINKS_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        //onView(withId(laano_view_pager)).perform(swipeLeft());
        onView(withId(R.id.links_delete)).check(doesNotExist());
    }
}
