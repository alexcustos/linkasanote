package com.bytesforge.linkasanote.utils;

import android.support.test.espresso.IdlingResource;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;


public final class SimpleCountingIdlingResource implements IdlingResource {

    private final String resourceName;
    private final AtomicInteger counter = new AtomicInteger(0);
    private volatile ResourceCallback resourceCallback;

    public SimpleCountingIdlingResource(String resourceName) {
        this.resourceName = checkNotNull(resourceName);
    }

    @Override
    public String getName() {
        return resourceName;
    }

    @Override
    public boolean isIdleNow() {
        return counter.get() == 0;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.resourceCallback = resourceCallback;
    }

    public void increment() {
        counter.getAndIncrement();
    }

    public void decrement() {
        int counterVal = counter.decrementAndGet();

        if (counterVal == 0 && null != resourceCallback) {
            resourceCallback.onTransitionToIdle();
        }

        if (counterVal < 0) {
            throw new IllegalArgumentException("Counter has been corrupted!");
        }
    }
}
