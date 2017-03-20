package com.bytesforge.linkasanote.sync;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.bytesforge.linkasanote.BuildConfig;
import com.bytesforge.linkasanote.R;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncNotifications {

    public static final String ACTION_SYNC_START = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_START";
    public static final String ACTION_SYNC_END = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_END";
    public static final String ACTION_SYNC_LINK = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_LINK";
    public static final String ACTION_SYNC_FAVORITE = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_FAVORITE";
    public static final String ACTION_SYNC_NOTE = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_NOTE";

    public static final String EXTRA_ACCOUNT_NAME = "ACCOUNT_NAME";
    public static final String EXTRA_ID = "ID";
    public static final String EXTRA_STATUS = "STATUS";

    public static final int STATUS_CREATED = 0;
    public static final int STATUS_UPDATED = 1;
    public static final int STATUS_DELETED = 2;

    private static final int NOTIFICATION_SYNC = 0;

    private final Context context;
    private final NotificationManagerCompat notificationManager;

    public SyncNotifications(Context context) {
        this.context = context;
        notificationManager = NotificationManagerCompat.from(context);
    }

    public void sendSyncBroadcast(String action, String accountName) {
        sendSyncBroadcast(action, accountName, null, -1);
    }

    public void sendSyncBroadcast(String action, String accountName, String id, int status) {
        Intent intent = new Intent(action);

        intent.putExtra(EXTRA_ACCOUNT_NAME, accountName);
        if (id != null) intent.putExtra(EXTRA_ID, id);
        if (status >= 0) intent.putExtra(EXTRA_STATUS, status);

        context.sendBroadcast(intent);
    }

    public void notifyFailedSynchronization(@NonNull String text) {
        checkNotNull(text);
        notifyFailedSynchronization(null, text);
    }

    public void notifyFailedSynchronization(String title, @NonNull String text) {
        checkNotNull(text);

        //notificationManager.cancel(NOTIFICATION_SYNC);
        String defaultTitle = context.getString(R.string.sync_adapter_title_failed_default);
        String notificationTitle = title == null ? defaultTitle : defaultTitle + ": " + title;

        Notification notification = new NotificationCompat.Builder(context)
                // TODO: change to simplified application icon
                .setSmallIcon(R.drawable.ic_sync_white)
                .setLargeIcon(getLauncherBitmap())
                .setColor(context.getResources().getColor(R.color.color_primary, context.getTheme()))
                .setTicker(notificationTitle)
                .setContentTitle(notificationTitle)
                .setContentText(text)
                .build();
        notificationManager.notify(NOTIFICATION_SYNC, notification);
    }

    private Bitmap getLauncherBitmap() {
        Drawable logo = context.getDrawable(R.mipmap.ic_launcher);
        if (logo instanceof BitmapDrawable) {
            return ((BitmapDrawable) logo).getBitmap();
        }
        return null;
    }
}
