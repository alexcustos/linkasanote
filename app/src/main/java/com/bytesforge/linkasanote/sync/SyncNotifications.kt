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
package com.bytesforge.linkasanote.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bytesforge.linkasanote.BuildConfig
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.sync.SyncNotifications
import com.google.common.base.Preconditions

class SyncNotifications(private val context: Context?) {
    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context!!)

    private var accountName: String? = null
    @JvmOverloads
    fun sendSyncBroadcast(action: String?, status: Int, id: String? = null, count: Int = -1) {
        Preconditions.checkNotNull(accountName)
        val intent = Intent(action)
        intent.putExtra(EXTRA_ACCOUNT_NAME, accountName)
        if (status >= 0) intent.putExtra(EXTRA_STATUS, status)
        if (id != null) intent.putExtra(EXTRA_ID, id)
        if (count >= 0) intent.putExtra(EXTRA_COUNT, count)
        context!!.sendBroadcast(intent)
    }

    fun notifyFailedSynchronization(text: String) {
        Preconditions.checkNotNull(text)
        notifyFailedSynchronization(null, text)
    }

    private fun initChannels(context: Context?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager == null) {
            Log.e(TAG, "Error while retrieving Notification Service")
            return
        }
        val channel = NotificationChannel(
            CHANNEL_NAME_SYNC,
            context.getString(R.string.sync_adapter_sync_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = context.getString(R.string.sync_adapter_sync_channel_description)
        notificationManager.createNotificationChannel(channel)
    }

    fun notifyFailedSynchronization(title: String?, text: String) {
        Preconditions.checkNotNull(text)
        //notificationManager.cancel(NOTIFICATION_SYNC);
        val defaultTitle = context!!.getString(R.string.sync_adapter_title_failed_default)
        val notificationTitle = if (title == null) defaultTitle else "$defaultTitle: $title"
        val color = ContextCompat.getColor(context, R.color.color_primary)
        val notification = NotificationCompat.Builder(context, CHANNEL_NAME_SYNC)
            .setSmallIcon(R.drawable.ic_error_white)
            .setLargeIcon(launcherBitmap)
            .setColor(color)
            .setTicker(notificationTitle)
            .setContentTitle(notificationTitle)
            .setContentText(text)
            .build()
        notificationManager.notify(NOTIFICATION_SYNC, notification)
    }

    private val launcherBitmap: Bitmap?
        get() {
            val logo = ContextCompat.getDrawable(context!!, R.mipmap.ic_launcher)
            return if (logo is BitmapDrawable) {
                logo.bitmap
            } else null
        }

    fun setAccountName(accountName: String?) {
        this.accountName = accountName
    }

    companion object {
        private val TAG = SyncNotifications::class.java.simpleName
        private const val CHANNEL_NAME_SYNC = "sync_channel"
        const val ACTION_SYNC = BuildConfig.APPLICATION_ID + ".ACTION_SYNC"
        const val ACTION_SYNC_LINKS = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_LINKS"
        const val ACTION_SYNC_FAVORITES = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_FAVORITES"
        const val ACTION_SYNC_NOTES = BuildConfig.APPLICATION_ID + ".ACTION_SYNC_NOTES"
        const val EXTRA_ACCOUNT_NAME = "ACCOUNT_NAME"
        const val EXTRA_STATUS = "STATUS"
        const val EXTRA_ID = "ID"
        const val EXTRA_COUNT = "COUNT"
        const val STATUS_SYNC_START = 10
        const val STATUS_SYNC_STOP = 11
        const val STATUS_CREATED = 20
        const val STATUS_UPDATED = 21
        const val STATUS_DELETED = 22
        const val STATUS_UPLOADED = 30
        const val STATUS_DOWNLOADED = 31
        private const val NOTIFICATION_SYNC = 0
    }

    init {
        initChannels(context)
    }
}