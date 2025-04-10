package com.example.maps

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class RouteRequest(val coordinates: List<List<Double>>)
data class RouteResponse(val features: List<Feature>)
data class Feature(val geometry: Geometry)
data class Geometry(val coordinates: List<List<Double>>)

interface RouteService {
    @Headers(
        "Authorization: ${Constants.API_KEY}",
        "Content-Type: application/json"
    )
    @POST("v2/directions/driving-car/geojson")
    suspend fun getRoute(@Body request: RouteRequest): RouteResponse
}

object RetrofitClient {
    val service: RouteService = Retrofit.Builder()
        .baseUrl("https://api.openrouteservice.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RouteService::class.java)
}
