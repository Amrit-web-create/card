package com.amrit.mycard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amrit.mycard.data.BankCard

// ---------------------------------------------------------------------------------------------
// Field definitions used by the editor and the card faces
// ---------------------------------------------------------------------------------------------

const val K_TEXT = 0
const val K_DIGITS = 1
const val K_DATE = 2
const val K_MULTI = 3
const val K_GENDER = 4
const val K_ALNUM = 5

data class FieldSpec(
    val key: String,
    val label: String,
    val kind: Int,
    val max: Int,
    val back: Boolean = false
)

val CardTypes = listOf("Credit", "Debit", "Aadhaar", "PAN", "Voter ID", "Driving Licence")

fun docFields(type: String): List<FieldSpec> = when (type) {
    "Aadhaar" -> listOf(
        FieldSpec("holder", "Full name", K_TEXT, 40),
        FieldSpec("number", "Aadhaar number (12 digits)", K_DIGITS, 12),
        FieldSpec("dob", "Date of birth", K_DATE, 8),
        FieldSpec("gender", "Gender", K_GENDER, 0),
        FieldSpec("father", "Father or guardian name (S/O)", K_TEXT, 40, back = true),
        FieldSpec("address", "Address", K_MULTI, 140, back = true),
        FieldSpec("pincode", "Pincode", K_DIGITS, 6, back = true),
        FieldSpec("vid", "VID (16 digits, optional)", K_DIGITS, 16, back = true)
    )
    "PAN" -> listOf(
        FieldSpec("holder", "Name", K_TEXT, 40),
        FieldSpec("number", "PAN number", K_ALNUM, 10),
        FieldSpec("father", "Father's name", K_TEXT, 40),
        FieldSpec("dob", "Date of birth", K_DATE, 8),
        FieldSpec("issued", "Date of issue (optional)", K_DATE, 8, back = true),
        FieldSpec("address", "Address (optional)", K_MULTI, 140, back = true)
    )
    "Voter ID" -> listOf(
        FieldSpec("holder", "Full name", K_TEXT, 40),
        FieldSpec("number", "EPIC number", K_ALNUM, 12),
        FieldSpec("father", "Father or husband name", K_TEXT, 40),
        FieldSpec("gender", "Gender", K_GENDER, 0),
        FieldSpec("dob", "Date of birth", K_DATE, 8),
        FieldSpec("address", "Address", K_MULTI, 140, back = true),
        FieldSpec("pincode", "Pincode", K_DIGITS, 6, back = true),
        FieldSpec("constituency", "Assembly constituency", K_TEXT, 40, back = true),
        FieldSpec("issued", "Date of issue (optional)", K_DATE, 8, back = true)
    )
    "Driving Licence" -> listOf(
        FieldSpec("holder", "Full name", K_TEXT, 40),
        FieldSpec("number", "Licence number", K_ALNUM, 20),
        FieldSpec("father", "Son, wife or daughter of", K_TEXT, 40),
        FieldSpec("dob", "Date of birth", K_DATE, 8),
        FieldSpec("blood", "Blood group", K_ALNUM, 3),
        FieldSpec("issued", "Date of issue", K_DATE, 8),
        FieldSpec("valid", "Valid till", K_DATE, 8),
        FieldSpec("classes", "Vehicle classes (e.g. LMV, MCWG)", K_TEXT, 30, back = true),
        FieldSpec("authority", "Issuing authority or RTO", K_TEXT, 40, back = true),
        FieldSpec("address", "Address", K_MULTI, 140, back = true),
        FieldSpec("pincode", "Pincode", K_DIGITS, 6, back = true)
    )
    else -> emptyList()
}

fun formatDate(raw: String): String {
    val d = raw.filter { it.isDigit() }.take(8)
    if (d.isEmpty()) return ""
    val sb = StringBuilder()
    d.forEachIndexed { i, c ->
        if (i == 2 || i == 4) sb.append('/')
        sb.append(c)
    }
    return sb.toString()
}

