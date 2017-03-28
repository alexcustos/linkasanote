package com.bytesforge.linkasanote.sync;

import com.bytesforge.linkasanote.ApplicationModule;
import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.RepositoryModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        ProviderModule.class,
        RepositoryModule.class})
public interface SyncServiceComponent {

    void inject(SyncService syncService);
}
