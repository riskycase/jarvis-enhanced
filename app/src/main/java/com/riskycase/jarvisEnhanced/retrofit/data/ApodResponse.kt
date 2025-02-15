package com.riskycase.jarvisEnhanced.retrofit.data

import com.google.gson.annotations.SerializedName

data class ApodResponse(val url: String, @SerializedName("hdurl") val hdUrl: String?)
