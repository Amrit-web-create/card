package com.amrit.mycard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amrit.mycard.data.BankCard
import kotlin.math.abs

/**
 * Home screen of the cards. The only action here is Archive. Editing and deleting a card
 * is done from Settings and needs the PIN.
 */
@Composable
fun CardsScreen(
    cards: List<BankCard>,
    rotateMode: Int,
    onAdd: () -> Unit,
    onArchive: (String, Boolean) -> Unit
) {
    var showArchive by remember { mutableStateOf(false) }
    val active = cards.filter { !it.archived }
    val archived = cards.filter { it.archived }
    val inArchive = showArchive && archived.isNotEmpty()

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
            SubHeader("Archived", "Hidden from your main list", onBack = { showArchive = false })
            CardList(
                cards = archived,
                archivedCount = 0,
                archivedMode = true,
                rotateMode = rotateMode,
                onOpenArchive = {},
                onArchive = onArchive
            )
        } else {
            ScreenHeader(
                title = "My Cards",
                subtitle = when {
                    active.isEmpty() -> "Nothing saved yet"
                    rotateMode == 2 -> "${active.size} saved. Tap to flip, drag in any direction"
                    rotateMode == 1 -> "${active.size} saved. Tap to flip, drag sideways"
                    else -> "${active.size} saved. Tap a card to flip it"
                },
                action = { AddRoundButton(onAdd) }
            )
            if (active.isEmpty() && archived.isEmpty()) {
                EmptyCards(onAdd)
            } else {
                CardList(
                    cards = active,
                    archivedCount = archived.size,
                    archivedMode = false,
                    rotateMode = rotateMode,
                    onOpenArchive = { showArchive = true },
                    onArchive = onArchive
                )
            }
        }
    }
}

@Composable
private fun CardList(
    cards: List<BankCard>,
    archivedCount: Int,
    archivedMode: Boolean,
    rotateMode: Int,
    onOpenArchive: () -> Unit,
    onArchive: (String, Boolean) -> Unit
) {
    val listState = rememberLazyListState()
    val headerCount = if (!archivedMode && archivedCount > 0) 1 else 0

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (headerCount == 1) {
            item(key = "archive_row") {
                ArchiveRow("Archived", archivedCount, onOpenArchive)
            }
        }
        if (cards.isEmpty()) {
            item(key = "empty_hint") {
                Text(
                    "No active cards. Tap the plus button to add one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                )
            }
        }
        itemsIndexed(cards, key = { _, c -> c.id }) { index, card ->
            val lazyIndex = index + headerCount
            Column(
                Modifier.graphicsLayer {
                    val info = listState.layoutInfo
                    val item = info.visibleItemsInfo.firstOrNull { it.index == lazyIndex }
                    if (item != null) {
                        val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                        val itemCenter = item.offset + item.size / 2f
                        val f = ((itemCenter - center) / (info.viewportEndOffset / 2f)).coerceIn(-1f, 1f)
                        val a = abs(f)
                        scaleX = 1f - a * 0.07f
                        scaleY = 1f - a * 0.07f
                        alpha = 1f - a * 0.3f
                    }
                }
            ) {
                Flip3DCard(card, mode = rotateMode)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onArchive(card.id, !archivedMode) }) {
                        Text(if (archivedMode) "Restore" else "Archive")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCards(onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.586f)
                .clip(RoundedCornerShape(22.dp))
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CardGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 44.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Add your first card",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Bank cards and ID cards are stored only on this phone, encrypted with a key held in the Android Keystore.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAdd, shape = RoundedCornerShape(18.dp)) {
            Text("Add card", modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        }
    }
}
