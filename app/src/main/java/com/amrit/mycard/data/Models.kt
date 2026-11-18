package com.amrit.mycard.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
    val archived: Boolean = false
)

data class PasswordEntry(
    val id: String = UUID.randomUUID().toString(),
    val appName: String = "",
    val username: String = "",
    val password: String = "",
    val colorIndex: Int = 0,
    val archived: Boolean = false
)

data class AppSettings(
    val themeMode: Int = 0,
    val accent: Int = 0,
    val secureScreen: Boolean = true,
    val rotate360: Boolean = true,
    val pinSalt: String = "",
    val pinHash: String = ""
)

data class Vault(
    val cards: List<BankCard> = emptyList(),
    val passwords: List<PasswordEntry> = emptyList(),
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
}

private fun JSONObject.toCard(): BankCard = BankCard(
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
    archived = optBoolean("archived", false)
)

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

fun Vault.toJsonString(): String {
    val root = JSONObject()
    val ca = JSONArray()
    cards.forEach { ca.put(it.toJson()) }
    val pa = JSONArray()
    passwords.forEach { pa.put(it.toJson()) }
    root.put("cards", ca)
    root.put("passwords", pa)
    root.put(
        "settings",
        JSONObject().apply {
            put("themeMode", settings.themeMode)
            put("accent", settings.accent)
            put("secureScreen", settings.secureScreen)
            put("rotate360", settings.rotate360)
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
    val so = root.optJSONObject("settings") ?: JSONObject()
    return Vault(
        cards = (0 until ca.length()).map { ca.getJSONObject(it).toCard() },
        passwords = (0 until pa.length()).map { pa.getJSONObject(it).toPassword() },
        settings = AppSettings(
            themeMode = so.optInt("themeMode", 0),
            accent = so.optInt("accent", 0),
            secureScreen = so.optBoolean("secureScreen", true),
            rotate360 = so.optBoolean("rotate360", true),
            pinSalt = so.optString("pinSalt"),
            pinHash = so.optString("pinHash")
        )
    )
}