fun isValidDate(raw: String): Boolean {
    val d = raw.filter { it.isDigit() }
    if (d.length != 8) return false
    val day = d.substring(0, 2).toInt()
    val month = d.substring(2, 4).toInt()
    val year = d.substring(4).toInt()
    return day in 1..31 && month in 1..12 && year in 1900..2100
}

fun docGradient(type: String): List<Color> = when (type) {
    "Aadhaar" -> listOf(Color(0xFFFFFFFF), Color(0xFFE9EEF7))
    "PAN" -> listOf(Color(0xFFD5E8FA), Color(0xFF8DB8EE))
    "Voter ID" -> listOf(Color(0xFFFFF6DB), Color(0xFFFFE29A))
    else -> listOf(Color(0xFFE8F5E9), Color(0xFFB9DDA0))
}

// ---------------------------------------------------------------------------------------------
// Small building blocks
// ---------------------------------------------------------------------------------------------

@Composable
private fun DText(
    text: String,
    size: Float,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    spacing: Float = 0f,
    family: FontFamily? = null,
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        letterSpacing = spacing.sp,
        fontFamily = family,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        lineHeight = (size * 1.25f).sp,
        modifier = modifier
    )
}

@Composable
private fun LabelValue(label: String, value: String, s: Float, ink: Color, labelColor: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        DText(label, 7.5f * s, labelColor, FontWeight.Medium, 0.4f)
        DText(value.ifBlank { "-" }, 11.5f * s, ink, FontWeight.SemiBold)
    }
}

@Composable
private fun PhotoBox(photoId: String, width: Dp, height: Dp, borderColor: Color) {
    val img by rememberVaultImage(photoId, 0, 480)
    val image = img
    Box(
        Modifier
            .size(width, height)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFDDE1E8))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val c = Color(0xFF9AA3B2)
                drawCircle(c, w * 0.20f, Offset(w * 0.5f, h * 0.36f))
                drawOval(c, Offset(w * 0.14f, h * 0.60f), Size(w * 0.72f, h * 0.70f))
            }
        }
    }
}

@Composable
private fun TricolourStrip(s: Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .height((5 * s).dp)
    ) {
        Box(Modifier.weight(1f).fillMaxSize().background(Color(0xFFFF9933)))
        Box(Modifier.weight(1f).fillMaxSize().background(Color.White))
        Box(Modifier.weight(1f).fillMaxSize().background(Color(0xFF138808)))
    }
}

