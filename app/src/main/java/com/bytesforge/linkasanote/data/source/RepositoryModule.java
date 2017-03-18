package com.bytesforge.linkasanote.data.source;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;

import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RepositoryModule {

    @Provides
    @Singleton
    @Local
    public DataSource provideLocalDataSource(ContentResolver contentResolver) {
        return new LocalDataSource(contentResolver);
    }

    @Provides
    @Singleton
    @Cloud
    public DataSource provideCloudDataSource(
            Context context, SharedPreferences sharedPreferences,
            ContentResolver contentResolver, BaseSchedulerProvider schedulerProvider,
            AccountManager accountManager) {
        return new CloudDataSource(context, sharedPreferences,
                contentResolver, schedulerProvider, accountManager);
    }

    @Provides
    @Singleton
    public Repository provideRepository(
            @Local DataSource localDataSource, @Cloud DataSource cloudDataSource) {
        return new Repository(localDataSource, cloudDataSource);
    }
}
