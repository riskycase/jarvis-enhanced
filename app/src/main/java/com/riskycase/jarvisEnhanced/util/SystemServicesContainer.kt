package com.riskycase.jarvisEnhanced.util

import com.riskycase.jarvisEnhanced.service.NotificationListener
import com.riskycase.jarvisEnhanced.service.WallpaperService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemServicesContainer @Inject constructor() {
    var notificationListener: NotificationListener? = null
    var wallpaperEngine: WallpaperService.MyWallpaperEngine? = null
}