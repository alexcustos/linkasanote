package com.bytesforge.linkasanote.data.source;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;

import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.cloud.CloudFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RepositoryModule {

    @Provides
    @Singleton
    public LocalFavorites provideLocalFavorites(
            Context context, ContentResolver contentResolver) {
        return new LocalFavorites(context, contentResolver);
    }

    @Provides
    @Singleton
    public CloudFavorites provideCloudFavorites(Context context, AccountManager accountManager) {
        return new CloudFavorites(context, accountManager);
    }

    @Provides
    @Singleton
    @Local
    public DataSource provideLocalDataSource(
            ContentResolver contentResolver, LocalFavorites localFavorites) {
        return new LocalDataSource(contentResolver, localFavorites);
    }

    @Provides
    @Singleton
    @Cloud
    public DataSource provideCloudDataSource(
            Context context, BaseSchedulerProvider schedulerProvider,
            LocalFavorites localFavorites, CloudFavorites cloudFavorites) {
        return new CloudDataSource(context, schedulerProvider, localFavorites, cloudFavorites);
    }

    @Provides
    @Singleton
    public Repository provideRepository(
            @Local DataSource localDataSource, @Cloud DataSource cloudDataSource) {
        return new Repository(localDataSource, cloudDataSource);
    }
}
