package com.bytesforge.linkasanote.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class TokenTextView extends TextView {

    public TokenTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        //setCompoundDrawablesWithIntrinsicBounds(0, 0, selected ? R.drawable.ic_close : 0, 0);
    }
}
