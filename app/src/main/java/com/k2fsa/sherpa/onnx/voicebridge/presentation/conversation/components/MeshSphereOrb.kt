package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import kotlin.math.cos
import kotlin.math.sin

// State colors for the mesh
private val ColorIdle = Color(0xFF555577)        // Dim silver
private val ColorInitializing = Color(0xFF5C6BC0) // Indigo
private val ColorListening = Color(0xFF00FF41)    // Matrix green
private val ColorTranscribing = Color(0xFF40C4FF) // Cyan-blue
private val ColorSending = Color(0xFFFFAB40)      // Amber
private val ColorSpeaking = Color(0xFF7C4DFF)     // Purple
private val ColorEntertaining = Color(0xFFE040FB) // Magenta

/**
 * A flowing 3D particle-mesh sphere inspired by the reference design.
 *
 * Dense dots arranged on a sphere, displaced by layered sine waves
 * to create an organic, flowing blob effect. Dots glow based on depth.
 * Very thin lines connect neighbors for the wireframe feel.
 * The whole thing rotates and breathes.
 */
@Composable
fun MeshSphereOrb(
    pipelineState: PipelineState,
    audioAmplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
) {
    // Dense mesh: 20 lat x 30 lon = 600+ particles
    val latSegs = 20
    val lonSegs = 30

    // Pre-compute base sphere points (unit sphere)
    val mesh = remember(latSegs, lonSegs) {
        val verts = mutableListOf<FloatArray>() // [x, y, z] unit sphere
        val edgesH = mutableListOf<IntArray>()  // horizontal edges
        val edgesV = mutableListOf<IntArray>()  // vertical edges
        val cols = lonSegs + 1

        for (lat in 0..latSegs) {
            val theta = Math.PI.toFloat() * lat / latSegs
            val sinT = sin(theta)
            val cosT = cos(theta)
            for (lon in 0..lonSegs) {
                val phi = 2f * Math.PI.toFloat() * lon / lonSegs
                verts.add(floatArrayOf(sinT * cos(phi), cosT, sinT * sin(phi)))
            }
        }
        for (lat in 0..latSegs) {
            for (lon in 0..lonSegs) {
                val i = lat * cols + lon
                if (lon < lonSegs) edgesH.add(intArrayOf(i, i + 1))
                if (lat < latSegs) edgesV.add(intArrayOf(i, i + cols))
            }
        }
        Triple(verts, edgesH, edgesV)
    }

    val (baseVerts, edgesH, edgesV) = mesh
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")

    // Time drives the flowing wave displacement
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_000_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "time",
    )

    // Y rotation
    val rotY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (pipelineState) {
                    PipelineState.IDLE -> 24000
                    PipelineState.INITIALIZING -> 8000
                    PipelineState.LISTENING, PipelineState.TRANSCRIBING -> 10000
                    PipelineState.SENDING -> 14000
                    PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 6000
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotY",
    )

    // Gentle X tilt for 3D depth
    val rotX by infiniteTransition.animateFloat(
        initialValue = -0.25f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rotX",
    )

    // Glow pulse
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (pipelineState) {
                    PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 500
                    PipelineState.LISTENING -> 1000
                    else -> 2500
                },
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    // Displacement intensity (animated, audio-reactive)
    val displacement by animateFloatAsState(
        targetValue = when (pipelineState) {
            PipelineState.IDLE -> 0.02f
            PipelineState.INITIALIZING -> 0.06f
            PipelineState.LISTENING, PipelineState.TRANSCRIBING ->
                0.10f + audioAmplitude * 0.30f
            PipelineState.SENDING -> 0.08f
            PipelineState.SPEAKING, PipelineState.ENTERTAINING ->
                0.12f + audioAmplitude * 0.25f
        },
        animationSpec = spring(dampingRatio = 0.5f),
        label = "disp",
    )

    // Animated state color
    val meshColor by animateColorAsState(
        targetValue = when (pipelineState) {
            PipelineState.IDLE -> ColorIdle
            PipelineState.INITIALIZING -> ColorInitializing
            PipelineState.LISTENING -> ColorListening
            PipelineState.TRANSCRIBING -> ColorTranscribing
            PipelineState.SENDING -> ColorSending
            PipelineState.SPEAKING -> ColorSpeaking
            PipelineState.ENTERTAINING -> ColorEntertaining
        },
        animationSpec = tween(600),
        label = "meshColor",
    )

    // Overall brightness
    val brightness = when (pipelineState) {
        PipelineState.IDLE -> 0.40f
        PipelineState.INITIALIZING -> 0.50f
        PipelineState.LISTENING, PipelineState.TRANSCRIBING -> 0.70f
        PipelineState.SENDING -> 0.55f
        PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 0.85f
    }

    val glowIntensity = when (pipelineState) {
        PipelineState.IDLE -> 0.06f
        PipelineState.LISTENING, PipelineState.TRANSCRIBING -> 0.14f
        PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 0.22f
        else -> 0.08f
    } * glowPulse

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val radius = this.size.minDimension * 0.36f
        val fov = 3.2f

        // Glow layers (soft radial behind the sphere)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    meshColor.copy(alpha = glowIntensity * 0.6f),
                    meshColor.copy(alpha = glowIntensity * 0.2f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = radius * 2.0f,
            ),
            radius = radius * 2.0f,
            center = Offset(cx, cy),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    meshColor.copy(alpha = glowIntensity),
                    meshColor.copy(alpha = glowIntensity * 0.3f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = radius * 1.3f,
            ),
            radius = radius * 1.3f,
            center = Offset(cx, cy),
        )

        // Transform all vertices: displace -> rotate -> project
        val t = time * 0.06f
        val projected = FloatArray(baseVerts.size * 3) // x, y, depth per vert
        for (i in baseVerts.indices) {
            val bv = baseVerts[i]
            val bx = bv[0]; val by = bv[1]; val bz = bv[2]

            // Layered sine-wave displacement (the "flowing" effect)
            val wave1 = sin(bx * 3f + t * 0.7f) * cos(by * 2.5f + t * 0.5f) * 0.5f
            val wave2 = sin(by * 4f + t * 0.9f + bz * 2f) * 0.3f
            val wave3 = cos(bz * 3.5f + t * 0.6f + bx * 1.5f) * 0.2f
            val d = 1f + (wave1 + wave2 + wave3) * displacement

            val dx = bx * d
            val dy = by * d
            val dz = bz * d

            // Rotate Y
            val cosY = cos(rotY); val sinY = sin(rotY)
            val rx = dx * cosY + dz * sinY
            val ry = dy
            val rz = -dx * sinY + dz * cosY

            // Rotate X
            val cosX = cos(rotX); val sinX = sin(rotX)
            val fx = rx
            val fy = ry * cosX - rz * sinX
            val fz = ry * sinX + rz * cosX

            // Perspective project
            val zz = fz + fov
            val scale = fov / zz.coerceAtLeast(0.2f)
            projected[i * 3] = fx * scale
            projected[i * 3 + 1] = fy * scale
            projected[i * 3 + 2] = fz  // depth
        }

        // Draw wireframe lines (very thin, subtle)
        fun drawEdges(edges: List<IntArray>, alphaMultiplier: Float) {
            for (e in edges) {
                val ai = e[0] * 3; val bi = e[1] * 3
                val ax = projected[ai]; val ay = projected[ai + 1]; val az = projected[ai + 2]
                val bx = projected[bi]; val by = projected[bi + 1]; val bz = projected[bi + 2]

                // Skip seam wrapping artifacts
                val ddx = ax - bx; val ddy = ay - by
                if (ddx * ddx + ddy * ddy > 0.5f) return@drawEdges

                val avgDepth = (az + bz) / 2f
                val depthFade = ((avgDepth + 1f) / 2f).coerceIn(0f, 1f)
                val alpha = brightness * alphaMultiplier * (0.05f + depthFade * 0.95f)
                if (alpha < 0.01f) return@drawEdges

                drawLine(
                    color = meshColor.copy(alpha = alpha),
                    start = Offset(cx + ax * radius, cy + ay * radius),
                    end = Offset(cx + bx * radius, cy + by * radius),
                    strokeWidth = 0.6f,
                )
            }
        }
        drawEdges(edgesH, 0.18f)
        drawEdges(edgesV, 0.12f)

        // Draw particles (dots) — the main visual
        for (i in baseVerts.indices) {
            val px = projected[i * 3]
            val py = projected[i * 3 + 1]
            val pz = projected[i * 3 + 2]

            val depthFade = ((pz + 1f) / 2f).coerceIn(0f, 1f) // 0=back, 1=front
            val dotAlpha = brightness * (0.10f + depthFade * 0.90f)
            val dotRadius = 1.0f + depthFade * 2.2f // bigger in front

            // Main dot
            drawCircle(
                color = meshColor.copy(alpha = dotAlpha),
                radius = dotRadius,
                center = Offset(cx + px * radius, cy + py * radius),
            )

            // Soft glow halo on front-facing particles
            if (depthFade > 0.5f) {
                drawCircle(
                    color = meshColor.copy(alpha = dotAlpha * 0.15f),
                    radius = dotRadius * 3f,
                    center = Offset(cx + px * radius, cy + py * radius),
                )
            }
        }
    }
}
