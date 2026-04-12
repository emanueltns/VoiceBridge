package freeapp.voicebridge.ai.presentation.conversation.components

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
import freeapp.voicebridge.ai.domain.model.PipelineState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val TWO_PI = (2.0 * PI).toFloat()

/**
 * Spectacular dual-shell wireframe orb with bloom glow, flowing color bands,
 * organic multi-harmonic displacement, and rim lighting.
 */
@Composable
fun MeshSphereOrb(
    pipelineState: PipelineState,
    audioAmplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
) {
    // Outer shell: dense mesh
    val oLatSegs = 30
    val oLonSegs = 44
    val oCols = oLonSegs + 1

    // Inner shell: sparser core
    val iLatSegs = 16
    val iLonSegs = 24
    val iCols = iLonSegs + 1

    data class MeshData(
        val verts: FloatArray,
        val edgesH: IntArray,
        val edgesV: IntArray,
        val vertCount: Int,
        val cols: Int,
    )

    fun buildMesh(latSegs: Int, lonSegs: Int, cols: Int): MeshData {
        val verts = FloatArray((latSegs + 1) * cols * 3)
        val eH = mutableListOf<Int>()
        val eV = mutableListOf<Int>()
        for (lat in 0..latSegs) {
            val theta = PI.toFloat() * lat / latSegs
            val sinT = sin(theta); val cosT = cos(theta)
            for (lon in 0..lonSegs) {
                val phi = TWO_PI * lon / lonSegs
                val idx = (lat * cols + lon) * 3
                verts[idx] = sinT * cos(phi)
                verts[idx + 1] = cosT
                verts[idx + 2] = sinT * sin(phi)
            }
        }
        for (lat in 0..latSegs) {
            for (lon in 0..lonSegs) {
                val i = lat * cols + lon
                if (lon < lonSegs) { eH.add(i); eH.add(i + 1) }
                if (lat < latSegs) { eV.add(i); eV.add(i + cols) }
            }
        }
        return MeshData(verts, eH.toIntArray(), eV.toIntArray(), (latSegs + 1) * cols, cols)
    }

    val outerMesh = remember { buildMesh(oLatSegs, oLonSegs, oCols) }
    val innerMesh = remember { buildMesh(iLatSegs, iLonSegs, iCols) }

    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100000f,
        animationSpec = infiniteRepeatable(tween(100_000_000, easing = LinearEasing), RepeatMode.Restart),
        label = "time",
    )

    val rotY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(
            tween(when (pipelineState) {
                PipelineState.IDLE -> 22000; PipelineState.INITIALIZING -> 10000
                PipelineState.LISTENING, PipelineState.TRANSCRIBING -> 13000
                PipelineState.SENDING -> 15000
                PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 7000
            }, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "rotY",
    )

    val rotX by infiniteTransition.animateFloat(
        initialValue = -0.35f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Reverse),
        label = "rotX",
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            tween(when (pipelineState) {
                PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 400
                PipelineState.LISTENING -> 800; else -> 2000
            }),
            RepeatMode.Reverse,
        ),
        label = "glow",
    )

    val displacement by animateFloatAsState(
        targetValue = when (pipelineState) {
            PipelineState.IDLE -> 0.05f; PipelineState.INITIALIZING -> 0.12f
            PipelineState.LISTENING -> 0.18f + audioAmplitude * 0.45f
            PipelineState.TRANSCRIBING -> 0.20f + audioAmplitude * 0.30f
            PipelineState.SENDING -> 0.14f
            PipelineState.SPEAKING -> 0.22f + audioAmplitude * 0.40f
            PipelineState.ENTERTAINING -> 0.28f + audioAmplitude * 0.35f
        },
        animationSpec = spring(dampingRatio = 0.4f), label = "disp",
    )

    val flowSpeed by animateFloatAsState(
        targetValue = when (pipelineState) {
            PipelineState.IDLE -> 0.3f; PipelineState.INITIALIZING -> 0.7f
            PipelineState.LISTENING -> 0.9f; PipelineState.TRANSCRIBING -> 1.1f
            PipelineState.SENDING -> 1.3f
            PipelineState.SPEAKING -> 1.1f + audioAmplitude * 0.5f
            PipelineState.ENTERTAINING -> 1.5f
        },
        animationSpec = spring(dampingRatio = 0.6f), label = "speed",
    )

    val meshHue by animateFloatAsState(
        targetValue = when (pipelineState) {
            PipelineState.IDLE -> 140f; PipelineState.INITIALIZING -> 130f
            PipelineState.LISTENING -> 130f; PipelineState.TRANSCRIBING -> 185f
            PipelineState.SENDING -> 35f; PipelineState.SPEAKING -> 260f
            PipelineState.ENTERTAINING -> 300f
        },
        animationSpec = tween(600), label = "hue",
    )

    val meshSat = when (pipelineState) {
        PipelineState.IDLE -> 0.30f; PipelineState.INITIALIZING -> 0.75f
        PipelineState.SENDING -> 0.85f; PipelineState.ENTERTAINING -> 0.80f
        else -> 0.75f
    }

    val brightness = when (pipelineState) {
        PipelineState.IDLE -> 0.45f; PipelineState.INITIALIZING -> 0.85f
        PipelineState.LISTENING, PipelineState.TRANSCRIBING -> 0.90f
        PipelineState.SENDING -> 0.75f
        PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 1.0f
    }

    val glowAlpha = when (pipelineState) {
        PipelineState.IDLE -> 0.08f; PipelineState.INITIALIZING -> 0.22f
        PipelineState.LISTENING, PipelineState.TRANSCRIBING -> 0.22f
        PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 0.32f
        else -> 0.14f
    } * glowPulse

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val canvasR = this.size.minDimension * 0.37f
        val fov = 3.5f
        val t = time * flowSpeed * 0.06f

        val cosRY = cos(rotY); val sinRY = sin(rotY)
        val cosRX = cos(rotX); val sinRX = sin(rotX)

        val glowColor = Color.hsl(meshHue, meshSat.coerceIn(0f, 1f), 0.45f)

        // ── Background glow (3 layers + core) ──
        for ((radiusMul, alphaMul) in listOf(3.0f to 0.20f, 2.0f to 0.35f, 1.4f to 0.65f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(glowColor.copy(alpha = glowAlpha * alphaMul), Color.Transparent),
                    center = Offset(cx, cy), radius = canvasR * radiusMul,
                ),
                radius = canvasR * radiusMul, center = Offset(cx, cy),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = glowAlpha * 0.20f), glowColor.copy(alpha = glowAlpha * 0.10f), Color.Transparent),
                center = Offset(cx, cy), radius = canvasR * 0.45f,
            ),
            radius = canvasR * 0.45f, center = Offset(cx, cy),
        )

        // ── Glass shell rim ring ──
        val rimAlpha = brightness * 0.35f
        // Outer glass ring — bright edge highlight
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.Transparent, Color.Transparent, Color.White.copy(alpha = rimAlpha * 0.6f), Color.White.copy(alpha = rimAlpha * 0.15f), Color.Transparent),
                center = Offset(cx, cy), radius = canvasR * 1.08f,
            ),
            radius = canvasR * 1.08f, center = Offset(cx, cy),
        )
        // Inner glass shell gradient (translucent surface)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.Transparent, glowColor.copy(alpha = brightness * 0.05f), Color.White.copy(alpha = brightness * 0.08f), Color.Transparent),
                center = Offset(cx, cy), radius = canvasR * 1.02f,
            ),
            radius = canvasR * 1.02f, center = Offset(cx, cy),
        )

        // ── Inner aurora light bands ──
        // Several curved bands of light rotating inside the sphere
        val auroraT = time * 0.04f
        val bandCount = 5
        val bandColors = listOf(
            Color(0xFF00FFCC), // cyan-green
            Color(0xFF66BBFF), // sky blue
            Color(0xFFCC66FF), // lavender
            Color(0xFF00FF88), // green
            Color(0xFFFF66CC), // pink
        )
        for (b in 0 until bandCount) {
            val bandPhase = b * TWO_PI / bandCount + auroraT * (0.3f + b * 0.1f)
            val bandTilt = PI.toFloat() * 0.3f + b * 0.25f
            val bandWidth = canvasR * (0.55f + b * 0.06f)
            val bandAlpha = brightness * 0.10f * glowPulse

            // Draw band as an elongated elliptical glow
            val bcx = cx + cos(bandPhase) * canvasR * 0.15f
            val bcy = cy + sin(bandPhase + bandTilt) * canvasR * 0.15f
            val bandColor = bandColors[b % bandColors.size]

            // Soft wide band
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(bandColor.copy(alpha = bandAlpha), bandColor.copy(alpha = bandAlpha * 0.3f), Color.Transparent),
                    center = Offset(bcx, bcy), radius = bandWidth,
                ),
                radius = bandWidth, center = Offset(bcx, bcy),
            )
        }

        // ── Specular reflection (bright highlights on glass surface) ──
        val specX = cx + cos(auroraT * 0.6f) * canvasR * 0.25f - canvasR * 0.15f
        val specY = cy - canvasR * 0.3f + sin(auroraT * 0.4f) * canvasR * 0.1f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = brightness * 0.25f), Color.White.copy(alpha = brightness * 0.05f), Color.Transparent),
                center = Offset(specX, specY), radius = canvasR * 0.25f,
            ),
            radius = canvasR * 0.25f, center = Offset(specX, specY),
        )
        // Secondary smaller spec
        val spec2X = cx + canvasR * 0.35f + cos(auroraT * 0.3f) * canvasR * 0.1f
        val spec2Y = cy + canvasR * 0.2f + sin(auroraT * 0.5f) * canvasR * 0.1f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = brightness * 0.12f), Color.Transparent),
                center = Offset(spec2X, spec2Y), radius = canvasR * 0.12f,
            ),
            radius = canvasR * 0.12f, center = Offset(spec2X, spec2Y),
        )

        // ── Transform helper ──
        fun displace(bx: Float, by: Float, bz: Float, shellR: Float, dispMul: Float): FloatArray {
            val w1 = sin(bx * 3f + t * 0.7f) * cos(by * 2.5f + t * 0.5f)
            val w2 = sin(by * 4f + t * 0.85f + bz * 2f) * 0.6f
            val w3 = cos(bz * 3.5f + t * 0.55f + bx * 1.5f) * 0.4f
            val w4 = sin((bx + by) * 5f + t * 1.1f) * 0.3f
            val w5 = cos((bz - bx) * 6f + t * 0.75f) * 0.2f
            val w6 = sin(bx * 8f + by * 4f - bz * 3f + t * 1.4f) * 0.12f
            val d = shellR * (1f + (w1 + w2 + w3 + w4 + w5 + w6) * displacement * dispMul)

            val dx = bx * d; val dy = by * d; val dz = bz * d
            val rx = dx * cosRY + dz * sinRY; val ry = dy; val rz = -dx * sinRY + dz * cosRY
            val fx = rx; val fy = ry * cosRX - rz * sinRX; val fz = ry * sinRX + rz * cosRX
            val zz = fz + fov; val sc = fov / zz.coerceAtLeast(0.3f)
            val len = sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(0.001f)
            return floatArrayOf(fx * sc, fy * sc, fz, 1f - abs(fz / len)) // x, y, depth, rim
        }

        fun projectMesh(mesh: MeshData, shellR: Float, dispMul: Float): FloatArray {
            val proj = FloatArray(mesh.vertCount * 4)
            for (i in 0 until mesh.vertCount) {
                val bi = i * 3
                val r = displace(mesh.verts[bi], mesh.verts[bi + 1], mesh.verts[bi + 2], shellR, dispMul)
                val pi = i * 4
                proj[pi] = r[0]; proj[pi + 1] = r[1]; proj[pi + 2] = r[2]; proj[pi + 3] = r[3]
            }
            return proj
        }

        fun colorForVertex(rim: Float, depth01: Float, surfaceFlow: Float): Color {
            val hueShift = rim * 12f - 4f + sin(surfaceFlow + t * 0.5f) * 8f
            return Color.hsl(
                hue = ((meshHue + hueShift) % 360f + 360f) % 360f,
                saturation = (meshSat + rim * 0.15f).coerceIn(0f, 1f),
                lightness = (0.35f + rim * 0.35f + depth01 * 0.15f).coerceIn(0.08f, 0.92f),
            )
        }

        // ── Draw edges with bloom (two passes: glow then sharp) ──
        fun drawEdges(edges: IntArray, proj: FloatArray, glowPass: Boolean, baseAlpha: Float) {
            var e = 0
            while (e < edges.size) {
                val ai = edges[e] * 4; val bi = edges[e + 1] * 4
                e += 2

                val ax = proj[ai]; val ay = proj[ai + 1]; val az = proj[ai + 2]; val aRim = proj[ai + 3]
                val bx = proj[bi]; val by = proj[bi + 1]; val bz = proj[bi + 2]; val bRim = proj[bi + 3]

                val ddx = ax - bx; val ddy = ay - by
                if (ddx * ddx + ddy * ddy > 0.25f) continue

                val avgDepth = (((az + bz) / 2f + 1.5f) / 3f).coerceIn(0f, 1f)
                val avgRim = (aRim + bRim) / 2f
                val rimBoost = 0.5f + avgRim * 0.5f

                val alpha: Float
                val strokeW: Float
                if (glowPass) {
                    // Wide, dim glow line
                    alpha = baseAlpha * 0.25f * avgDepth * rimBoost * brightness
                    strokeW = 2.5f + avgRim * 3f
                } else {
                    // Sharp bright line
                    alpha = baseAlpha * (0.25f + avgDepth * 0.75f) * rimBoost * brightness
                    strokeW = 0.5f + avgRim * 0.7f
                }
                if (alpha < 0.005f) continue

                val surfaceFlow = (ax + ay) * 3f
                val c = colorForVertex(avgRim, avgDepth, surfaceFlow)

                drawLine(
                    color = c.copy(alpha = alpha.coerceIn(0f, 1f)),
                    start = Offset(cx + ax * canvasR, cy + ay * canvasR),
                    end = Offset(cx + bx * canvasR, cy + by * canvasR),
                    strokeWidth = strokeW,
                )
            }
        }

        // ── INNER SHELL (dimmer, slower, smaller) ──
        val innerProj = projectMesh(innerMesh, 0.5f, 0.6f)

        // Inner edges — glow only, no sharp pass (ghostly core)
        drawEdges(innerMesh.edgesH, innerProj, true, 0.22f)
        drawEdges(innerMesh.edgesV, innerProj, true, 0.16f)

        // Inner dots
        for (i in 0 until innerMesh.vertCount) {
            val pi = i * 4
            val px = innerProj[pi]; val py = innerProj[pi + 1]; val pz = innerProj[pi + 2]; val rim = innerProj[pi + 3]
            val depth01 = ((pz + 1.5f) / 3f).coerceIn(0f, 1f)
            val alpha = 0.15f * depth01 * (0.5f + rim * 0.5f) * brightness
            if (alpha < 0.01f) continue
            val c = colorForVertex(rim, depth01, (px + py) * 3f)
            drawCircle(c.copy(alpha = alpha.coerceIn(0f, 1f)), 0.8f + depth01 * 1f, Offset(cx + px * canvasR, cy + py * canvasR))
        }

        // ── OUTER SHELL ──
        val outerProj = projectMesh(outerMesh, 1.0f, 1.0f)

        // Pass 1: glow (wide dim lines)
        drawEdges(outerMesh.edgesH, outerProj, true, 0.40f)
        drawEdges(outerMesh.edgesV, outerProj, true, 0.30f)
        // Pass 2: sharp (thin bright lines)
        drawEdges(outerMesh.edgesH, outerProj, false, 0.55f)
        drawEdges(outerMesh.edgesV, outerProj, false, 0.42f)

        // Outer dots with bloom halos and specular
        for (i in 0 until outerMesh.vertCount) {
            val pi = i * 4
            val px = outerProj[pi]; val py = outerProj[pi + 1]; val pz = outerProj[pi + 2]; val rim = outerProj[pi + 3]
            val depth01 = ((pz + 1.5f) / 3f).coerceIn(0f, 1f)
            val rimBoost = 0.5f + rim * 0.5f
            val dotAlpha = (0.25f + depth01 * 0.75f) * rimBoost * brightness
            if (dotAlpha < 0.01f) continue

            val surfaceFlow = (px + py) * 3f
            val c = colorForVertex(rim, depth01, surfaceFlow)
            val dotR = 0.7f + depth01 * 1.5f + rim * 1.2f
            val sx = cx + px * canvasR; val sy = cy + py * canvasR

            // Main dot
            drawCircle(c.copy(alpha = dotAlpha.coerceIn(0f, 1f)), dotR, Offset(sx, sy))

            // Bloom halo on rim + front particles
            if (rim > 0.4f && depth01 > 0.25f) {
                val bloom = (rim - 0.4f) / 0.6f * depth01
                drawCircle(c.copy(alpha = (bloom * 0.12f * brightness).coerceIn(0f, 1f)), dotR * 5f, Offset(sx, sy))
            }

            // Specular highlight — bright white on front-facing surfaces
            if (depth01 > 0.75f && rim < 0.4f) {
                val spec = (depth01 - 0.75f) * 4f * (1f - rim / 0.4f)
                drawCircle(Color.White.copy(alpha = (spec * 0.30f * brightness).coerceIn(0f, 1f)), dotR * 0.7f, Offset(sx, sy))
            }

            // Hot rim sparkle — white flash on strongest rim edges
            if (rim > 0.75f && depth01 > 0.4f) {
                val sparkle = (rim - 0.75f) * 4f * depth01
                drawCircle(Color.White.copy(alpha = (sparkle * 0.35f * brightness).coerceIn(0f, 1f)), dotR * 0.5f, Offset(sx, sy))
            }
        }
    }
}
