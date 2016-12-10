package com.bytesforge.linkasanote.utils.schedulers;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import rx.Scheduler;
import rx.schedulers.Schedulers;

@Singleton
public class ImmediateSchedulerProvider implements BaseSchedulerProvider {

    @NonNull
    @Override
    public Scheduler computation() {
        return Schedulers.immediate();
    }

    @NonNull
    @Override
    public Scheduler io() {
        return Schedulers.immediate();
    }

    @NonNull
    @Override
    public Scheduler ui() {
        return Schedulers.immediate();
    }
}
