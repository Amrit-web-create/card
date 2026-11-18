package com.amrit.mycard.ui

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
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
import com.amrit.mycard.data.DocTypes

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

private class DateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val t = text.text
        val out = StringBuilder()
        t.forEachIndexed { i, c ->
            if (i == 2 || i == 4) out.append('/')
            out.append(c)
        }
        val n = t.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 4 -> offset + 1
                else -> offset + 2
            }

            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 5 -> offset - 1
                else -> offset - 2
            }.coerceIn(0, n)
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}

@Composable
fun AddCardScreen(
    initial: BankCard?,
    rotateMode: Int,
    onPickPhoto: ((String) -> Unit) -> Unit,
    onSave: (BankCard) -> Unit,
    onBack: () -> Unit
) {
    var type by rememberSaveable { mutableStateOf(initial?.type ?: "Debit") }
    var bank by rememberSaveable { mutableStateOf(initial?.bank ?: "") }
    var holder by rememberSaveable { mutableStateOf(initial?.holder ?: "") }
    var number by rememberSaveable { mutableStateOf(initial?.number ?: "") }
    var expiry by rememberSaveable { mutableStateOf(initial?.expiry?.filter { it.isDigit() } ?: "") }
    var cvv by rememberSaveable { mutableStateOf(initial?.cvv ?: "") }
    var network by rememberSaveable { mutableStateOf(initial?.network ?: "Visa") }
    var international by rememberSaveable { mutableStateOf(initial?.international ?: false) }
    var design by rememberSaveable { mutableIntStateOf(initial?.design ?: 0) }
    var showHolder by rememberSaveable { mutableStateOf(initial?.showHolder ?: true) }
    var photoId by rememberSaveable { mutableStateOf(initial?.photoId ?: "") }
    val extras = remember {
        mutableStateMapOf<String, String>().also { m -> initial?.extra?.let { m.putAll(it) } }
    }
    var error by remember { mutableStateOf<String?>(null) }
    var flipped by remember { mutableStateOf(false) }

    val isDoc = type in DocTypes
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
        archived = initial?.archived ?: false,
        showHolder = showHolder,
        photoId = if (isDoc) photoId else "",
        extra = if (isDoc) extras.toMap() else emptyMap()
    )

    fun valueOf(key: String): String = when (key) {
        "holder" -> holder
        "number" -> number
        else -> extras[key] ?: ""
    }

    fun setValueOf(key: String, v: String) {
        when (key) {
            "holder" -> holder = v
            "number" -> number = v
            else -> extras[key] = v
        }
    }

    fun validateBank(): String? {
        if (bank.isBlank()) return "Enter the bank name"
        if (showHolder && holder.isBlank()) return "Enter the card holder name or turn the holder name off"
        if (number.length < 13) return "Card number needs at least 13 digits"
        if (expiry.length != 4) return "Enter expiry as MM/YY"
        val month = expiry.substring(0, 2).toIntOrNull() ?: 0
        if (month !in 1..12) return "Expiry month must be 01 to 12"
        if (cvv.isNotEmpty() && cvv.length < 3) return "CVV needs 3 or 4 digits"
        return null
    }

    fun validateDoc(): String? {
        if (holder.isBlank()) return "Enter the name"
        when (type) {
            "Aadhaar" -> if (number.length != 12) return "Aadhaar number must be 12 digits"
            "PAN" -> if (!Regex("[A-Z]{5}[0-9]{4}[A-Z]").matches(number)) return "PAN number looks like ABCDE1234F"
            "Voter ID" -> if (number.length < 8) return "Enter the EPIC number"
            else -> if (number.length < 8) return "Enter the licence number"
        }
        for (f in docFields(type)) {
            val v = extras[f.key] ?: ""
            if (f.kind == K_DATE && v.isNotEmpty() && !isValidDate(v)) return "${f.label}: enter as DD/MM/YYYY"
            if (f.key == "pincode" && v.isNotEmpty() && v.length != 6) return "Pincode must be 6 digits"
            if (f.key == "vid" && v.isNotEmpty() && v.length != 16) return "VID must be 16 digits"
        }
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
        SubHeader(
            if (initial == null) "Add card" else "Edit card",
            if (initial == null) "Bank card or ID card" else type,
            onBack
        )

        Box(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
            Flip3DCard(preview, flipped = flipped, mode = rotateMode)
        }
        Text(
            "Live preview. Tap to flip, drag to rotate.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Column(
            Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (initial == null) {
                Text("Card type", fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CardTypes.forEach { t ->
                        SelectPill(t, type == t) {
                            if (t != type) {
                                type = t
                                number = ""
                                extras.clear()
                                error = null
                                flipped = false
                            }
                        }
                    }
                }
            }

            if (!isDoc) {
                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it.take(24) },
                    label = { Text("Bank name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) flipped = false }
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it.filter { c -> c.isDigit() }.take(19) },
                    label = { Text("Card number") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = GroupTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) flipped = false }
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
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { if (it.isFocused) flipped = false }
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
                            .onFocusChanged { if (it.isFocused) flipped = true }
                    )
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
                        Text("Show holder name on card", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Real cards show only the name, no label",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = showHolder, onCheckedChange = { showHolder = it })
                }
                if (showHolder) {
                    OutlinedTextField(
                        value = holder,
                        onValueChange = { holder = it.take(26) },
                        label = { Text("Card holder name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (it.isFocused) flipped = false }
                    )
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
            } else {
                docFields(type).forEach { f ->
                    val focusMod = Modifier.onFocusChanged { if (it.isFocused) flipped = f.back }
                    when (f.kind) {
                        K_GENDER -> {
                            Text(f.label, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("Male", "Female", "Other").forEach { g ->
                                    SelectPill(g, valueOf(f.key) == g) { setValueOf(f.key, g) }
                                }
                            }
                        }
                        K_MULTI -> OutlinedTextField(
                            value = valueOf(f.key),
                            onValueChange = { setValueOf(f.key, it.take(f.max)) },
                            label = { Text(f.label) },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth().then(focusMod)
                        )
                        K_DATE -> OutlinedTextField(
                            value = valueOf(f.key),
                            onValueChange = { setValueOf(f.key, it.filter { c -> c.isDigit() }.take(f.max)) },
                            label = { Text(f.label) },
                            placeholder = { Text("DD/MM/YYYY") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = DateTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().then(focusMod)
                        )
                        K_DIGITS -> OutlinedTextField(
                            value = valueOf(f.key),
                            onValueChange = { setValueOf(f.key, it.filter { c -> c.isDigit() }.take(f.max)) },
                            label = { Text(f.label) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = if (f.max >= 12) GroupTransformation() else VisualTransformation.None,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().then(focusMod)
                        )
                        K_ALNUM -> OutlinedTextField(
                            value = valueOf(f.key),
                            onValueChange = {
                                setValueOf(f.key, it.filter { c -> c.isLetterOrDigit() }.uppercase().take(f.max))
                            },
                            label = { Text(f.label) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth().then(focusMod)
                        )
                        else -> OutlinedTextField(
                            value = valueOf(f.key),
                            onValueChange = { setValueOf(f.key, it.take(f.max)) },
                            label = { Text(f.label) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth().then(focusMod)
                        )
                    }
                }

                Text("Photo", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val img by rememberVaultImage(photoId, 0, 480)
                    val image = img
                    Box(
                        Modifier
                            .size(78.dp, 98.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (image != null) {
                            Image(
                                bitmap = image,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            PhotoGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 30.dp)
                        }
                    }
                    Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { onPickPhoto { id -> photoId = id } },
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(if (photoId.isEmpty()) "Choose from gallery" else "Change photo") }
                        if (photoId.isNotEmpty()) {
                            TextButton(onClick = { photoId = "" }) {
                                Text("Remove photo", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                Text(
                    "You can crop the photo right after picking it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = {
                    val problem = if (isDoc) validateDoc() else validateBank()
                    if (problem != null) {
                        error = problem
                    } else if (isDoc) {
                        onSave(
                            preview.copy(
                                bank = "",
                                expiry = "",
                                cvv = "",
                                extra = extras.filterValues { it.isNotBlank() }
                            )
                        )
                    } else {
                        onSave(preview.copy(expiry = formatExpiry(expiry), extra = emptyMap(), photoId = ""))
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
