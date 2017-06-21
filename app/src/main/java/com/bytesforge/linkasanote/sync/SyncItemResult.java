/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

