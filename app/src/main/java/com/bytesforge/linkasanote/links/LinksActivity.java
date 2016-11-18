package com.bytesforge.linkasanote.links;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.utils.ActivityUtils;

import javax.inject.Inject;

public class LinksActivity extends AppCompatActivity {

    @Inject LinksPresenter linksPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.links_activity);

        LinksFragment linksFragment =
                (LinksFragment) getSupportFragmentManager().findFragmentById(R.id.contentFrame);
        if (linksFragment == null) {
            linksFragment = LinksFragment.newInstance();
            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), linksFragment, R.id.contentFrame);
        }

        DaggerLinksComponent.builder()
                .linksPresenterModule(new LinksPresenterModule(linksFragment))
                .build().inject(this);
    }
}
