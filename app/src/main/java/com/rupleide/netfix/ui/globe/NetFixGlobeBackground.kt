package com.rupleide.netfix.ui.globe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import android.graphics.Paint as NativePaint

enum class GlobeStatus { Off, Connecting, Connected, Error }

enum class GlobeQuality { Full, Lite, Static }

@Immutable
data class GeoPoint(val lat: Float, val lon: Float)

object GlobeDefaults {
    val Home = GeoPoint(55.8f, 43.4f)
    val Endpoints = listOf(
        GeoPoint(52.37f, 4.90f),
        GeoPoint(50.11f, 8.68f),
        GeoPoint(60.57f, 27.19f),
        GeoPoint(1.35f, 103.82f),
    )
    val EndpointPool = listOf(
        GeoPoint(52.37f, 4.90f),
        GeoPoint(50.11f, 8.68f),
        GeoPoint(60.57f, 27.19f),
        GeoPoint(1.35f, 103.82f),
        GeoPoint(51.51f, -0.13f),
        GeoPoint(48.86f, 2.35f),
        GeoPoint(59.33f, 18.07f),
        GeoPoint(52.23f, 21.01f),
        GeoPoint(41.01f, 28.98f),
        GeoPoint(25.20f, 55.27f),
        GeoPoint(43.24f, 76.95f),
        GeoPoint(35.68f, 139.69f),
        GeoPoint(37.57f, 126.98f),
        GeoPoint(22.32f, 114.17f),
        GeoPoint(19.08f, 72.88f),
        GeoPoint(40.71f, -74.01f),
        GeoPoint(37.77f, -122.42f),
        GeoPoint(-23.55f, -46.63f),
        GeoPoint(-33.87f, 151.21f),
        GeoPoint(30.04f, 31.24f),
        GeoPoint(-26.20f, 28.05f),
    )

    private val TimeZoneHomes = mapOf(
        "Europe/Kaliningrad" to GeoPoint(54.71f, 20.51f),
        "Europe/Moscow" to Home,
        "Europe/Simferopol" to GeoPoint(44.95f, 34.10f),
        "Europe/Kirov" to GeoPoint(58.60f, 49.66f),
        "Europe/Volgograd" to GeoPoint(48.71f, 44.51f),
        "Europe/Samara" to GeoPoint(53.20f, 50.15f),
        "Europe/Saratov" to GeoPoint(51.53f, 46.03f),
        "Europe/Astrakhan" to GeoPoint(46.35f, 48.04f),
        "Europe/Ulyanovsk" to GeoPoint(54.32f, 48.39f),
        "Asia/Yekaterinburg" to GeoPoint(56.84f, 60.61f),
        "Asia/Omsk" to GeoPoint(54.99f, 73.37f),
        "Asia/Novosibirsk" to GeoPoint(55.03f, 82.92f),
        "Asia/Barnaul" to GeoPoint(53.35f, 83.77f),
        "Asia/Tomsk" to GeoPoint(56.50f, 84.97f),
        "Asia/Novokuznetsk" to GeoPoint(53.76f, 87.12f),
        "Asia/Krasnoyarsk" to GeoPoint(56.01f, 92.85f),
        "Asia/Irkutsk" to GeoPoint(52.29f, 104.30f),
        "Asia/Chita" to GeoPoint(52.03f, 113.50f),
        "Asia/Yakutsk" to GeoPoint(62.03f, 129.73f),
        "Asia/Vladivostok" to GeoPoint(43.12f, 131.89f),
        "Asia/Magadan" to GeoPoint(59.56f, 150.80f),
        "Asia/Sakhalin" to GeoPoint(46.96f, 142.74f),
        "Asia/Kamchatka" to GeoPoint(53.04f, 158.65f),
        "Europe/Minsk" to GeoPoint(53.90f, 27.57f),
        "Europe/Kyiv" to GeoPoint(50.45f, 30.52f),
        "Europe/Kiev" to GeoPoint(50.45f, 30.52f),
        "Asia/Almaty" to GeoPoint(43.24f, 76.95f),
        "Asia/Tashkent" to GeoPoint(41.30f, 69.24f),
        "Asia/Bishkek" to GeoPoint(42.87f, 74.59f),
        "Asia/Tbilisi" to GeoPoint(41.72f, 44.79f),
        "Asia/Yerevan" to GeoPoint(40.18f, 44.51f),
        "Asia/Baku" to GeoPoint(40.41f, 49.87f),
    )

