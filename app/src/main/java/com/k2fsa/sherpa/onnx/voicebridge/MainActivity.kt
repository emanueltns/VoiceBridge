package com.k2fsa.sherpa.onnx.voicebridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.VoiceBridgeTheme
import com.k2fsa.sherpa.onnx.voicebridge.presentation.navigation.VoiceBridgeNavGraph
import com.k2fsa.sherpa.onnx.voicebridge.service.OrbOverlayManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var orbOverlay: OrbOverlayManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceBridgeTheme {
                VoiceBridgeNavGraph()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // App going to background — show floating orb if a conversation is active
        if (orbOverlay.canShowOverlay()) {
            orbOverlay.show()
        }
    }

    override fun onStart() {
        super.onStart()
        // App coming to foreground — hide the overlay
        orbOverlay.hide()
    }
}
