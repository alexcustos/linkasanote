/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bytesforge.linkasanote;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.support.annotation.VisibleForTesting;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;

import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.RepositoryModule;
import com.bytesforge.linkasanote.settings.SettingsModule;
import com.bytesforge.linkasanote.utils.UuidUtils;
import com.bytesforge.linkasanote.utils.schedulers.SchedulerProviderModule;
import com.facebook.stetho.Stetho;

import java.lang.ref.WeakReference;

public class LaanoApplication extends MultiDexApplication {

    private static final boolean STRICT_MODE = false;
    private static final boolean STETHO_MODE = false;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private ApplicationComponent applicationComponent;
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

        if (BuildConfig.DEBUG && STETHO_MODE) {
            Stetho.initializeWithDefaults(this);
        }

        if (BuildConfig.DEBUG && STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .penaltyLog()
                    //.penaltyDeath() // NOTE: it wants contentProvider's databaseHelper closed
                    .build());
        }
    }

    public ApplicationComponent getApplicationComponent() {
        return applicationComponent;
    }

    @VisibleForTesting
    public void setApplicationComponent(ApplicationComponent applicationComponent) {
        this.applicationComponent = applicationComponent;
    }

    private static Context getContext() {
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
