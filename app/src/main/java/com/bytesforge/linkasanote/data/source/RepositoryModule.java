package com.bytesforge.linkasanote.data.source;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;

import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.cloud.CloudFavorites;
import com.bytesforge.linkasanote.data.source.cloud.CloudLinks;
import com.bytesforge.linkasanote.data.source.cloud.CloudNotes;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalLinks;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.data.source.local.LocalTags;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RepositoryModule {

    @Provides
    @Singleton
    public LocalTags provideLocalTags(ContentResolver contentResolver) {
        return new LocalTags(contentResolver);
    }

    @Provides
    @Singleton
    public LocalLinks provideLocalLinks(
            Context context, ContentResolver contentResolver, LocalTags localTags) {
        return new LocalLinks(context, contentResolver, localTags);
    }

    @Provides
    @Singleton
    public CloudLinks provideCloudLinks(
            Context context, AccountManager accountManager, Settings settings) {
        return new CloudLinks(context, accountManager, settings);
    }

    @Provides
    @Singleton
    public LocalFavorites provideLocalFavorites(
            Context context, ContentResolver contentResolver, LocalTags localTags) {
        return new LocalFavorites(context, contentResolver, localTags);
    }

    @Provides
    @Singleton
    public CloudFavorites provideCloudFavorites(
            Context context, AccountManager accountManager, Settings settings) {
        return new CloudFavorites(context, accountManager, settings);
    }

    @Provides
    @Singleton
    public LocalNotes provideLocalNotes(
            Context context, ContentResolver contentResolver, LocalTags localTags) {
        return new LocalNotes(context, contentResolver, localTags);
    }

    @Provides
    @Singleton
    public CloudNotes provideCloudNotes(
            Context context, AccountManager accountManager, Settings settings) {
        return new CloudNotes(context, accountManager, settings);
    }

    @Provides
    @Singleton
    @Local
    public DataSource provideLocalDataSource(
            ContentResolver contentResolver, LocalLinks localLinks,
            LocalFavorites localFavorites, LocalNotes localNotes, LocalTags localTags) {
        return new LocalDataSource(contentResolver, localLinks,
                localFavorites, localNotes, localTags);
    }

    @Provides
    @Singleton
    @Cloud
    public DataSource provideCloudDataSource(
            Context context, BaseSchedulerProvider schedulerProvider,
            LocalLinks localLinks, CloudLinks cloudLinks,
            LocalFavorites localFavorites, CloudFavorites cloudFavorites,
            LocalNotes localNotes, CloudNotes cloudNotes) {
        return new CloudDataSource(context, schedulerProvider,
                localLinks, cloudLinks,
                localFavorites, cloudFavorites,
                localNotes, cloudNotes);
    }

    @Provides
    @Singleton
    public Repository provideRepository(
            @Local DataSource localDataSource, @Cloud DataSource cloudDataSource) {
        return new Repository(localDataSource, cloudDataSource);
    }
}
