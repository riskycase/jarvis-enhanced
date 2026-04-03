package com.riskycase.jarvisEnhanced.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MusicListenEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val album: String,
    val playerPackage: String,
    val playerName: String,
    val startTime: Long,
    val durationMs: Long
)
