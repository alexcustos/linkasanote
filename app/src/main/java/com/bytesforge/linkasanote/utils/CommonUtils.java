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

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Patterns;

import com.google.common.base.Strings;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

public final class CommonUtils {

    private static final String TAG = CommonUtils.class.getSimpleName();

    public static final String HTTP_PROTOCOL = "http://";
    public static final int HTTP_DEFAULT_PORT = 80;
    public static final String HTTPS_PROTOCOL = "https://";
    public static final int HTTPS_DEFAULT_PORT = 443;
    public static final String DEFAULT_PROTOCOL = HTTPS_PROTOCOL;

    private CommonUtils() {
    }

    public static String convertIdn(@NonNull final String serverUrl, boolean toAscii) {
        if (!Patterns.WEB_URL.matcher(serverUrl).matches()) {
            return null;
        }
        String normalizedUrl = CommonUtils.normalizeUrlProtocol(serverUrl);
        URL url;
        try {
            url = new URL(normalizedUrl);
        } catch (MalformedURLException e) {
            // Only normalized URLs accepted
            return null;
        }
        String host = url.getHost();
        int port = url.getPort();
        String authority = (toAscii ? IDN.toASCII(host) : IDN.toUnicode(host)) +
                (port == -1 ? "" : ":" + port);
        return new Uri.Builder()
                .scheme(url.getProtocol())
                .encodedAuthority(authority)
                .encodedPath(url.getPath())
                //.encodedQuery(url.getQuery())
                //.encodedFragment(url.getRef())
                .build().toString();
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

    public static void logStackTrace(@NonNull String tag_e, @NonNull Throwable throwable) {
        checkNotNull(tag_e);
        checkNotNull(throwable);
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String tag = tag_e.substring(tag_e.lastIndexOf(".") + 1);
        if (!tag_e.equals(tag)) {
            Log.e(tag, "Stack trace: " + tag_e);
        }
        Log.e(tag, sw.toString());
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static String strFirstLine(String str) {
        str = (str == null ? null : str.trim());
        if (Strings.isNullOrEmpty(str)) return null;

        String separator = System.getProperty("line.separator");
        return str.split(separator, 2)[0].trim();
    }

    public static String formatDateTime(@NonNull Context context, @NonNull Date date) {
        checkNotNull(context);
        checkNotNull(date);
        String datePart = DateFormat.getMediumDateFormat(context).format(date);
        String timePart;
        if (DateFormat.is24HourFormat(context)) {
            timePart = DateFormat.format("HH:mm", date).toString();
        } else {
            timePart = DateFormat.getTimeFormat(context).format(date);
        }
        return datePart + " " + timePart;
    }
}
