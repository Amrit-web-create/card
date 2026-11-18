package com.amrit.mycard.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amrit.mycard.data.BankCard
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

data class CardDesign(val name: String, val colors: List<Color>, val ink: Color = Color.White)

val LocalCardInk = compositionLocalOf { Color.White }

val CardDesigns = listOf(
    CardDesign("Aurora", listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2), Color(0xFF3A7BD5))),
    CardDesign("Midnight", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
    CardDesign("Ember", listOf(Color(0xFFFF512F), Color(0xFFDD2476), Color(0xFF8E1B5E))),
    CardDesign("Emerald", listOf(Color(0xFF003D33), Color(0xFF008F7A), Color(0xFF2AD19C))),
    CardDesign("Carbon", listOf(Color(0xFF1B1C1E), Color(0xFF34373B), Color(0xFF55595F))),
    CardDesign("Toon", listOf(Color(0xFF1E6FFF), Color(0xFF3FA0FF), Color(0xFF7FD1FF))),
    CardDesign("Neon", listOf(Color(0xFF0B0221), Color(0xFF1A0B3D), Color(0xFF2E0F63))),
    CardDesign("Galaxy", listOf(Color(0xFF05010F), Color(0xFF1B0A3C), Color(0xFF3A1C71))),
    CardDesign("Candy", listOf(Color(0xFFFF4FA3), Color(0xFFFF7A59), Color(0xFFFFB347))),
    CardDesign("Royal", listOf(Color(0xFF14100A), Color(0xFF2E2412), Color(0xFF4A3A18))),
    CardDesign("Gold", listOf(Color(0xFF9A7420), Color(0xFFE9C766), Color(0xFFB78A2B)), Color(0xFF2A1C04)),
    CardDesign("Porcelain", listOf(Color(0xFFFFFFFF), Color(0xFFF1EEE8), Color(0xFFDCD6CB)), Color(0xFF1C1814)),
    CardDesign("Lattice", listOf(Color(0xFF5E1A2E), Color(0xFF7C2339), Color(0xFF4A1424))),
    CardDesign("Pine", listOf(Color(0xFF04140F), Color(0xFF0B2E22), Color(0xFF123F2F)))
)

val CardNetworks = listOf("Visa", "Mastercard", "RuPay", "Amex", "Other")

private const val CARD_RATIO = 1.586f

@Composable
private fun rememberLoop(ms: Int): State<Float> {
    val transition = rememberInfiniteTransition(label = "loop")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ms, easing = LinearEasing)),
        label = "loopT"
    )
}

