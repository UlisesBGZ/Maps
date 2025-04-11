package com.example.maps

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    var permissionGranted by remember { mutableStateOf(false) }

    // Asegurarse de que la aceleración de hardware esté habilitada
    val window = (context as? Activity)?.window
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        window?.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }

    // Verificar los permisos de ubicación
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            permissionGranted = true
        }
    }

    // Esperar la respuesta de los permisos
    if (permissionGranted) {
        AndroidView(
            factory = {
                Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

                val mapView = MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK) // Establecer la fuente de tiles
                    setMultiTouchControls(true) // Habilitar controles multitáctiles
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                coroutineScope.launch {
                    // Obtener la ubicación actual
                    val location = LocationUtils.getCurrentLocation(context)
                    if (location != null) {
                        // Obtener la ruta hacia el hogar (aquí debes implementar la función 'getRouteToHome')
                        val route = getRouteToHome(location.latitude, location.longitude)

                        withContext(Dispatchers.Main) {
                            // Convertir la ruta en puntos GeoPoint y agregar al mapa
                            routePoints = route.map { GeoPoint(it[1], it[0]) }

                            val polyline = Polyline().apply {
                                setPoints(routePoints)
                                color = android.graphics.Color.BLUE
                                width = 10f
                            }

                            // Agregar la polilínea al mapa
                            mapView.overlays.add(polyline)

                            // Configurar el mapa (centrado y zoom)
                            mapView.controller.setZoom(16.0)
                            mapView.controller.setCenter(GeoPoint(location.latitude, location.longitude))

                            // Agregar el overlay de ubicación
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
    } else {
        // Si no tienes permisos, puedes mostrar un mensaje o una pantalla alternativa.
    }
}
