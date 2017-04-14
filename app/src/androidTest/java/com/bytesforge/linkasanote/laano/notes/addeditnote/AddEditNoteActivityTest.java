package com.bytesforge.linkasanote.laano.notes.addeditnote;

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
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddEditNoteActivityTest {

    private final String NOTE_NAME = "Note";
    private final String[] NOTE_TAGS = new String[]{"first", "second",  "third"};

    @Rule
    public ActivityTestRule<AddEditNoteActivity> addEditNoteActivityTestRule =
            new ActivityTestRule<>(AddEditNoteActivity.class);

    @Before
    public void registerIdlingResource() {
        Espresso.registerIdlingResources(
                addEditNoteActivityTestRule.getActivity().getCountingIdlingResource());
    }

    @After
    public void unregisterIdlingResource() {
        Espresso.unregisterIdlingResources(
                addEditNoteActivityTestRule.getActivity().getCountingIdlingResource());
    }

    @Test
    public void orientationChange_editTextFieldsPersists() throws InterruptedException {
        // NOTE: last tag is incomplete if there is no a space at the end
        String tags = Arrays.stream(NOTE_TAGS).collect(Collectors.joining(","));
        fillNoteFields(NOTE_NAME, tags);

        AndroidTestUtils.rotateOrientation(addEditNoteActivityTestRule);

        onView(withId(R.id.note_note)).check(matches(withText(NOTE_NAME)));
        String uncompletedTag = NOTE_TAGS[NOTE_TAGS.length - 1];
        onView(withId(R.id.note_tags)).check(matches(withText(containsString(uncompletedTag))));
    }

    private void fillNoteFields(String name, String tags) {
        onView(withId(R.id.note_note)).perform(typeText(name), closeSoftKeyboard());
        onView(withId(R.id.note_tags)).perform(typeText(tags), closeSoftKeyboard());
    }
}
