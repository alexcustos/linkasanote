package com.bytesforge.linkasanote.laano.links.addeditlink;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.utils.TokenTextView;
import com.tokenautocomplete.TokenCompleteTextView;

public class LinkTagsCompletionView extends TokenCompleteTextView<Tag> {

    public LinkTagsCompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View getViewForObject(Tag tag) {
        TokenTextView token = (TokenTextView) inflate(getContext(), R.layout.token_tag, null);
        token.setText(tag.getName());
        return token;
    }

    @Override
    protected Tag defaultObject(String completionText) {
        return new Tag(completionText);
    }
}
