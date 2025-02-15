package com.riskycase.jarvisEnhanced

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.riskycase.jarvisEnhanced.util.wallpaper.NasaApodFetchWorker
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class JarvisEnhancedApplication : Application(), Configuration.Provider {

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