    fun homeForDevice(): GeoPoint {
        val zone = java.util.TimeZone.getDefault()
        TimeZoneHomes[zone.id]?.let { return it }
        val offsetHours = zone.rawOffset / 3_600_000f
        return GeoPoint(48f, (offsetHours * 15f).coerceIn(-170f, 170f))
    }

    fun randomEndpoints(home: GeoPoint = Home): List<GeoPoint> {
        val distant = EndpointPool.filter {
            kotlin.math.abs(it.lat - home.lat) > 4f || kotlin.math.abs(it.lon - home.lon) > 6f
        }
        return distant.shuffled().take((3..5).random())
    }
}

private object GlobeMotion {
    val Standard = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)
    val Decelerate = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    val Accelerate = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    const val DEBOUNCE_MS = 180L
    const val ARC_STAGGER_MS = 60L
    const val ARC_IN_MS = 450
    const val ARC_OUT_MS = 250
    const val STATE_MS = 350
    const val FLASH_IN_MS = 120
    const val FLASH_OUT_MS = 280
    const val MESH_IN_MS = 450
    const val FULL_FRAME_GAP_MS = 33L
    const val LITE_FRAME_GAP_MS = 50L
    const val MAX_FRAME_GAP_MS = 100L
    const val FRAME_GAP_STEP_MS = 17L
    const val LATE_THRESHOLD_MS = 20f
    const val CALM_THRESHOLD_MS = 6f
    const val CALM_TICKS_TO_RECOVER = 90
    const val WARMUP_TICKS = 90
}

private object GlobePalette {
    val DotOff = Color(0xFF5A5A60)
    val DotConnecting = Color(0xFF7A7A82)
    val DotConnected = Color(0xFF8E8E96)
    val DotError = Color(0xFF6A5F5F)
    val Accent = Color(0xFFA1A1AA)
    val AccentConnected = Color(0xFF22C55E)
    val AccentError = Color(0xFFEF4444)
    val Home = Color(0xFF22C55E)
    val Comet = Color(0xFFF4F4F5)
    val OceanLight = Color(0xFF1E1E1E)
    val OceanDark = Color(0xFF131313)
}

private const val TAU = 6.2831855f
private const val PI_F = 3.1415927f
private const val RAD_PER_DEG = PI_F / 180f
private const val SPIN_SPEED = TAU / 120f
private const val FOCUS_OFFSET = 0.22f
private const val SWAY_AMPLITUDE = 0.16f
private const val SWAY_PERIOD = 22f
private const val COMET_SPEED = 0.32f
private const val RIPPLE_PERIOD = 2.4f
private const val SCAN_PERIOD = 1.2f
private const val BUCKET_COUNT = 8
private val BucketAlpha = FloatArray(BUCKET_COUNT) { index ->
    val center = (index + 0.5f) / BUCKET_COUNT
    minOf(1f, 1.05f * Math.pow(center.toDouble(), 0.9).toFloat())
}
private val BucketWidthDp = FloatArray(BUCKET_COUNT) { index -> 1.2f + 1.2f * index / (BUCKET_COUNT - 1) }

