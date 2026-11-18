package com.amrit.mycard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Brush
import com.amrit.mycard.data.BankCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.amrit.mycard.data.AppSettings

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    cards: List<BankCard>,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onSetPin: (String?) -> Unit,
    onEditCard: (BankCard) -> Unit,
    onArchiveCard: (String, Boolean) -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    val hasPin = settings.pinHash.isNotEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("Settings", "Make it yours")
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Appearance") {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelectPill("System", settings.themeMode == 0) { onChange { it.copy(themeMode = 0) } }
                    SelectPill("Light", settings.themeMode == 1) { onChange { it.copy(themeMode = 1) } }
                    SelectPill("Dark", settings.themeMode == 2) { onChange { it.copy(themeMode = 2) } }
                    SelectPill("Black", settings.themeMode == 3) { onChange { it.copy(themeMode = 3) } }
                }
                Text("Accent colour", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AccentColors.forEachIndexed { i, c ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .then(
                                        if (settings.accent == i) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { onChange { it.copy(accent = i) } }
                            )
                            Text(AccentNames[i], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            SectionCard("Cards") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Rotate card 360", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Drag a card sideways with your finger to spin it all the way around. It never spins by itself. Tap still flips it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = settings.rotate360, onCheckedChange = { v -> onChange { it.copy(rotate360 = v) } })
                }
            }

            SectionCard("Manage cards") {
                if (cards.isEmpty()) {
                    Text(
                        "No cards saved yet. Add one from the Cards tab.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                cards.forEach { card ->
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(48.dp, 31.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        Brush.linearGradient(
                                            CardDesigns[card.design.coerceIn(0, CardDesigns.lastIndex)].colors
                                        )
                                    )
                            )
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(start = 14.dp)
                            ) {
                                Text(card.bank.ifBlank { "Card" }, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(
                                    card.type + " ending " + card.number.takeLast(4) + if (card.archived) " - Archived" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onEditCard(card) }) { Text("Edit card") }
                            TextButton(onClick = { onArchiveCard(card.id, !card.archived) }) {
                                Text(if (card.archived) "Restore" else "Archive")
                            }
                        }
                    }
                }
            }

            SectionCard("Security") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Block screenshots", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Hides the app in recent apps and stops screen capture",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = settings.secureScreen, onCheckedChange = { v -> onChange { it.copy(secureScreen = v) } })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("App PIN", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (hasPin) "Enabled. Asked every time the app opens." else "Off. Anyone with your phone can open the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SelectPill(if (hasPin) "Change PIN" else "Set PIN", true) { showPinDialog = true }
                    if (hasPin) SelectPill("Remove PIN", false) { onSetPin(null) }
                }
            }

            SectionCard("About") {
                Text(
                    "My Card keeps everything offline. Cards and passwords are encrypted with AES-256-GCM, using a key that lives in the Android Keystore and never leaves this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                "Copyright \u00A9 Amrit Bharadwaj All Rights Reserved",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }
    }

    if (showPinDialog) {
        PinDialog(onDismiss = { showPinDialog = false }, onConfirm = {
            onSetPin(it)
            showPinDialog = false
        })
    }
}

@Composable
private fun PinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Set a 4 digit PIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("New PIN") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = again,
                    onValueChange = { again = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (pin.length != 4) error = "PIN must be 4 digits"
                            else if (pin != again) error = "PINs do not match"
                            else onConfirm(pin)
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Save") }
                }
            }
        }
    }
}
