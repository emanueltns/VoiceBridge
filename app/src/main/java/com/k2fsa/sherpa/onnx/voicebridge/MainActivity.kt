package com.k2fsa.sherpa.onnx.voicebridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.VoiceBridgeTheme
import com.k2fsa.sherpa.onnx.voicebridge.presentation.navigation.VoiceBridgeNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceBridgeTheme {
                VoiceBridgeNavGraph()
            }
        }
    }
}
