package com.riskycase.jarvisEnhanced

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.riskycase.jarvisEnhanced.receiver.BatteryEventsBroadcastReceiver
import com.riskycase.jarvisEnhanced.util.SystemServicesContainer
import com.riskycase.jarvisEnhanced.util.wallpaper.MediaUtils
import com.riskycase.jarvisEnhanced.util.wallpaper.NasaApodFetchWorker
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class JarvisEnhancedApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var batteryEventsBroadcastReceiver: BatteryEventsBroadcastReceiver

    @Inject
    lateinit var mediaUtils: MediaUtils

    @Inject
    lateinit var systemServicesContainer: SystemServicesContainer

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HiltWorkerFactoryEntryPoint {
        fun workerFactory(): HiltWorkerFactory
    }

    override fun getWorkManagerConfiguration() = Configuration.Builder().setWorkerFactory(
        EntryPoints.get(this, HiltWorkerFactoryEntryPoint::class.java).workerFactory()
    ).build()

    override fun onCreate() {
        super.onCreate()

        val batteryEventsIntentFiler = IntentFilter()
        batteryEventsIntentFiler.addAction(Intent.ACTION_BATTERY_CHANGED)
        batteryEventsIntentFiler.addAction(BatteryManager.ACTION_CHARGING)
        batteryEventsIntentFiler.addAction(BatteryManager.ACTION_DISCHARGING)

        ContextCompat.registerReceiver(
            applicationContext,
            batteryEventsBroadcastReceiver,
            batteryEventsIntentFiler,
            ContextCompat.RECEIVER_EXPORTED
        )

        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                systemServicesContainer.batteryEventsBroadcastReceiver?.sendBatteryInfoUpdate()
                mainHandler.postDelayed(this, 2500)
            }
        })

        mediaUtils.sendFullUpdate()

        val constraints = Constraints.Builder().setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED).build()

        val nasaApodFetchRequest =
            PeriodicWorkRequestBuilder<NasaApodFetchWorker>(8, TimeUnit.HOURS).setConstraints(
                constraints
            ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "nasaApodFetchWork",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            nasaApodFetchRequest
        )
    }
}