@Composable
private fun ArcMark(s: Float, color: Color) {
    Canvas(Modifier.size((24 * s).dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        for (i in 1..3) {
            val r = size.width * 0.15f * i
            drawArc(
                color = color,
                startAngle = 200f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = size.width * 0.07f, cap = StrokeCap.Round)
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Fronts
// ---------------------------------------------------------------------------------------------

@Composable
fun DocFront(card: BankCard) {
    when (card.type) {
        "Aadhaar" -> AadhaarFront(card)
        "PAN" -> PanFront(card)
        "Voter ID" -> VoterFront(card)
        else -> LicenceFront(card)
    }
}

@Composable
fun DocBack(card: BankCard) {
    when (card.type) {
        "Aadhaar" -> AadhaarBack(card)
        "PAN" -> PanBack(card)
        "Voter ID" -> VoterBack(card)
        else -> LicenceBack(card)
    }
}

@Composable
private fun AadhaarFront(card: BankCard) {
    val ink = Color(0xFF1A1F2B)
    val soft = Color(0xFF6B7280)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(docGradient("Aadhaar")))
    ) {
        val s = maxWidth.value / 340f
        Column(Modifier.fillMaxSize()) {
            TricolourStrip(s)
            Row(
                Modifier.padding(start = (16 * s).dp, end = (16 * s).dp, top = (7 * s).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArcMark(s, Color(0xFFE53935))
                Spacer(Modifier.width((8 * s).dp))
                DText("Aadhaar", 20f * s, Color(0xFFC62828), FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                DText("IDENTITY CARD", 8f * s, soft, FontWeight.SemiBold, 1.5f)
            }
            Row(Modifier.padding(start = (16 * s).dp, end = (16 * s).dp, top = (8 * s).dp)) {
                PhotoBox(card.photoId, (68 * s).dp, (82 * s).dp, Color(0xFFB8BFCC))
                Spacer(Modifier.width((14 * s).dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((5 * s).dp)) {
                    DText(card.holder.ifBlank { "Full name" }, 16f * s, ink, FontWeight.Bold)
                    LabelValue("DATE OF BIRTH", formatDate(card.extra["dob"] ?: ""), s, ink, soft)
                    LabelValue("GENDER", card.extra["gender"] ?: "", s, ink, soft)
                }
            }
            Spacer(Modifier.weight(1f))
            DText(
                formatCardNumber12(card.number),
                23f * s,
                ink,
                FontWeight.Bold,
                2f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (16 * s).dp)
            )
            Spacer(Modifier.height((4 * s).dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE4E8F0))
                    .padding(vertical = (4 * s).dp),
                contentAlignment = Alignment.Center
            ) { DText("Personal copy, not an official document", 8f * s, soft, FontWeight.Medium) }
        }
    }
}

private fun formatCardNumber12(raw: String): String {
    val d = raw.filter { it.isDigit() }
    return if (d.isEmpty()) "0000 0000 0000" else d.chunked(4).joinToString(" ")
}

@Composable
private fun PanFront(card: BankCard) {
    val navy = Color(0xFF0B2A5B)
    val soft = Color(0xFF3F5C8C)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFFD9EBFB), Color(0xFF9CC3F0), Color(0xFFC3DAF6))))
    ) {
        val s = maxWidth.value / 340f
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.22f), size.height * 0.7f, Offset(size.width * 0.95f, size.height * 0.1f))
            drawCircle(Color.White.copy(alpha = 0.16f), size.height * 0.5f, Offset(size.width * 0.05f, size.height))
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding((14 * s).dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    DText("PERMANENT ACCOUNT NUMBER CARD", 10f * s, navy, FontWeight.Bold, 0.8f)
                    DText("Personal copy, not an official document", 7.5f * s, soft)
                }
                TricolourDots(s)
            }
            Row {
                PhotoBox(card.photoId, (58 * s).dp, (72 * s).dp, navy.copy(alpha = 0.4f))
                Spacer(Modifier.width((12 * s).dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((4 * s).dp)) {
                    DText("PERMANENT ACCOUNT NUMBER", 7.5f * s, soft, FontWeight.Medium, 0.4f)
                    DText(
                        card.number.uppercase().ifBlank { "ABCDE1234F" },
                        21f * s,
                        navy,
                        FontWeight.Bold,
                        2.5f,
                        FontFamily.Monospace
                    )
                    LabelValue("NAME", card.holder.uppercase(), s, navy, soft)
                    LabelValue("FATHER'S NAME", (card.extra["father"] ?: "").uppercase(), s, navy, soft)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                LabelValue("DATE OF BIRTH", formatDate(card.extra["dob"] ?: ""), s, navy, soft, Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .width((92 * s).dp)
                            .height((20 * s).dp)
                            .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
                    )
                    DText("SIGNATURE", 7f * s, soft, FontWeight.Medium, 1f)
                }
            }
        }
    }
}

@Composable
private fun TricolourDots(s: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy((3 * s).dp)) {
        Box(Modifier.size((9 * s).dp).clip(RoundedCornerShape(50)).background(Color(0xFFFF9933)))
        Box(Modifier.size((9 * s).dp).clip(RoundedCornerShape(50)).background(Color.White))
        Box(Modifier.size((9 * s).dp).clip(RoundedCornerShape(50)).background(Color(0xFF138808)))
    }
}

