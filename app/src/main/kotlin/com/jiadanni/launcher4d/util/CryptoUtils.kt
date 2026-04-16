package com.jiadanni.launcher4d.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoUtils {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "Launcher4dWeatherKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }

        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun encrypt(plaintext: String?): String {
        if (plaintext.isNullOrEmpty()) return ""
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            val ivAndEncrypted = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, ivAndEncrypted, iv.size, encryptedBytes.size)

            return Base64.encodeToString(ivAndEncrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            return ""
        }
    }

    fun decrypt(encryptedText: String?): String {
        if (encryptedText.isNullOrEmpty()) return ""
        try {
            val ivAndEncrypted = Base64.decode(encryptedText, Base64.DEFAULT)
            if (ivAndEncrypted.size < 12) return "" // Probably not encrypted or corrupted

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = ByteArray(12)
            System.arraycopy(ivAndEncrypted, 0, iv, 0, 12)
            val gcmParameterSpec = GCMParameterSpec(128, iv)

            val encryptedBytes = ByteArray(ivAndEncrypted.size - 12)
            System.arraycopy(ivAndEncrypted, 12, encryptedBytes, 0, encryptedBytes.size)

            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmParameterSpec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            return ""
        }
    }
}
