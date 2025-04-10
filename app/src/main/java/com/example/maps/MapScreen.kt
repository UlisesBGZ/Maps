package com.example.maps

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    AndroidView(
        factory = {
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

            val mapView = MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            coroutineScope.launch {
                val location = LocationUtils.getCurrentLocation(context)
                if (location != null) {
                    val route = getRouteToHome(location.latitude, location.longitude)

                    withContext(Dispatchers.Main) {
                        routePoints = route.map { GeoPoint(it[1], it[0]) }

                        val polyline = Polyline().apply {
                            setPoints(routePoints)
                            color = android.graphics.Color.BLUE
                            width = 10f
                        }

                        mapView.overlays.add(polyline)
                        mapView.controller.setZoom(16.0)
                        mapView.controller.setCenter(GeoPoint(location.latitude, location.longitude))

                        val locationOverlay = MyLocationNewOverlay(mapView)
                        locationOverlay.enableMyLocation()
                        mapView.overlays.add(locationOverlay)
                    }
                }
            }

            mapView
        },
        modifier = modifier
    )
}
