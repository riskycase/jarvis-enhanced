package com.riskycase.jarvisEnhanced.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.riskycase.jarvisEnhanced.R
import com.riskycase.jarvisEnhanced.database.AppDatabase
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.models.Snap
import com.riskycase.jarvisEnhanced.util.Constants
import com.riskycase.jarvisEnhanced.util.NotificationMaker

class NotificationListener : NotificationListenerService() {

    private lateinit var appDatabase: AppDatabase
    var running: Boolean = false
    lateinit var filters: List<Filter>

    fun setup(context: Context) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "monitor",
            "Monitoring notification",
            NotificationManager.IMPORTANCE_NONE
        )
        channel.description = "Channel for persistent notification to keep monitoring alive"
        channel.group = "system"
        val notificationGroup = NotificationChannelGroup("system", "Channels")
        notificationManager.createNotificationChannelGroup(notificationGroup)
        notificationManager.createNotificationChannel(channel)
    }

    fun makeNotification(context: Context) {

        setup(context)
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, "monitor")
            .setSmallIcon(R.drawable.ic_snapchat_icon)
            .setContentTitle("Monitoring for new messages")
            .setAutoCancel(false)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    context.packageManager.getLaunchIntentForPackage(packageName),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setChannelId("monitor")
            .setOngoing(true)
            .setGroup(Constants.MONITOR_NOTIFICATION_ID)
            .setGroupSummary(true)

        notificationManager.notify(Constants.MONITOR_NOTIFICATION_ID, 1, builder.build())
    }

    fun clear(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(Constants.MONITOR_NOTIFICATION_ID, 1)
    }

    override fun onBind(intent: Intent?): IBinder? {
        makeNotification(applicationContext)
        NotificationMaker().makeNotification(applicationContext)
        running = true
        appDatabase = AppDatabase.getDatabase(applicationContext)
        appDatabase.filterDao().getAllLive().observeForever{ filters -> this.filters = filters }
        return super.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        clear(applicationContext)
        running = false
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        appDatabase = AppDatabase.getDatabase(applicationContext)
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                val events =
                    (applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager).queryEvents(
                        System.currentTimeMillis() - 1000,
                        System.currentTimeMillis()
                    )
                while (events.hasNextEvent()) {
                    val event = UsageEvents.Event()
                    events.getNextEvent(event)
                    if (event.packageName == Constants.SNAPCHAT_PACKAGE_NAME && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        NotificationMaker().clear(applicationContext)
                        Thread {
                            appDatabase.snapDao().clear()
                        }.start()
                    }
                }
                mainHandler.postDelayed(this, 500)
            }
        })

        super.onCreate()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == Constants.SNAPCHAT_PACKAGE_NAME || sbn.packageName == "net.dinglisch.android.taskerm") {
            var sender: String? = null
            val snapchatFilters =
                filters.filter { filter -> filter.packageName == Constants.SNAPCHAT_PACKAGE_NAME }
            val notificationText =
                sbn.notification.extras.getString(Notification.EXTRA_TEXT, "default value")
            val notificationTitle =
                sbn.notification.extras.getString(Notification.EXTRA_TITLE, "default value")
            val matchedFilter = snapchatFilters.find { filter ->
                notificationText.matches(
                    Regex(filter.text.replace("$", "(.+)"))
                ) && notificationTitle.matches(
                    Regex(filter.title.replace("$", "(.+)"))
                )
            }
            if (matchedFilter != null) {
                if (matchedFilter.text.contains("$"))
                    sender = Regex(matchedFilter.text.replace("$", "(.+)")).find(notificationText)?.value
                else if (matchedFilter.title.contains("$"))
                    sender = Regex(matchedFilter.title.replace("$", "(.+)")).find(notificationTitle)?.value
            }
            if (!sender.isNullOrBlank()) {
                val snap = Snap(sbn.key.plus("|").plus(sbn.postTime), sender, sbn.postTime)
                Thread {
                    appDatabase.snapDao().add(snap)
                    super.cancelNotification(sbn.key)
                    NotificationMaker().makeNotification(applicationContext)
                }.start()
            }
        }
    }
}