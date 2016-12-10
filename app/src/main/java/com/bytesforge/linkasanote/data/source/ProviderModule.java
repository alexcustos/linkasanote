package com.bytesforge.linkasanote.data.source;

import android.content.ContentResolver;
import android.content.Context;

import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.squareup.sqlbrite.BriteContentResolver;
import com.squareup.sqlbrite.SqlBrite;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ProviderModule {

    @Provides
    @Singleton
    SqlBrite provideSqlBrite() {
        return new SqlBrite.Builder().build();
    }

    @Provides
    @Singleton
    ContentResolver provideContentResolver(Context context) {
        return context.getContentResolver();
    }

    @Provides
    @Singleton
    BriteContentResolver provideBriteResolver(
            SqlBrite sqlBrite,
            ContentResolver contentResolver,
            BaseSchedulerProvider schedulerProvider) {
        return sqlBrite.wrapContentProvider(contentResolver, schedulerProvider.io());
    }
}
