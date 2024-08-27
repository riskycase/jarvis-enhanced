package com.riskycase.jarvisEnhanced.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Snap(
    @PrimaryKey val key: String,
    val sender: String,
    val sent: Long)
