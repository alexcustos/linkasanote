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
        if (tagsViewWidths.get(view.getId()) > 0) {
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
        for (Tag tag : tags) {
            TokenTextView tokenView = (TokenTextView) inflater.inflate(
                    R.layout.token_tag, (ViewGroup) null, false);
            tokenView.setText(tag.getName());
            int maxWidth = tagsViewWidths.get(view.getId());
            ViewSpan viewSpan = new ViewSpan(tokenView, maxWidth);
            SpannableStringBuilder tagBuilder = new SpannableStringBuilder(",, ");
            tagBuilder.setSpan(viewSpan, 0, tagBuilder.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            noteTags.append(tagBuilder);
        }
        view.setText(noteTags);
    }

    public static void invalidateTagsViewWidths() {
        tagsViewWidths.clear();
    }
}
