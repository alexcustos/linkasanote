package com.bytesforge.linkasanote;

import android.app.Application;

import com.bytesforge.linkasanote.settings.DaggerSettingsComponent;
import com.bytesforge.linkasanote.settings.SettingsComponent;
import com.bytesforge.linkasanote.settings.SettingsModule;

public class LaanoApplication extends Application {

    SettingsComponent settingsComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        settingsComponent = DaggerSettingsComponent.builder()
                .applicationModule(new ApplicationModule(getApplicationContext()))
                .settingsModule(new SettingsModule())
                .build();
    }

    public SettingsComponent getSettingsComponent() {
        return settingsComponent;
    }
}
