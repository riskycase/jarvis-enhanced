package com.riskycase.jarvisEnhanced.util

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.riskycase.jarvisEnhanced.R
import com.riskycase.jarvisEnhanced.repository.SnapRepository

class NotificationMaker {

    fun setup(context: Context) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "messages",
            "Message notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Channel for all message notifications"
        channel.group = "system"
        val notificationGroup = NotificationChannelGroup("system", "Channels")
        notificationManager.createNotificationChannelGroup(notificationGroup)
        notificationManager.createNotificationChannel(channel)
    }

    fun makeNotification(context: Context, application: Application) {

        setup(context)
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        Thread {
            val snapRepository = SnapRepository(application)

            val snaps = snapRepository.allSnaps.value!!

            if (snaps.isNotEmpty()) {

                val senders = mutableMapOf<String, Int>()

                for (snap in snaps) {
                    if (senders.containsKey(snap.sender))
                        senders[snap.sender] = senders[snap.sender]!!.plus(1)
                    else
                        senders[snap.sender] = 1
                }
                val sendersText = senders.keys.joinToString(", ") { key ->
                    return@joinToString if (senders[key] == 1)
                        key
                    else
                        key.plus(" (${senders[key]})")
                }

                val builder = NotificationCompat.Builder(context, "messages")
                    .setSmallIcon(R.drawable.ic_snapchat_icon)
                    .setContentTitle(
                        if (snaps.size == 1) {
                            "You have 1 new snap"
                        } else {
                            "You have ${snaps.size} new snaps"
                        }
                    )
                    .setStyle(NotificationCompat.BigTextStyle().bigText("from ".plus(sendersText)))
                    .setContentText("last received from ${snaps[0].sender}")
                    .setNumber(senders.size)
                    .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                    .setWhen(snaps[0].sent)
                    .setAutoCancel(true)
                    .setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            0,
                            context.packageManager.getLaunchIntentForPackage(Constants.SNAPCHAT_PACKAGE_NAME),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .setChannelId("messages")
                    .setGroup(Constants.SNAP_NOTIFICATION_ID)
                    .setGroupSummary(true)

                notificationManager.notify(Constants.SNAP_NOTIFICATION_ID, 1, builder.build())
            }
        }.start()
    }

    fun clear(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(Constants.SNAP_NOTIFICATION_ID, 1)
    }
}