@Composable
private fun VoterFront(card: BankCard) {
    val maroon = Color(0xFF5B1A4A)
    val soft = Color(0xFF8A6A3A)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(docGradient("Voter ID")))
    ) {
        val s = maxWidth.value / 340f
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF5B1A8A), Color(0xFF8E2DE2))))
                    .padding(horizontal = (14 * s).dp, vertical = (8 * s).dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DText("ELECTOR PHOTO IDENTITY CARD", 11f * s, Color.White, FontWeight.Bold, 1f, modifier = Modifier.weight(1f))
                    TricolourDots(s)
                }
            }
            Row(Modifier.padding((14 * s).dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((4 * s).dp)) {
                    DText("EPIC NO.", 7.5f * s, soft, FontWeight.Medium, 0.4f)
                    DText(
                        card.number.uppercase().ifBlank { "ABC1234567" },
                        20f * s,
                        maroon,
                        FontWeight.Bold,
                        2f,
                        FontFamily.Monospace
                    )
                    LabelValue("ELECTOR'S NAME", card.holder, s, maroon, soft)
                    LabelValue("FATHER / HUSBAND NAME", card.extra["father"] ?: "", s, maroon, soft)
                    Row {
                        LabelValue("GENDER", card.extra["gender"] ?: "", s, maroon, soft, Modifier.weight(1f))
                        LabelValue("DATE OF BIRTH", formatDate(card.extra["dob"] ?: ""), s, maroon, soft, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.width((12 * s).dp))
                PhotoBox(card.photoId, (66 * s).dp, (82 * s).dp, maroon.copy(alpha = 0.4f))
            }
            Spacer(Modifier.weight(1f))
            DText(
                "Personal copy, not an official document",
                7.5f * s,
                soft,
                modifier = Modifier.padding(start = (14 * s).dp, bottom = (6 * s).dp)
            )
        }
    }
}

