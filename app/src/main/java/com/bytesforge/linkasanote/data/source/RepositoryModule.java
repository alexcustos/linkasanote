package com.bytesforge.linkasanote.data.source;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;

import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;

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
    public DataSource provideCloudDataSource(Context context, SharedPreferences sharedPreferences) {
        return new CloudDataSource(context, sharedPreferences);
    }

    @Provides
    @Singleton
    public Repository provideRepository(
            @Local DataSource localDataSource, @Cloud DataSource cloudDataSource) {
        return new Repository(localDataSource, cloudDataSource);
    }
}
