package com.boliby.screenwriter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.boliby.screenwriter.ui.PlaceholderScreen
import com.boliby.screenwriter.ui.theme.ScreenWriterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenWriterTheme {
                PlaceholderScreen()
            }
        }
    }
}
