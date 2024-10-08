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
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.riskycase.jarvisEnhanced.R
import com.riskycase.jarvisEnhanced.datastore.settingsDataStore
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.models.Snap
import com.riskycase.jarvisEnhanced.repository.FilterRepository
import com.riskycase.jarvisEnhanced.repository.SnapRepository
import com.riskycase.jarvisEnhanced.util.Constants
import com.riskycase.jarvisEnhanced.util.NotificationMaker
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListener @Inject constructor() : NotificationListenerService() {

    private lateinit var filters: List<Filter>

    @Inject
    lateinit var filterRepository: FilterRepository

    @Inject
    lateinit var snapRepository: SnapRepository

    @Inject
    lateinit var notificationMaker: NotificationMaker

    @Inject
    @ApplicationContext
    lateinit var context: Context

    fun setup() {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "monitor", "Monitoring notification", NotificationManager.IMPORTANCE_NONE
        )
        channel.description = "Channel for persistent notification to keep monitoring alive"
        channel.group = "system"
        val notificationGroup = NotificationChannelGroup("system", "Channels")
        notificationManager.createNotificationChannelGroup(notificationGroup)
        notificationManager.createNotificationChannel(channel)
    }

    private fun makeNotification(): Notification {
        setup()
        val builder =
            NotificationCompat.Builder(context, "monitor").setSmallIcon(R.drawable.ic_snapchat_icon)
                .setContentTitle("Monitoring for new messages").setAutoCancel(false)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        context.packageManager.getLaunchIntentForPackage(context.packageName),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).setChannelId("monitor").setOngoing(true)
                .setGroup(Constants.MONITOR_NOTIFICATION_ID).setGroupSummary(true)

        return builder.build()
    }

    private fun clear() {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            Constants.MONITOR_NOTIFICATION_ID, 1
        )
    }

    private fun getSender(sbn: StatusBarNotification): String? {
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
            if (matchedFilter.text.contains("$")) sender =
                Regex(matchedFilter.text.replace("$", "(.+)")).find(notificationText)?.value
            else if (matchedFilter.title.contains("$")) sender = Regex(
                matchedFilter.title.replace(
                    "$", "(.+)"
                )
            ).find(notificationTitle)?.value
        }
        return sender
    }

    fun goForeground() {
        ServiceCompat.startForeground(
            this,
            Constants.MONITOR_FOREGROUND_NOTIFICATION_ID,
            makeNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            } else {
                0
            }
        )

    }

    fun readPendingSnaps() {

        filterRepository.allFilters.observeForever { filters -> this.filters = filters }

        activeNotifications.filter { it.packageName == Constants.SNAPCHAT_PACKAGE_NAME || it.packageName == "net.dinglisch.android.taskerm" }
            .map {
                val sender = getSender(it)
                if (sender.isNullOrBlank()) return@map null
                return@map Snap(it.key.plus("|").plus(it.postTime), sender, it.postTime)
            }.filterNotNull().forEach(snapRepository::add)
        notificationMaker.makeNotification()
    }

    override fun onBind(intent: Intent?): IBinder? {
        notificationMaker.makeNotification()
        filterRepository.allFilters.observeForever { filters -> this.filters = filters }
        return super.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        clear()
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                val events =
                    (applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager).queryEvents(
                        System.currentTimeMillis() - 1000, System.currentTimeMillis()
                    )
                while (events.hasNextEvent()) {
                    val event = UsageEvents.Event()
                    events.getNextEvent(event)
                    if (event.packageName == Constants.SNAPCHAT_PACKAGE_NAME && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        notificationMaker.clear()
                        Thread {
                            snapRepository.clear()
                        }.start()
                    }
                }
                mainHandler.postDelayed(this, runBlocking {
                    applicationContext.settingsDataStore.data.map { settings -> if (settings.hasCheckDurationMilliseconds()) settings.checkDurationMilliseconds else 500 }
                        .first().toLong()
                })
            }
        })

        super.onCreate()

        goForeground()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == Constants.SNAPCHAT_PACKAGE_NAME || sbn.packageName == "net.dinglisch.android.taskerm") {
            val sender = getSender(sbn)
            if (!sender.isNullOrBlank()) {
                val snap = Snap(sbn.key.plus("|").plus(sbn.postTime), sender, sbn.postTime)
                Thread {
                    snapRepository.add(snap)
                    super.cancelNotification(sbn.key)
                    notificationMaker.makeNotification()
                }.start()
            }
        }
    }
}