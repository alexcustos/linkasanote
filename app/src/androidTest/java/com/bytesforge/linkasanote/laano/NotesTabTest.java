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

import android.content.Context;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
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
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.RepositoryModule;
import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;
import com.bytesforge.linkasanote.laano.notes.NotesFragment;
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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
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
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NotesTabTest {

    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private LaanoApplication laanoApplication = (LaanoApplication) context.getApplicationContext();

    private String FAVORITE_TITLE;
    private String NOTES_TITLE;

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

    private final List<Note> NOTES;

    public NotesTabTest() {
        MockitoAnnotations.initMocks(this);
        NOTES = AndroidTestUtils.buildNotes();
    }

    private ApplicationComponent setupMockApplicationComponent(Repository repository) {
        ApplicationComponent oldApplicationComponent = laanoApplication.getApplicationComponent();
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
        FAVORITE_TITLE = resources.getString(R.string.laano_tab_favorites_title);
        NOTES_TITLE = resources.getString(R.string.laano_tab_notes_title);

        // Tab
        onView(withItemTextId(NOTES_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        assertThat((laanoActivity.getCurrentFragment()).getTitle(), Matchers.equalTo(NOTES_TITLE));
        assertThat(laanoActivity.getCurrentFragment(), instanceOf(NotesFragment.class));
    }

    @Test
    public void addNotesToNotesRecyclerView_CheckIfPersistOnOrientationChange() {
        List<Note> notes = NOTES.subList(0, 2);
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(notes));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.fromCallable(() -> false));
        laanoActivityTestRule.launchActivity(null);

        for (Note note : notes) {
            onView(allOf(withId(R.id.note_note), withItemTextRv(note.getNote())))
                    .check(matches(isDisplayed()));
        }
        AndroidTestUtils.rotateOrientation(laanoActivityTestRule);
        int position = 0;
        for (Note note : notes) {
            onView(withId(R.id.rv_notes)).perform(scrollToPosition(position++));
            onView(allOf(withId(R.id.note_note), withItemTextRv(note.getNote())))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void clickOnActionModeMenuItem_switchesToActionMode() {
        List<Note> notes = new ArrayList<>();
        notes.add(NOTES.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(notes));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.fromCallable(() -> false));
        laanoActivityTestRule.launchActivity(null);

        openActionBarOverflowOrOptionsMenu(context);
        // NOTE: R.id.toolbar_note_action_mode does not work
        onView(withText(R.string.toolbar_notes_item_mode_action)).perform(click());
        onView(withText(context.getResources()
                .getString(R.string.laano_notes_action_mode_selected, 0, notes.size())))
                .check(matches(isDisplayed()));
        onView(withId(R.id.notes_delete)).check(matches(isDisplayed()));
        onView(withId(R.id.note_checkbox)).check(matches(isNotChecked()));
        onView(withId(R.id.note_edit)).check(matches(isDisplayed()));
    }

    @Test
    public void longClickOnRecyclerViewItem_switchesToActionModeAndSelectCurrentOne() {
        List<Note> notes = new ArrayList<>();
        notes.add(NOTES.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(notes));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.fromCallable(() -> false));
        laanoActivityTestRule.launchActivity(null);
        //AndroidTestUtils.sleep(250);

        onView(withId(R.id.rv_notes))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
        onView(withText(context.getResources()
                .getString(R.string.laano_notes_action_mode_selected, 1, notes.size())))
                .check(matches(isDisplayed()));
        onView(withId(R.id.notes_delete)).check(matches(isDisplayed()));
        onView(withId(R.id.note_checkbox)).check(matches(isChecked()));
        onView(withId(R.id.note_edit)).check(matches(isDisplayed()));
    }

    @Test
    public void actionMode_persistsOnOrientationChange() {
        List<Note> notes = new ArrayList<>();
        notes.add(NOTES.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(notes));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.fromCallable(() -> false));
        laanoActivityTestRule.launchActivity(null);
        //AndroidTestUtils.sleep(250);

        onView(withId(R.id.rv_notes))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
        AndroidTestUtils.rotateOrientation(laanoActivityTestRule);
        onView(withText(context.getResources()
                .getString(R.string.laano_notes_action_mode_selected, 1, notes.size())))
                .check(matches(isDisplayed()));
        onView(withId(R.id.notes_delete)).check(matches(isDisplayed()));
        onView(withId(R.id.note_checkbox)).check(matches(isChecked()));
        onView(withId(R.id.note_edit)).check(matches(isDisplayed()));
    }

    @Test
    public void tagChange_disablesActionMode() {
        List<Note> notes = new ArrayList<>();
        notes.add(NOTES.get(0));
        when(mockRepository.getLinks()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getFavorites()).thenReturn(Observable.fromIterable(Collections.emptyList()));
        when(mockRepository.getNotes()).thenReturn(Observable.fromIterable(notes));
        when(mockRepository.isConflictedLinks()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedFavorites()).thenReturn(Single.fromCallable(() -> false));
        when(mockRepository.isConflictedNotes()).thenReturn(Single.fromCallable(() -> false));
        laanoActivityTestRule.launchActivity(null);
        //AndroidTestUtils.sleep(250);

        onView(withId(R.id.rv_notes))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
        onView(withItemTextId(FAVORITE_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        //onView(withId(laano_view_pager)).perform(swipeRight());
        onView(withId(R.id.notes_delete)).check(doesNotExist());

        onView(withItemTextId(NOTES_TITLE, R.id.tab_layout))
                .perform(click())
                .check(matches(isDisplayed()));
        //onView(withId(laano_view_pager)).perform(swipeLeft());
        onView(withId(R.id.notes_delete)).check(doesNotExist());
    }
}
