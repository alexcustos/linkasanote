package com.bytesforge.linkasanote.utils;

import android.content.Intent;
import android.net.Uri;
import android.text.style.ClickableSpan;
import android.view.View;

public class ActionViewClickableSpan extends ClickableSpan {

    private Uri uri;

    public ActionViewClickableSpan(Uri uri) {
        this.uri = uri;
    }

    @Override
    public void onClick(View widget) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        widget.getContext().startActivity(intent);
    }
}
