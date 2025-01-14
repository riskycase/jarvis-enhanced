package com.riskycase.jarvisEnhanced.dagger

import android.content.Context
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

}