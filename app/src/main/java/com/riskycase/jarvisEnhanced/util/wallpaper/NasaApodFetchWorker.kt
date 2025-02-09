package com.riskycase.jarvisEnhanced.util.wallpaper

import android.content.Context
import android.graphics.BitmapFactory
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.riskycase.jarvisEnhanced.datastore.settingsDataStore
import com.riskycase.jarvisEnhanced.retrofit.interfaces.NasaApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

@HiltWorker
class NasaApodFetchWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backgroundImageUtils: BackgroundImageUtils
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val nasaApi = Retrofit.Builder().baseUrl("https://api.nasa.gov")
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(NasaApi::class.java)
            val imageData = nasaApi.getApod(applicationContext.settingsDataStore.data.map {
                if (it.hasNasaApodApiKey()) it.nasaApodApiKey else "DEMO_KEY"
            }.first())
            applicationContext.openFileOutput("wallpaper", Context.MODE_PRIVATE).use {
                it.write(nasaApi.downloadImage(imageData.hdUrl ?: imageData.url).bytes())
            }
            applicationContext.openFileInput("wallpaper").use {
                backgroundImageUtils.setBackgroundImage(BitmapFactory.decodeStream(it))
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}