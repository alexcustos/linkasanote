package com.bytesforge.linkasanote.settings;

import android.content.SharedPreferences;

import com.bytesforge.linkasanote.ApplicationModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {SettingsModule.class, ApplicationModule.class})
public interface SettingsComponent {

    SharedPreferences getSharedPreferences();
}
