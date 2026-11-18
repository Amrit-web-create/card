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
    val images = ImageStore(app, store)

    var vault by mutableStateOf(store.load())
        private set

    var unlocked by mutableStateOf(vault.settings.pinHash.isEmpty())
        private set

    init {
        try {
            val keep = vault.cards.map { it.photoId }.filter { it.isNotEmpty() }.toSet() + vault.photos.map { it.id }
            images.purgeExcept(keep)
        } catch (e: Exception) {
            // Cleaning leftovers is best effort only.
        }
    }

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
        val old = list.firstOrNull { it.id == card.id }
        if (old != null && old.photoId.isNotEmpty() && old.photoId != card.photoId) {
            images.delete(old.photoId)
        }
        val updated = if (old != null) list.map { if (it.id == card.id) card else it } else list + card
        commit(vault.copy(cards = updated))
    }

    fun setCardArchived(id: String, archived: Boolean) {
        commit(vault.copy(cards = vault.cards.map { if (it.id == id) it.copy(archived = archived) else it }))
    }

    fun deleteCard(id: String) {
        val old = vault.cards.firstOrNull { it.id == id }
        if (old != null) images.delete(old.photoId)
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

    fun setPasswordArchived(id: String, archived: Boolean) {
        commit(vault.copy(passwords = vault.passwords.map { if (it.id == id) it.copy(archived = archived) else it }))
    }

    fun deletePassword(id: String) {
        commit(vault.copy(passwords = vault.passwords.filter { it.id != id }))
    }

    fun addPhoto(item: PhotoItem) {
        commit(vault.copy(photos = listOf(item) + vault.photos))
    }

    fun bumpPhoto(id: String) {
        commit(vault.copy(photos = vault.photos.map { if (it.id == id) it.copy(rev = it.rev + 1) else it }))
    }

    fun setPhotoArchived(id: String, archived: Boolean) {
        commit(vault.copy(photos = vault.photos.map { if (it.id == id) it.copy(archived = archived) else it }))
    }

    fun deletePhoto(id: String) {
        images.delete(id)
        commit(vault.copy(photos = vault.photos.filter { it.id != id }))
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        commit(vault.copy(settings = transform(vault.settings)))
    }

    fun hasPin(): Boolean = vault.settings.pinHash.isNotEmpty()

    fun setPin(pin: String?) {
        if (pin == null) {
            updateSettings { it.copy(pinSalt = "", pinHash = "") }
        } else {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
            updateSettings { it.copy(pinSalt = salt, pinHash = hash(salt, pin)) }
        }
    }

    /** Checks a PIN without changing the lock state. */
    fun verifyPin(pin: String): Boolean {
        val s = vault.settings
        return s.pinHash.isNotEmpty() && hash(s.pinSalt, pin) == s.pinHash
    }

    fun tryUnlock(pin: String): Boolean {
        val ok = verifyPin(pin)
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
