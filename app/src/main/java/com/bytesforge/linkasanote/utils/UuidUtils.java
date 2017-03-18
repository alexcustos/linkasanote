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
            Log.e(TAG, "Invalid UUID key was detected [" + key + "]", e);
        }
        return null;
    }

    public static boolean isKeyValidUuid(String key) {
        return key.length() == KEY_LENGTH; // && uuidFromKey(key) != null;
    }
}