private fun DrawScope.drawToon(t: Float) {
    val w = size.width
    val h = size.height
    val a = (t * 2 * PI).toFloat()
    val ink = Color(0xFF1B2B4B)

    // twinkling stars
    val starSpots = listOf(Offset(0.12f, 0.34f), Offset(0.42f, 0.22f), Offset(0.90f, 0.56f), Offset(0.30f, 0.50f))
    starSpots.forEachIndexed { i, p ->
        val k = 0.5f + 0.5f * sin(a * 2f + i * 1.7f)
        val r = h * 0.03f * (0.5f + k)
        val c = Offset(w * p.x, h * p.y)
        drawLine(Color(0xFFFFE066).copy(alpha = 0.55f + 0.4f * k), Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(Color(0xFFFFE066).copy(alpha = 0.55f + 0.4f * k), Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = 4f, cap = StrokeCap.Round)
    }

    // clouds (soft, drifting)
    fun cloud(cx: Float, cy: Float, s: Float) {
        val parts = listOf(
            Triple(-1f, 0.15f, 0.55f),
            Triple(-0.3f, -0.25f, 0.70f),
            Triple(0.5f, 0f, 0.60f),
            Triple(1.1f, 0.2f, 0.45f)
        )
        parts.forEach { drawCircle(ink.copy(alpha = 0.45f), s * it.third + 3f, Offset(cx + s * it.first, cy + s * it.second)) }
        parts.forEach { drawCircle(Color.White.copy(alpha = 0.42f), s * it.third, Offset(cx + s * it.first, cy + s * it.second)) }
    }
    cloud(((t) % 1f) * w * 1.5f - w * 0.25f, h * 0.50f, h * 0.06f)
    cloud(((t + 0.55f) % 1f) * w * 1.5f - w * 0.25f, h * 0.17f, h * 0.045f)

    // hills
    val hill1 = Path().apply {
        moveTo(0f, h)
        lineTo(0f, h * 0.84f)
        cubicTo(w * 0.2f, h * 0.72f, w * 0.4f, h * 0.72f, w * 0.55f, h * 0.86f)
        cubicTo(w * 0.70f, h * 0.98f, w * 0.85f, h * 0.80f, w, h * 0.82f)
        lineTo(w, h)
        close()
    }
    drawPath(hill1, Color(0xFF2FCB75))
    drawPath(hill1, ink, style = Stroke(4f))
    val hill2 = Path().apply {
        moveTo(0f, h)
        lineTo(0f, h * 0.93f)
        cubicTo(w * 0.3f, h * 0.84f, w * 0.6f, h * 1.0f, w, h * 0.90f)
        lineTo(w, h)
        close()
    }
    drawPath(hill2, Color(0xFF1EA85E))
    drawPath(hill2, ink, style = Stroke(4f))

    // happy sun
    val c = Offset(w * 0.66f, h * 0.30f)
    val r = h * 0.10f
    for (i in 0 until 10) {
        val ang = a * 0.6f + i * (2f * PI.toFloat() / 10f)
        val s = c + Offset(cos(ang), sin(ang)) * (r * 1.3f)
        val e = c + Offset(cos(ang), sin(ang)) * (r * 1.75f)
        drawLine(ink, s, e, strokeWidth = 9f, cap = StrokeCap.Round)
        drawLine(Color(0xFFFFD43B), s, e, strokeWidth = 4.5f, cap = StrokeCap.Round)
    }
    drawCircle(ink, r + 3.5f, c)
    drawCircle(Color(0xFFFFD43B), r, c)
    drawCircle(Color(0xFFFF8A65).copy(alpha = 0.65f), r * 0.16f, c + Offset(-r * 0.55f, r * 0.22f))
    drawCircle(Color(0xFFFF8A65).copy(alpha = 0.65f), r * 0.16f, c + Offset(r * 0.55f, r * 0.22f))
    val blink = if ((t * 3f) % 1f > 0.92f) 0.15f else 1f
    for (side in listOf(-1f, 1f)) {
        val ex = c.x + side * r * 0.36f
        val eh = r * 0.30f * blink
        drawOval(ink, Offset(ex - r * 0.09f, c.y - r * 0.15f - eh / 2f), Size(r * 0.18f, eh))
    }
    drawArc(
        ink,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(c.x - r * 0.36f, c.y - r * 0.08f),
        size = Size(r * 0.72f, r * 0.52f),
        style = Stroke(4f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawDecor(index: Int, t: Float) {
    val w = size.width
    val h = size.height
    val a = (t * 2 * PI).toFloat()
    when (index) {
        0 -> {
            drawCircle(Color.White.copy(alpha = 0.10f), h * 0.75f, Offset(w * 0.95f + sin(a) * w * 0.03f, h * 0.05f + cos(a) * h * 0.04f))
            drawCircle(Color.White.copy(alpha = 0.08f), h * 0.50f, Offset(w * 0.10f + cos(a) * w * 0.03f, h))
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.12f), h * 0.35f, Offset(w * 0.55f + sin(a + 2f) * w * 0.05f, h * 0.60f + cos(a) * h * 0.05f))
        }
        1 -> {
            for (i in 0..7) {
                drawCircle(
                    Color.White.copy(alpha = 0.06f),
                    h * (0.25f + i * 0.17f) + sin(a + i * 0.6f) * h * 0.03f,
                    Offset(w * 0.85f, h * 0.95f),
                    style = Stroke(2.5f)
                )
            }
        }
        2 -> {
            for (i in 0..6) {
                val o = i * 0.13f
                val m = sin(a + i * 0.5f) * h * 0.08f
                val p = Path().apply {
                    moveTo(0f, h * (0.15f + o))
                    cubicTo(w * 0.3f, h * o + m, w * 0.6f, h * (0.5f + o) - m, w, h * (0.2f + o))
                }
                drawPath(p, Color.White.copy(alpha = 0.12f), style = Stroke(3f))
            }
        }
        3 -> {
            drawCircle(Color.White.copy(alpha = 0.07f), h * 0.60f, Offset(w * 0.15f + sin(a) * w * 0.04f, h * 0.20f))
            drawCircle(Color.White.copy(alpha = 0.07f), h * 0.60f, Offset(w * 0.35f + cos(a) * w * 0.04f, h * 0.55f))
            drawCircle(Color.White.copy(alpha = 0.06f), h * 0.45f, Offset(w * 0.95f, h * 0.90f + sin(a) * h * 0.05f))
        }
        4 -> {
            var x = -h + t * 28f
            while (x < w) {
                drawLine(Color.White.copy(alpha = 0.05f), Offset(x, h), Offset(x + h, 0f), strokeWidth = 10f)
                x += 28f
            }
        }
        5 -> drawToon(t)
        6 -> {
            val horizon = h * 0.60f
            drawCircle(Color(0xFFFF2BD6).copy(alpha = 0.22f), h * 0.30f, Offset(w * 0.5f, horizon))
            drawCircle(Color(0xFFFF2BD6).copy(alpha = 0.18f), h * 0.20f, Offset(w * 0.5f, horizon))
            for (k in 0 until 8) {
                val p = (k + t) / 8f
                val y = horizon + (h - horizon) * p * p
                drawLine(Color(0xFF00F0FF).copy(alpha = 0.15f + 0.5f * p), Offset(0f, y), Offset(w, y), strokeWidth = 2.5f)
            }
            for (i in -7..7) {
                drawLine(
                    Color(0xFFFF2BD6).copy(alpha = 0.45f),
                    Offset(w * 0.5f, horizon),
                    Offset(w * 0.5f + i * w * 0.17f, h),
                    strokeWidth = 2f
                )
            }
            drawLine(Color(0xFF00F0FF).copy(alpha = 0.8f), Offset(0f, horizon), Offset(w, horizon), strokeWidth = 3f)
        }
        7 -> {
            for (i in 0 until 32) {
                val x = ((i * 37) % 100) / 100f * w
                val y = ((i * 61) % 100) / 100f * h
                val r = 1.2f + (i % 3)
                val al = 0.30f + 0.70f * abs(sin(a + i * 0.9f))
                drawCircle(Color.White.copy(alpha = al), r, Offset(x, y))
            }
            val pc = Offset(w * 0.82f + sin(a) * w * 0.012f, h * 0.26f)
            val pr = h * 0.15f
            drawCircle(Color(0xFF7E57C2).copy(alpha = 0.55f), pr, pc)
            drawCircle(Color(0xFFB39DDB).copy(alpha = 0.25f), pr * 0.55f, pc + Offset(-pr * 0.25f, -pr * 0.25f))
            drawOval(
                Color(0xFFD1C4E9).copy(alpha = 0.6f),
                Offset(pc.x - pr * 1.7f, pc.y - pr * 0.3f),
                Size(pr * 3.4f, pr * 0.6f),
                style = Stroke(3f)
            )
            if (t < 0.35f) {
                val q = t / 0.35f
                val head = Offset(w * (1.0f - q * 1.1f), h * (0.05f + q * 0.5f))
                val tail = head + Offset(w * 0.12f, -h * 0.08f)
                drawLine(Color.White.copy(alpha = 1f - q), head, tail, strokeWidth = 3f, cap = StrokeCap.Round)
            }
        }
        8 -> {
            for (i in 0 until 9) {
                val bx = w * ((i * 0.113f + 0.05f) % 1f)
                val p = (t + i * 0.11f) % 1f
                val by = h * (1.1f - p * 1.3f)
                val br = h * (0.05f + (i % 3) * 0.025f)
                val c = Offset(bx + sin(a + i) * w * 0.015f, by)
                drawCircle(Color.White.copy(alpha = 0.16f), br, c)
                drawCircle(Color.White.copy(alpha = 0.40f), br, c, style = Stroke(2f))
                drawCircle(Color.White.copy(alpha = 0.55f), br * 0.18f, c + Offset(-br * 0.4f, -br * 0.4f))
            }
        }
        10 -> {
            var x = -h + t * 36f
            while (x < w) {
                drawLine(Color.White.copy(alpha = 0.16f), Offset(x, h), Offset(x + h, 0f), strokeWidth = 14f)
                x += 36f
            }
            for (i in 0 until 8) {
                val sx = ((i * 47) % 100) / 100f * w
                val sy = ((i * 31) % 100) / 100f * h
                val al = 0.10f + 0.45f * abs(sin(a + i * 1.1f))
                drawCircle(Color.White.copy(alpha = al), 2.6f, Offset(sx, sy))
            }
        }
        11 -> {
            val g = Color(0xFF8A8174)
            drawCircle(g.copy(alpha = 0.07f), h * 0.7f, Offset(w * 0.9f + sin(a) * w * 0.03f, h * 0.1f))
            drawCircle(g.copy(alpha = 0.06f), h * 0.5f, Offset(w * 0.1f + cos(a) * w * 0.03f, h * 0.95f))
            for (i in 1..4) {
                drawCircle(g.copy(alpha = 0.07f), h * (0.2f + i * 0.14f), Offset(w * 0.85f, h * 0.85f), style = Stroke(1.6f))
            }
        }
        12 -> {
            val step = 34f
            var x = -h + t * step
            while (x < w + h) {
                drawLine(Color.White.copy(alpha = 0.13f), Offset(x, 0f), Offset(x + h, h), strokeWidth = 2f)
                drawLine(Color.White.copy(alpha = 0.13f), Offset(x + h, 0f), Offset(x, h), strokeWidth = 2f)
                x += step
            }
        }
        13 -> {
            val ox = w * (0.78f + 0.08f * sin(a))
            val oy = h * (0.22f + 0.06f * cos(a))
            drawCircle(Color(0xFFD7B35A).copy(alpha = 0.10f), h * 0.55f, Offset(ox, oy))
            drawCircle(Color(0xFFD7B35A).copy(alpha = 0.10f), h * 0.35f, Offset(ox, oy))
            drawCircle(Color(0xFF3FD19A).copy(alpha = 0.10f), h * 0.45f, Offset(w * 0.15f, h * 0.95f))
        }
        else -> {
            val gold = Color(0xFFE5B94A)
            val c = Offset(w * 0.85f, h * 0.15f)
            for (i in 0 until 14) {
                val ang = a * 0.5f + i * (2f * PI.toFloat() / 14f)
                drawLine(
                    gold.copy(alpha = 0.14f),
                    c,
                    c + Offset(cos(ang), sin(ang)) * (h * 1.4f),
                    strokeWidth = 5f
                )
            }
            for (i in 1..3) {
                drawCircle(gold.copy(alpha = 0.28f), h * 0.22f * i, c, style = Stroke(2f))
            }
            for (i in 0 until 10) {
                val x = ((i * 53) % 100) / 100f * w
                val y = ((i * 29) % 100) / 100f * h
                val al = 0.15f + 0.6f * abs(sin(a + i * 1.3f))
                drawCircle(gold.copy(alpha = al), 2.2f, Offset(x, y))
            }
        }
    }
}

@Composable
private fun CText(
    text: String,
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    spacing: Float = 0f,
    alpha: Float = 1f,
    family: FontFamily? = null,
    italic: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ink = LocalCardInk.current
    Text(
        text = text,
        color = ink.copy(alpha = alpha),
        fontSize = size.sp,
        fontWeight = weight,
        letterSpacing = spacing.sp,
        fontFamily = family,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        maxLines = 1,
        style = TextStyle(shadow = if (ink.luminance() > 0.5f) Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 4f) else null),
        modifier = modifier
    )
}

@Composable
fun NetworkLogo(network: String, s: Float) {
    when (network) {
        "Visa" -> CText("VISA", 26f * s, FontWeight.ExtraBold, -1f, italic = true)
        "Mastercard" -> Canvas(Modifier.size((46 * s).dp, (30 * s).dp)) {
            val r = size.height / 2f
            drawCircle(Color(0xFFEB001B), r, Offset(r, r))
            drawCircle(Color(0xFFF79E1B).copy(alpha = 0.92f), r, Offset(size.width - r, r))
        }
        "RuPay" -> Row {
            CText("Ru", 22f * s, FontWeight.ExtraBold, italic = true)
            Text(
                "Pay",
                color = Color(0xFFFF9800),
                fontSize = (22f * s).sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic
            )
        }
        "Amex" -> Box(
            Modifier
                .background(Color(0xFF1F7AC4), RoundedCornerShape(4.dp))
                .padding(horizontal = (6 * s).dp, vertical = (3 * s).dp)
        ) { CText("AMEX", 14f * s, FontWeight.ExtraBold, 1f) }
        else -> CText(network.uppercase(), 14f * s, FontWeight.Bold, 1f, 0.9f)
    }
}

@Composable
private fun ChipGlyph(s: Float) {
    Canvas(Modifier.size((44 * s).dp, (33 * s).dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFFF7DE95), Color(0xFFC9A24B))),
            cornerRadius = CornerRadius(w * 0.18f)
        )
        val line = Color(0xFF7A5C1A).copy(alpha = 0.55f)
        drawLine(line, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 2f)
        drawLine(line, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 2f)
        drawLine(line, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 2f)
    }
}

