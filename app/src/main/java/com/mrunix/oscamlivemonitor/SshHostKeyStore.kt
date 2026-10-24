package com.mrunix.oscamlivemonitor

import android.content.Context
import android.util.Base64
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.UserInfo
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

data class SshHostKeyCandidate(
    val alias: String,
    val type: String,
    val keyBase64: String,
    val fingerprint: String
)

class SshHostKeyVerificationException(
    val candidate: SshHostKeyCandidate,
    val changed: Boolean
) : Exception(
    if (changed) {
        "La chiave SSH del server è cambiata"
    } else {
        "Host SSH non ancora attendibile"
    }
)

class SshHostKeyStore(
    context: Context
) : HostKeyRepository {

    private val preferences =
        context.getSharedPreferences(
            "ssh_known_hosts",
            Context.MODE_PRIVATE
        )

    private val pendingKeys =
        ConcurrentHashMap<String, SshHostKeyCandidate>()

    override fun check(
        host: String,
        key: ByteArray
    ): Int {
        val encoded =
            Base64.encodeToString(
                key,
                Base64.NO_WRAP
            )

        val saved =
            preferences.getString(
                preferenceKey(host),
                null
            )

        return when {
            saved == null -> {
                pendingKeys[host] =
                    createCandidate(
                        alias = host,
                        key = key
                    )

                HostKeyRepository.NOT_INCLUDED
            }

            saved == encoded -> {
                pendingKeys.remove(host)
                HostKeyRepository.OK
            }

            else -> {
                pendingKeys[host] =
                    createCandidate(
                        alias = host,
                        key = key
                    )

                HostKeyRepository.CHANGED
            }
        }
    }

    override fun add(
        hostkey: HostKey,
        ui: UserInfo?
    ) {
        val rawKey =
            Base64.decode(
                hostkey.key,
                Base64.DEFAULT
            )

        trust(
            alias = hostkey.host,
            keyBase64 =
                Base64.encodeToString(
                    rawKey,
                    Base64.NO_WRAP
                )
        )
    }

    override fun remove(
        host: String,
        type: String?
    ) {
        forget(host)
    }

    override fun remove(
        host: String,
        type: String?,
        key: ByteArray?
    ) {
        if (key == null) {
            forget(host)
            return
        }

        val saved =
            preferences.getString(
                preferenceKey(host),
                null
            )

        val encoded =
            Base64.encodeToString(
                key,
                Base64.NO_WRAP
            )

        if (saved == encoded) {
            forget(host)
        }
    }

    override fun getKnownHostsRepositoryID(): String {
        return "OscamLiveMonitor SSH known hosts"
    }

    override fun getHostKey(): Array<HostKey> {
        return preferences.all
            .mapNotNull { (key, value) ->
                if (
                    !key.startsWith(PREFERENCE_PREFIX) ||
                    value !is String
                ) {
                    return@mapNotNull null
                }

                runCatching {
                    val alias =
                        decodeAlias(
                            key.removePrefix(
                                PREFERENCE_PREFIX
                            )
                        )

                    HostKey(
                        alias,
                        Base64.decode(
                            value,
                            Base64.DEFAULT
                        )
                    )
                }.getOrNull()
            }
            .toTypedArray()
    }

    override fun getHostKey(
        host: String?,
        type: String?
    ): Array<HostKey> {
        return getHostKey()
            .filter { hostKey ->
                (host == null ||
                    hostKey.host.equals(
                        host,
                        ignoreCase = true
                    )) &&
                    (type == null ||
                        hostKey.type == type)
            }
            .toTypedArray()
    }

    fun pending(
        alias: String
    ): SshHostKeyCandidate? {
        return pendingKeys[alias]
    }

    fun trust(
        alias: String,
        keyBase64: String
    ) {
        preferences.edit()
            .putString(
                preferenceKey(alias),
                keyBase64
            )
            .apply()

        pendingKeys.remove(alias)
    }

    fun forget(
        alias: String
    ) {
        preferences.edit()
            .remove(
                preferenceKey(alias)
            )
            .apply()

        pendingKeys.remove(alias)
    }

    private fun createCandidate(
        alias: String,
        key: ByteArray
    ): SshHostKeyCandidate {
        val type =
            runCatching {
                HostKey(alias, key).type
            }.getOrDefault("SSH")

        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(key)

        val fingerprint =
            "SHA256:" +
                Base64.encodeToString(
                    digest,
                    Base64.NO_WRAP or
                        Base64.NO_PADDING
                )

        return SshHostKeyCandidate(
            alias = alias,
            type = type,
            keyBase64 =
                Base64.encodeToString(
                    key,
                    Base64.NO_WRAP
                ),
            fingerprint = fingerprint
        )
    }

    private fun preferenceKey(
        alias: String
    ): String {
        val encoded =
            Base64.encodeToString(
                alias.toByteArray(
                    Charsets.UTF_8
                ),
                Base64.URL_SAFE or
                    Base64.NO_WRAP or
                    Base64.NO_PADDING
            )

        return PREFERENCE_PREFIX + encoded
    }

    private fun decodeAlias(
        encoded: String
    ): String {
        return String(
            Base64.decode(
                encoded,
                Base64.URL_SAFE or
                    Base64.NO_WRAP or
                    Base64.NO_PADDING
            ),
            Charsets.UTF_8
        )
    }

    companion object {
        private const val PREFERENCE_PREFIX =
            "host_"

        fun alias(
            host: String,
            port: Int
        ): String {
            val cleanHost = host.trim()

            return if (port == 22) {
                cleanHost
            } else {
                "[$cleanHost]:$port"
            }
        }
    }
}
