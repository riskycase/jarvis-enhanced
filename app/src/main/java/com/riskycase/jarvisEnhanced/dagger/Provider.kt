package com.riskycase.jarvisEnhanced.dagger

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.media.session.MediaSessionManager
import android.os.BatteryManager
import com.riskycase.jarvisEnhanced.service.NotificationListener
import com.riskycase.jarvisEnhanced.util.Animator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class Provider {

    @Provides
    @Singleton
    fun providesBatteryManager(@ApplicationContext context: Context): BatteryManager {
        return (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
    }

    @Provides
    @Singleton
    fun providesKeyguardManager(@ApplicationContext context: Context): KeyguardManager {
        return (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
    }

    @Provides
    @Singleton
    fun providesMediaSessionManager(@ApplicationContext context: Context): MediaSessionManager {
        return (context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager)
    }

    @Provides
    @Named("EvenDateFormat")
    @Singleton
    fun providesEvenDateFormat(): SimpleDateFormat {
        return SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
    }

    @Provides
    @Named("OddDateFormat")
    @Singleton
    fun providesOddDateFormat(): SimpleDateFormat {
        return SimpleDateFormat("HH mm ss", Locale.ENGLISH)
    }

    @Provides
    @Named("NotificationListenerServiceComponentName")
    @Singleton
    fun providesNotificationListenerServiceComponentName(@ApplicationContext context: Context): ComponentName {
        return ComponentName(context, NotificationListener::class.java)
    }

    @Provides
    @Named("7SegmentAllOnString")
    @Singleton
    fun providesAllOnString(): String {
        return "88:88:88"
    }

}