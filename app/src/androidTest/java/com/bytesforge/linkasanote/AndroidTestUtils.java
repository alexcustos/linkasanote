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

package com.bytesforge.linkasanote;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;

import com.bytesforge.linkasanote.data.source.local.LocalContract;

import java.util.Collection;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.runner.lifecycle.Stage.RESUMED;

public class AndroidTestUtils extends SharedUtils {

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

    /* EXAMPLE: UI instrumentation
    public static void grantPermissionIfNeeded(String permissionNeeded) {
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
    } */

    public static void cleanUpProvider(ContentResolver contentResolver) {
        final Uri favoriteUri = LocalContract.FavoriteEntry.buildUri();
        contentResolver.delete(favoriteUri, null, null);

        final Uri linkUri = LocalContract.LinkEntry.buildUri();
        contentResolver.delete(linkUri, null, null);

        final Uri noteUri = LocalContract.NoteEntry.buildUri();
        contentResolver.delete(noteUri, null, null);

        final Uri tagUri = LocalContract.TagEntry.buildUri();
        contentResolver.delete(tagUri, null, null);
    }
}
