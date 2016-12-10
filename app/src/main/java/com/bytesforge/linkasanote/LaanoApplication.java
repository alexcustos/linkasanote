package com.bytesforge.linkasanote;

import android.app.Application;

import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.RepositoryModule;
import com.bytesforge.linkasanote.settings.SettingsModule;
import com.bytesforge.linkasanote.utils.schedulers.SchedulerProviderModule;

public class LaanoApplication extends Application {

    ApplicationComponent applicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(getApplicationContext()))
                .settingsModule(new SettingsModule())
                .repositoryModule(new RepositoryModule())
                .providerModule(new ProviderModule())
                .schedulerProviderModule(new SchedulerProviderModule())
                .build();
    }

    public ApplicationComponent getApplicationComponent() {
        return applicationComponent;
    }
}
