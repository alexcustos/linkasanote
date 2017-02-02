package com.bytesforge.linkasanote;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;

import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.RepositoryModule;
import com.bytesforge.linkasanote.settings.SettingsModule;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.SchedulerProviderModule;
import com.squareup.sqlbrite.BriteContentResolver;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        SettingsModule.class,
        RepositoryModule.class,
        ProviderModule.class,
        SchedulerProviderModule.class})
public interface ApplicationComponent {

    Context getContext();
    SharedPreferences getSharedPreferences();
    Repository getRepository();
    ContentResolver getContentResolver();
    BriteContentResolver getBriteContentResolver();
    BaseSchedulerProvider getSchedulerProvider();
}
