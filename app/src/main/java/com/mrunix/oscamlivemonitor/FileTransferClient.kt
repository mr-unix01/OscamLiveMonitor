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

data class RemoteProperties(
    val name: String,
    val path: String,
    val type: String,
    val size: Long,
    val permissions: Int,
    val permissionsText: String,
    val modifiedTimeMillis: Long?,
    val owner: String?,
    val group: String?,
    val fileCount: Int?,
    val directoryCount: Int?
)

class FileTransferClient {

    private var sftpSession: Session? = null
    private var sftpChannel: ChannelSftp? = null

    private var lastHost: String? = null
    private var lastPort: Int = 22
    private var lastUsername: String = ""
    private var lastPassword: String = ""
    private var currentPath: String = "/"

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
                lastHost = host
                lastPort = port
                lastUsername = username
                lastPassword = password
                currentPath = "/"

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

                session.setServerAliveInterval(30_000)
                session.setServerAliveCountMax(3)

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
                        connectedChannel()

                    channel.cd(path)

                    val realPath = channel.pwd()
                    currentPath = realPath

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
                        connectedChannel()

                    channel.cd(path)

                    val realPath = channel.pwd()
                    currentPath = realPath
                    realPath
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
                        connectedChannel()

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
                        connectedChannel()

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
                        connectedChannel()

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
                        connectedChannel()

                    channel.chmod(
                        permissions,
                        name
                    )
                }
            }
        }

    suspend fun getProperties(
        name: String
    ): Result<RemoteProperties> =
        withContext(Dispatchers.IO) {
            sftpMutex.withLock {
                runCatching {
                    val channel = connectedChannel()

                    val base =
                        channel.pwd()
                            .trimEnd('/')
                            .ifBlank { "/" }

                    val fullPath =
                        if (name == "..") {
                            base
                        } else if (base == "/") {
                            "/$name"
                        } else {
                            "$base/$name"
                        }

                    val attrs =
                        channel.lstat(fullPath)

                    var totalSize =
                        if (attrs.isDir) 0L
                        else attrs.size

                    var fileCount: Int? = null
                    var directoryCount: Int? = null

                    if (
                        attrs.isDir &&
                        !attrs.isLink
                    ) {
                        val stats =
                            countSftpDirectory(
                                channel,
                                fullPath
                            )

                        totalSize = stats.first
                        fileCount = stats.second
                        directoryCount = stats.third
                    }

                    RemoteProperties(
                        name = name,
                        path = fullPath,
                        type =
                            when {
                                attrs.isLink ->
                                    "Collegamento simbolico"

                                attrs.isDir ->
                                    "Cartella"

                                else ->
                                    "File"
                            },
                        size = totalSize,
                        permissions =
                            attrs.permissions and 0x1FF,
                        permissionsText =
                            attrs.permissionsString,
                        modifiedTimeMillis =
                            attrs.mTime
                                .toLong()
                                .times(1000L),
                        owner =
                            attrs.uId.toString(),
                        group =
                            attrs.gId.toString(),
                        fileCount = fileCount,
                        directoryCount =
                            directoryCount
                    )
                }
            }
        }

    private fun countSftpDirectory(
        channel: ChannelSftp,
        path: String
    ): Triple<Long, Int, Int> {
        var totalSize = 0L
        var files = 0
        var directories = 0

        @Suppress("UNCHECKED_CAST")
        val entries =
            channel.ls(path)
                as java.util.Vector<ChannelSftp.LsEntry>

        entries
            .filter {
                it.filename != "." &&
                it.filename != ".."
            }
            .forEach { entry ->
                val child =
                    "${path.trimEnd('/')}/${entry.filename}"

                when {
                    entry.attrs.isLink -> {
                        files++
                        totalSize += entry.attrs.size
                    }

                    entry.attrs.isDir -> {
                        directories++

                        val sub =
                            countSftpDirectory(
                                channel,
                                child
                            )

                        totalSize += sub.first
                        files += sub.second
                        directories += sub.third
                    }

                    else -> {
                        files++
                        totalSize += entry.attrs.size
                    }
                }
            }

        return Triple(
            totalSize,
            files,
            directories
        )
    }

    suspend fun renameEntry(
        oldName: String,
        newName: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            sftpMutex.withLock {
                runCatching {
                    validateEntryName(oldName)
                    validateEntryName(newName)

                    val channel = connectedChannel()

                    channel.rename(
                        oldName,
                        newName
                    )
                }
            }
        }

    suspend fun deleteFile(
        name: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            sftpMutex.withLock {
                runCatching {
                    validateEntryName(name)

                    val channel = connectedChannel()
                    channel.rm(name)
                }
            }
        }

    suspend fun deleteDirectory(
        name: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            sftpMutex.withLock {
                runCatching {
                    validateEntryName(name)

                    val channel = connectedChannel()

                    val base =
                        channel.pwd()
                            .trimEnd('/')
                            .ifBlank { "/" }

                    val path =
                        if (base == "/") {
                            "/$name"
                        } else {
                            "$base/$name"
                        }

                    val attrs =
                        channel.lstat(path)

                    if (attrs.isLink) {
                        channel.rm(path)
                    } else {
                        deleteDirectoryRecursive(
                            channel,
                            path
                        )
                    }
                }
            }
        }

    suspend fun createDirectory(
        name: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            sftpMutex.withLock {
                runCatching {
                    validateEntryName(name)

                    val channel = connectedChannel()
                    channel.mkdir(name)
                }
            }
        }

    private fun deleteDirectoryRecursive(
        channel: ChannelSftp,
        path: String
    ) {
        @Suppress("UNCHECKED_CAST")
        val entries =
            channel.ls(path)
                as java.util.Vector<ChannelSftp.LsEntry>

        entries
            .filter {
                it.filename != "." &&
                it.filename != ".."
            }
            .forEach { entry ->
                val child =
                    "${path.trimEnd('/')}/${entry.filename}"

                when {
                    entry.attrs.isLink ->
                        channel.rm(child)

                    entry.attrs.isDir ->
                        deleteDirectoryRecursive(
                            channel,
                            child
                        )

                    else ->
                        channel.rm(child)
                }
            }

        channel.rmdir(path)
    }

    private fun validateEntryName(
        name: String
    ) {
        if (
            name.isBlank() ||
            name == "." ||
            name == ".." ||
            name.contains("/")
        ) {
            error("Nome non valido")
        }
    }

    fun disconnect() {
        disconnectInternal()
    }

    private fun connectedChannel(): ChannelSftp {
        val session = sftpSession
        val channel = sftpChannel

        if (
            session?.isConnected == true &&
            channel?.isConnected == true
        ) {
            return channel
        }

        val host =
            lastHost
                ?: error("SFTP non connesso")

        disconnectInternal()

        val jsch = JSch()

        val newSession =
            jsch.getSession(
                lastUsername,
                host,
                lastPort
            )

        if (lastPassword.isNotEmpty()) {
            newSession.setPassword(
                lastPassword.toByteArray()
            )
        }

        newSession.setConfig(
            "StrictHostKeyChecking",
            "no"
        )

        newSession.setServerAliveInterval(30_000)
        newSession.setServerAliveCountMax(3)

        newSession.connect(10_000)

        val newChannel =
            newSession.openChannel("sftp") as ChannelSftp

        newChannel.connect(10_000)

        sftpSession = newSession
        sftpChannel = newChannel

        if (currentPath != "/") {
            val ripristinata =
                runCatching {
                    newChannel.cd(currentPath)
                }.isSuccess

            if (!ripristinata) {
                newChannel.cd("/")
                currentPath = "/"
            }
        }

        return newChannel
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
