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
package com.bytesforge.linkasanote.sync

class SyncItemResult(private val status: Status) {
    enum class Status {
        FAILS_COUNT, DB_ACCESS_ERROR, SOURCE_NOT_READY
    }

    var failsCount = 0
        private set
    val isDbAccessError: Boolean
        get() = status == Status.DB_ACCESS_ERROR
    val isSourceNotReady: Boolean
        get() = status == Status.SOURCE_NOT_READY

    fun incFailsCount() {
        failsCount++
    }

    val isSuccess: Boolean
        get() = status == Status.FAILS_COUNT && failsCount == 0
    val isFatal: Boolean
        get() = status == Status.DB_ACCESS_ERROR || status == Status.SOURCE_NOT_READY
}