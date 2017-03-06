package com.bytesforge.linkasanote.data.source;

import android.content.ContentResolver;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ProviderModule {

    @Provides
    @Singleton
    public ContentResolver provideContentResolver(Context context) {
        return context.getContentResolver();
    }
}
