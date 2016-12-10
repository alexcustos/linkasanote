package com.bytesforge.linkasanote.data.source;

import android.content.ContentResolver;

import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;
import com.squareup.sqlbrite.BriteContentResolver;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RepositoryModule {

    @Provides
    @Singleton
    @Local
    DataSource provideLocalDataSource(
            ContentResolver contentResolver,
            BriteContentResolver briteResolver) {
        return new LocalDataSource(contentResolver, briteResolver);
    }

    @Provides
    @Singleton
    @Cloud
    DataSource provideCloudDataSource() {
        return new CloudDataSource();
    }
}
