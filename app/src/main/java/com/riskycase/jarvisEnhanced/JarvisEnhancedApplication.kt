package com.riskycase.jarvisEnhanced

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.riskycase.jarvisEnhanced.util.NasaApodFetchWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class JarvisEnhancedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

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