package com.bytesforge.linkasanote.utils;

import android.support.annotation.NonNull;

import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URL;

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
}
