package com.amrit.mycard.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.amrit.mycard.data.ImageStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CropResult(val bytes: ByteArray, val secondary: Boolean)

class CropRequest(
    val bitmap: Bitmap,
    val title: String,
    val primaryLabel: String,
    val secondaryLabel: String?,
    val initialAspect: Int,
    val deferred: CompletableDeferred<CropResult?>
)

private class AspectOption(val label: String, val ratio: Float?)

private val Aspects = listOf(
    AspectOption("Original", null),
    AspectOption("Square", 1f),
    AspectOption("3:4", 0.75f),
    AspectOption("4:3", 1.3333f),
    AspectOption("16:9", 1.7778f),
    AspectOption("Card", 1.586f)
)

/**
 * Full screen cropper. Pinch to zoom and drag to move the photo under the frame,
 * pick a frame shape, rotate if needed. Always offers a way to save without cropping.
 */
@Composable
fun CropScreen(req: CropRequest) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var bmp by remember(req) { mutableStateOf(req.bitmap) }
    var aspectIndex by remember(req) { mutableIntStateOf(req.initialAspect.coerceIn(0, Aspects.lastIndex)) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offX by remember { mutableFloatStateOf(0f) }
    var offY by remember { mutableFloatStateOf(0f) }
    var busy by remember { mutableStateOf(false) }

    BackHandler(enabled = true) { req.deferred.complete(null) }

    val pad = with(density) { 24.dp.toPx() }
    val iw = bmp.width.toFloat()
    val ih = bmp.height.toFloat()
    val availW = max(1f, box.width - 2 * pad)
    val availH = max(1f, box.height - 2 * pad)
    val ratio = Aspects[aspectIndex].ratio ?: (iw / ih)
    var fw = availW
    var fh = fw / ratio
    if (fh > availH) {
        fh = availH
        fw = fh * ratio
    }
    val minScale = max(fw / iw, fh / ih)

    LaunchedEffect(aspectIndex, bmp, box) {
        scale = minScale
        offX = 0f
        offY = 0f
    }

    val image = remember(bmp) { bmp.asImageBitmap() }

    fun doCrop(): ByteArray {
        val frameLeft = (box.width - fw) / 2f
        val frameTop = (box.height - fh) / 2f
        val imgLeft = box.width / 2f + offX - iw * scale / 2f
        val imgTop = box.height / 2f + offY - ih * scale / 2f
        var sx = ((frameLeft - imgLeft) / scale).roundToInt()
        var sy = ((frameTop - imgTop) / scale).roundToInt()
        var sw = (fw / scale).roundToInt()
        var sh = (fh / scale).roundToInt()
        sx = sx.coerceIn(0, bmp.width - 1)
        sy = sy.coerceIn(0, bmp.height - 1)
        sw = sw.coerceIn(1, bmp.width - sx)
        sh = sh.coerceIn(1, bmp.height - sy)
        return ImageStore.toJpeg(Bitmap.createBitmap(bmp, sx, sy, sw, sh))
    }

    fun finish(secondary: Boolean, cropIt: Boolean) {
        if (busy) return
        busy = true
        val current = bmp
        scope.launch {
            val bytes = withContext(Dispatchers.Default) {
                if (cropIt) doCrop() else ImageStore.toJpeg(current)
            }
            req.deferred.complete(CropResult(bytes, secondary))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { req.deferred.complete(null) }) { Text("Cancel", color = Color.White) }
            Text(
                req.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            TextButton(onClick = {
                val m = Matrix().apply { postRotate(90f) }
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }) { Text("Rotate", color = Color.White) }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { box = it }
                .pointerInput(minScale, fw, fh, iw, ih) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(minScale, minScale * 8f)
                        val maxX = max(0f, (iw * scale - fw) / 2f)
                        val maxY = max(0f, (ih * scale - fh) / 2f)
                        offX = (offX + pan.x).coerceIn(-maxX, maxX)
                        offY = (offY + pan.y).coerceIn(-maxY, maxY)
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                if (box.width > 0 && box.height > 0) {
                    val w = iw * scale
                    val h = ih * scale
                    val left = size.width / 2f + offX - w / 2f
                    val top = size.height / 2f + offY - h / 2f
                    drawImage(
                        image,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bmp.width, bmp.height),
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(max(1, w.roundToInt()), max(1, h.roundToInt()))
                    )
                    val fl = (size.width - fw) / 2f
                    val ft = (size.height - fh) / 2f
                    val dim = Color.Black.copy(alpha = 0.62f)
                    drawRect(dim, Offset.Zero, Size(size.width, ft))
                    drawRect(dim, Offset(0f, ft + fh), Size(size.width, max(0f, size.height - ft - fh)))
                    drawRect(dim, Offset(0f, ft), Size(fl, fh))
                    drawRect(dim, Offset(fl + fw, ft), Size(max(0f, size.width - fl - fw), fh))
                    drawRect(Color.White, Offset(fl, ft), Size(fw, fh), style = Stroke(3f))
                    val grid = Color.White.copy(alpha = 0.35f)
                    for (i in 1..2) {
                        drawLine(grid, Offset(fl + fw * i / 3f, ft), Offset(fl + fw * i / 3f, ft + fh), strokeWidth = 1.5f)
                        drawLine(grid, Offset(fl, ft + fh * i / 3f), Offset(fl + fw, ft + fh * i / 3f), strokeWidth = 1.5f)
                    }
                    val corner = min(fw, fh) * 0.08f
                    val c = Color.White
                    val sw = 7f
                    drawLine(c, Offset(fl, ft), Offset(fl + corner, ft), strokeWidth = sw)
                    drawLine(c, Offset(fl, ft), Offset(fl, ft + corner), strokeWidth = sw)
                    drawLine(c, Offset(fl + fw, ft), Offset(fl + fw - corner, ft), strokeWidth = sw)
                    drawLine(c, Offset(fl + fw, ft), Offset(fl + fw, ft + corner), strokeWidth = sw)
                    drawLine(c, Offset(fl, ft + fh), Offset(fl + corner, ft + fh), strokeWidth = sw)
                    drawLine(c, Offset(fl, ft + fh), Offset(fl, ft + fh - corner), strokeWidth = sw)
                    drawLine(c, Offset(fl + fw, ft + fh), Offset(fl + fw - corner, ft + fh), strokeWidth = sw)
                    drawLine(c, Offset(fl + fw, ft + fh), Offset(fl + fw, ft + fh - corner), strokeWidth = sw)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Aspects.forEachIndexed { i, a ->
                val selected = i == aspectIndex
                Box(
                    Modifier
                        .background(
                            if (selected) Color.White else Color.White.copy(alpha = 0.14f),
                            RoundedCornerShape(50)
                        )
                        .clickable { aspectIndex = i }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        a.label,
                        color = if (selected) Color.Black else Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (req.secondaryLabel != null) {
                    OutlinedButton(
                        onClick = { finish(secondary = true, cropIt = true) },
                        enabled = !busy,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) { Text(req.secondaryLabel, color = Color.White) }
                }
                Button(
                    onClick = { finish(secondary = false, cropIt = true) },
                    enabled = !busy,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) { Text(req.primaryLabel, fontWeight = FontWeight.Bold) }
            }
            TextButton(
                onClick = { finish(secondary = false, cropIt = false) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save without cropping", color = Color.White.copy(alpha = 0.8f)) }
        }
    }
}
