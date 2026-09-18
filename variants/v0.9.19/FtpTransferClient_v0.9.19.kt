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

    private var lastHost: String? = null
    private var lastPort: Int = 21
    private var lastUsername: String = ""
    private var lastPassword: String = ""
    private var currentPath: String = "/"

    private var lastActivityElapsed: Long = 0L

    private val mutex = Mutex()

    private val directoryCache =
        mutableMapOf<String, RemoteDirectory>()

    private fun currentEntryPath(
        name: String
    ): String {
        val base =
            currentPath
                .trimEnd('/')
                .ifBlank { "/" }

        return if (base == "/") {
            "/$name"
        } else {
            "$base/$name"
        }
    }

    private fun resolveDirectoryPath(
        path: String
    ): String {
        if (path == "/") {
            return "/"
        }

        if (path.startsWith("/")) {
            return path.trimEnd('/')
                .ifBlank { "/" }
        }

        return currentEntryPath(
            path.trim('/')
        )
    }

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
                    lastHost = host
                    lastPort = port
                    lastUsername = username
                    lastPassword = password
                    currentPath = "/"
                    directoryCache.clear()

                    disconnectInternal()

                    val ftp = FTPClient()
                    ftp.connect(host, port)
                    if (!FTPReply.isPositiveCompletion(ftp.replyCode)) {
                        error(
                            "Server FTP: ${ftp.replyString.trim()}"
                        )
                    }
                    val loginOk =
                        ftp.login(username, password)
                    if (!loginOk) {
                        error(
                            "Login FTP fallito: ${ftp.replyString.trim()}"
                        )
                    }

                    ftp.enterLocalPassiveMode()
                    ftp.setFileType(FTP.BINARY_FILE_TYPE)
                    client = ftp
                    lastActivityElapsed =
                        android.os.SystemClock.elapsedRealtime()
                }
            }
        }

    suspend fun openDirectory(
        path: String
    ): Result<RemoteDirectory> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching {
                    val realPath =
                        resolveDirectoryPath(path)

                    /*
                     * Se torniamo in una directory già visitata,
                     * mostriamo subito la copia locale.
                     *
                     * Se si apre la directory corrente
                     * (pulsante Refresh), leggiamo il server.
                     */
                    if (realPath != currentPath) {
                        directoryCache[realPath]
                            ?.let { cached ->
                                currentPath = realPath
                                return@runCatching cached
                            }
                    }

                    val ftp =
                        connectedClient()
                    val files =
                        ftp.listFiles(realPath)
                    val entries =
                        files
                            .filter {
                                it.name != "."
                            }
                            .map { file ->
                                RemoteFile(
                                    name = file.name,
                                    isDirectory =
                                        file.isDirectory,
                                    size = file.size,
                                    permissions =
                                        permissionsToInt(file),
                                    permissionsText =
                                        permissionsToText(file),
                                    isSymbolicLink =
                                        file.isSymbolicLink
                                )
                            }
                            .sortedWith(
                                compareByDescending<RemoteFile> {
                                    it.isDirectory
                                }.thenBy {
                                    it.name.lowercase()
                                }
                            )

                    val directory =
                        RemoteDirectory(
                            path = realPath,
                            entries = entries
                        )

                    currentPath = realPath
                    directoryCache[realPath] = directory

                    directory
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
                        connectedClient()

                    directoryCache.clear()

                    val ok =
                        inputStream.use {
                            ftp.storeFile(currentEntryPath(name), it)
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
                        connectedClient()

                    val ok =
                        outputStream.use {
                            ftp.retrieveFile(currentEntryPath(name), it)
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
                        connectedClient()

                    directoryCache.clear()

                    val mode =
                        permissions.toString(8)

                    val ok =
                        ftp.sendSiteCommand(
                            "CHMOD $mode ${currentEntryPath(name)}"
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

    suspend fun getProperties(
        name: String
    ): Result<RemoteProperties> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching {
                    val ftp =
                        connectedClient()

                    val base =
                        currentPath
                            .trimEnd('/')
                            .ifBlank { "/" }

                    val fullPath =
                        if (base == "/") {
                            "/$name"
                        } else {
                            "$base/$name"
                        }

                    val file =
                        ftp.listFiles(base)
                            .firstOrNull {
                                it.name == name
                            }
                            ?: error(
                                "Impossibile leggere le proprietà"
                            )

                    var totalSize =
                        if (file.isDirectory) 0L
                        else file.size

                    var fileCount: Int? = null
                    var directoryCount: Int? = null

                    if (
                        file.isDirectory &&
                        !file.isSymbolicLink
                    ) {
                        val stats =
                            countFtpDirectory(
                                ftp,
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
                                file.isSymbolicLink ->
                                    "Collegamento simbolico"

                                file.isDirectory ->
                                    "Cartella"

                                else ->
                                    "File"
                            },
                        size = totalSize,
                        permissions =
                            permissionsToInt(file),
                        permissionsText =
                            permissionsToText(file),
                        modifiedTimeMillis =
                            file.timestamp
                                ?.timeInMillis,
                        owner =
                            file.user
                                ?.takeIf {
                                    it.isNotBlank()
                                },
                        group =
                            file.group
                                ?.takeIf {
                                    it.isNotBlank()
                                },
                        fileCount = fileCount,
                        directoryCount =
                            directoryCount
                    )
                }
            }
        }

    private fun countFtpDirectory(
        ftp: FTPClient,
        path: String
    ): Triple<Long, Int, Int> {
        var totalSize = 0L
        var files = 0
        var directories = 0

        val entries =
            ftp.listFiles(path)

        entries
            .filter {
                it.name != "." &&
                it.name != ".."
            }
            .forEach { entry ->
                val child =
                    "${path.trimEnd('/')}/${entry.name}"

                when {
                    entry.isSymbolicLink -> {
                        files++
                        totalSize += entry.size
                    }

                    entry.isDirectory -> {
                        directories++

                        val sub =
                            countFtpDirectory(
                                ftp,
                                child
                            )

                        totalSize += sub.first
                        files += sub.second
                        directories += sub.third
                    }

                    else -> {
                        files++
                        totalSize += entry.size
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
            mutex.withLock {
                runCatching {
                    validateEntryName(oldName)
                    validateEntryName(newName)

                    val ftp =
                        connectedClient()

                    directoryCache.clear()

                    if (
                        !ftp.rename(
                            currentEntryPath(oldName),
                            currentEntryPath(newName)
                        )
                    ) {
                        error(
                            ftp.replyString.trim()
                                .ifBlank {
                                    "Rinomina FTP fallita"
                                }
                        )
                    }
                }
            }
        }

    suspend fun deleteFile(
        name: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching {
                    validateEntryName(name)

                    val ftp =
                        connectedClient()

                    directoryCache.clear()

                    if (!ftp.deleteFile(currentEntryPath(name))) {
                        error(
                            ftp.replyString.trim()
                                .ifBlank {
                                    "Eliminazione FTP fallita"
                                }
                        )
                    }
                }
            }
        }

    suspend fun deleteDirectory(
        name: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching {
                    validateEntryName(name)

                    val ftp =
                        connectedClient()

                    directoryCache.clear()

                    val base =
                        currentPath
                            .trimEnd('/')
                            .ifBlank { "/" }

                    val path =
                        if (base == "/") {
                            "/$name"
                        } else {
                            "$base/$name"
                        }

                    /*
                     * Se è un link simbolico a una directory,
                     * DELE lo elimina senza seguire il link.
                     * Se è una vera directory, DELE fallisce
                     * e passiamo alla cancellazione ricorsiva.
                     */
                    if (!ftp.deleteFile(path)) {
                        deleteDirectoryRecursive(
                            ftp,
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
            mutex.withLock {
                runCatching {
                    validateEntryName(name)

                    val ftp =
                        connectedClient()

                    directoryCache.clear()

                    if (!ftp.makeDirectory(currentEntryPath(name))) {
                        error(
                            ftp.replyString.trim()
                                .ifBlank {
                                    "Creazione cartella FTP fallita"
                                }
                        )
                    }
                }
            }
        }

    private fun deleteDirectoryRecursive(
        ftp: FTPClient,
        path: String
    ) {
        val entries =
            ftp.listFiles(path)

        entries
            .filter {
                it.name != "." &&
                it.name != ".."
            }
            .forEach { entry ->
                val child =
                    "${path.trimEnd('/')}/${entry.name}"

                when {
                    entry.isSymbolicLink -> {
                        if (!ftp.deleteFile(child)) {
                            error(
                                ftp.replyString.trim()
                                    .ifBlank {
                                        "Eliminazione link FTP fallita"
                                    }
                            )
                        }
                    }

                    entry.isDirectory ->
                        deleteDirectoryRecursive(
                            ftp,
                            child
                        )

                    else -> {
                        if (!ftp.deleteFile(child)) {
                            error(
                                ftp.replyString.trim()
                                    .ifBlank {
                                        "Eliminazione file FTP fallita"
                                    }
                            )
                        }
                    }
                }
            }

        if (!ftp.removeDirectory(path)) {
            error(
                ftp.replyString.trim()
                    .ifBlank {
                        "Eliminazione cartella FTP fallita"
                    }
            )
        }
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
        lastHost = null
        currentPath = "/"
        directoryCache.clear()
        disconnectInternal()
    }

    private fun connectedClient(): FTPClient {
        val ftp = client

        if (
            ftp != null &&
            ftp.isConnected
        ) {
            val now =
                android.os.SystemClock.elapsedRealtime()

            /*
             * Durante l'uso continuo evitiamo un NOOP
             * prima di ogni comando FTP.
             *
             * Dopo almeno 30 secondi di inattività
             * verifichiamo invece che il server sia
             * ancora raggiungibile.
             */
            if (
                lastActivityElapsed != 0L &&
                now - lastActivityElapsed < 30_000L
            ) {
                lastActivityElapsed = now
                return ftp
            }
            val alive =
                runCatching {
                    ftp.sendNoOp()
                }.getOrDefault(false)
            if (alive) {
                lastActivityElapsed =
                    android.os.SystemClock.elapsedRealtime()

                return ftp
            }
        }

        val host =
            lastHost
                ?: error("FTP non connesso")

        disconnectInternal()

        val newFtp = FTPClient()

        newFtp.connect(
            host,
            lastPort
        )

        if (
            !FTPReply.isPositiveCompletion(
                newFtp.replyCode
            )
        ) {
            runCatching {
                newFtp.disconnect()
            }

            error("Connessione FTP rifiutata")
        }

        if (
            !newFtp.login(
                lastUsername,
                lastPassword
            )
        ) {
            val risposta =
                newFtp.replyString.trim()

            runCatching {
                newFtp.disconnect()
            }

            error(
                risposta.ifBlank {
                    "Login FTP fallito"
                }
            )
        }

        newFtp.enterLocalPassiveMode()
        newFtp.setFileType(
            FTP.BINARY_FILE_TYPE
        )

        client = newFtp
        lastActivityElapsed =
            android.os.SystemClock.elapsedRealtime()

        return newFtp
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
        lastActivityElapsed = 0L
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
