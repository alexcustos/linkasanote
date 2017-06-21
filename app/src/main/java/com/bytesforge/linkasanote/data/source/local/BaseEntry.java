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

package com.bytesforge.linkasanote.data.source.local;

import android.provider.BaseColumns;

public interface BaseEntry extends BaseColumns {

    // NOTE: entry_id must not be part of SyncState
    String COLUMN_NAME_ENTRY_ID = "entry_id";
    String COLUMN_NAME_CREATED = "created";
    String COLUMN_NAME_UPDATED = "updated";
    // SyncState
    String COLUMN_NAME_ETAG = "etag";
    String COLUMN_NAME_DUPLICATED = "duplicated";
    String COLUMN_NAME_CONFLICTED = "conflicted";
    String COLUMN_NAME_DELETED = "deleted";
    String COLUMN_NAME_SYNCED = "synced";
}

