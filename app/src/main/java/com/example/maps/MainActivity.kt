package com.example.maps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.maps.ui.theme.MapsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapsTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MapScreen()
                }
            }
        }
    }
}
