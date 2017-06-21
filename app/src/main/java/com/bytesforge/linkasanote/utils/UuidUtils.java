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

package com.bytesforge.linkasanote.utils;


import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Base64;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UuidUtils {

    private static final String TAG = UuidUtils.class.getSimpleName();
    private static final int KEY_LENGTH = 22;

    private UuidUtils() {
    }

    @VisibleForTesting
    public static int FLAGS = Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE;

    public static String generateKey() {
        UUID uuid = UUID.randomUUID();
        return keyFromUuid(uuid);
    }

    public static String keyFromUuid(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return Base64.encodeToString(byteBuffer.array(), FLAGS);
    }

    @Nullable
    public static UUID uuidFromKey(String key) {
        try {
            byte[] bytes = Base64.decode(key, FLAGS);
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            return new UUID(byteBuffer.getLong(), byteBuffer.getLong());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid UUID key was detected [" + key + "]");
            CommonUtils.logStackTrace(TAG, e);
        }
        return null;
    }

    public static boolean isKeyValidUuid(String key) {
        return key.length() == KEY_LENGTH; // && uuidFromKey(key) != null;
    }
}
