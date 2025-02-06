package com.riskycase.jarvisEnhanced.util

import android.icu.util.Calendar

fun getHandRotations(calendar: Calendar, value: Int): Float {
    return when (value) {
        Calendar.HOUR -> ((calendar.get(Calendar.HOUR)
            .toFloat() / 12f) + (calendar.get(Calendar.MINUTE).toFloat() / 720f) + (calendar.get(
            Calendar.SECOND
        ).toFloat() / 43200f) + calendar.get(Calendar.MILLISECOND).toFloat() / 43200000f) * 360f

        Calendar.MINUTE -> ((calendar.get(Calendar.MINUTE)
            .toFloat() / 60f) + (calendar.get(Calendar.SECOND).toFloat() / 3600f) + calendar.get(
            Calendar.MILLISECOND
        ).toFloat() / 3600000f) * 360f

        Calendar.SECOND -> ((calendar.get(Calendar.SECOND)
            .toFloat() / 60f) + calendar.get(Calendar.MILLISECOND).toFloat() / 60000f) * 360f

        else -> 0f
    }
}