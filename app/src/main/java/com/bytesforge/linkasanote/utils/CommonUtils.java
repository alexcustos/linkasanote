package com.bytesforge.linkasanote.utils;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Strings;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

public class CommonUtils {

    public static final String HTTP_PROTOCOL = "http://";
    public static final int HTTP_DEFAULT_PORT = 80;
    public static final String HTTPS_PROTOCOL = "https://";
    public static final int HTTPS_DEFAULT_PORT = 443;
    public static final String DEFAULT_PROTOCOL = HTTPS_PROTOCOL;

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

    @NonNull
    public static String normalizeUrlProtocol(@NonNull String url) {
        checkNotNull(url);
        boolean protocolIsEmpty = false;
        if (!url.startsWith(HTTP_PROTOCOL) && !url.startsWith(HTTPS_PROTOCOL)) {
            url = DEFAULT_PROTOCOL + url;
            protocolIsEmpty = true;
        }
        if (protocolIsEmpty) {
            Uri uri = Uri.parse(url);
            int port = uri.getPort();
            String scheme = uri.getScheme();
            if (port == HTTPS_DEFAULT_PORT) {
                scheme = HTTPS_PROTOCOL.substring(0, HTTPS_PROTOCOL.length() - 3);
                port = -1;
            }
            if (port == HTTP_DEFAULT_PORT) {
                scheme = HTTP_PROTOCOL.substring(0, HTTP_PROTOCOL.length() - 3);
                port = -1;
            }
            String authority = uri.getHost() + (port < 0 ? "" : ":" + port);
            Uri.Builder uriBuilder = new Uri.Builder()
                    .scheme(scheme)
                    .encodedAuthority(authority)
                    .encodedPath(uri.getPath())
                    .encodedQuery(uri.getQuery())
                    .encodedFragment(uri.getFragment());
            return uriBuilder.build().toString();
        }
        return url;
    }

    public static <T> T[] arrayAdd(@NonNull final T[] array, final T element) {
        checkNotNull(array);
        final T[] newArray = Arrays.copyOf(array, array.length + 1);
        newArray[newArray.length - 1] = element;

        return newArray;
    }

    public static String charRepeat(final char ch, final int repeat) {
        if (repeat <= 0) return "";

        final char[] buffer = new char[repeat];
        Arrays.fill(buffer, ch);

        return new String(buffer);
    }

    public static String strRepeat(final String str, final int repeat, final String delimiter) {
        if (repeat <= 0 || Strings.isNullOrEmpty(str)) return "";

        if (Strings.isNullOrEmpty(delimiter)) {
            return new String(new char[repeat]).replace("\0", str);
        } else {
            return new String(new char[repeat - 1]).replace("\0", str + delimiter) + str;
        }
    }

    public static String getTempDir(@NonNull Context context) {
        return checkNotNull(context).getCacheDir().getAbsolutePath();
    }

    public static void logStackTrace(@NonNull String tag, @NonNull Throwable throwable) {
        checkNotNull(tag);
        checkNotNull(throwable);
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        Log.e(tag, sw.toString());
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
