package com.bytesforge.linkasanote.utils.schedulers;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class SchedulerProviderModule {

    @Provides
    @Singleton
    BaseSchedulerProvider provideSchedulerProvider() {
        return new SchedulerProvider();
    }
}
