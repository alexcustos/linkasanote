package com.bytesforge.linkasanote.sync;

class SyncItemResult {

    public enum Status {FAILS_COUNT, DB_ACCESS_ERROR, SOURCE_NOT_READY}

    private final Status status;
    private int failsCount;

    public SyncItemResult(Status status) {
        this.status = status;
        failsCount = 0;
    }

    public boolean isDbAccessError() {
        return status == Status.DB_ACCESS_ERROR;
    }

    public boolean isSourceNotReady() {
        return status == Status.SOURCE_NOT_READY;
    }

    public int getFailsCount() {
        return failsCount;
    }

    public void incFailsCount() {
        failsCount++;
    }

    public boolean isSuccess() {
        return status == Status.FAILS_COUNT && failsCount == 0;
    }

    public boolean isFatal() {
        return status == Status.DB_ACCESS_ERROR || status == Status.SOURCE_NOT_READY;
    }
}

