package com.bytesforge.linkasanote;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.RepositoryModule;
import com.bytesforge.linkasanote.settings.SettingsModule;
import com.bytesforge.linkasanote.utils.UuidUtils;
import com.bytesforge.linkasanote.utils.schedulers.SchedulerProviderModule;

import java.lang.ref.WeakReference;

public class LaanoApplication extends Application {

    ApplicationComponent applicationComponent;
    private static WeakReference<Context> context;

    private static String applicationId;
    private static final String SETTING_APPLICATION_ID = "APPLICATION_ID";

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

    @VisibleForTesting
    public void setApplicationComponent(ApplicationComponent applicationComponent) {
        this.applicationComponent = applicationComponent;
    }

    public static Context getContext() {
        return context.get();
    }

    public synchronized static String getApplicationId() {
        if (applicationId == null) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences(
                    SETTING_APPLICATION_ID, Context.MODE_PRIVATE);
            applicationId = sharedPreferences.getString(SETTING_APPLICATION_ID, null);
            if (applicationId == null) {
                applicationId = UuidUtils.generateKey();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(SETTING_APPLICATION_ID, applicationId);
                editor.apply();
            }
        }
        return applicationId;
    }
}
