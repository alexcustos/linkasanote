package com.bytesforge.linkasanote.utils;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UuidUtils {

    public static String generateKey() {
        UUID uuid = UUID.randomUUID();

        return keyFromUuid(uuid);
    }

    public static String keyFromUuid(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);

        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());

        return Base64.encodeToString(
                byteBuffer.array(), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
    }

    public static UUID uuidFromKey(String key) {
        byte[] bytes = Base64.decode(key, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

        return new UUID(byteBuffer.getLong(), byteBuffer.getLong());
    }
}
