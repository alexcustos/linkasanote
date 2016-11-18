package com.bytesforge.linkasanote;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public final class ApplicationModule {

    private final Context context;

    public ApplicationModule(Context context) {
        this.context = context;
    }

    @Provides
    Context provideContext() {
        return context;
    }
}
