package com.amrit.mycard.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amrit.mycard.data.PasswordEntry

@Composable
fun PasswordsScreen(
    entries: List<PasswordEntry>,
    onAdd: () -> Unit,
    onEdit: (PasswordEntry) -> Unit,
    onDelete: (String) -> Unit,
    onArchive: (String, Boolean) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<PasswordEntry?>(null) }
    var showArchive by remember { mutableStateOf(false) }
    val archived = entries.filter { it.archived }
    val active = entries.filter { !it.archived }
    val inArchive = showArchive && archived.isNotEmpty()
    val filtered = active.filter {
        query.isBlank() || it.appName.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true)
    }

    LaunchedEffect(archived.isEmpty()) {
        if (archived.isEmpty()) showArchive = false
    }
    BackHandler(enabled = inArchive) { showArchive = false }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        if (inArchive) {
            SubHeader("Archived passwords", "Hidden from your main list", onBack = { showArchive = false })
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(archived, key = { it.id }) { entry ->
                    PasswordTile(
                        entry = entry,
                        archivedMode = true,
                        onEdit = { onEdit(entry) },
                        onArchive = { onArchive(entry.id, false) },
                        onDelete = { toDelete = entry }
                    )
                }
            }
        } else {
            ScreenHeader(
                title = "Passwords",
                subtitle = if (active.isEmpty()) "Nothing saved yet" else "${active.size} saved, encrypted on this device",
                action = { AddRoundButton(onAdd) }
            )
            if (active.isEmpty() && archived.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(60.dp))
                    Box(
                        Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CardGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 40.dp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "No passwords yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Save an app name with its password. Everything stays offline and encrypted.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onAdd, shape = RoundedCornerShape(18.dp)) {
                        Text("Add password", modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
                    }
                }
            } else {
                if (active.size > 3) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(30) },
                        placeholder = { Text("Search passwords") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 130.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (archived.isNotEmpty() && query.isBlank()) {
                        item(key = "archive_row") {
                            ArchiveRow("Archived", archived.size) { showArchive = true }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item(key = "empty_hint") {
                            Text(
                                if (query.isBlank()) "No active passwords. Tap the plus button to add one." else "No match found.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp)
                            )
                        }
                    }
                    items(filtered, key = { it.id }) { entry ->
                        PasswordTile(
                            entry = entry,
                            archivedMode = false,
                            onEdit = { onEdit(entry) },
                            onArchive = { onArchive(entry.id, true) },
                            onDelete = { toDelete = entry }
                        )
                    }
                }
            }
        }
    }

    val target = toDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete password?") },
            text = { Text("The saved password for ${target.appName} will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    toDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PasswordTile(
    entry: PasswordEntry,
    archivedMode: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    val color = TileColors[entry.colorIndex.coerceIn(0, TileColors.lastIndex)]
    val strong = lerp(color, Color.Black, 0.25f)
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    fun copy() {
        clipboard.setText(AnnotatedString(entry.password))
        Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = !expanded
                        if (!expanded) reveal = false
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(color, lerp(color, Color.Black, 0.35f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        entry.appName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                ) {
                    Text(
                        entry.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        entry.username.ifBlank { "No username" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        "\u2022".repeat(entry.password.length.coerceIn(6, 12)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.16f))
                        .clickable { copy() }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text("Copy", color = strong, fontWeight = FontWeight.SemiBold)
                }
            }
            if (expanded) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(color.copy(alpha = 0.12f))
                            .clickable { reveal = !reveal }
                            .padding(16.dp)
                    ) {
                        Text(
                            if (reveal) entry.password else "Tap to reveal password",
                            fontFamily = if (reveal) FontFamily.Monospace else FontFamily.Default,
                            fontWeight = FontWeight.Medium,
                            color = if (reveal) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onEdit) { Text("Edit") }
                        TextButton(onClick = onArchive) { Text(if (archivedMode) "Restore" else "Archive") }
                        TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}
