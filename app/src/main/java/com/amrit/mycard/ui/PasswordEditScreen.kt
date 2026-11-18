package com.amrit.mycard.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amrit.mycard.data.PasswordEntry
import java.security.SecureRandom

private fun generatePassword(length: Int = 16): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#\$%&*?"
    val random = SecureRandom()
    return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
}

private fun passwordStrength(p: String): Int {
    if (p.isEmpty()) return 0
    var s = 0
    if (p.length >= 8) s++
    if (p.length >= 12) s++
    if (p.any { it.isDigit() } && p.any { it.isLetter() }) s++
    if (p.any { !it.isLetterOrDigit() } && p.any { it.isUpperCase() }) s++
    return s.coerceAtLeast(1)
}

@Composable
fun PasswordEditScreen(initial: PasswordEntry?, onSave: (PasswordEntry) -> Unit, onBack: () -> Unit) {
    var appName by rememberSaveable { mutableStateOf(initial?.appName ?: "") }
    var username by rememberSaveable { mutableStateOf(initial?.username ?: "") }
    var password by rememberSaveable { mutableStateOf(initial?.password ?: "") }
    var colorIndex by rememberSaveable { mutableIntStateOf(initial?.colorIndex ?: 0) }
    var show by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val color by animateColorAsState(TileColors[colorIndex.coerceIn(0, TileColors.lastIndex)], label = "pwColor")
    val strength = passwordStrength(password)
    val strengthLabel = when (strength) {
        0 -> "Enter a password"
        1 -> "Weak"
        2 -> "Fair"
        3 -> "Good"
        else -> "Strong"
    }
    val strengthColor = when (strength) {
        1 -> Color(0xFFE53935)
        2 -> Color(0xFFFFB300)
        3 -> Color(0xFF7CB342)
        else -> Color(0xFF00C985)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        SubHeader(
            if (initial == null) "Save password" else "Edit password",
            "Stored encrypted on this device",
            onBack
        )

        Box(
            Modifier
                .padding(horizontal = 22.dp, vertical = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(color, lerp(color, Color.Black, 0.4f))))
                .padding(22.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            appName.take(1).uppercase().ifEmpty { "?" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(
                            appName.ifBlank { "App name" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            maxLines = 1
                        )
                        Text(
                            username.ifBlank { "Username or email" },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height(26.dp))
                Text(
                    if (password.isEmpty()) "Password" else if (show) password else "\u2022".repeat(password.length.coerceIn(6, 18)),
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        Column(
            Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = appName,
                onValueChange = { appName = it.take(30) },
                label = { Text("App name") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.take(60) },
                label = { Text("Username or email (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.take(64) },
                label = { Text("Password") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { show = !show }) { Text(if (show) "Hide" else "Show") }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (i in 0 until 4) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (i < strength) strengthColor else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    strengthLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = {
                password = generatePassword()
                show = true
            }) { Text("Generate strong password") }

            Text("Colour", fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TileColors.forEachIndexed { i, c ->
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (colorIndex == i) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { colorIndex = i }
                    )
                }
            }

            if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = {
                    if (appName.isBlank()) {
                        error = "Enter the app name"
                    } else if (password.isEmpty()) {
                        error = "Enter the password"
                    } else {
                        onSave(
                            PasswordEntry(
                                id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                                appName = appName.trim(),
                                username = username.trim(),
                                password = password,
                                colorIndex = colorIndex,
                                archived = initial?.archived ?: false
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(if (initial == null) "Save password" else "Update password", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