@Composable
private fun ContactlessGlyph(s: Float) {
    val ink = LocalCardInk.current
    Canvas(Modifier.size((26 * s).dp)) {
        val w = size.width
        for (i in 1..3) {
            val r = w * 0.30f * i
            drawArc(
                color = ink.copy(alpha = 0.85f),
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(-r, size.height / 2f - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
            )
        }
    }
}

fun formatCardNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return "0000 0000 0000 0000"
    return digits.chunked(4).joinToString(" ")
}

fun formatExpiry(raw: String): String {
    val d = raw.filter { it.isDigit() }
    return when {
        d.isEmpty() -> "MM/YY"
        d.length <= 2 -> d
        else -> d.substring(0, 2) + "/" + d.substring(2)
    }
}

@Composable
fun CardFront(card: BankCard) {
    val ink = CardDesigns[card.design.coerceIn(0, CardDesigns.lastIndex)].ink
    CompositionLocalProvider(LocalCardInk provides ink) {
        if (card.isDoc) DocFront(card) else BankFront(card)
    }
}

@Composable
fun CardBack(card: BankCard) {
    val ink = CardDesigns[card.design.coerceIn(0, CardDesigns.lastIndex)].ink
    CompositionLocalProvider(LocalCardInk provides ink) {
        if (card.isDoc) DocBack(card) else BankBack(card)
    }
}

