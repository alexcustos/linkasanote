package com.bytesforge.linkasanote;

import android.app.Application;
import android.content.Context;

import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.RepositoryModule;
import com.bytesforge.linkasanote.settings.SettingsModule;
import com.bytesforge.linkasanote.utils.schedulers.SchedulerProviderModule;

import java.lang.ref.WeakReference;

public class LaanoApplication extends Application {

    ApplicationComponent applicationComponent;
    private static WeakReference<Context> context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = new WeakReference<>(this);

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

    public static Context getContext() {
        return context.get();
    }
}
