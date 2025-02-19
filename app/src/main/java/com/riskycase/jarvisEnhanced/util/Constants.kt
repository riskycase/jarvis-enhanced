package com.riskycase.jarvisEnhanced.util

class Constants {
    companion object {
        const val SNAPCHAT_PACKAGE_NAME: String = "com.snapchat.android"
        const val MONITOR_NOTIFICATION_ID: String = "monitor"
        const val SNAP_NOTIFICATION_ID: String = "snap"
        const val MONITOR_FOREGROUND_NOTIFICATION_ID: Int = 1
        const val databaseName: String = "appDatabase"
    }
}

object Destinations {
    const val HOME = "home"
    const val FILTERS = "filters"
    const val EDIT_FILTER = "editFilter"
    const val SETTINGS = "settings"
}

object SocketIOConstants {
    const val DEVICE_ID = "deviceId"
    const val DEVICE_SECRET = "secret"
    const val AUTH_TOKEN = "authToken"
    class AuthTokenBody {
        lateinit var authToken: String
    }
}