@Composable
private fun BankFront(card: BankCard) {
    val design = CardDesigns[card.design.coerceIn(0, CardDesigns.lastIndex)]
    val designIndex = card.design.coerceIn(0, CardDesigns.lastIndex)
    val shine = rememberLoop(4600)
    val loop = rememberLoop(9000)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(design.colors))
    ) {
        val s = maxWidth.value / 340f
        Canvas(Modifier.fillMaxSize()) {
            drawDecor(designIndex, loop.value)
            val x = size.width * (-0.6f + shine.value * 2.2f)
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.16f), Color.Transparent),
                    start = Offset(x, 0f),
                    end = Offset(x + size.width * 0.45f, size.height * 0.45f)
                )
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding((20 * s).dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    CText(
                        card.bank.ifBlank { "BANK NAME" }.uppercase(),
                        16f * s,
                        FontWeight.Bold,
                        1f
                    )
                    if (card.international) {
                        CText("INTERNATIONAL", 9f * s, FontWeight.SemiBold, 2f, 0.75f)
                    }
                }
                CText(card.type.uppercase(), 11f * s, FontWeight.SemiBold, 2f, 0.8f)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChipGlyph(s)
                ContactlessGlyph(s)
            }
            CText(
                formatCardNumber(card.number),
                23f * s,
                FontWeight.Medium,
                2f,
                family = FontFamily.Monospace
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    if (card.showHolder) {
                        if (card.holder.isBlank()) {
                            CText("CARD HOLDER", 14f * s, FontWeight.SemiBold, 1.2f, 0.55f)
                        } else {
                            CText(card.holder.uppercase(), 14f * s, FontWeight.SemiBold, 1.2f)
                        }
                    }
                }
                Column(Modifier.padding(end = (14 * s).dp)) {
                    CText("VALID THRU", 8f * s, FontWeight.Medium, 1.5f, 0.65f)
                    CText(formatExpiry(card.expiry), 14f * s, FontWeight.SemiBold, 1.2f)
                }
                NetworkLogo(card.network, s)
            }
        }
    }
}

