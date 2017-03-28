package com.bytesforge.linkasanote.utils;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class TokenTextView extends AppCompatTextView {

    public TokenTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        //setCompoundDrawablesWithIntrinsicBounds(0, 0, selected ? R.drawable.ic_close : 0, 0);
    }
}
