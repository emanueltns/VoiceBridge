package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components

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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Dense wireframe mesh sphere with organic blob displacement,
 * purple palette, and rim-light glow — matching the Maia reference.
 */
@Composable
fun MeshSphereOrb(
    pipelineState: PipelineState,
    audioAmplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
) {
    val latSegs = 28
    val lonSegs = 40
    val cols = lonSegs + 1

    // Pre-compute unit sphere vertices and edge indices
    val mesh = remember(latSegs, lonSegs) {
        val verts = FloatArray((latSegs + 1) * cols * 3)
        val edgesH = mutableListOf<Int>() // pairs: i0, i1
        val edgesV = mutableListOf<Int>()

        for (lat in 0..latSegs) {
            val theta = PI.toFloat() * lat / latSegs
            val sinT = sin(theta); val cosT = cos(theta)
            for (lon in 0..lonSegs) {
                val phi = 2f * PI.toFloat() * lon / lonSegs
                val idx = (lat * cols + lon) * 3
                verts[idx] = sinT * cos(phi)
                verts[idx + 1] = cosT
                verts[idx + 2] = sinT * sin(phi)
            }
        }
        for (lat in 0..latSegs) {
            for (lon in 0..lonSegs) {
                val i = lat * cols + lon
                if (lon < lonSegs) { edgesH.add(i); edgesH.add(i + 1) }
                if (lat < latSegs) { edgesV.add(i); edgesV.add(i + cols) }
            }
        }
        Triple(verts, edgesH.toIntArray(), edgesV.toIntArray())
    }

    val (baseVerts, edgesH, edgesV) = mesh
    val vertCount = (latSegs + 1) * cols
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    // Time
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100_000_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "time",
    )

    // Y rotation
    val rotY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (pipelineState) {
                    PipelineState.IDLE -> 25000
                    PipelineState.INITIALIZING -> 12000
                    PipelineState.LISTENING, PipelineState.TRANSCRIBING -> 14000
                    PipelineState.SENDING -> 16000
                    PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 8000
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotY",
    )

    // X tilt
    val rotX by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
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
                    PipelineState.LISTENING -> 900
                    else -> 2200
                },
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    // Displacement amplitude
    val displacement by animateFloatAsState(
        targetValue = when (pipelineState) {
            PipelineState.IDLE -> 0.05f
            PipelineState.INITIALIZING -> 0.10f
            PipelineState.LISTENING -> 0.16f + audioAmplitude * 0.40f
            PipelineState.TRANSCRIBING -> 0.18f + audioAmplitude * 0.25f
            PipelineState.SENDING -> 0.12f
            PipelineState.SPEAKING -> 0.20f + audioAmplitude * 0.35f
            PipelineState.ENTERTAINING -> 0.25f + audioAmplitude * 0.30f
        },
        animationSpec = spring(dampingRatio = 0.45f),
        label = "disp",
    )

    // Flow speed
    val flowSpeed by animateFloatAsState(
        targetValue = when (pipelineState) {
            PipelineState.IDLE -> 0.3f
            PipelineState.INITIALIZING -> 0.6f
            PipelineState.LISTENING -> 0.8f
            PipelineState.TRANSCRIBING -> 1.0f
            PipelineState.SENDING -> 1.1f
            PipelineState.SPEAKING -> 1.0f + audioAmplitude * 0.4f
            PipelineState.ENTERTAINING -> 1.4f
        },
        animationSpec = spring(dampingRatio = 0.6f),
        label = "speed",
    )

    // State-dependent color — each state gets a distinct hue
    val meshHue by animateFloatAsState(
        targetValue = when (pipelineState) {
            PipelineState.IDLE -> 140f         // muted green
            PipelineState.INITIALIZING -> 130f // green (loading)
            PipelineState.LISTENING -> 130f    // matrix green
            PipelineState.TRANSCRIBING -> 185f // cyan (hearing)
            PipelineState.SENDING -> 35f       // amber (thinking)
            PipelineState.SPEAKING -> 260f     // purple (talking)
            PipelineState.ENTERTAINING -> 300f // magenta (fun fact)
        },
        animationSpec = tween(600),
        label = "hue",
    )

    val meshSaturation = when (pipelineState) {
        PipelineState.IDLE -> 0.30f
        PipelineState.INITIALIZING -> 0.75f
        PipelineState.SENDING -> 0.85f
        PipelineState.ENTERTAINING -> 0.80f
        else -> 0.75f
    }

    val overallBrightness = when (pipelineState) {
        PipelineState.IDLE -> 0.45f
        PipelineState.INITIALIZING -> 0.85f
        PipelineState.LISTENING, PipelineState.TRANSCRIBING -> 0.90f
        PipelineState.SENDING -> 0.75f
        PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 1.0f
    }

    val glowIntensity = when (pipelineState) {
        PipelineState.IDLE -> 0.08f
        PipelineState.INITIALIZING -> 0.22f
        PipelineState.LISTENING, PipelineState.TRANSCRIBING -> 0.20f
        PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 0.30f
        else -> 0.12f
    } * glowPulse

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val radius = this.size.minDimension * 0.37f
        val fov = 3.5f

        val cosRY = cos(rotY); val sinRY = sin(rotY)
        val cosRX = cos(rotX); val sinRX = sin(rotX)

        // Glow color from current hue
        val glowColor = Color.hsl(meshHue, meshSaturation.coerceAtLeast(0.4f), 0.45f)

        // ── Background glow layers ──
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = glowIntensity * 0.35f),
                    glowColor.copy(alpha = glowIntensity * 0.08f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = radius * 2.8f,
            ),
            radius = radius * 2.8f,
            center = Offset(cx, cy),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = glowIntensity * 0.7f),
                    glowColor.copy(alpha = glowIntensity * 0.15f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = radius * 1.5f,
            ),
            radius = radius * 1.5f,
            center = Offset(cx, cy),
        )
        // Inner core glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = glowIntensity * 0.15f),
                    glowColor.copy(alpha = glowIntensity * 0.25f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = radius * 0.6f,
            ),
            radius = radius * 0.6f,
            center = Offset(cx, cy),
        )

        // ── Transform all vertices ──
        val t = time * flowSpeed * 0.06f
        // projected: x, y, z (depth), rim factor per vertex
        val proj = FloatArray(vertCount * 4)

        for (i in 0 until vertCount) {
            val bi = i * 3
            val bx = baseVerts[bi]; val by = baseVerts[bi + 1]; val bz = baseVerts[bi + 2]

            // Multi-frequency sine displacement for organic blob
            val w1 = sin(bx * 3f + t * 0.7f) * cos(by * 2.5f + t * 0.5f)
            val w2 = sin(by * 4f + t * 0.85f + bz * 2f) * 0.6f
            val w3 = cos(bz * 3.5f + t * 0.55f + bx * 1.5f) * 0.4f
            val w4 = sin((bx + by) * 5f + t * 1.1f) * 0.3f
            val w5 = cos((bz - bx) * 6f + t * 0.75f) * 0.2f
            val d = 1f + (w1 + w2 + w3 + w4 + w5) * displacement

            val dx = bx * d; val dy = by * d; val dz = bz * d

            // Rotate Y
            val rx = dx * cosRY + dz * sinRY
            val ry = dy
            val rz = -dx * sinRY + dz * cosRY

            // Rotate X
            val fx = rx
            val fy = ry * cosRX - rz * sinRX
            val fz = ry * sinRX + rz * cosRX

            // Perspective
            val zz = fz + fov
            val scale = fov / zz.coerceAtLeast(0.3f)

            val pi = i * 4
            proj[pi] = fx * scale       // screen x (normalized)
            proj[pi + 1] = fy * scale   // screen y
            proj[pi + 2] = fz           // depth

            // Rim factor: how much this point is on the silhouette edge
            // On a sphere, normal ≈ position. View dir = (0,0,1).
            // Rim = 1 when normal perpendicular to view (edge), 0 when facing camera
            val len = sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(0.001f)
            val nz = fz / len
            proj[pi + 3] = 1f - abs(nz) // rim: 0=facing, 1=edge
        }

        // ── Draw wireframe edges ──
        fun drawEdgeSet(edges: IntArray, baseAlpha: Float) {
            var e = 0
            while (e < edges.size) {
                val ai = edges[e] * 4; val bi = edges[e + 1] * 4
                e += 2

                val ax = proj[ai]; val ay = proj[ai + 1]; val az = proj[ai + 2]; val aRim = proj[ai + 3]
                val bx = proj[bi]; val by = proj[bi + 1]; val bz = proj[bi + 2]; val bRim = proj[bi + 3]

                // Skip wrap-around seam artifacts
                val ddx = ax - bx; val ddy = ay - by
                if (ddx * ddx + ddy * ddy > 0.25f) continue

                val avgDepth = ((az + bz) / 2f + 1.5f) / 3f
                val depthFade = avgDepth.coerceIn(0f, 1f)
                val avgRim = (aRim + bRim) / 2f

                // Rim-light: edges on the silhouette are brightest
                val rimBoost = 0.5f + avgRim * 0.5f
                val alpha = baseAlpha * (0.20f + depthFade * 0.80f) * rimBoost * overallBrightness
                if (alpha < 0.008f) continue

                // Hue shifts slightly along the surface
                val hueShift = (avgRim * 15f - 5f)
                val lineColor = Color.hsl(
                    hue = (meshHue + hueShift).coerceIn(0f, 360f),
                    saturation = (meshSaturation + avgRim * 0.2f).coerceIn(0f, 1f),
                    lightness = (0.4f + avgRim * 0.35f + depthFade * 0.15f).coerceIn(0.1f, 0.9f),
                )

                drawLine(
                    color = lineColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                    start = Offset(cx + ax * radius, cy + ay * radius),
                    end = Offset(cx + bx * radius, cy + by * radius),
                    strokeWidth = 0.5f + avgRim * 0.6f, // thicker on rim
                )
            }
        }

        drawEdgeSet(edgesH, 0.50f)
        drawEdgeSet(edgesV, 0.38f)

        // ── Draw vertex dots ──
        for (i in 0 until vertCount) {
            val pi = i * 4
            val px = proj[pi]; val py = proj[pi + 1]; val pz = proj[pi + 2]; val rim = proj[pi + 3]

            val depthFade = ((pz + 1.5f) / 3f).coerceIn(0f, 1f)
            val rimBoost = 0.5f + rim * 0.5f

            val dotAlpha = (0.20f + depthFade * 0.80f) * rimBoost * overallBrightness
            if (dotAlpha < 0.01f) continue

            val hueShift = rim * 15f - 5f
            val dotColor = Color.hsl(
                hue = (meshHue + hueShift).coerceIn(0f, 360f),
                saturation = (meshSaturation + rim * 0.25f).coerceIn(0f, 1f),
                lightness = (0.45f + rim * 0.35f + depthFade * 0.1f).coerceIn(0.1f, 0.95f),
            )

            val dotR = 0.6f + depthFade * 1.2f + rim * 1.0f
            val screenX = cx + px * radius
            val screenY = cy + py * radius

            // Main dot
            drawCircle(
                color = dotColor.copy(alpha = dotAlpha.coerceIn(0f, 1f)),
                radius = dotR,
                center = Offset(screenX, screenY),
            )

            // Glow halo on rim-facing and front dots
            if (rim > 0.5f && depthFade > 0.3f) {
                val haloStrength = (rim - 0.5f) * 2f * depthFade
                drawCircle(
                    color = dotColor.copy(alpha = (haloStrength * 0.12f * overallBrightness).coerceIn(0f, 1f)),
                    radius = dotR * 5f,
                    center = Offset(screenX, screenY),
                )
            }

            // Bright white sparkle on the strongest rim points
            if (rim > 0.75f && depthFade > 0.5f) {
                val sparkle = (rim - 0.75f) * 4f * depthFade
                drawCircle(
                    color = Color.White.copy(alpha = (sparkle * 0.20f * overallBrightness).coerceIn(0f, 1f)),
                    radius = dotR * 0.6f,
                    center = Offset(screenX, screenY),
                )
            }
        }
    }
}
