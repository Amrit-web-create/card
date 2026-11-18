package com.amrit.mycard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.amrit.mycard.data.AppSettings
import com.amrit.mycard.data.BankCard

private class PinGate(val title: String, val message: String, val action: () -> Unit)

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
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
    verifyPin: (String) -> Boolean,
    onEditCard: (BankCard) -> Unit,
    onDeleteCard: (String) -> Unit
) {
    var showSetDialog by remember { mutableStateOf(false) }
    var needPin by remember { mutableStateOf(false) }
    var gate by remember { mutableStateOf<PinGate?>(null) }
    val hasPin = settings.pinHash.isNotEmpty()

    fun requirePin(title: String, message: String, action: () -> Unit) {
        if (!hasPin) {
            needPin = true
        } else {
            gate = PinGate(title, message, action)
        }
    }

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
                    SelectPill("Cream", settings.themeMode == 4) { onChange { it.copy(themeMode = 4) } }
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

            SectionCard("Card rotation") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SelectPill("Off", settings.rotateMode == 0) { onChange { it.copy(rotateMode = 0) } }
                    SelectPill("Sideways", settings.rotateMode == 1) { onChange { it.copy(rotateMode = 1) } }
                    SelectPill("Free 3D", settings.rotateMode == 2) { onChange { it.copy(rotateMode = 2) } }
                }
                Text(
                    when (settings.rotateMode) {
                        2 -> "Drag a card in any direction to turn it in 3D, even diagonally for a curved turn. To scroll the list, drag beside or below the card. A card never spins by itself."
                        1 -> "Drag a card sideways to spin it all the way around. Scrolling the list works normally. A card never spins by itself."
                        else -> "Cards only flip when you tap them."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard("Manage cards") {
                Text(
                    "Editing or deleting a card needs your PIN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (cards.isEmpty()) {
                    Text(
                        "No cards saved yet. Add one from the Cards tab.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                cards.forEach { card ->
                    val colors = if (card.isDoc) docGradient(card.type)
                    else CardDesigns[card.design.coerceIn(0, CardDesigns.lastIndex)].colors
                    val title = if (card.isDoc) card.type else card.bank.ifBlank { "Card" }
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(48.dp, 31.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Brush.linearGradient(colors))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(7.dp))
                            )
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(start = 14.dp)
                            ) {
                                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(
                                    (if (card.isDoc) card.holder.ifBlank { card.type } else card.type) +
                                        " ending " + card.number.takeLast(4) +
                                        if (card.archived) " - Archived" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                requirePin("Edit card", "Enter your PIN to edit this card.") { onEditCard(card) }
                            }) { Text("Edit card") }
                            TextButton(onClick = {
                                requirePin("Delete card", "Enter your PIN to delete this card. This cannot be undone.") {
                                    onDeleteCard(card.id)
                                }
                            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
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
                Column {
                    Text("App PIN", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (hasPin) "On. Asked when the app opens, and before editing or deleting a card."
                        else "Off. Set a PIN to lock the app and to allow editing or deleting cards.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (hasPin) {
                        SelectPill("Change PIN", true) {
                            requirePin("Change PIN", "Enter your current PIN.") { showSetDialog = true }
                        }
                        SelectPill("Turn off PIN", false) {
                            requirePin("Turn off PIN", "Enter your PIN to turn it off.") { onSetPin(null) }
                        }
                    } else {
                        SelectPill("Set PIN", true) { showSetDialog = true }
                    }
                }
            }

            SectionCard("About") {
                Text(
                    "My Card keeps everything offline. Cards, passwords and photos are encrypted with AES-256-GCM, using a key that lives in the Android Keystore and never leaves this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Version 1.1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    if (needPin) {
        AlertDialog(
            onDismissRequest = { needPin = false },
            title = { Text("Set a PIN first") },
            text = { Text("Editing or deleting a card needs your PIN. Set a PIN now, then try again.") },
            confirmButton = {
                TextButton(onClick = {
                    needPin = false
                    showSetDialog = true
                }) { Text("Set PIN") }
            },
            dismissButton = { TextButton(onClick = { needPin = false }) { Text("Not now") } }
        )
    }

    if (showSetDialog) {
        SetPinDialog(
            onDismiss = { showSetDialog = false },
            onConfirm = {
                onSetPin(it)
                showSetDialog = false
            }
        )
    }

    val g = gate
    if (g != null) {
        PinPromptDialog(
            title = g.title,
            message = g.message,
            verify = verifyPin,
            onDismiss = { gate = null },
            onSuccess = {
                gate = null
                g.action()
            }
        )
    }
}

@Composable
private fun PinPromptDialog(
    title: String,
    message: String,
    verify: (String) -> Boolean,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter { c -> c.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("PIN") },
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
                            if (verify(pin)) {
                                onSuccess()
                            } else {
                                error = "Wrong PIN"
                                pin = ""
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Confirm") }
                }
            }
        }
    }
}

@Composable
private fun SetPinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
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
