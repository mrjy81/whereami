package com.example.whereami

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NeshanApi {
    @GET("v5/reverse")
    fun getAddress(
        @Header("Api-Key") apiKey: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Call<NeshanResponse>
}