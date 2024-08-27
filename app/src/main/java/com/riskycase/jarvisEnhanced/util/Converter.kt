package com.riskycase.jarvisEnhanced.util

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import com.riskycase.jarvisEnhanced.models.Snap
import com.riskycase.jarvisEnhanced.ui.components.SnapListItemComponent

class Converter(val context: Context) {

    @Composable
    fun SnapToSnapItemComponent(snap: Snap) {
        SnapListItemComponent(
            snap.sender,
            DateUtils
                .getRelativeDateTimeString(
                    context,
                    snap.sent,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.DAY_IN_MILLIS,
                    0
                )
                .toString()
        )
    }
}