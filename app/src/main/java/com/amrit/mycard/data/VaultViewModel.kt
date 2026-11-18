package com.amrit.mycard.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.security.MessageDigest
import java.security.SecureRandom

class VaultViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SecureStore(app)

    var vault by mutableStateOf(store.load())
        private set

    var unlocked by mutableStateOf(vault.settings.pinHash.isEmpty())
        private set

    private fun commit(v: Vault) {
        vault = v
        try {
            store.save(v)
        } catch (e: Exception) {
            // Keep the in-memory state even if the disk write fails.
        }
    }

    fun saveCard(card: BankCard) {
        val list = vault.cards
        val updated = if (list.any { it.id == card.id }) {
            list.map { if (it.id == card.id) card else it }
        } else {
            list + card
        }
        commit(vault.copy(cards = updated))
    }

    fun setCardArchived(id: String, archived: Boolean) {
        commit(vault.copy(cards = vault.cards.map { if (it.id == id) it.copy(archived = archived) else it }))
    }

    fun setPasswordArchived(id: String, archived: Boolean) {
        commit(vault.copy(passwords = vault.passwords.map { if (it.id == id) it.copy(archived = archived) else it }))
    }

    fun deleteCard(id: String) {
        commit(vault.copy(cards = vault.cards.filter { it.id != id }))
    }

    fun savePassword(entry: PasswordEntry) {
        val list = vault.passwords
        val updated = if (list.any { it.id == entry.id }) {
            list.map { if (it.id == entry.id) entry else it }
        } else {
            list + entry
        }
        commit(vault.copy(passwords = updated))
    }

    fun deletePassword(id: String) {
        commit(vault.copy(passwords = vault.passwords.filter { it.id != id }))
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        commit(vault.copy(settings = transform(vault.settings)))
    }

    fun setPin(pin: String?) {
        if (pin == null) {
            updateSettings { it.copy(pinSalt = "", pinHash = "") }
        } else {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
            updateSettings { it.copy(pinSalt = salt, pinHash = hash(salt, pin)) }
        }
    }

    fun tryUnlock(pin: String): Boolean {
        val s = vault.settings
        val ok = hash(s.pinSalt, pin) == s.pinHash
        if (ok) unlocked = true
        return ok
    }

    fun lock() {
        if (vault.settings.pinHash.isNotEmpty()) unlocked = false
    }

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