@Composable
private fun BankBack(card: BankCard) {
    val design = CardDesigns[card.design.coerceIn(0, CardDesigns.lastIndex)]
    val designIndex = card.design.coerceIn(0, CardDesigns.lastIndex)
    val loop = rememberLoop(9000)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(design.colors.reversed()))
    ) {
        val s = maxWidth.value / 340f
        Canvas(Modifier.fillMaxSize()) { drawDecor(designIndex, loop.value) }
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height((24 * s).dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height((46 * s).dp)
                    .background(Color.Black.copy(alpha = 0.85f))
            )
            Spacer(Modifier.height((18 * s).dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (20 * s).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height((36 * s).dp)
                        .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        "Authorised signature",
                        color = Color(0xFF8A8FA3),
                        fontSize = (10 * s).sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(start = (10 * s).dp)
                    )
                }
                Spacer(Modifier.width((10 * s).dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CText("CVV", 8f * s, FontWeight.Medium, 1.5f, 0.75f)
                    Box(
                        Modifier
                            .width((64 * s).dp)
                            .height((30 * s).dp)
                            .background(Color.White, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            card.cvv.ifBlank { "000" },
                            color = Color.Black,
                            fontSize = (15 * s).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding((20 * s).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    CText(card.bank.ifBlank { "BANK NAME" }.uppercase(), 12f * s, FontWeight.Bold, 1f)
                    CText(
                        if (card.international) "International card" else "Domestic card",
                        9f * s,
                        alpha = 0.7f
                    )
                }
                NetworkLogo(card.network, s * 0.85f)
            }
        }
    }
}

/**
 * 3D card. Tap flips it. Rotation [mode]: 0 = off, 1 = sideways spin by finger, 2 = free 3D by finger
 * (any direction, diagonal drags make curved turns). It never rotates on its own.
 * [flipped] lets a screen flip the card from outside (for example when the CVV field is focused).
 */
@Composable
fun Flip3DCard(
    card: BankCard,
    modifier: Modifier = Modifier,
    flipped: Boolean = false,
    mode: Int = 2
) {
    val angleY = remember { Animatable(0f) }
    val angleX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val spec = spring<Float>(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)

    LaunchedEffect(flipped) {
        val k = (angleY.value / 180f).roundToInt()
        val isBack = k % 2 != 0
        if (isBack != flipped) {
            angleY.animateTo((k + 1) * 180f, spec)
        }
    }

    val float = rememberInfiniteTransition(label = "float")
    val tilt by float.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tilt"
    )
    val density = LocalDensity.current.density

    val ry = angleY.value * (PI / 180.0)
    val rx = angleX.value * (PI / 180.0)
    val showFront = cos(ry) * cos(rx) >= 0.0

    fun settle() {
        scope.launch {
            launch { angleY.animateTo((angleY.value / 180f).roundToInt() * 180f, spec) }
            launch { angleX.animateTo((angleX.value / 360f).roundToInt() * 360f, spec) }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(CARD_RATIO)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    scope.launch {
                        val ky = (angleY.value / 180f).roundToInt()
                        launch { angleY.animateTo((ky + 1) * 180f, spec) }
                        launch { angleX.animateTo((angleX.value / 360f).roundToInt() * 360f, spec) }
                    }
                })
            }
            .pointerInput(mode) {
                if (mode == 2) {
                    detectDragGestures(
                        onDragStart = {
                            scope.launch {
                                angleY.stop()
                                angleX.stop()
                            }
                        },
                        onDragEnd = { settle() },
                        onDragCancel = { settle() }
                    ) { change, drag ->
                        change.consume()
                        scope.launch {
                            angleY.snapTo(angleY.value + drag.x * 0.55f)
                            angleX.snapTo(angleX.value - drag.y * 0.55f)
                        }
                    }
                } else if (mode == 1) {
                    detectHorizontalDragGestures(
                        onDragStart = { scope.launch { angleY.stop() } },
                        onDragEnd = { settle() },
                        onDragCancel = { settle() }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch { angleY.snapTo(angleY.value + dragAmount * 0.55f) }
                    }
                }
            }
            .graphicsLayer {
                rotationY = angleY.value
                rotationX = angleX.value + if (mode == 0) tilt else 0f
                cameraDistance = 14f * density
                shadowElevation = 26f
                shape = RoundedCornerShape(22.dp)
                clip = true
                ambientShadowColor = Color.Black
                spotShadowColor = Color.Black
            }
    ) {
        if (showFront) {
            CardFront(card)
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                CardBack(card)
            }
        }
    }
}
