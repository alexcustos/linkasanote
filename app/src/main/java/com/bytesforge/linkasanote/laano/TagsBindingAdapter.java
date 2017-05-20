package com.bytesforge.linkasanote.laano;

import android.databinding.BindingAdapter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.utils.TokenTextView;
import com.tokenautocomplete.ViewSpan;

import java.util.List;

public class TagsBindingAdapter {

    private static final String TAG = TagsBindingAdapter.class.getSimpleName();

    private static SparseIntArray tagsViewWidths = new SparseIntArray();

    private TagsBindingAdapter() {
    }

    @BindingAdapter({"android:text"})
    public static void showNoteTags(TextView view, List<Tag> tags) {
        if (view == null || tags == null || tags.isEmpty()) return;

        view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        // NOTE: it's to prevent the view to be changed from empty to filled when adapter rebind the item
        if (tagsViewWidths.get(view.getId()) > 0 && view.getLineCount() > 0) {
            setTagsTextView(view, tags);
        } else {
            ViewTreeObserver observer = view.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    tagsViewWidths.put(view.getId(), view.getMeasuredWidth());
                    setTagsTextView(view, tags);
                }
            });
        }
    }

    private static void setTagsTextView(TextView view, List<Tag> tags) {
        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        SpannableStringBuilder noteTags = new SpannableStringBuilder();
        int spanHeight = 0;
        for (Tag tag : tags) {
            TokenTextView tokenView = (TokenTextView) inflater.inflate(
                    R.layout.token_tag, (ViewGroup) null, false);
            tokenView.setText(tag.getName());
            int maxWidth = tagsViewWidths.get(view.getId());
            ViewSpan viewSpan = new ViewSpan(tokenView, maxWidth);
            if (spanHeight == 0) {
                spanHeight = viewSpan.getHeight();
            }
            SpannableStringBuilder tagBuilder = new SpannableStringBuilder(",, ");
            tagBuilder.setSpan(viewSpan, 0, tagBuilder.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            noteTags.append(tagBuilder);
        }
        view.setText(noteTags);
        if (view.getLineCount() == 1) {
            view.getLayoutParams().height = spanHeight;
        }
    }

    public static void invalidateTagsViewWidths() {
        tagsViewWidths.clear();
    }
}