@Composable
private fun LicenceFront(card: BankCard) {
    val green = Color(0xFF14501C)
    val soft = Color(0xFF4F6F4A)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(docGradient("Driving Licence")))
    ) {
        val s = maxWidth.value / 340f
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0..5) {
                drawCircle(
                    Color.White.copy(alpha = 0.18f),
                    size.height * (0.25f + i * 0.14f),
                    Offset(size.width * 0.9f, size.height * 0.95f),
                    style = Stroke(2f)
                )
            }
        }
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(green)
                    .padding(horizontal = (14 * s).dp, vertical = (7 * s).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DText("DRIVING LICENCE", 13f * s, Color.White, FontWeight.Bold, 1.5f, modifier = Modifier.weight(1f))
                TricolourDots(s)
            }
            Row(Modifier.padding(start = (14 * s).dp, end = (14 * s).dp, top = (9 * s).dp)) {
                PhotoBox(card.photoId, (62 * s).dp, (78 * s).dp, green.copy(alpha = 0.4f))
                Spacer(Modifier.width((12 * s).dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((3 * s).dp)) {
                    DText("DL NO.", 7.5f * s, soft, FontWeight.Medium, 0.4f)
                    DText(
                        card.number.uppercase().ifBlank { "XX0000000000000" },
                        16f * s,
                        green,
                        FontWeight.Bold,
                        1.2f,
                        FontFamily.Monospace
                    )
                    LabelValue("NAME", card.holder, s, green, soft)
                    LabelValue("S/D/W OF", card.extra["father"] ?: "", s, green, soft)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = (14 * s).dp, end = (14 * s).dp),
                horizontalArrangement = Arrangement.spacedBy((8 * s).dp)
            ) {
                LabelValue("DOB", formatDate(card.extra["dob"] ?: ""), s, green, soft, Modifier.weight(1f))
                LabelValue("BLOOD", card.extra["blood"] ?: "", s, green, soft, Modifier.weight(0.6f))
                LabelValue("ISSUED", formatDate(card.extra["issued"] ?: ""), s, green, soft, Modifier.weight(1f))
                LabelValue("VALID TILL", formatDate(card.extra["valid"] ?: ""), s, green, soft, Modifier.weight(1f))
            }
            DText(
                "Personal copy, not an official document",
                7.5f * s,
                soft,
                modifier = Modifier.padding(start = (14 * s).dp, top = (4 * s).dp, bottom = (6 * s).dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Backs
// ---------------------------------------------------------------------------------------------

@Composable
private fun BackLayout(
    type: String,
    title: String,
    head: Color,
    ink: Color,
    soft: Color,
    rows: List<Pair<String, String>>,
    extraTop: @Composable (Float) -> Unit = {}
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(docGradient(type).reversed()))
    ) {
        val s = maxWidth.value / 340f
        Column(Modifier.fillMaxSize()) {
            extraTop(s)
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(head)
                    .padding(horizontal = (14 * s).dp, vertical = (6 * s).dp)
            ) { DText(title, 11f * s, Color.White, FontWeight.Bold, 1.2f) }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (14 * s).dp, vertical = (8 * s).dp),
                verticalArrangement = Arrangement.spacedBy((5 * s).dp)
            ) {
                rows.forEach { (label, value) ->
                    if (label == "Address") {
                        Column {
                            DText("ADDRESS", 7.5f * s, soft, FontWeight.Medium, 0.4f)
                            DText(value.ifBlank { "-" }, 11f * s, ink, FontWeight.SemiBold, maxLines = 3)
                        }
                    } else {
                        Row {
                            DText(label.uppercase(), 8f * s, soft, FontWeight.Medium, 0.4f, modifier = Modifier.width((96 * s).dp))
                            DText(value.ifBlank { "-" }, 11f * s, ink, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            DText(
                "Personal copy, not an official document",
                7.5f * s,
                soft,
                modifier = Modifier.padding(start = (14 * s).dp, bottom = (6 * s).dp)
            )
        }
    }
}

@Composable
private fun AadhaarBack(card: BankCard) {
    val father = card.extra["father"] ?: ""
    val address = (if (father.isNotBlank()) "S/O $father, " else "") + (card.extra["address"] ?: "")
    BackLayout(
        type = "Aadhaar",
        title = "ADDRESS",
        head = Color(0xFFC62828),
        ink = Color(0xFF1A1F2B),
        soft = Color(0xFF6B7280),
        rows = listOf(
            "Address" to address,
            "Pincode" to (card.extra["pincode"] ?: ""),
            "VID" to (card.extra["vid"] ?: "").chunked(4).joinToString(" "),
            "Aadhaar no." to card.number.chunked(4).joinToString(" ")
        ),
        extraTop = { s -> TricolourStrip(s) }
    )
}

@Composable
private fun PanBack(card: BankCard) {
    BackLayout(
        type = "PAN",
        title = "CARD DETAILS",
        head = Color(0xFF0B2A5B),
        ink = Color(0xFF0B2A5B),
        soft = Color(0xFF3F5C8C),
        rows = listOf(
            "PAN" to card.number.uppercase(),
            "Name" to card.holder.uppercase(),
            "Date of issue" to formatDate(card.extra["issued"] ?: ""),
            "Address" to (card.extra["address"] ?: "")
        )
    )
}

@Composable
private fun VoterBack(card: BankCard) {
    val pin = card.extra["pincode"] ?: ""
    val address = (card.extra["address"] ?: "") + if (pin.isNotBlank()) " - $pin" else ""
    BackLayout(
        type = "Voter ID",
        title = "ADDRESS AND CONSTITUENCY",
        head = Color(0xFF5B1A8A),
        ink = Color(0xFF5B1A4A),
        soft = Color(0xFF8A6A3A),
        rows = listOf(
            "Address" to address,
            "Constituency" to (card.extra["constituency"] ?: ""),
            "Date of issue" to formatDate(card.extra["issued"] ?: ""),
            "EPIC no." to card.number.uppercase()
        )
    )
}

@Composable
private fun LicenceBack(card: BankCard) {
    val pin = card.extra["pincode"] ?: ""
    val address = (card.extra["address"] ?: "") + if (pin.isNotBlank()) " - $pin" else ""
    BackLayout(
        type = "Driving Licence",
        title = "LICENCE DETAILS",
        head = Color(0xFF14501C),
        ink = Color(0xFF14501C),
        soft = Color(0xFF4F6F4A),
        rows = listOf(
            "Address" to address,
            "Vehicle classes" to (card.extra["classes"] ?: ""),
            "Authority" to (card.extra["authority"] ?: ""),
            "Valid till" to formatDate(card.extra["valid"] ?: ""),
            "DL no." to card.number.uppercase()
        )
    )
}
