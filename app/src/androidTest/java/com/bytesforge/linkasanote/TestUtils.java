package com.bytesforge.linkasanote;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.v7.widget.Toolbar;

import java.util.Collection;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.runner.lifecycle.Stage.RESUMED;

public class TestUtils {

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
}
