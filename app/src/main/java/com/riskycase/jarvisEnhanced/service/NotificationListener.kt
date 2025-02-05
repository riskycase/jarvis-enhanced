package com.riskycase.jarvisEnhanced.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.riskycase.jarvisEnhanced.R
import com.riskycase.jarvisEnhanced.datastore.settingsDataStore
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.models.Snap
import com.riskycase.jarvisEnhanced.repository.FilterRepository
import com.riskycase.jarvisEnhanced.repository.SnapRepository
import com.riskycase.jarvisEnhanced.util.Constants
import com.riskycase.jarvisEnhanced.util.NotificationListenerConnector
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
    private var componentName: ComponentName? = null

    @Inject
    lateinit var filterRepository: FilterRepository

    @Inject
    lateinit var snapRepository: SnapRepository

    @Inject
    lateinit var notificationMaker: NotificationMaker

    @Inject
    lateinit var notificationListenerConnector: NotificationListenerConnector

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

    private fun getSender(sbn: StatusBarNotification, filters: List<Filter>): String? {
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

    private fun toggleNotificationListenerService(componentName: ComponentName) {
        val pm = packageManager
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun goForeground() {
        startForeground(
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

        Thread {
            val filters = filterRepository.getAllFilters()
            activeNotifications.filter { it.packageName == Constants.SNAPCHAT_PACKAGE_NAME || it.packageName == "net.dinglisch.android.taskerm" }
                .map {
                    val sender = getSender(it, filters)
                    if (sender.isNullOrBlank()) return@map null
                    cancelNotification(it.key)
                    return@map Snap(it.key.plus("|").plus(it.postTime), sender, it.postTime)
                }.filterNotNull().forEach {
                    try {
                        snapRepository.add(it)
                    } catch (_: SQLiteConstraintException) {
                    }
                }
            notificationMaker.makeNotification()
        }.start()

    }

    fun getMediaNotificationByPackageName(packageName: String?): Bitmap? {
        return activeNotifications.filter { it.notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION) }
            .firstOrNull { it.packageName == packageName }?.notification?.let {
                it.getLargeIcon() ?: it.smallIcon
            }?.loadDrawable(applicationContext)?.toBitmap()
    }

    override fun onBind(intent: Intent?): IBinder? {
        notificationMaker.makeNotification()
        filterRepository.allFiltersLive.observeForever { filters -> this.filters = filters }
        notificationListenerConnector.notificationListener  = this
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
            val sender = getSender(sbn, filters)
            if (!sender.isNullOrBlank()) {
                val snap = Snap(sbn.key.plus("|").plus(sbn.postTime), sender, sbn.postTime)
                Thread {
                    try {
                        snapRepository.add(snap)
                    } catch (e: SQLiteConstraintException) {
                        // Nothing to do here, Snapchat is being a bitch
                    }
                    super.cancelNotification(sbn.key)
                    notificationMaker.makeNotification()
                }.start()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        readPendingSnaps()
        if (componentName == null) {
            componentName = ComponentName(this, this::class.java)
        }

        componentName?.let {
            requestRebind(it)
            toggleNotificationListenerService(it)
        }
        return START_REDELIVER_INTENT
    }
}