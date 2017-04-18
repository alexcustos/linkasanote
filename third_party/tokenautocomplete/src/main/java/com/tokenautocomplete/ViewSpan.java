package com.tokenautocomplete;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.view.ViewGroup;

/**
 * Span that holds a view it draws when rendering
 *
 * Created on 2/3/15.
 * @author mgod
 *
 * Updated on 15/04/2017
 * Aleksandr Borisenko
 */
public class ViewSpan extends ReplacementSpan {
    protected View view;
    private int maxWidth;

    public ViewSpan(View v, int maxWidth) {
        super();
        this.maxWidth = maxWidth;
        view = v;
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void prepView() {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    @Override
    public void draw(
            @NonNull Canvas canvas, CharSequence text,
            @IntRange(from = 0) int start, @IntRange(from = 0) int end,
            float x, int top, int y, int bottom, @NonNull Paint paint) {
        prepView();

        canvas.save();
        canvas.translate(x, top);
        view.draw(canvas);
        canvas.restore();
    }

    @Override
    public int getSize(
            @NonNull Paint paint, CharSequence text,
            @IntRange(from = 0) int start, @IntRange(from = 0) int end,
            @Nullable Paint.FontMetricsInt fm) {
        prepView();
        if (fm != null) {
            int height = view.getMeasuredHeight();
            int top_need = height - (fm.bottom - fm.top);
            if (top_need > 0) {
                int top_patch = top_need / 2;
                fm.top -= top_patch;
                fm.bottom += top_need - top_patch;
                // NOTE: only the first line has 1dp margin which have to be compensated with this gap
                int ascent_need = height - (fm.descent - fm.ascent) + dpToPx(2);
                if (ascent_need > 0) {
                    int ascent_patch = ascent_need / 2;
                    fm.ascent -= ascent_patch;
                    fm.descent += ascent_need - ascent_patch;
                }
            }
        }
        return view.getRight();
    }

    private static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
