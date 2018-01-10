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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.bytesforge.linkasanote.BuildConfig;
import com.bytesforge.linkasanote.R;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncNotifications {
    private static final String TAG = SyncNotifications.class.getSimpleName();

    private static final String CHANNEL_NAME_SYNC = "sync_channel";

    public static final String ACTION_SYNC = BuildConfig.APPLICATION_ID + ".ACTION_SYNC";
    public static final String ACTION_SYNC_LINKS =
            BuildConfig.APPLICATION_ID + ".ACTION_SYNC_LINKS";
    public static final String ACTION_SYNC_FAVORITES =
            BuildConfig.APPLICATION_ID + ".ACTION_SYNC_FAVORITES";
    public static final String ACTION_SYNC_NOTES =
            BuildConfig.APPLICATION_ID + ".ACTION_SYNC_NOTES";

    public static final String EXTRA_ACCOUNT_NAME = "ACCOUNT_NAME";
    public static final String EXTRA_STATUS = "STATUS";
    public static final String EXTRA_ID = "ID";
    public static final String EXTRA_COUNT = "COUNT";

    public static final int STATUS_SYNC_START = 10;
    public static final int STATUS_SYNC_STOP = 11;
    public static final int STATUS_CREATED = 20;
    public static final int STATUS_UPDATED = 21;
    public static final int STATUS_DELETED = 22;
    public static final int STATUS_UPLOADED = 30;
    public static final int STATUS_DOWNLOADED = 31;

    private static final int NOTIFICATION_SYNC = 0;

    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private String accountName;

    public SyncNotifications(Context context) {
        this.context = context;
        notificationManager = NotificationManagerCompat.from(context);
        initChannels(this.context);
    }

    public void sendSyncBroadcast(String action, int status) {
        sendSyncBroadcast(action, status, null, -1);
    }

    public void sendSyncBroadcast(String action, int status, String id) {
        sendSyncBroadcast(action, status, id, -1);
    }

    public void sendSyncBroadcast(String action, int status, String id, int count) {
        checkNotNull(accountName);
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ACCOUNT_NAME, accountName);
        if (status >= 0) intent.putExtra(EXTRA_STATUS, status);
        if (id != null) intent.putExtra(EXTRA_ID, id);
        if (count >= 0) intent.putExtra(EXTRA_COUNT, count);

        context.sendBroadcast(intent);
    }

    public void notifyFailedSynchronization(@NonNull String text) {
        checkNotNull(text);
        notifyFailedSynchronization(null, text);
    }

    private void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e(TAG, "Error while retrieving Notification Service");
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_NAME_SYNC,
                context.getString(R.string.sync_adapter_sync_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.sync_adapter_sync_channel_description));
        notificationManager.createNotificationChannel(channel);
    }

    public void notifyFailedSynchronization(String title, @NonNull String text) {
        checkNotNull(text);
        //notificationManager.cancel(NOTIFICATION_SYNC);
        String defaultTitle = context.getString(R.string.sync_adapter_title_failed_default);
        String notificationTitle = title == null ? defaultTitle : defaultTitle + ": " + title;

        int color = ContextCompat.getColor(context, R.color.color_primary);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_NAME_SYNC)
                .setSmallIcon(R.drawable.ic_error_white)
                .setLargeIcon(getLauncherBitmap())
                .setColor(color)
                .setTicker(notificationTitle)
                .setContentTitle(notificationTitle)
                .setContentText(text)
                .build();
        notificationManager.notify(NOTIFICATION_SYNC, notification);
    }

    private Bitmap getLauncherBitmap() {
        Drawable logo = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);
        if (logo instanceof BitmapDrawable) {
            return ((BitmapDrawable) logo).getBitmap();
        }
        return null;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
}
