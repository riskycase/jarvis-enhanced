package com.riskycase.jarvisEnhanced.dagger

import android.app.KeyguardManager
import android.content.Context
import android.media.session.MediaSessionManager
import android.os.BatteryManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class Provider {

    @Provides
    fun providesBatterManager(@ApplicationContext context: Context): BatteryManager {
        return (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
    }

    @Provides
    fun providesKeyguardManager(@ApplicationContext context: Context): KeyguardManager {
        return (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
    }

    @Provides
    fun providesMediaSessionManager(@ApplicationContext context: Context): MediaSessionManager {
        return (context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager)
    }

}