package com.riskycase.jarvisEnhanced.util.wallpaper

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.riskycase.jarvisEnhanced.datastore.settingsDataStore
import com.riskycase.jarvisEnhanced.retrofit.interfaces.NasaApi
import com.riskycase.jarvisEnhanced.util.SystemServicesContainer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

@HiltWorker
class NasaApodFetchWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backgroundImageUtils: BackgroundImageUtils,
    private val systemServicesContainer: SystemServicesContainer
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val nasaApi = Retrofit.Builder().baseUrl("https://api.nasa.gov")
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(NasaApi::class.java)
            val imageData = nasaApi.getApod(applicationContext.settingsDataStore.data.map {
                if (it.hasNasaApodApiKey()) it.nasaApodApiKey else "DEMO_KEY"
            }.first())
            val filesDir = applicationContext.filesDir
            // Download to temp files first, then atomically rename
            val tmpWallpaper = File(filesDir, "wallpaper.tmp")
            val tmpSmall = File(filesDir, "wallpaper_small.tmp")
            tmpWallpaper.outputStream().use {
                it.write(nasaApi.downloadImage(imageData.hdUrl ?: imageData.url).bytes())
            }
            tmpSmall.outputStream().use {
                it.write(nasaApi.downloadImage(imageData.url).bytes())
            }
            // Atomic rename — old file is always either fully old or fully new
            tmpSmall.renameTo(File(filesDir, "wallpaper_small"))
            tmpWallpaper.renameTo(File(filesDir, "wallpaper"))
            backgroundImageUtils.loadFromDisk()
            systemServicesContainer.wallpaperEngine?.notifyColorsChanged()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}