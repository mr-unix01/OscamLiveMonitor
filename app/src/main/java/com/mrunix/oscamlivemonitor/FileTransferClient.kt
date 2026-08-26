package com.mrunix.oscamlivemonitor

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

data class RemoteFile(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val permissions: Int,
    val permissionsText: String
)

data class RemoteDirectory(
    val path: String,
    val entries: List<RemoteFile>
)

class FileTransferClient {

    private var sftpSession: Session? = null
    private var sftpChannel: ChannelSftp? = null

    private val sftpMutex = Mutex()

    val connected: Boolean
        get() =
            sftpSession?.isConnected == true &&
            sftpChannel?.isConnected == true

    suspend fun connectSftp(
        host: String,
        port: Int,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        sftpMutex.withLock {
            runCatching {
                disconnectInternal()

                val jsch = JSch()

                val session = jsch.getSession(
                    username,
                    host,
                    port
                )

                if (password.isNotEmpty()) {
                    session.setPassword(
                        password.toByteArray()
                    )
                }

                session.setConfig(
                    "StrictHostKeyChecking",
                    "no"
                )

                session.connect(10_000)

                val channel =
                    session.openChannel("sftp") as ChannelSftp

                channel.connect(10_000)

                sftpSession = session
                sftpChannel = channel
            }
        }
    }

    suspend fun openDirectory(
        path: String
    ): Result<RemoteDirectory> =
        withContext(Dispatchers.IO) {
            sftpMutex.withLock {
                runCatching {
                    val channel =
                        sftpChannel
                            ?: error("SFTP non connesso")

                    if (!channel.isConnected) {
                        error("Canale SFTP non connesso")
                    }

                    channel.cd(path)

                    val realPath = channel.pwd()

                    @Suppress("UNCHECKED_CAST")
                    val rawEntries =
                        channel.ls(".")
                            as java.util.Vector<ChannelSftp.LsEntry>

                    val entries =
                        rawEntries
                            .filter {
                                it.filename != "."
                            }
                            .map { entry ->
                                RemoteFile(
                                    name = entry.filename,
                                    isDirectory =
                                        entry.attrs.isDir ||
                                        (
                                            entry.attrs.isLink &&
                                            runCatching {
                                                channel.stat(
                                                    entry.filename
                                                ).isDir
                                            }.getOrDefault(false)
                                        ),
                                    size = entry.attrs.size,
                                    permissions =
                                        entry.attrs.permissions and 0x1FF,
                                    permissionsText =
                                        entry.attrs.permissionsString
                                )
                            }
                            .sortedWith(
                                compareByDescending<RemoteFile> {
                                    it.isDirectory
                                }.thenBy {
                                    it.name.lowercase()
                                }
                            )

                    RemoteDirectory(
                        path = realPath,
                        entries = entries
                    )
                }
            }
        }

    suspend fun changeDirectory(
        path: String
    ): Result<String> =
        withContext(Dispatchers.IO) {
            sftpMutex.withLock {
                runCatching {
                    val channel =
                        sftpChannel
                            ?: error("SFTP non connesso")

                    if (!channel.isConnected) {
                        error("Canale SFTP non connesso")
                    }

                    channel.cd(path)
                    channel.pwd()
                }
            }
        }

    suspend fun listDirectory(
        path: String
    ): Result<List<RemoteFile>> =
        withContext(Dispatchers.IO) {
            sftpMutex.withLock {
                runCatching {
                    val channel =
                        sftpChannel
                            ?: error("SFTP non connesso")

                    if (!channel.isConnected) {
                        error("Canale SFTP non connesso")
                    }

                    @Suppress("UNCHECKED_CAST")
                    val entries =
                        channel.ls(path)
                            as java.util.Vector<ChannelSftp.LsEntry>

                    entries
                        .filter {
                            it.filename != "."
                        }
                        .map { entry ->
                            RemoteFile(
                                name = entry.filename,
                                isDirectory = entry.attrs.isDir,
                                size = entry.attrs.size,
                                permissions =
                                    entry.attrs.permissions and 0x1FF,
                                permissionsText =
                                    entry.attrs.permissionsString
                            )
                        }
                        .sortedWith(
                            compareByDescending<RemoteFile> {
                                it.isDirectory
                            }.thenBy {
                                it.name.lowercase()
                            }
                        )
                }
            }
        }

    suspend fun uploadFile(
        name: String,
        inputStream: InputStream
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            sftpMutex.withLock {
                runCatching {
                    val channel =
                        sftpChannel
                            ?: error("SFTP non connesso")

                    if (!channel.isConnected) {
                        error("Canale SFTP non connesso")
                    }

                    inputStream.use {
                        channel.put(
                            it,
                            name,
                            ChannelSftp.OVERWRITE
                        )
                    }
                }
            }
        }


    suspend fun downloadFile(
        name: String,
        outputStream: OutputStream
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            sftpMutex.withLock {
                runCatching {
                    val channel =
                        sftpChannel
                            ?: error("SFTP non connesso")

                    if (!channel.isConnected) {
                        error("Canale SFTP non connesso")
                    }

                    outputStream.use {
                        channel.get(name, it)
                    }
                }
            }
        }


    suspend fun changePermissions(
        name: String,
        permissions: Int
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            sftpMutex.withLock {
                runCatching {
                    val channel =
                        sftpChannel
                            ?: error("SFTP non connesso")

                    if (!channel.isConnected) {
                        error("Canale SFTP non connesso")
                    }

                    channel.chmod(
                        permissions,
                        name
                    )
                }
            }
        }

    fun disconnect() {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        runCatching {
            sftpChannel?.disconnect()
        }

        runCatching {
            sftpSession?.disconnect()
        }

        sftpChannel = null
        sftpSession = null
    }
}