@Composable
fun NetFixGlobeBackground(
    status: GlobeStatus,
    modifier: Modifier = Modifier,
    quality: GlobeQuality = rememberGlobeQuality(),
    home: GeoPoint = GlobeDefaults.Home,
    endpoints: List<GeoPoint> = GlobeDefaults.Endpoints,
    centerXFraction: Float = 0.5f,
    centerYFraction: Float = 0.462f,
    radiusFraction: Float = 0.78f,
    radiusOfHeight: Boolean = false,
    tiltDegrees: Float = 24f,
    ambientClock: MutableFloatState? = null,
    ambientPeriod: Float = 18f,
) {
    val shown = rememberDebouncedStatus(status)

    val mesh by produceState<GlobeMesh?>(null, quality, home, endpoints) {
        value = withContext(Dispatchers.Default) { GlobeMesh.build(quality, home, endpoints) }
    }

    val focusAngle = remember(home) { -home.lon * RAD_PER_DEG + FOCUS_OFFSET }
    val tilt = tiltDegrees * RAD_PER_DEG
    val clock = remember { GlobeClock(focusAngle - 0.9f) }

    val transition = updateTransition(shown, label = "globe")
    val stateSpec = remember { tween<Float>(GlobeMotion.STATE_MS, easing = GlobeMotion.Standard) }

    val dotColor by transition.animateColor(
        transitionSpec = { tween(GlobeMotion.STATE_MS, easing = GlobeMotion.Standard) },
        label = "dotColor",
    ) {
        when (it) {
            GlobeStatus.Off -> GlobePalette.DotOff
            GlobeStatus.Connecting -> GlobePalette.DotConnecting
            GlobeStatus.Connected -> GlobePalette.DotConnected
            GlobeStatus.Error -> GlobePalette.DotError
        }
    }
    val accent by transition.animateColor(
        transitionSpec = { tween(GlobeMotion.STATE_MS, easing = GlobeMotion.Standard) },
        label = "accent",
    ) {
        when (it) {
            GlobeStatus.Off -> GlobePalette.Accent
            GlobeStatus.Connecting -> GlobePalette.AccentConnected
            GlobeStatus.Connected -> GlobePalette.AccentConnected
            GlobeStatus.Error -> GlobePalette.AccentError
        }
    }
    val homeColor by transition.animateColor(
        transitionSpec = { tween(GlobeMotion.STATE_MS, easing = GlobeMotion.Standard) },
        label = "homeColor",
    ) {
        when (it) {
            GlobeStatus.Off -> GlobePalette.Accent
            GlobeStatus.Connecting -> GlobePalette.AccentConnected
            GlobeStatus.Connected -> GlobePalette.Home
            GlobeStatus.Error -> GlobePalette.AccentError
        }
    }
    val dotAlpha by transition.animateFloat({ stateSpec }, label = "dotAlpha") {
        when (it) {
            GlobeStatus.Off -> 0.5f
            GlobeStatus.Connecting -> 0.72f
            GlobeStatus.Connected -> 0.88f
            GlobeStatus.Error -> 0.55f
        }
    }
    val glow by transition.animateFloat({ stateSpec }, label = "glow") {
        when (it) {
            GlobeStatus.Off -> 0.3f
            GlobeStatus.Connecting -> 0.6f
            GlobeStatus.Connected -> 0.9f
            GlobeStatus.Error -> 0.45f
        }
    }
    val homeAlpha by transition.animateFloat({ stateSpec }, label = "homeAlpha") {
        if (it == GlobeStatus.Off) 0f else 1f
    }
    val scanAlpha by transition.animateFloat({ stateSpec }, label = "scan") {
        if (it == GlobeStatus.Connecting) 1f else 0f
    }
    val liveAlpha by transition.animateFloat({ stateSpec }, label = "live") {
        if (it == GlobeStatus.Connected) 1f else 0f
    }
    val focusState = transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow)
        },
        label = "focus",
    ) {
        if (it == GlobeStatus.Connecting || it == GlobeStatus.Connected) 1f else 0f
    }

    val arcProgress = remember(endpoints.size) { List(endpoints.size) { Animatable(0f) } }
    val flash = remember { Animatable(0f) }
    val meshAlpha = remember { Animatable(0f) }

    LaunchedEffect(shown) {
        val wantsFocus = shown == GlobeStatus.Connecting || shown == GlobeStatus.Connected
        if (wantsFocus && !clock.focusing) {
            clock.seekDelta = wrapPi(clock.freeAngle - focusAngle)
            clock.focusing = true
        } else if (!wantsFocus && clock.focusing) {
            clock.freeAngle = wrapTau(clock.displayAngle(focusAngle, focusState.value))
            clock.focusing = false
        }
    }

    LaunchedEffect(shown, arcProgress) {
        if (shown == GlobeStatus.Connected) {
            launch {
                flash.snapTo(0f)
                flash.animateTo(1f, tween(GlobeMotion.FLASH_IN_MS, easing = GlobeMotion.Decelerate))
                flash.animateTo(0f, tween(GlobeMotion.FLASH_OUT_MS, easing = GlobeMotion.Accelerate))
            }
            snapshotFlow { focusState.value }.first { it > 0.85f }
            arcProgress.forEachIndexed { index, progress ->
                launch {
                    delay(index * GlobeMotion.ARC_STAGGER_MS)
                    progress.animateTo(1f, tween(GlobeMotion.ARC_IN_MS, easing = GlobeMotion.Decelerate))
                }
            }
        } else {
            arcProgress.forEach { progress ->
                launch {
                    progress.animateTo(0f, tween(GlobeMotion.ARC_OUT_MS, easing = GlobeMotion.Accelerate))
                }
            }
        }
    }

    LaunchedEffect(mesh) {
        if (mesh != null) {
            meshAlpha.animateTo(1f, tween(GlobeMotion.MESH_IN_MS, easing = GlobeMotion.Decelerate))
        }
    }

    val uiActive = com.rupleide.netfix.ui.main.rememberIsUiActive() && !com.rupleide.netfix.data.isActionSheetVisibleGlobal
    LaunchedEffect(quality, uiActive) {
        if (quality == GlobeQuality.Static || !uiActive) return@LaunchedEffect
        val baseGap = if (quality == GlobeQuality.Lite) GlobeMotion.LITE_FRAME_GAP_MS else GlobeMotion.FULL_FRAME_GAP_MS
        var gap = baseGap
        var lateAverage = 0f
        var calmTicks = 0
        var warmupTicks = 0
        var last = -1L
        while (isActive) {
            withFrameNanos { now ->
                if (last >= 0L) {
                    val intervalMs = (now - last) / 1_000_000f
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                    clock.time += dt
                    if (!clock.focusing) {
                        clock.freeAngle = wrapTau(
                            clock.freeAngle + SPIN_SPEED * (1f - focusState.value) * dt,
                        )
                    }
                    ambientClock?.let { it.floatValue = (it.floatValue + dt) % ambientPeriod }
                    warmupTicks++
                    if (warmupTicks > GlobeMotion.WARMUP_TICKS) {
                        lateAverage = lateAverage * 0.9f + (intervalMs - gap).coerceIn(0f, 40f) * 0.1f
                    }
                }
                last = now
            }
            if (lateAverage > GlobeMotion.LATE_THRESHOLD_MS && gap < GlobeMotion.MAX_FRAME_GAP_MS) {
                gap = minOf(GlobeMotion.MAX_FRAME_GAP_MS, gap + GlobeMotion.FRAME_GAP_STEP_MS)
                lateAverage = 0f
                calmTicks = 0
            } else if (lateAverage < GlobeMotion.CALM_THRESHOLD_MS && gap > baseGap) {
                calmTicks++
                if (calmTicks >= GlobeMotion.CALM_TICKS_TO_RECOVER) {
                    gap = maxOf(baseGap, gap - GlobeMotion.FRAME_GAP_STEP_MS)
                    calmTicks = 0
                }
            } else {
                calmTicks = 0
            }
            delay((gap - 17L).coerceAtLeast(1L))
        }
    }

    Spacer(
        modifier
            .fillMaxSize()
            .graphicsLayer { }
            .drawWithCache {
                val radius = (if (radiusOfHeight) size.height else size.width) * radiusFraction
                val center = Offset(size.width * centerXFraction, size.height * centerYFraction)
                val currentMesh = mesh
                val frame = currentMesh?.let { GlobeFrame(it, antiAlias = quality != GlobeQuality.Lite) }
                val sizes = GlobeSizes(
                    bucketWidths = FloatArray(BUCKET_COUNT) { BucketWidthDp[it].dp.toPx() },
                    arcStroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                    arcGlowStroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                    ringStroke = Stroke(width = 1.5.dp.toPx()),
                    node = 2.5.dp.toPx(),
                    home = 4.dp.toPx(),
                    homeHalo = 9.dp.toPx(),
                    ripple = 22.dp.toPx(),
                    comet = 2.6.dp.toPx(),
                    cometGlow = 7.dp.toPx(),
                    cometStep = 0.5.dp.toPx(),
                )
                val full = quality == GlobeQuality.Full

                onDrawBehind {

                    if (currentMesh != null && frame != null) {
                        val fade = meshAlpha.value
                        val time = clock.time
                        val angle = clock.displayAngle(focusAngle, focusState.value)
                        frame.projector.setup(angle, tilt)

                        drawDots(currentMesh, frame, sizes, center, radius, dotColor, dotAlpha * fade)

                        drawArcs(
                            mesh = currentMesh,
                            frame = frame,
                            sizes = sizes,
                            center = center,
                            radius = radius,
                            progress = arcProgress,
                            color = accent,
                            liveAlpha = liveAlpha * fade,
                            time = time,
                            full = full,
                        )

                        drawHome(
                            mesh = currentMesh,
                            frame = frame,
                            sizes = sizes,
                            center = center,
                            radius = radius,
                            color = homeColor,
                            accent = accent,
                            alpha = homeAlpha * fade,
                            scanAlpha = scanAlpha,
                            liveAlpha = liveAlpha,
                            time = time,
                        )
                    }
                }
            },
    )
}

