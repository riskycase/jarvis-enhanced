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
    const val WALLPAPER_PREVIEW = "wallpaperPreview"
}

object SocketIOConstants {
    const val DEVICE_ID = "deviceId"
    const val DEVICE_SECRET = "secret"
    const val AUTH_TOKEN = "authToken"
    class AuthTokenBody {
        lateinit var authToken: String
    }
    const val COMMAND = "command"
    object CommandCategories {
        const val MUSIC = "music"
    }
}

object SocketChannels {
    const val BATTERY_DETAILS = "batteryDetails"
    const val DELTA_BATTERY_DETAILS = "delta.batteryDetails"
    object BatteryDetails {
        const val BATTERY_LEVEL = "level"
        const val BATTERY_STATUS = "status"
        const val BATTERY_TEMPERATURE = "temperature"
        const val BATTERY_HEALTH = "health"
        const val BATTERY_POWER_SOURCE = "powerSource"
        const val BATTERY_CURRENT = "current"
        const val BATTERY_VOLTAGE = "voltage"
    }
    const val MUSIC_DETAILS = "musicDetails"
    object MusicDetails {
        const val MUSIC_PLAYING_STATE = "playingState"
        const val MUSIC_TITLE = "title"
        const val MUSIC_ALBUM = "album"
        const val MUSIC_ARTIST = "artist"
        const val MUSIC_DURATION = "duration"
        const val MUSIC_POSITION = "position"
        const val MUSIC_UPDATED_AT = "updatedAt"
        const val MUSIC_PLAYER_NAME = "playerName"
    }
    object MusicCommands {
        const val PREVIOUS = "prev"
        const val PAUSE = "pause"
        const val PLAY = "play"
        const val NEXT = "next"
        const val SEEK_TO = "seekTo"
    }
    const val MUSIC_ALBUM_ART_DETAILS = "delta.albumArtDetails"
}