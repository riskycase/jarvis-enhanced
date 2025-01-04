package com.riskycase.jarvisEnhanced.util

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.riskycase.jarvisEnhanced.datastore.settingsDataStore
import com.riskycase.jarvisEnhanced.retrofit.interfaces.NasaApi
import com.riskycase.jarvisEnhanced.service.WallpaperService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class NasaApodFetchWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val nasaApi = Retrofit.Builder()
                .baseUrl("https://api.nasa.gov")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NasaApi::class.java)
            val imageData = nasaApi.getApod(applicationContext.settingsDataStore.data.map {
                if (it.hasNasaApodApiKey()) it.nasaApodApiKey else "DEMO_KEY"
            }.first())
            applicationContext.openFileOutput("wallpaper", Context.MODE_PRIVATE).use {
                it.write(nasaApi.downloadImage(imageData.hdUrl ?: imageData.url).bytes())
            }
            applicationContext.startService(
                Intent(
                    applicationContext,
                    WallpaperService::class.java
                )
            )
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}