@Composable
fun rememberGlobeQuality(performanceMode: Boolean = false): GlobeQuality {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val powerSave = rememberPowerSaveMode()
    val isTv = remember(configuration) {
        (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    val animationsOff = remember(configuration) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    return when {
        animationsOff -> GlobeQuality.Static
        performanceMode || isTv || powerSave -> GlobeQuality.Lite
        else -> GlobeQuality.Full
    }
}

@Composable
private fun rememberPowerSaveMode(): Boolean {
    val context = LocalContext.current
    val powerManager = remember(context) { context.getSystemService(PowerManager::class.java) }
    var saving by remember { mutableStateOf(powerManager?.isPowerSaveMode == true) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                saving = powerManager?.isPowerSaveMode == true
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        saving = powerManager?.isPowerSaveMode == true
        onDispose { context.unregisterReceiver(receiver) }
    }
    return saving
}

@Composable
private fun rememberDebouncedStatus(status: GlobeStatus): GlobeStatus {
    var shown by remember { mutableStateOf(status) }
    LaunchedEffect(status) {
        if (status != shown) {
            delay(GlobeMotion.DEBOUNCE_MS)
            shown = status
        }
    }
    return shown
}

@Stable
private class GlobeClock(initialAngle: Float) {
    var time by mutableFloatStateOf(0f)
    var freeAngle by mutableFloatStateOf(initialAngle)
    var seekDelta by mutableFloatStateOf(0f)
    var focusing by mutableStateOf(false)

    fun displayAngle(focusAngle: Float, focus: Float): Float =
        if (focusing) {
            focusAngle + seekDelta * (1f - focus) + SWAY_AMPLITUDE * sin(time * TAU / SWAY_PERIOD) * focus
        } else {
            freeAngle
        }
}

private class GlobeSizes(
    val bucketWidths: FloatArray,
    val arcStroke: Stroke,
    val arcGlowStroke: Stroke,
    val ringStroke: Stroke,
    val node: Float,
    val home: Float,
    val homeHalo: Float,
    val ripple: Float,
    val comet: Float,
    val cometGlow: Float,
    val cometStep: Float,
)

private class GlobeProjector {
    private var cosA = 1f
    private var sinA = 0f
    private var cosT = 1f
    private var sinT = 0f
    var px = 0f
        private set
    var py = 0f
        private set
    var pz = 0f
        private set

    fun setup(angle: Float, tilt: Float) {
        cosA = cos(angle)
        sinA = sin(angle)
        cosT = cos(tilt)
        sinT = sin(tilt)
    }

    fun depth(y: Float, z: Float, x: Float): Float {
        val z1 = -x * sinA + z * cosA
        return y * sinT + z1 * cosT
    }

    fun project(x: Float, y: Float, z: Float) {
        val x1 = x * cosA + z * sinA
        val z1 = -x * sinA + z * cosA
        px = x1
        py = y * cosT - z1 * sinT
        pz = y * sinT + z1 * cosT
    }

    fun onFront(): Boolean = pz > 0f

    fun visibleAboveSurface(): Boolean = pz > 0f || px * px + py * py > 1f
}

private class GlobeFrame(mesh: GlobeMesh, antiAlias: Boolean) {
    val buckets = Array(BUCKET_COUNT) { FloatArray(mesh.dotCount * 2) }
    val counts = IntArray(BUCKET_COUNT)
    val projector = GlobeProjector()
    val path = Path()
    val paint = NativePaint().apply {
        isAntiAlias = antiAlias
        style = NativePaint.Style.STROKE
        strokeCap = if (antiAlias) NativePaint.Cap.ROUND else NativePaint.Cap.SQUARE
    }
}

private class GlobeMesh(
    val dots: FloatArray,
    val dotCount: Int,
    val home: FloatArray,
    val endpoints: FloatArray,
    val arcs: Array<FloatArray>,
    val arcSegments: Int,
) {
    companion object {
        fun build(quality: GlobeQuality, home: GeoPoint, endpoints: List<GeoPoint>): GlobeMesh {
            val land = landDots(quality)
            val buffer = land.first
            val count = land.second

            val homeVec = FloatArray(3).also { geoToUnit(home, it, 0) }
            val endVecs = FloatArray(endpoints.size * 3)
            endpoints.forEachIndexed { i, point -> geoToUnit(point, endVecs, i * 3) }

            val segments = if (quality == GlobeQuality.Full) 48 else 24
            val arcs = Array(endpoints.size) { k ->
                buildArc(homeVec, endVecs, k * 3, segments)
            }

            return GlobeMesh(buffer, count, homeVec, endVecs, arcs, segments)
        }

        private val landCache = HashMap<GlobeQuality, Pair<FloatArray, Int>>()

        private fun landDots(quality: GlobeQuality): Pair<FloatArray, Int> = synchronized(landCache) {
            landCache.getOrPut(quality) {
                val mask = GlobeLandMask.decode()
                val samples = if (quality == GlobeQuality.Full) 15_000 else 7_000
                val buffer = FloatArray(samples * 3)
                var count = 0
                val golden = (PI * (3.0 - sqrt(5.0))).toFloat()
                for (i in 0 until samples) {
                    val y = 1f - 2f * (i + 0.5f) / samples
                    val ring = sqrt(1f - y * y)
                    val phi = golden * i
                    val x = cos(phi) * ring
                    val z = sin(phi) * ring
                    val lat = asin(y) / RAD_PER_DEG
                    val lon = atan2(x, z) / RAD_PER_DEG
                    if (GlobeLandMask.isLand(mask, lat, lon)) {
                        buffer[count * 3] = x
                        buffer[count * 3 + 1] = y
                        buffer[count * 3 + 2] = z
                        count++
                    }
                }
                buffer.copyOf(count * 3) to count
            }
        }

        private fun geoToUnit(point: GeoPoint, out: FloatArray, offset: Int) {
            val lat = point.lat * RAD_PER_DEG
            val lon = point.lon * RAD_PER_DEG
            out[offset] = cos(lat) * sin(lon)
            out[offset + 1] = sin(lat)
            out[offset + 2] = cos(lat) * cos(lon)
        }

        private fun buildArc(a: FloatArray, ends: FloatArray, offset: Int, segments: Int): FloatArray {
            val bx = ends[offset]
            val by = ends[offset + 1]
            val bz = ends[offset + 2]
            val dot = (a[0] * bx + a[1] * by + a[2] * bz).coerceIn(-0.9999f, 0.9999f)
            val omega = acos(dot)
            val sinOmega = sin(omega)
            val lift = 0.06f + 0.22f * (omega / PI_F)
            val out = FloatArray((segments + 1) * 3)
            for (j in 0..segments) {
                val t = j.toFloat() / segments
                val wa = sin((1f - t) * omega) / sinOmega
                val wb = sin(t * omega) / sinOmega
                val h = 1f + lift * sin(PI_F * t)
                out[j * 3] = (a[0] * wa + bx * wb) * h
                out[j * 3 + 1] = (a[1] * wa + by * wb) * h
                out[j * 3 + 2] = (a[2] * wa + bz * wb) * h
            }
            return out
        }
    }
}

private fun DrawScope.drawDots(
    mesh: GlobeMesh,
    frame: GlobeFrame,
    sizes: GlobeSizes,
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    if (alpha <= 0.01f) return
    val p = frame.projector
    val counts = frame.counts
    counts.fill(0)
    val dots = mesh.dots
    for (i in 0 until mesh.dotCount) {
        val base = i * 3
        val z = p.depth(dots[base + 1], dots[base + 2], dots[base])
        if (z <= 0.04f) continue
        p.project(dots[base], dots[base + 1], dots[base + 2])
        val bucket = (z * BUCKET_COUNT).toInt().coerceIn(0, BUCKET_COUNT - 1)
        val c = counts[bucket]
        val buffer = frame.buckets[bucket]
        buffer[c] = center.x + p.px * radius
        buffer[c + 1] = center.y - p.py * radius
        counts[bucket] = c + 2
    }
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val paint = frame.paint
        for (bucket in 0 until BUCKET_COUNT) {
            val c = counts[bucket]
            if (c == 0) continue
            paint.color = color.copy(alpha = (alpha * BucketAlpha[bucket]).coerceIn(0f, 1f)).toArgb()
            paint.strokeWidth = sizes.bucketWidths[bucket]
            native.drawPoints(frame.buckets[bucket], 0, c, paint)
        }
    }
}

private fun projectArcAt(samples: FloatArray, segments: Int, fraction: Float, p: GlobeProjector) {
    val position = fraction.coerceIn(0f, 1f) * segments
    val index = floor(position).toInt().coerceAtMost(segments - 1)
    val t = position - index
    val a = index * 3
    val b = a + 3
    p.project(
        samples[a] + (samples[b] - samples[a]) * t,
        samples[a + 1] + (samples[b + 1] - samples[a + 1]) * t,
        samples[a + 2] + (samples[b + 2] - samples[a + 2]) * t,
    )
}

private fun DrawScope.drawArcs(
    mesh: GlobeMesh,
    frame: GlobeFrame,
    sizes: GlobeSizes,
    center: Offset,
    radius: Float,
    progress: List<Animatable<Float, *>>,
    color: Color,
    liveAlpha: Float,
    time: Float,
    full: Boolean,
) {
    val p = frame.projector
    val path = frame.path
    val segments = mesh.arcSegments
    for (k in mesh.arcs.indices) {
        val value = progress.getOrNull(k)?.value ?: 0f
        if (value <= 0.001f) continue
        val samples = mesh.arcs[k]
        val end = value * segments
        val lastWhole = floor(end).toInt().coerceAtMost(segments)

        path.rewind()
        var pen = false
        for (j in 0..lastWhole) {
            p.project(samples[j * 3], samples[j * 3 + 1], samples[j * 3 + 2])
            if (p.visibleAboveSurface()) {
                val sx = center.x + p.px * radius
                val sy = center.y - p.py * radius
                if (pen) path.lineTo(sx, sy) else path.moveTo(sx, sy)
                pen = true
            } else {
                pen = false
            }
        }
        if (end > lastWhole && lastWhole < segments) {
            projectArcAt(samples, segments, value, p)
            if (p.visibleAboveSurface() && pen) {
                path.lineTo(center.x + p.px * radius, center.y - p.py * radius)
            }
        }

        if (full) drawPath(path, color, alpha = 0.18f * value, style = sizes.arcGlowStroke)
        drawPath(path, color, alpha = 0.85f * value, style = sizes.arcStroke)

        if (value >= 0.999f) {
            val e = k * 3
            p.project(mesh.endpoints[e], mesh.endpoints[e + 1], mesh.endpoints[e + 2])
            if (p.onFront()) {
                drawCircle(
                    color,
                    sizes.node,
                    Offset(center.x + p.px * radius, center.y - p.py * radius),
                )
            }

            if (liveAlpha > 0.01f) {
                val head = (time * COMET_SPEED + k * 0.29f) % 1f
                val tail = if (full) 4 else 1
                for (q in 0 until tail) {
                    val u = head - q * 0.025f
                    if (u < 0f) break
                    projectArcAt(samples, segments, u, p)
                    if (!p.visibleAboveSurface()) continue
                    val position = Offset(center.x + p.px * radius, center.y - p.py * radius)
                    val fadeOut = 1f - q * 0.28f
                    if (q == 0) {
                        drawCircle(GlobePalette.Comet, sizes.cometGlow, position, alpha = 0.22f * liveAlpha)
                    }
                    drawCircle(
                        GlobePalette.Comet,
                        sizes.comet - q * sizes.cometStep,
                        position,
                        alpha = liveAlpha * fadeOut,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawHome(
    mesh: GlobeMesh,
    frame: GlobeFrame,
    sizes: GlobeSizes,
    center: Offset,
    radius: Float,
    color: Color,
    accent: Color,
    alpha: Float,
    scanAlpha: Float,
    liveAlpha: Float,
    time: Float,
) {
    if (alpha <= 0.01f) return
    val p = frame.projector
    p.project(mesh.home[0], mesh.home[1], mesh.home[2])
    if (!p.onFront()) return
    val position = Offset(center.x + p.px * radius, center.y - p.py * radius)

    if (liveAlpha > 0.01f) {
        val phase = (time / RIPPLE_PERIOD) % 1f
        val eased = GlobeMotion.Decelerate.transform(phase)
        drawCircle(
            color,
            sizes.home + sizes.ripple * eased,
            position,
            alpha = (1f - phase) * 0.5f * liveAlpha,
            style = sizes.ringStroke,
        )
    }

    if (scanAlpha > 0.01f) {
        for (ring in 0..1) {
            val phase = (time / SCAN_PERIOD + ring * 0.5f) % 1f
            val eased = GlobeMotion.Decelerate.transform(phase)
            drawCircle(
                accent,
                sizes.home + sizes.ripple * 1.4f * eased,
                position,
                alpha = (1f - phase) * 0.6f * scanAlpha,
                style = sizes.ringStroke,
            )
        }
    }

    drawCircle(color, sizes.homeHalo, position, alpha = 0.22f * alpha)
    drawCircle(color, sizes.home, position, alpha = alpha)
}

private fun wrapPi(angle: Float): Float {
    var a = angle % TAU
    if (a > PI_F) a -= TAU
    if (a < -PI_F) a += TAU
    return a
}

private fun wrapTau(angle: Float): Float {
    val a = angle % TAU
    return if (a < 0f) a + TAU else a
}

@Preview(widthDp = 390, heightDp = 844, showBackground = true, backgroundColor = 0xFF0E0E12)
@Composable
private fun NetFixGlobeBackgroundPreview() {
    var status by remember { mutableStateOf(GlobeStatus.Off) }
    LaunchedEffect(Unit) {
        val cycle = listOf(GlobeStatus.Connecting, GlobeStatus.Connected, GlobeStatus.Error, GlobeStatus.Off)
        var index = 0
        while (true) {
            delay(3_000)
            status = cycle[index % cycle.size]
            index++
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF0E0E12))) {
        NetFixGlobeBackground(status = status, quality = GlobeQuality.Full)
    }
}
