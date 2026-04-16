package com.jiadanni.launcher4d.util

import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CryptoUtilsTest {

    @Test
    fun encrypt_returnsEmptyString_onException() {
        // Mocking Base64.encodeToString to throw an exception
        Mockito.mockStatic(Base64::class.java).use { mockedBase64 ->
            mockedBase64.`when`<String> {
                Base64.encodeToString(Mockito.any(), anyInt())
            }.thenThrow(RuntimeException("Mocked exception"))

            val result = CryptoUtils.encrypt("any text")
            assertEquals("", result)
        }
    }

    @Test
    fun decrypt_returnsEmptyString_onException() {
        // Mocking Base64.decode to throw an exception
        Mockito.mockStatic(Base64::class.java).use { mockedBase64 ->
            mockedBase64.`when`<ByteArray> {
                Base64.decode(anyString(), anyInt())
            }.thenThrow(RuntimeException("Mocked exception"))

            val result = CryptoUtils.decrypt("any encrypted text")
            assertEquals("", result)
        }
    }

    @Test
    fun decrypt_returnsEmptyString_whenInputTooShort() {
        // Any string that decodes to less than 12 bytes
        val shortEncryptedText = "short" // Base64 for "short" is "c2hvcnQ="

        Mockito.mockStatic(Base64::class.java).use { mockedBase64 ->
            mockedBase64.`when`<ByteArray> {
                Base64.decode(anyString(), anyInt())
            }.thenReturn(ByteArray(11))

            val result = CryptoUtils.decrypt(shortEncryptedText)
            assertEquals("", result)
        }
    }
}
