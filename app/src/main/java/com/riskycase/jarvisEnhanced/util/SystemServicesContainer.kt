package com.riskycase.jarvisEnhanced.util

import com.riskycase.jarvisEnhanced.receiver.BatteryEventsBroadcastReceiver
import com.riskycase.jarvisEnhanced.service.NotificationListener
import com.riskycase.jarvisEnhanced.wallpaper.WallpaperService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemServicesContainer @Inject constructor() {
    var notificationListener: NotificationListener? = null
    var wallpaperEngine: WallpaperService.WallpaperEngine? = null
    var batteryEventsBroadcastReceiver: BatteryEventsBroadcastReceiver? = null
}