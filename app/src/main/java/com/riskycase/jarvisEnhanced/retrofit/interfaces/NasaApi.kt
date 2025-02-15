package com.riskycase.jarvisEnhanced.retrofit.interfaces

import com.riskycase.jarvisEnhanced.retrofit.data.ApodResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface NasaApi {
    @GET("planetary/apod")
    suspend fun getApod(@Query("api_key") apiKey: String): ApodResponse

    @GET
    suspend fun downloadImage(@Url url: String): ResponseBody
}