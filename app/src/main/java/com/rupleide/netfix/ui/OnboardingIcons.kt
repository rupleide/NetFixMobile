package com.rupleide.netfix.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private fun circlePath(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun roundRectPath(x: Float, y: Float, w: Float, h: Float, r: Float): String =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r $r" +
        "v${h - 2 * r}a$r $r 0 0 1 ${-r} $r" +
        "h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}" +
        "v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

private fun strokeIcon(
    name: String,
    vararg paths: String,
    width: Float = 2f,
    join: StrokeJoin = StrokeJoin.Round
): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    paths.forEach { data ->
        builder.addPath(
            pathData = PathParser().parsePathString(data).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = join
        )
    }
    return builder.build()
}

internal object OnbIcons {
    val Power by lazy { strokeIcon("onbPower", "M12 3v8", "M6.3 7.2a8 8 0 1 0 11.4 0") }
    val Plane by lazy { strokeIcon("onbPlane", "M21 4L3 11l6 2 2 6 3-4 5 4z") }
    val Play by lazy { strokeIcon("onbPlay", roundRectPath(3f, 5f, 18f, 14f, 4f), "M10 9l5 3-5 3z") }
    val Gamepad by lazy {
        strokeIcon(
            "onbGamepad",
            roundRectPath(2f, 7f, 20f, 11f, 5.5f),
            "M7 11v3M5.5 12.5h3",
            "M16 12v.01",
            "M18.5 13.5v.01"
        )
    }
    val Bolt by lazy { strokeIcon("onbBolt", "M13 2L4 14h7l-1 8 9-12h-7z") }
    val Refresh by lazy { strokeIcon("onbRefresh", "M20 11a8 8 0 1 0-2.3 5.7", "M20 4v7h-7") }
    val Code by lazy { strokeIcon("onbCode", "M8 7l-5 5 5 5", "M16 7l5 5-5 5") }
    val Phone by lazy { strokeIcon("onbPhone", roundRectPath(7f, 2f, 10f, 20f, 2.5f), "M11 18h2") }
    val PhoneSmall by lazy { strokeIcon("onbPhoneSmall", roundRectPath(6f, 3f, 12f, 18f, 3f), "M11 18h2") }
    val Tv by lazy { strokeIcon("onbTv", roundRectPath(2f, 4f, 20f, 13f, 2.5f), "M8 21h8", "M12 17v4") }
    val Dial by lazy { strokeIcon("onbDial", circlePath(12f, 12f, 9f), circlePath(12f, 12f, 4.5f), "M12 12l6-4") }
    val Stack by lazy {
        strokeIcon("onbStack", "M4 18h4", "M10 14h4v4h-4z", "M16 9h4v9h-4z", "M4 14h4v4")
    }
    val Check by lazy { strokeIcon("onbCheck", "M5 12.5l4.5 4.5L19 7.5", width = 2.4f) }
    val CheckSmall by lazy { strokeIcon("onbCheckSmall", "M5 12.5l4.5 4.5L19 7.5", width = 3f) }
    val Shield by lazy {
        strokeIcon("onbShield", "M12 3l8 3v6c0 4.5-3.4 8-8 9-4.6-1-8-4.5-8-9V6z", "M9 12l2 2 4-4")
    }
    val ShieldAlert by lazy {
        strokeIcon("onbShieldAlert", "M12 3l8 3v6c0 4.5-3.4 8-8 9-4.6-1-8-4.5-8-9V6z", "M12 8v5", "M12 16.5v.01")
    }
    val Lock by lazy { strokeIcon("onbLock", roundRectPath(5f, 10f, 14f, 10f, 3f), "M8 10V7a4 4 0 0 1 8 0v3") }
    val Help by lazy {
        strokeIcon(
            "onbHelp",
            circlePath(12f, 12f, 9f),
            "M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6",
            "M12 17v.01"
        )
    }
    val WifiOff by lazy {
        strokeIcon(
            "onbWifiOff",
            "M2 8.5a15 15 0 0 1 20 0",
            "M5.5 12a10 10 0 0 1 13 0",
            "M9 15.5a5 5 0 0 1 6 0",
            "M12 19v.01",
            "M3 3l18 18"
        )
    }
    val Wifi by lazy { strokeIcon("onbWifi", "M5 18c2-6 12-6 14 0", "M8 13.5c2-3 6-3 8 0", "M12 9v.01") }
    val Close by lazy { strokeIcon("onbClose", "M7 7l10 10", "M17 7L7 17", width = 2.4f) }
    val Minus by lazy { strokeIcon("onbMinus", "M7 12h10", width = 3f) }
    val Info by lazy { strokeIcon("onbInfo", circlePath(12f, 12f, 9f), "M12 11v5", "M12 8v.01") }
    val Alert by lazy { strokeIcon("onbAlert", circlePath(12f, 12f, 9f), "M12 8v5", "M12 16v.01") }
    val Exclaim by lazy { strokeIcon("onbExclaim", "M12 7v6", "M12 17v.01", width = 3f) }
    val Bulb by lazy {
        strokeIcon(
            "onbBulb",
            "M9 18h6",
            "M10 21h4",
            "M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z"
        )
    }
    val Search by lazy { strokeIcon("onbSearch", circlePath(10.5f, 10.5f, 6.5f), "M20 20l-4.8-4.8") }
    val Target by lazy { strokeIcon("onbTarget", circlePath(12f, 12f, 9f), circlePath(12f, 12f, 4.5f), "M12 12v.01") }
    val CheckCircle by lazy { strokeIcon("onbCheckCircle", circlePath(12f, 12f, 9f), "M8.5 12.5l2.5 2.5 4.5-5") }
    val ChevronLeft by lazy { strokeIcon("onbChevronLeft", "M15 6l-6 6 6 6") }
}
