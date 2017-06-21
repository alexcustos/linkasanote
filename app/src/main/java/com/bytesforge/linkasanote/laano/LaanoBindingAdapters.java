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

import android.databinding.BindingAdapter;
import android.databinding.BindingConversion;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.ActionViewClickableSpan;
import com.bytesforge.linkasanote.utils.ActivityUtils;
import com.bytesforge.linkasanote.utils.CommonUtils;

import java.util.regex.Matcher;

public class LaanoBindingAdapters {

    private static final String TAG = LaanoBindingAdapters.class.getSimpleName();

    private LaanoBindingAdapters() {
    }

    @BindingConversion
    public static ColorDrawable convertColorToDrawable(int color) {
        return new ColorDrawable(color);
    }

    @BindingAdapter({"progressOverlay"})
    public static void showProgressOverlay(FrameLayout view, boolean progressOverlay) {
        if (progressOverlay) {
            ActivityUtils.animateAlpha(view, View.VISIBLE,
                    Settings.GLOBAL_PROGRESS_OVERLAY_ALPHA,
                    Settings.GLOBAL_PROGRESS_OVERLAY_DURATION,
                    Settings.GLOBAL_PROGRESS_OVERLAY_SHOW_DELAY);
        } else {
            ActivityUtils.animateAlpha(view, View.GONE, 0,
                    Settings.GLOBAL_PROGRESS_OVERLAY_DURATION, 0);
        }
    }

    @BindingAdapter({"imageButtonEnabled"})
    public static void setImageButtonEnabled(ImageButton view, boolean enabled) {
        view.setClickable(enabled);
        view.setFocusable(enabled);
        view.setEnabled(enabled);

        if (enabled) view.setAlpha(1.0f);
        else view.setAlpha(Settings.GLOBAL_ICON_ALPHA_DISABLED);
    }

    @BindingAdapter({"srcCompat"})
    public static void setSrcCompat(ImageButton view, Drawable drawable) {
        view.setImageDrawable(drawable);
    }

    @BindingAdapter({"scrollableText"})
    public static void setScrollableText(TextView view, String text) {
        view.setText(text);
        view.setMovementMethod(new ScrollingMovementMethod());
    }

    // NOTE: for compatibility with the KitKat
    @BindingAdapter({"drawableStartCompat"})
    public static void setDrawableStartCompat(TextView view, Drawable drawable) {
        Drawable[] drawables = view.getCompoundDrawables();
        view.setCompoundDrawablesWithIntrinsicBounds(drawable, drawables[1], drawables[2], drawables[3]);
    }

    @BindingAdapter({"drawableTopCompat"})
    public static void setDrawableTopCompat(TextView view, Drawable drawable) {
        Drawable[] drawables = view.getCompoundDrawables();
        view.setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawable, drawables[2], drawables[3]);
    }

    @BindingAdapter({"drawableEndCompat"})
    public static void setDrawableEndCompat(TextView view, Drawable drawable) {
        Drawable[] drawables = view.getCompoundDrawables();
        view.setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[2], drawable, drawables[3]);
    }

    @BindingAdapter({"drawableBottomCompat"})
    public static void setDrawableBottomCompat(TextView view, Drawable drawable) {
        Drawable[] drawables = view.getCompoundDrawables();
        view.setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[2], drawables[3], drawable);
    }

    @BindingAdapter({"tintCompat"})
    public static void setTintCompat(ImageView view, int color) {
        Drawable drawable = view.getDrawable();
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, color);
    }

    @BindingAdapter({"webUrlText"})
    public static void setWebEmailText(TextView view, String text) {
        SpannableString spannableString = new SpannableString(text);
        // NOTE: the WEB_URL pattern depends on API and/or device firmware
        Matcher webUrlMatcher = Patterns.WEB_URL.matcher(text);
        while (webUrlMatcher.find()) {
            String webUrl = webUrlMatcher.group(0);
            if (webUrl != null && (webUrl.startsWith(CommonUtils.HTTP_PROTOCOL)
                    || webUrl.startsWith(CommonUtils.HTTPS_PROTOCOL))) {
                int start = webUrlMatcher.start(0);
                int end = webUrlMatcher.end(0);
                Uri uri = Uri.parse(webUrl);
                spannableString.setSpan(new ActionViewClickableSpan(uri), start, end, 0);
            }
        }
        view.setText(spannableString);
        view.setMovementMethod(new LinkMovementMethod());
    }
}
