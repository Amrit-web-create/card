package com.amrit.mycard.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

val DocTypes = listOf("Aadhaar", "PAN", "Voter ID", "Driving Licence")

data class BankCard(
    val id: String = UUID.randomUUID().toString(),
    val bank: String = "",
    val holder: String = "",
    val number: String = "",
    val expiry: String = "",
    val cvv: String = "",
    val type: String = "Debit",
    val network: String = "Visa",
    val international: Boolean = false,
    val design: Int = 0,
    val archived: Boolean = false,
    val showHolder: Boolean = true,
    val photoId: String = "",
    val extra: Map<String, String> = emptyMap()
) {
    val isDoc: Boolean get() = type in DocTypes
}

data class PasswordEntry(
    val id: String = UUID.randomUUID().toString(),
    val appName: String = "",
    val username: String = "",
    val password: String = "",
    val colorIndex: Int = 0,
    val archived: Boolean = false
)

data class PhotoItem(
    val id: String = UUID.randomUUID().toString(),
    val addedAt: Long = 0L,
    val archived: Boolean = false,
    val rev: Int = 0
)

data class AppSettings(
    val themeMode: Int = 0,
    val accent: Int = 0,
    val secureScreen: Boolean = true,
    /** 0 = off, 1 = sideways only, 2 = free 3D (any direction) */
    val rotateMode: Int = 2,
    val pinSalt: String = "",
    val pinHash: String = ""
)

data class Vault(
    val cards: List<BankCard> = emptyList(),
    val passwords: List<PasswordEntry> = emptyList(),
    val photos: List<PhotoItem> = emptyList(),
    val settings: AppSettings = AppSettings()
)

private fun BankCard.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("bank", bank)
    put("holder", holder)
    put("number", number)
    put("expiry", expiry)
    put("cvv", cvv)
    put("type", type)
    put("network", network)
    put("international", international)
    put("design", design)
    put("archived", archived)
    put("showHolder", showHolder)
    put("photoId", photoId)
    val ex = JSONObject()
    extra.forEach { (k, v) -> ex.put(k, v) }
    put("extra", ex)
}

private fun JSONObject.toCard(): BankCard {
    val ex = optJSONObject("extra")
    val extraMap = HashMap<String, String>()
    if (ex != null) {
        val it = ex.keys()
        while (it.hasNext()) {
            val k = it.next()
            extraMap[k] = ex.optString(k)
        }
    }
    return BankCard(
        id = optString("id", UUID.randomUUID().toString()),
        bank = optString("bank"),
        holder = optString("holder"),
        number = optString("number"),
        expiry = optString("expiry"),
        cvv = optString("cvv"),
        type = optString("type", "Debit"),
        network = optString("network", "Visa"),
        international = optBoolean("international", false),
        design = optInt("design", 0),
        archived = optBoolean("archived", false),
        showHolder = optBoolean("showHolder", true),
        photoId = optString("photoId"),
        extra = extraMap
    )
}

private fun PasswordEntry.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("appName", appName)
    put("username", username)
    put("password", password)
    put("colorIndex", colorIndex)
    put("archived", archived)
}

private fun JSONObject.toPassword(): PasswordEntry = PasswordEntry(
    id = optString("id", UUID.randomUUID().toString()),
    appName = optString("appName"),
    username = optString("username"),
    password = optString("password"),
    colorIndex = optInt("colorIndex", 0),
    archived = optBoolean("archived", false)
)

private fun PhotoItem.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("addedAt", addedAt)
    put("archived", archived)
    put("rev", rev)
}

private fun JSONObject.toPhoto(): PhotoItem = PhotoItem(
    id = optString("id"),
    addedAt = optLong("addedAt", 0L),
    archived = optBoolean("archived", false),
    rev = optInt("rev", 0)
)

fun Vault.toJsonString(): String {
    val root = JSONObject()
    val ca = JSONArray()
    cards.forEach { ca.put(it.toJson()) }
    val pa = JSONArray()
    passwords.forEach { pa.put(it.toJson()) }
    val ph = JSONArray()
    photos.forEach { ph.put(it.toJson()) }
    root.put("cards", ca)
    root.put("passwords", pa)
    root.put("photos", ph)
    root.put(
        "settings",
        JSONObject().apply {
            put("themeMode", settings.themeMode)
            put("accent", settings.accent)
            put("secureScreen", settings.secureScreen)
            put("rotateMode", settings.rotateMode)
            put("pinSalt", settings.pinSalt)
            put("pinHash", settings.pinHash)
        }
    )
    return root.toString()
}

fun parseVault(text: String): Vault {
    val root = JSONObject(text)
    val ca = root.optJSONArray("cards") ?: JSONArray()
    val pa = root.optJSONArray("passwords") ?: JSONArray()
    val ph = root.optJSONArray("photos") ?: JSONArray()
    val so = root.optJSONObject("settings") ?: JSONObject()
    val legacyRotate = if (so.optBoolean("rotate360", true)) 2 else 0
    return Vault(
        cards = (0 until ca.length()).map { ca.getJSONObject(it).toCard() },
        passwords = (0 until pa.length()).map { pa.getJSONObject(it).toPassword() },
        photos = (0 until ph.length()).map { ph.getJSONObject(it).toPhoto() },
        settings = AppSettings(
            themeMode = so.optInt("themeMode", 0),
            accent = so.optInt("accent", 0),
            secureScreen = so.optBoolean("secureScreen", true),
            rotateMode = so.optInt("rotateMode", legacyRotate),
            pinSalt = so.optString("pinSalt"),
            pinHash = so.optString("pinHash")
        )
    )
}
