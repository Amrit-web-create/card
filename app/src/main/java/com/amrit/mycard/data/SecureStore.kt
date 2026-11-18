package com.amrit.mycard.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the whole vault (and every photo) as AES-256-GCM encrypted data.
 * The key lives inside the Android Keystore and never leaves the device.
 */
class SecureStore(private val context: Context) {

    private val alias = "mycard_master_key"
    private val provider = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"

    private fun vaultFile() = File(context.filesDir, "vault.enc")

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(provider)
        ks.load(null)
        val existing = ks.getKey(alias, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    /** Returns iv (12 bytes) followed by the cipher text. */
    fun encryptBytes(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        return iv + cipher.doFinal(plain)
    }

    fun decryptBytes(data: ByteArray): ByteArray {
        val iv = data.copyOfRange(0, 12)
        val body = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(body)
    }

    fun load(): Vault {
        val file = vaultFile()
        if (!file.exists()) return Vault()
        return try {
            parseVault(String(decryptBytes(file.readBytes()), Charsets.UTF_8))
        } catch (e: Exception) {
            Vault()
        }
    }

    fun save(vault: Vault) {
        val data = encryptBytes(vault.toJsonString().toByteArray(Charsets.UTF_8))
        val tmp = File(context.filesDir, "vault.enc.tmp")
        tmp.writeBytes(data)
        val target = vaultFile()
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }
}
