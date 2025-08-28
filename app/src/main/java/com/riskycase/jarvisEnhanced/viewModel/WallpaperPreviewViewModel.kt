package com.riskycase.jarvisEnhanced.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.service.wallpaper.WallpaperService as SystemWallpaperService
import com.riskycase.jarvisEnhanced.wallpaper.WallpaperService as MyWallpaperService

@HiltViewModel
class WallpaperPreviewViewModel @Inject constructor(
    private val wallpaperService: MyWallpaperService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var engine: SystemWallpaperService.Engine? = null

    fun cleanup() {
        engine?.onDestroy()
        engine = null
    }

    fun getWallpaperEngine(): SystemWallpaperService.Engine {
        return (engine ?: wallpaperService.onCreateEngine()).also {
            engine = it
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }

}