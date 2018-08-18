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

package com.bytesforge.linkasanote.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bytesforge.linkasanote.R;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ActivityUtils {

    private static final String TAG = ActivityUtils.class.getSimpleName();

    private ActivityUtils() {
    }

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

    @VisibleForTesting
    public static void clearClipboard(@NonNull Context context, boolean showToast) {
        ClipboardManager clipboardManager =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) return;

        ClipData clipData = ClipData.newPlainText(null, null);
        clipboardManager.setPrimaryClip(clipData);
        if (showToast)
            Toast.makeText(context, R.string.toast_clipboard_cleared, Toast.LENGTH_SHORT).show();
    }

    public static void clearClipboard(@NonNull Context context) {
        clearClipboard(context, true);
    }

    public static Spanned fromHtmlCompat(@NonNull String source) {
        checkNotNull(source);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            //noinspection deprecation
            return Html.fromHtml(source);
        }
    }
}
