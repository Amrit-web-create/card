package com.amrit.mycard.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(onSubmit: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "Del")
    )

    fun press(k: String) {
        if (k == "Del") {
            pin = pin.dropLast(1)
            return
        }
        if (k.isEmpty() || pin.length >= 4) return
        error = false
        pin += k
        if (pin.length == 4) {
            if (!onSubmit(pin)) {
                error = true
                pin = ""
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("My Card", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            if (error) "Wrong PIN, try again" else "Enter your PIN",
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 0 until 4) {
                val filled = i < pin.length
                val c by animateColorAsState(
                    if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    label = "dot"
                )
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(c)
                )
            }
        }
        Spacer(Modifier.height(36.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    row.forEach { k ->
                        Box(
                            Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    if (k.isEmpty()) MaterialTheme.colorScheme.background
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable(enabled = k.isNotEmpty()) { press(k) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                k,
                                style = if (k == "Del") MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
