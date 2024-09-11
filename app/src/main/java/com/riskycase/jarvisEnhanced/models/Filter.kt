package com.riskycase.jarvisEnhanced.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Filter(
    @PrimaryKey(autoGenerate = true) var id: Int,
    var title: String,
    var text: String,
    var packageName: String
)
