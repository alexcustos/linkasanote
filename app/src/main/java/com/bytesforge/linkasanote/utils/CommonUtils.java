package com.bytesforge.linkasanote.utils;

import android.support.annotation.NonNull;

import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

public class CommonUtils {

    public static String convertIdn(@NonNull final String serverUrl, boolean toAscii) {
        URL url;
        try {
            url = new URL(serverUrl);
        } catch (MalformedURLException e) {
            // Only normalized URLs accepted
            return null;
        }
        String host = url.getHost();
        int port = url.getPort();

        return url.getProtocol() + "://" +
                (toAscii ? IDN.toASCII(host) : IDN.toUnicode(host)) +
                ((port == -1 || port == 80) ? "" : ":" + port) +
                url.getPath();
    }

    public static <T> T[] arrayAdd(@NonNull final T[] array, final T element) {
        checkNotNull(array);

        final T[] newArray = (T[]) Arrays.copyOf(array, array.length + 1);
        newArray[newArray.length - 1] = element;

        return newArray;
    }

    public static String charRepeat(final char ch, final int repeat) {
        if (repeat <= 0) return "";

        final char[] buffer = new char[repeat];
        Arrays.fill(buffer, ch);

        return new String(buffer);
    }
}
