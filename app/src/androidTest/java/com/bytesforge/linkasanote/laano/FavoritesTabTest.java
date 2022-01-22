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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.res.Resources;

import com.bytesforge.linkasanote.AndroidTestUtils;
import com.bytesforge.linkasanote.ApplicationComponent;
import com.bytesforge.linkasanote.ApplicationModule;
import com.bytesforge.linkasanote.DaggerApplicationComponent;
import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.RepositoryModule;
import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;
import com.bytesforge.linkasanote.laano.favorites.FavoritesFragment;
import com.bytesforge.linkasanote.settings.SettingsModule;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
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

import static com.bytesforge.linkasanote.EspressoMatchers.withItemTextId;
import static com.bytesforge.linkasanote.EspressoMatchers.withItemTextRv;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mockito.Mockito.when;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FavoritesTabTest {

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
                }

                @Override
                protected void afterActivityFinished() {
                    super.afterActivityFinished();
                    restoreApplicationComponent(applicationComponent);
                }
            };

    private final List<Favorite> FAVORITES;

    public FavoritesTabTest() {
        MockitoAnnotations.initMocks(this);
        FAVORITES = AndroidTestUtils.buildFavorites();
    }

    private ApplicationComponent setupMockApplicationComponent(Repository repository) {
        ApplicationComponent oldApplicationComponent = laanoApplication.getApplicationComponent();
        /* EXAMPLE: how to partially mock the module
        RepositoryModule repositoryModule = Mockito.spy(new RepositoryModule());
        Mockito.doReturn(repository).when(repositoryModule)
                .provideRepository(any(DataSource.class), any(DataSource.class));*/
        ApplicationComponent applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(laanoApplication))
                .settingsModule(new SettingsModule())
                .repositoryModule(new RepositoryModule() {

                    @Override
                    public Repository provideRepository(
                            LocalDataSource localDataSource, CloudDataSource cloudDataSource,
                            BaseSchedulerProvider schedulerProvider) {
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
        onView(withItemTextId(FAVORITES_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        assertThat((laanoActivity.getCurrentFragment()).getTitle(), Matchers.equalTo(FAVORITES_TITLE));
        assertThat(laanoActivity.getCurrentFragment(), instanceOf(FavoritesFragment.class));
    }

    @Test
    public void addFavoritesToFavoritesRecyclerView_CheckIfPersistOnOrientationChange() {
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(FAVORITES));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.just(false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.just(false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.just(false));
        laanoActivityTestRule.launchActivity(null);

        for (Favorite favorite : FAVORITES) {
            onView(withItemTextRv(favorite.getName())).check(matches(isDisplayed()));
        }
        AndroidTestUtils.rotateOrientation(laanoActivityTestRule);
        for (Favorite favorite : FAVORITES) {
            onView(withItemTextRv(favorite.getName())).check(matches(isDisplayed()));
        }
    }

    @Test
    public void clickOnActionModeMenuItem_switchesToActionMode() {
        List<Favorite> favorites = new ArrayList<>();
        favorites.add(FAVORITES.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(favorites));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.just(false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.just(false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.just(false));
        laanoActivityTestRule.launchActivity(null);

        openActionBarOverflowOrOptionsMenu(context);
        // NOTE: R.id.toolbar_favorite_action_mode does not work
        onView(withText(R.string.toolbar_favorites_item_action_mode)).perform(click());
        onView(withText(context.getResources()
                .getString(R.string.laano_favorites_action_mode_selected, 0, favorites.size())))
                .check(matches(isDisplayed()));
        onView(withId(R.id.favorites_delete)).check(matches(isDisplayed()));
        onView(withId(R.id.favorite_checkbox)).check(matches(isNotChecked()));
        onView(withId(R.id.favorite_edit)).check(matches(isDisplayed()));
    }

    @Test
    public void longClickOnRecyclerViewItem_switchesToActionModeAndSelectCurrentOne() {
        List<Favorite> favorites = new ArrayList<>();
        favorites.add(FAVORITES.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(favorites));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.just(false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.just(false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.just(false));
        laanoActivityTestRule.launchActivity(null);

        onView(withId(R.id.rv_favorites))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
        onView(withText(context.getResources()
                .getString(R.string.laano_favorites_action_mode_selected, 1, favorites.size())))
                .check(matches(isDisplayed()));
        onView(withId(R.id.favorites_delete)).check(matches(isDisplayed()));
        onView(withId(R.id.favorite_checkbox)).check(matches(isChecked()));
        onView(withId(R.id.favorite_edit)).check(matches(isDisplayed()));
    }

    @Test
    public void actionMode_persistsOnOrientationChange() {
        List<Favorite> favorites = new ArrayList<>();
        favorites.add(FAVORITES.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(favorites));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.just(false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.just(false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.just(false));
        laanoActivityTestRule.launchActivity(null);

        onView(withId(R.id.rv_favorites))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
        AndroidTestUtils.rotateOrientation(laanoActivityTestRule);
        onView(withText(context.getResources()
                .getString(R.string.laano_favorites_action_mode_selected, 1, favorites.size())))
                .check(matches(isDisplayed()));
        onView(withId(R.id.favorites_delete)).check(matches(isDisplayed()));
        onView(withId(R.id.favorite_checkbox)).check(matches(isChecked()));
        onView(withId(R.id.favorite_edit)).check(matches(isDisplayed()));
    }

    @Test
    public void tagChange_disablesActionMode() {
        List<Favorite> favorites = new ArrayList<>();
        favorites.add(FAVORITES.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(favorites));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.just(false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.just(false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.just(false));
        laanoActivityTestRule.launchActivity(null);

        onView(withId(R.id.rv_favorites))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
        onView(withItemTextId(LINKS_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        //onView(withId(laano_view_pager)).perform(swipeRight());
        onView(withId(R.id.favorites_delete)).check(doesNotExist());

        onView(withItemTextId(FAVORITES_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        //onView(withId(laano_view_pager)).perform(swipeLeft());
        onView(withId(R.id.favorites_delete)).check(doesNotExist());
    }
}