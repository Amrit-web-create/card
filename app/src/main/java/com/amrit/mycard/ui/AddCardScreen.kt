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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.amrit.mycard.data.BankCard

private class GroupTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val t = text.text
        val out = StringBuilder()
        t.forEachIndexed { i, c ->
            if (i > 0 && i % 4 == 0) out.append(' ')
            out.append(c)
        }
        val n = t.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 0) 0 else offset + (offset - 1) / 4

            override fun transformedToOriginal(offset: Int): Int =
                (offset - offset / 5).coerceIn(0, n)
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}

private class ExpiryTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val t = text.text
        val out = if (t.length <= 2) t else t.substring(0, 2) + "/" + t.substring(2)
        val n = t.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 2) offset else offset + 1

            override fun transformedToOriginal(offset: Int): Int =
                (if (offset <= 2) offset else offset - 1).coerceIn(0, n)
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

@Composable
fun AddCardScreen(initial: BankCard?, rotate360: Boolean, onSave: (BankCard) -> Unit, onBack: () -> Unit) {
    var bank by rememberSaveable { mutableStateOf(initial?.bank ?: "") }
    var holder by rememberSaveable { mutableStateOf(initial?.holder ?: "") }
    var number by rememberSaveable { mutableStateOf(initial?.number ?: "") }
    var expiry by rememberSaveable { mutableStateOf(initial?.expiry?.filter { it.isDigit() } ?: "") }
    var cvv by rememberSaveable { mutableStateOf(initial?.cvv ?: "") }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: "Debit") }
    var network by rememberSaveable { mutableStateOf(initial?.network ?: "Visa") }
    var international by rememberSaveable { mutableStateOf(initial?.international ?: false) }
    var design by rememberSaveable { mutableIntStateOf(initial?.design ?: 0) }
    var error by remember { mutableStateOf<String?>(null) }
    var flipped by remember { mutableStateOf(false) }

    val cardId = remember { initial?.id ?: java.util.UUID.randomUUID().toString() }
    val preview = BankCard(
        id = cardId,
        bank = bank,
        holder = holder,
        number = number,
        expiry = expiry,
        cvv = cvv,
        type = type,
        network = network,
        international = international,
        design = design,
        archived = initial?.archived ?: false
    )

    fun validate(): String? {
        if (bank.isBlank()) return "Enter the bank name"
        if (holder.isBlank()) return "Enter the card holder name"
        if (number.length < 13) return "Card number needs at least 13 digits"
        if (expiry.length != 4) return "Enter expiry as MM/YY"
        val month = expiry.substring(0, 2).toIntOrNull() ?: 0
        if (month !in 1..12) return "Expiry month must be 01 to 12"
        if (cvv.isNotEmpty() && cvv.length < 3) return "CVV needs 3 or 4 digits"
        return null
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                if (initial == null) "Add card" else "Edit card",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 14.dp)
            )
        }

        Box(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
            Flip3DCard(preview, flipped = flipped, freeRotate = rotate360)
        }
        Text(
            "Live preview. Tap to flip, drag sideways to spin. CVV shows on the back.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Column(
            Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = bank,
                onValueChange = { bank = it.take(24) },
                label = { Text("Bank name") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = number,
                onValueChange = { number = it.filter { c -> c.isDigit() }.take(19) },
                label = { Text("Card number") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                visualTransformation = GroupTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = expiry,
                    onValueChange = { expiry = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Expiry") },
                    placeholder = { Text("MM/YY") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = ExpiryTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cvv,
                    onValueChange = { cvv = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("CVV (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { flipped = it.isFocused }
                )
            }
            OutlinedTextField(
                value = holder,
                onValueChange = { holder = it.take(26) },
                label = { Text("Card holder name") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Card type", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectPill("Debit", type == "Debit") { type = "Debit" }
                SelectPill("Credit", type == "Credit") { type = "Credit" }
            }

            Text("Network", fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CardNetworks.forEach { n -> SelectPill(n, network == n) { network = n } }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("International card", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Shown on the card face",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = international, onCheckedChange = { international = it })
            }

            Text("Card design", fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CardDesigns.forEachIndexed { i, d ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(d.colors))
                                .then(
                                    if (design == i) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                )
                                .clickable { design = i }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            d.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (design == i) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = {
                    val problem = validate()
                    if (problem != null) {
                        error = problem
                    } else {
                        onSave(preview.copy(expiry = formatExpiry(expiry)))
                    }
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    if (initial == null) "Save card" else "Update card",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
