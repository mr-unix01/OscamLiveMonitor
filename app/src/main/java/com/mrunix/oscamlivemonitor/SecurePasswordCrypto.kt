package com.mrunix.oscamlivemonitor

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurePasswordCrypto {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS =
        "oscam_live_monitor_passwords_v1"

    private const val TRANSFORMATION =
        "AES/GCM/NoPadding"

    private const val PREFIX = "enc:v1:"
    private const val GCM_TAG_LENGTH = 128

    fun isEncrypted(value: String): Boolean {
        return value.startsWith(PREFIX)
    }

    fun encrypt(value: String): String {
        if (value.isEmpty()) {
            return ""
        }

        if (isEncrypted(value)) {
            return value
        }

        val cipher =
            Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateKey()
        )

        val iv =
            Base64.encodeToString(
                cipher.iv,
                Base64.NO_WRAP
            )

        val encrypted =
            Base64.encodeToString(
                cipher.doFinal(
                    value.toByteArray(Charsets.UTF_8)
                ),
                Base64.NO_WRAP
            )

        return "$PREFIX$iv:$encrypted"
    }

    fun decrypt(value: String): String {
        if (
            value.isEmpty() ||
            !isEncrypted(value)
        ) {
            return value
        }

        return runCatching {
            val payload =
                value.removePrefix(PREFIX)

            val separator =
                payload.indexOf(':')

            require(separator > 0)

            val iv =
                Base64.decode(
                    payload.substring(
                        0,
                        separator
                    ),
                    Base64.NO_WRAP
                )

            val encrypted =
                Base64.decode(
                    payload.substring(
                        separator + 1
                    ),
                    Base64.NO_WRAP
                )

            val cipher =
                Cipher.getInstance(
                    TRANSFORMATION
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(
                    GCM_TAG_LENGTH,
                    iv
                )
            )

            String(
                cipher.doFinal(encrypted),
                Charsets.UTF_8
            )
        }.getOrElse {
            ""
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore =
            KeyStore.getInstance(KEYSTORE)
                .apply {
                    load(null)
                }

        val existingKey =
            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE
            )

        val specification =
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(256)
                .build()

        keyGenerator.init(specification)

        return keyGenerator.generateKey()
    }
}
