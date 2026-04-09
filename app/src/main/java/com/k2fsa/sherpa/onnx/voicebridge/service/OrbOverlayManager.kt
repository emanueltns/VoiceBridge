package com.k2fsa.sherpa.onnx.voicebridge.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.k2fsa.sherpa.onnx.voicebridge.MainActivity
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.MeshSphereOrb
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.VoiceBridgeTheme
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private const val TAG = "OrbOverlay"
private const val ORB_SIZE_DP = 100

@Singleton
class OrbOverlayManager @Inject constructor(
    private val context: Context,
) {
    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var isShowing = false

    private var pipelineStateFlow: StateFlow<PipelineState>? = null
    private var audioAmplitudeFlow: StateFlow<Float>? = null

    fun bindFlows(pipelineState: StateFlow<PipelineState>, audioAmplitude: StateFlow<Float>) {
        pipelineStateFlow = pipelineState
        audioAmplitudeFlow = audioAmplitude
    }

    fun canShowOverlay(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun show() {
        if (isShowing || !canShowOverlay()) return
        val psFlow = pipelineStateFlow ?: return
        val ampFlow = audioAmplitudeFlow ?: return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        val lifecycleOwner = OverlayLifecycleOwner()

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                val pipelineState by psFlow.collectAsState()
                val amplitude by ampFlow.collectAsState()

                VoiceBridgeTheme {
                    MeshSphereOrb(
                        pipelineState = pipelineState,
                        audioAmplitude = amplitude,
                        size = ORB_SIZE_DP.dp,
                        modifier = Modifier.size(ORB_SIZE_DP.dp),
                    )
                }
            }
        }

        // Drag + tap handling
        var startX = 0
        var startY = 0
        var startTouchX = 0f
        var startTouchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startTouchX
                    val dy = event.rawY - startTouchY
                    if (abs(dx) > 5 || abs(dy) > 5) moved = true
                    params.x = startX + dx.toInt()
                    params.y = startY + dy.toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        // Tap → bring app to foreground
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(intent)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(view, params)
            overlayView = view
            isShowing = true
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            Log.i(TAG, "Overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay: ${e.message}")
        }
    }

    fun hide() {
        if (!isShowing) return
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        overlayView = null
        isShowing = false
        Log.i(TAG, "Overlay hidden")
    }

    fun isVisible(): Boolean = isShowing
}

/**
 * Minimal LifecycleOwner + SavedStateRegistryOwner for the overlay ComposeView.
 */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
