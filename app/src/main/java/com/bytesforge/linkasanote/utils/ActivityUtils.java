package com.bytesforge.linkasanote.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;

import static com.google.common.base.Preconditions.checkNotNull;

public class ActivityUtils {

    public static void addFragmentToActivity(
            @NonNull FragmentManager fragmentManager,
            @NonNull Fragment fragment,
            int frameId, @Nullable String tag) {
        checkNotNull(fragmentManager);
        checkNotNull(fragment);
        fragmentManager
                .beginTransaction()
                .add(frameId, fragment)
                .commit();
    }

    public static void addFragmentToActivity(
            @NonNull FragmentManager fragmentManager,
            @NonNull Fragment fragment,
            int frameId) {
        checkNotNull(fragmentManager);
        checkNotNull(fragment);
        addFragmentToActivity(fragmentManager, fragment, frameId, null);
    }

    public static void replaceFragmentInActivity(
            @NonNull FragmentManager fragmentManager,
            @NonNull Fragment fragment,
            int frameId, @Nullable String tag) {
        checkNotNull(fragmentManager);
        checkNotNull(fragment);
        fragmentManager
                .beginTransaction()
                .replace(frameId, fragment, tag)
                .commit();
    }

    public static void replaceFragmentInActivity(
            @NonNull FragmentManager fragmentManager,
            @NonNull Fragment fragment,
            int frameId) {
        checkNotNull(fragmentManager);
        checkNotNull(fragment);
        replaceFragmentInActivity(fragmentManager, fragment, frameId, null);
    }

    public static void disableViewGroupControls(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            child.setEnabled(false);
            if (child instanceof ViewGroup) {
                disableViewGroupControls((ViewGroup) child);
            }
        }
    }

    public static void enableViewGroupControls(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            child.setEnabled(true);
            if (child instanceof ViewGroup) {
                enableViewGroupControls((ViewGroup) child);
            }
        }
    }

    public static void animateAlpha(
            @NonNull final View view, final int toVisibility,
            final float toAlpha, final long duration, final long delay) {
        checkNotNull(view);

        boolean show = (toVisibility == View.VISIBLE);
        if (show) {
            view.setAlpha(0);
        }
        view.setVisibility(View.VISIBLE);
        view.animate()
                .setDuration(duration)
                .setStartDelay(delay)
                .alpha(show ? toAlpha : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(toVisibility);
                    }
                });
    }
}
