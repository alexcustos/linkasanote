package com.bytesforge.linkasanote;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.runner.lifecycle.Stage.RESUMED;

public class AndroidTestUtils {

    public static String KEY_PREFIX = CommonUtils.charRepeat('A', 21);
    public static final List<Tag> TAGS = new ArrayList<Tag>() {{
        add(new Tag("first"));
        add(new Tag("second"));
    }};
    public static final List<Tag> TAGS2 = new ArrayList<Tag>() {{
        addAll(TAGS);
        add(new Tag("third"));
    }};
    public static final List<Tag> TAGS3 = new ArrayList<Tag>() {{
        addAll(TAGS2);
        add(new Tag("fourth"));
    }};

    private static void rotateToLandscape(ActivityTestRule<? extends Activity> activity) {
        activity.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private static void rotateToPortrait(ActivityTestRule<? extends Activity> activity) {
        activity.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public static void rotateOrientation(ActivityTestRule<? extends Activity> activity) {
        int currentOrientation =
                activity.getActivity().getResources().getConfiguration().orientation;

        switch (currentOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                rotateToPortrait(activity);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                rotateToLandscape(activity);
                break;
            default:
                rotateToLandscape(activity);
        }
    }

    public static String getToolbarNavigationContentDescription(
            @NonNull Activity activity,
            @IdRes int toolbarId) {
        Toolbar toolbar = (Toolbar) activity.findViewById(toolbarId);
        if (toolbar != null) {
            return (String) toolbar.getNavigationContentDescription();
        } else {
            throw new RuntimeException("No toolbar found");
        }
    }

    public static Activity getCurrentActivity() throws IllegalStateException {
        final Activity[] resumedActivity = new Activity[1];

        getInstrumentation().runOnMainSync(() -> {
            Collection resumedActivities =
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(RESUMED);
            if (resumedActivities.iterator().hasNext()) {
                resumedActivity[0] = (Activity) resumedActivities.iterator().next();
            } else {
                throw new IllegalStateException("No Activity is stage RESUMED");
            }
        });

        return resumedActivity[0];
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException("Cannot execute Thread.sleep()");
        }
    }

    // Permissions

    private static final int PERMISSION_DIALOG_DELAY = 1000;
    private static final int GRANT_BUTTON_INDEX = 1;

    private static boolean hasNeededPermission(String permissionNeeded) {
        Context context = InstrumentationRegistry.getTargetContext();
        int permissionStatus = ContextCompat.checkSelfPermission(context, permissionNeeded);
        return permissionStatus == PackageManager.PERMISSION_GRANTED;
    }

    public static void allowPermissionIfNeeded(String permissionNeeded) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !hasNeededPermission(permissionNeeded)) {
                sleep(PERMISSION_DIALOG_DELAY);
                UiDevice device = UiDevice.getInstance(getInstrumentation());
                UiObject allowsPermissions = device.findObject(new UiSelector()
                        .clickable(true).checkable(false).index(GRANT_BUTTON_INDEX));
                if (allowsPermissions.exists()) {
                    allowsPermissions.click();
                }
            }
        } catch (UiObjectNotFoundException e) {
            System.out.println("There is not permissions dialog to interact with");
        }
    }

    public static List<Favorite> buildFavorites() {
        return new ArrayList<Favorite>() {{
            add(new Favorite(KEY_PREFIX + 'A', "Favorite", TAGS));
            add(new Favorite(KEY_PREFIX + 'B', "Favorite #2", TAGS2));
            add(new Favorite(KEY_PREFIX + 'C', "Favorite #3", TAGS3));
        }};
    }

    public static List<Link> buildLinks() {
        return new ArrayList<Link>() {{
            add(new Link(KEY_PREFIX + 'A', "http://laano.net/link", "Title for Link", false, TAGS));
            add(new Link(KEY_PREFIX + 'B', "http://laano.net/link2", "Title for Link #2", true, TAGS2));
            add(new Link(KEY_PREFIX + 'C', "http://laano.net/link3", "Title for Link #3", false, TAGS3));
        }};
    }

    public static List<Note> buildNotes() {
        return new ArrayList<Note>() {{
            add(new Note(KEY_PREFIX + 'A', "Note", TAGS));
            add(new Note(KEY_PREFIX + 'B', "Note #2", TAGS2));
            add(new Note(KEY_PREFIX + 'C', "Note #3", TAGS3));
        }};
    }
}
