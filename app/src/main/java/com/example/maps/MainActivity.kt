package com.example.maps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.maps.ui.theme.MapsTheme
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private var routeOverlay: Polyline? = null

    companion object {
        const val LOCATION_REQUEST_CODE = 100
        const val API_KEY = "5b3ce3597851110001cf624833b2664445a64c48856c720518b3b802"
    }

    private val locationState = mutableStateOf<Location?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkAndRequestLocationEnabled()
        checkLocationPermission()

        setContent {
            MapsTheme {
                val context = LocalContext.current
                var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LocationMap(locationState.value) // Map in the background

                        // Controls on top of the map
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            // Search Bar
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Buscar dirección") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(onClick = { centerLocation() }) {
                                    Text("Centrar ubicación")
                                }
                                Button(onClick = { clearRoute() }) {
                                    Text("Limpiar ruta")
                                }
                            }
                        }

                        // Another Row for search button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            Button(onClick = {
                                locationState.value?.let { location ->
                                    searchAndRoute(searchQuery.text, location.latitude, location.longitude, context)
                                }
                            }) {
                                Text("Buscar")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
        }
    }

    private fun checkAndRequestLocationEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            Toast.makeText(this, "Por favor activa la ubicación", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Permiso denegado para obtener ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setWaitForAccurateLocation(true)
                .build()

            fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    locationState.value = locationResult.lastLocation
                }
            }, mainLooper)
        }
    }

    private fun centerLocation() {
        locationState.value?.let {
            mapView.controller.setZoom(16.0)
            mapView.controller.setCenter(GeoPoint(it.latitude, it.longitude))
        }
    }

    private fun clearRoute() {
        routeOverlay?.let {
            mapView.overlays.remove(it)
            routeOverlay = null
            mapView.invalidate()
        }
    }

    private fun searchAndRoute(address: String, lat: Double, lon: Double, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val encodedAddress = java.net.URLEncoder.encode(address, "UTF-8")
                val geoUrl = "https://nominatim.openstreetmap.org/search?q=$encodedAddress&format=json&limit=1"
                val geoResponse = URL(geoUrl).readText()
                val geoJson = JSONObject("{\"results\": $geoResponse}")
                val results = geoJson.getJSONArray("results")
                if (results.length() > 0) {
                    val destObj = results.getJSONObject(0)
                    val destLat = destObj.getDouble("lat")
                    val destLon = destObj.getDouble("lon")
                    drawRoute(lat, lon, destLat, destLon, context)
                } else {
                    showToast("Dirección no encontrada", context)
                }
            } catch (e: Exception) {
                showToast("Error al buscar dirección", context)
            }
        }
    }

    private fun drawRoute(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.openrouteservice.org/v2/directions/driving-car?api_key=$API_KEY&start=$fromLon,$fromLat&end=$toLon,$toLat")
                val connection = url.openConnection() as HttpsURLConnection
                val data = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(data)
                val coordinates = json.getJSONArray("features")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates")

                val geoPoints = mutableListOf<GeoPoint>()
                for (i in 0 until coordinates.length()) {
                    val point = coordinates.getJSONArray(i)
                    geoPoints.add(GeoPoint(point.getDouble(1), point.getDouble(0)))
                }

                routeOverlay = Polyline().apply {
                    setPoints(geoPoints)
                    width = 8f
                    color = android.graphics.Color.BLUE
                }

                runOnUiThread {
                    mapView.overlays.add(routeOverlay)
                    mapView.invalidate()
                }

            } catch (e: Exception) {
                showToast("Error al trazar la ruta", context)
            }
        }
    }

    private fun showToast(message: String, context: Context) {
        runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    fun LocationMap(location: Location?) {
        AndroidView(factory = { context ->
            mapView = MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                myLocationOverlay = MyLocationNewOverlay(this).apply {
                    enableMyLocation()
                }
                overlays.add(myLocationOverlay)
            }

            location?.let {
                val controller: IMapController = mapView.controller
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(it.latitude, it.longitude))
            }

            mapView
        }, update = { view ->
            location?.let {
                val controller = view.controller
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(it.latitude, it.longitude))
            }
        })
    }
}
