package com.bytesforge.linkasanote.laano;

import android.databinding.BindingAdapter;
import android.databinding.BindingConversion;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.ActivityUtils;

public class LaanoBindingAdapters {

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

    @BindingAdapter({"app:srcCompat"})
    public static void setSrcCompat(ImageButton view, Drawable drawable) {
        view.setImageDrawable(drawable);
    }

    @BindingAdapter({"scrollableText"})
    public static void setScrollableText(TextView view, String text) {
        view.setText(text);
        view.setMovementMethod(new ScrollingMovementMethod());
    }
}
