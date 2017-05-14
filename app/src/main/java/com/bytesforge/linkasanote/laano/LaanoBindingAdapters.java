package com.bytesforge.linkasanote.laano;

import android.databinding.BindingAdapter;
import android.databinding.BindingConversion;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
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
}
