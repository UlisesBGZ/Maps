package com.example.maps

suspend fun getRouteToHome(currentLat: Double, currentLon: Double): List<List<Double>> {
    val request = RouteRequest(
        coordinates = listOf(
            listOf(currentLon, currentLat),
            listOf(Constants.HOME_LON, Constants.HOME_LAT)
        )
    )
    val response = RetrofitClient.service.getRoute(request)
    return response.features.first().geometry.coordinates
}
