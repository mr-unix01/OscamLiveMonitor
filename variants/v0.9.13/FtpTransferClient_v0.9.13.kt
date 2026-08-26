package com.mrunix.oscamlivemonitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.InputStream
import java.io.OutputStream

class FtpTransferClient {

    private var client: FTPClient? = null
    private val mutex = Mutex()

    val connected: Boolean
        get() = client?.isConnected == true

    suspend fun connect(
        host: String,
        port: Int,
        username: String,
        password: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching {
                    disconnectInternal()

                    val ftp = FTPClient()

                    ftp.connect(host, port)

                    if (!FTPReply.isPositiveCompletion(ftp.replyCode)) {
                        error(
                            "Server FTP: ${ftp.replyString.trim()}"
                        )
                    }

                    if (!ftp.login(username, password)) {
                        error(
                            "Login FTP fallito: ${ftp.replyString.trim()}"
                        )
                    }

                    ftp.enterLocalPassiveMode()
                    ftp.setFileType(FTP.BINARY_FILE_TYPE)

                    client = ftp
                }
            }
        }

    suspend fun openDirectory(
        path: String
    ): Result<RemoteDirectory> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching {
                    val ftp =
                        client
                            ?: error("FTP non connesso")

                    if (!ftp.isConnected) {
                        error("FTP non connesso")
                    }

                    if (!ftp.changeWorkingDirectory(path)) {
                        error(
                            ftp.replyString.trim()
                                .ifBlank {
                                    "Directory non disponibile"
                                }
                        )
                    }

                    val realPath =
                        ftp.printWorkingDirectory()
                            ?: path

                    val files =
                        ftp.listFiles(".")

                    val entries =
                        files
                            .filter {
                                it.name != "."
                            }
                            .map { file ->
                                RemoteFile(
                                    name = file.name,
                                    isDirectory =
                                      file.isDirectory ||
                                          (file.isSymbolicLink &&
                                          run {
                                              val entered =
                                                  ftp.changeWorkingDirectory(file.name)

                                              if (entered) {
                                                  ftp.changeWorkingDirectory(realPath)
                                              }

                                              entered
                                          }),
                                    size = file.size,
                                    permissions =
                                        permissionsToInt(file),
                                    permissionsText =
                                        permissionsToText(file)
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

    suspend fun uploadFile(
        name: String,
        inputStream: InputStream
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching {
                    val ftp =
                        client
                            ?: error("FTP non connesso")

                    if (!ftp.isConnected) {
                        error("FTP non connesso")
                    }

                    val ok =
                        inputStream.use {
                            ftp.storeFile(name, it)
                        }

                    if (!ok) {
                        error(
                            ftp.replyString.trim()
                                .ifBlank {
                                    "Upload FTP fallito"
                                }
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
            mutex.withLock {
                runCatching {
                    val ftp =
                        client
                            ?: error("FTP non connesso")

                    if (!ftp.isConnected) {
                        error("FTP non connesso")
                    }

                    val ok =
                        outputStream.use {
                            ftp.retrieveFile(name, it)
                        }

                    if (!ok) {
                        error(
                            ftp.replyString.trim()
                                .ifBlank {
                                    "Download FTP fallito"
                                }
                        )
                    }
                }
            }
        }


    suspend fun changePermissions(
        name: String,
        permissions: Int
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching {
                    val ftp =
                        client
                            ?: error("FTP non connesso")

                    if (!ftp.isConnected) {
                        error("FTP non connesso")
                    }

                    val mode =
                        permissions.toString(8)

                    val ok =
                        ftp.sendSiteCommand(
                            "CHMOD $mode $name"
                        )

                    if (!ok) {
                        error(
                            ftp.replyString.trim()
                                .ifBlank {
                                    "Cambio permessi FTP fallito"
                                }
                        )
                    }
                }
            }
        }

    fun disconnect() {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        val ftp = client

        if (ftp != null) {
            runCatching {
                if (ftp.isConnected) {
                    ftp.logout()
                }
            }

            runCatching {
                if (ftp.isConnected) {
                    ftp.disconnect()
                }
            }
        }

        client = null
    }

    private fun permissionsToInt(
        file: FTPFile
    ): Int {
        fun triplet(access: Int): Int {
            var value = 0

            if (
                file.hasPermission(
                    access,
                    FTPFile.READ_PERMISSION
                )
            ) {
                value += 4
            }

            if (
                file.hasPermission(
                    access,
                    FTPFile.WRITE_PERMISSION
                )
            ) {
                value += 2
            }

            if (
                file.hasPermission(
                    access,
                    FTPFile.EXECUTE_PERMISSION
                )
            ) {
                value += 1
            }

            return value
        }

        val owner =
            triplet(FTPFile.USER_ACCESS)
        val group =
            triplet(FTPFile.GROUP_ACCESS)
        val world =
            triplet(FTPFile.WORLD_ACCESS)

        return (owner shl 6) or
            (group shl 3) or
            world
    }

    private fun permissionsToText(
        file: FTPFile
    ): String {
        fun part(access: Int): String =
            buildString {
                append(
                    if (
                        file.hasPermission(
                            access,
                            FTPFile.READ_PERMISSION
                        )
                    ) "r" else "-"
                )

                append(
                    if (
                        file.hasPermission(
                            access,
                            FTPFile.WRITE_PERMISSION
                        )
                    ) "w" else "-"
                )

                append(
                    if (
                        file.hasPermission(
                            access,
                            FTPFile.EXECUTE_PERMISSION
                        )
                    ) "x" else "-"
                )
            }

        return part(FTPFile.USER_ACCESS) +
            part(FTPFile.GROUP_ACCESS) +
            part(FTPFile.WORLD_ACCESS)
    }
}
