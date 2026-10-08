package com.mrunix.oscamlivemonitor

import android.content.res.Configuration
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun CompactIconButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.clickable(
            enabled = enabled,
            onClick = onClick
        ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun TerminalToolsScreen(
    serverKey: String,
    serverName: String,
    defaultHost: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val terminalFocusRequester = remember { FocusRequester() }
    val editorFocusRequester = remember { FocusRequester() }

    val landscape =
        configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE

    val scope = rememberCoroutineScope()
    val client = remember { TerminalClient() }
    val fileClient = remember { FileTransferClient() }
    val ftpClient = remember { FtpTransferClient() }

    val safeKey = remember(serverKey) {
        serverKey.replace(
            Regex("[^A-Za-z0-9_]"),
            "_"
        )
    }

    val ansiEscapeRegex = remember {
        Regex("\\u001B\\[[;?0-9]*[ -/]*[@-~]")
    }

    val preferences = remember {
        context.getSharedPreferences(
            "oscam_terminal",
            android.content.Context.MODE_PRIVATE
        )
    }

    var protocol by remember {
        mutableStateOf(
            runCatching {
                TerminalProtocol.valueOf(
                    preferences.getString(
                        "${safeKey}_protocol",
                        TerminalProtocol.SSH.name
                    ) ?: TerminalProtocol.SSH.name
                )
            }.getOrDefault(TerminalProtocol.SSH)
        )
    }

    var host by remember {
        mutableStateOf(
            preferences.getString(
                "${safeKey}_host",
                defaultHost
            ) ?: defaultHost
        )
    }

    var port by remember {
        mutableStateOf(
            preferences.getString(
                "${safeKey}_port",
                if (protocol == TerminalProtocol.SSH) "22" else "23"
            ) ?: if (protocol == TerminalProtocol.SSH) "22" else "23"
        )
    }

    var username by remember {
        mutableStateOf(
            preferences.getString(
                "${safeKey}_username",
                ""
            ) ?: ""
        )
    }

    var password by remember {
        mutableStateOf(
            preferences.getString(
                "${safeKey}_password",
                ""
            ) ?: ""
        )
    }

    var showPassword by remember {
        mutableStateOf(false)
    }

    var connected by remember {
        mutableStateOf(false)
    }

    var ultimaConnessioneTerminale by remember(
        safeKey,
        protocol
    ) {
        mutableStateOf(
            preferences.getLong(
                "${safeKey}_last_${protocol.name.lowercase()}",
                0L
            )
        )
    }

    var mostraFile by remember {
        mutableStateOf(false)
    }

    var fileSftp by remember {
        mutableStateOf(true)
    }

    var ultimaConnessioneFile by remember(
        safeKey,
        fileSftp
    ) {
        mutableStateOf(
            preferences.getLong(
                if (fileSftp) {
                    "${safeKey}_last_sftp"
                } else {
                    "${safeKey}_last_ftp"
                },
                0L
            )
        )
    }

    var fileHost by remember {
        mutableStateOf(defaultHost)
    }

    var filePort by remember {
        mutableStateOf("22")
    }

    var fileUsername by remember {
        mutableStateOf(username)
    }

    var filePassword by remember {
        mutableStateOf(password)
    }

    var mostraFilePassword by remember {
        mutableStateOf(false)
    }

    var fileStatus by remember {
        mutableStateOf("Non connesso")
    }

    var fileConnected by remember {
        mutableStateOf(false)
    }

    var filePath by remember {
        mutableStateOf("")
    }

    var fileEntries by remember {
        mutableStateOf(emptyList<RemoteFile>())
    }

    var uploadInCorso by remember {
        mutableStateOf(false)
    }

    

    var downloadInCorso by remember {
        mutableStateOf(false)
    }

    var downloadNome by remember {
        mutableStateOf<String?>(null)
    }

    var fileMenuEntry by remember {
        mutableStateOf<RemoteFile?>(null)
    }

    var permissionsEntry by remember {
        mutableStateOf<RemoteFile?>(null)
    }

    var permissionsValue by remember {
        mutableStateOf("")
    }

    var renameTarget by remember {
        mutableStateOf<RemoteFile?>(null)
    }

    var renameValue by remember {
        mutableStateOf("")
    }

    var deleteTarget by remember {
        mutableStateOf<RemoteFile?>(null)
    }

    var createDirectoryOpen by remember {
        mutableStateOf(false)
    }

    var createDirectoryValue by remember {
        mutableStateOf("")
    }

    var propertiesData by remember {
        mutableStateOf<RemoteProperties?>(null)
    }

    var propertiesBusy by remember {
        mutableStateOf(false)
    }

    var editorEntry by remember {
        mutableStateOf<RemoteFile?>(null)
    }

    val editorState =
        androidx.compose.foundation.text.input.rememberTextFieldState()

    var editorOriginalText by remember {
        mutableStateOf("")
    }

    var editorBusy by remember {
        mutableStateOf(false)
    }
val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (
                uri != null &&
                fileConnected &&
                !uploadInCorso
            ) {
                val nomeFile =
                    runCatching {
                        context.contentResolver.query(
                            uri,
                            arrayOf(OpenableColumns.DISPLAY_NAME),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                cursor.getString(0)
                            } else {
                                null
                            }
                        }
                    }.getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: uri.lastPathSegment
                            ?.substringAfterLast('/')
                            ?.takeIf { it.isNotBlank() }
                        ?: "file"

                uploadInCorso = true
                fileStatus = "Upload $nomeFile..."

                scope.launch {
                    val streamResult =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                context.contentResolver
                                    .openInputStream(uri)
                                    ?: error(
                                        "Impossibile aprire il file"
                                    )
                            }
                        }

                    if (streamResult.isFailure) {
                        uploadInCorso = false
                        fileStatus =
                            "Errore apertura file: " +
                            (
                                streamResult.exceptionOrNull()
                                    ?.message
                                    ?: "errore"
                            )
                        return@launch
                    }

                    val inputStream =
                        streamResult.getOrThrow()

                    val risultato =
                        if (fileSftp) {
                            fileClient.uploadFile(
                                name = nomeFile,
                                inputStream = inputStream
                            )
                        } else {
                            ftpClient.uploadFile(
                                name = nomeFile,
                                inputStream = inputStream
                            )
                        }

                    if (risultato.isSuccess) {
                        val directory =
                            if (fileSftp) {
                                fileClient.openDirectory(filePath)
                            } else {
                                ftpClient.openDirectory(filePath)
                            }

                        if (directory.isSuccess) {
                            val remoto =
                                directory.getOrThrow()

                            filePath = remoto.path
                            fileEntries = remoto.entries
                        }

                        fileStatus =
                            "Upload completato: $nomeFile"
                    } else {
                        fileStatus =
                            "Errore upload: " +
                            (
                                risultato.exceptionOrNull()
                                    ?.message
                                    ?: "errore"
                            )
                    }

                    uploadInCorso = false
                }
            }
        }


    val saveFileLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("*/*")
        ) { uri ->
            val nomeFile = downloadNome

            if (
                uri != null &&
                nomeFile != null &&
                fileConnected &&
                !downloadInCorso
            ) {
                downloadInCorso = true
                fileStatus = "Download $nomeFile..."

                scope.launch {
                    val streamResult =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                context.contentResolver
                                    .openOutputStream(uri, "w")
                                    ?: error("Impossibile creare il file")
                            }
                        }

                    if (streamResult.isFailure) {
                        downloadInCorso = false
                        downloadNome = null
                        fileStatus =
                            "Errore apertura destinazione: " +
                                (
                                    streamResult.exceptionOrNull()
                                        ?.message
                                        ?: "errore"
                                )
                        return@launch
                    }

                    val outputStream =
                        streamResult.getOrThrow()

                    val risultato =
                        if (fileSftp) {
                            fileClient.downloadFile(
                                name = nomeFile,
                                outputStream = outputStream
                            )
                        } else {
                            ftpClient.downloadFile(
                                name = nomeFile,
                                outputStream = outputStream
                            )
                        }

                    fileStatus =
                        if (risultato.isSuccess) {
                            "Download completato: $nomeFile"
                        } else {
                            "Errore download: " +
                                (
                                    risultato.exceptionOrNull()
                                        ?.message
                                        ?: "errore"
                                )
                        }

                    downloadInCorso = false
                    downloadNome = null
                }
            } else {
                downloadNome = null
            }
        }


    fileMenuEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = {
                fileMenuEntry = null
            },
            title = {
                Text(entry.name)
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (entry.isDirectory) {
                            "Gestisci cartella"
                        } else {
                            "Gestisci file"
                        }
                    )

                    if (!entry.isDirectory) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                fileMenuEntry = null
                                editorBusy = true
                                fileStatus =
                                    "Apertura ${entry.name}..."

                                scope.launch {
                                    val buffer =
                                        java.io.ByteArrayOutputStream()

                                    val risultato =
                                        if (fileSftp) {
                                            fileClient.downloadFile(
                                                entry.name,
                                                buffer
                                            )
                                        } else {
                                            ftpClient.downloadFile(
                                                entry.name,
                                                buffer
                                            )
                                        }

                                    if (risultato.isSuccess) {
                                        val bytes =
                                            buffer.toByteArray()

                                        if (
                                            bytes.size >
                                            2 * 1024 * 1024
                                        ) {
                                            fileStatus =
                                                "File troppo grande per l'editor"
                                        } else if (
                                            bytes.take(1024)
                                                .any {
                                                    it == 0.toByte()
                                                }
                                        ) {
                                            fileStatus =
                                                "File binario: modifica non disponibile"
                                        } else {
                                            val testo =
                                                bytes.toString(
                                                    Charsets.UTF_8
                                                )

                                            editorOriginalText =
                                                testo
                                            editorState.edit {
                                                replace(0, length, testo)
                                                selection = androidx.compose.ui.text.TextRange(0)
                                            }
                                            editorEntry = entry

                                            fileStatus =
                                                "File aperto: ${entry.name}"
                                        }
                                    } else {
                                        fileStatus =
                                            "Errore apertura: " +
                                            (
                                                risultato
                                                    .exceptionOrNull()
                                                    ?.message
                                                    ?: "errore"
                                            )
                                    }

                                    editorBusy = false
                                }
                            }
                        ) {
                            Text("Modifica")
                        }
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            fileMenuEntry = null
                            propertiesBusy = true
                            propertiesData = null

                            fileStatus =
                                "Lettura proprietà ${entry.name}..."

                            scope.launch {
                                val risultato =
                                    if (fileSftp) {
                                        fileClient.getProperties(
                                            entry.name
                                        )
                                    } else {
                                        ftpClient.getProperties(
                                            entry.name
                                        )
                                    }

                                if (risultato.isSuccess) {
                                    propertiesData =
                                        risultato.getOrThrow()

                                    fileStatus =
                                        "Proprietà: ${entry.name}"
                                } else {
                                    fileStatus =
                                        "Errore proprietà: " +
                                        (
                                            risultato
                                                .exceptionOrNull()
                                                ?.message
                                                ?: "errore"
                                        )
                                }

                                propertiesBusy = false
                            }
                        }
                    ) {
                        Text("Proprietà")
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            renameValue = entry.name
                            renameTarget = entry
                            fileMenuEntry = null
                        }
                    ) {
                        Text("Rinomina")
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            permissionsValue =
                                entry.permissions
                                    .toString(8)
                                    .padStart(3, '0')

                            permissionsEntry = entry
                            fileMenuEntry = null
                        }
                    ) {
                        Text("Permessi")
                    }

                    if (!entry.isDirectory) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                downloadNome = entry.name
                                fileMenuEntry = null

                                saveFileLauncher.launch(
                                    entry.name
                                )
                            }
                        ) {
                            Text("Scarica")
                        }
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            deleteTarget = entry
                            fileMenuEntry = null
                        }
                    ) {
                        Text("Elimina")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        fileMenuEntry = null
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }


    renameTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = {
                renameTarget = null
            },
            title = {
                Text(
                    if (entry.isDirectory) {
                        "Rinomina cartella"
                    } else {
                        "Rinomina file"
                    }
                )
            },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = {
                        renameValue = it
                    },
                    label = {
                        Text("Nuovo nome")
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nuovoNome =
                            renameValue.trim()

                        if (
                            nuovoNome.isBlank() ||
                            nuovoNome == "." ||
                            nuovoNome == ".." ||
                            nuovoNome.contains("/")
                        ) {
                            fileStatus =
                                "Nome non valido"
                        } else {
                            renameTarget = null

                            scope.launch {
                                val risultato =
                                    if (fileSftp) {
                                        fileClient.renameEntry(
                                            entry.name,
                                            nuovoNome
                                        )
                                    } else {
                                        ftpClient.renameEntry(
                                            entry.name,
                                            nuovoNome
                                        )
                                    }

                                if (risultato.isSuccess) {
                                    val directory =
                                        if (fileSftp) {
                                            fileClient.openDirectory(
                                                filePath
                                            )
                                        } else {
                                            ftpClient.openDirectory(
                                                filePath
                                            )
                                        }

                                    if (directory.isSuccess) {
                                        val remoto =
                                            directory.getOrThrow()

                                        filePath = remoto.path
                                        fileEntries =
                                            remoto.entries
                                    }

                                    fileStatus =
                                        "Rinominato: ${entry.name} → $nuovoNome"
                                } else {
                                    fileStatus =
                                        "Errore rinomina: " +
                                        (
                                            risultato
                                                .exceptionOrNull()
                                                ?.message
                                                ?: "errore"
                                        )
                                }
                            }
                        }
                    }
                ) {
                    Text("Rinomina")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        renameTarget = null
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }


    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = {
                deleteTarget = null
            },
            title = {
                Text(
                    if (entry.isDirectory) {
                        "Elimina cartella?"
                    } else {
                        "Elimina file?"
                    }
                )
            },
            text = {
                Text(
                    if (entry.isDirectory) {
                        "La cartella \"${entry.name}\" e tutto il suo contenuto verranno eliminati definitivamente."
                    } else {
                        "Il file \"${entry.name}\" verrà eliminato definitivamente."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null

                        scope.launch {
                            val risultato =
                                if (fileSftp) {
                                    if (entry.isDirectory) {
                                        fileClient
                                            .deleteDirectory(
                                                entry.name
                                            )
                                    } else {
                                        fileClient
                                            .deleteFile(
                                                entry.name
                                            )
                                    }
                                } else {
                                    if (entry.isDirectory) {
                                        ftpClient
                                            .deleteDirectory(
                                                entry.name
                                            )
                                    } else {
                                        ftpClient
                                            .deleteFile(
                                                entry.name
                                            )
                                    }
                                }

                            if (risultato.isSuccess) {
                                val directory =
                                    if (fileSftp) {
                                        fileClient.openDirectory(
                                            filePath
                                        )
                                    } else {
                                        ftpClient.openDirectory(
                                            filePath
                                        )
                                    }

                                if (directory.isSuccess) {
                                    val remoto =
                                        directory.getOrThrow()

                                    filePath = remoto.path
                                    fileEntries =
                                        remoto.entries
                                }

                                fileStatus =
                                    "Eliminato: ${entry.name}"
                            } else {
                                fileStatus =
                                    "Errore eliminazione: " +
                                    (
                                        risultato
                                            .exceptionOrNull()
                                            ?.message
                                            ?: "errore"
                                    )
                            }
                        }
                    }
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }


    if (createDirectoryOpen) {
        AlertDialog(
            onDismissRequest = {
                createDirectoryOpen = false
            },
            title = {
                Text("Nuova cartella")
            },
            text = {
                OutlinedTextField(
                    value = createDirectoryValue,
                    onValueChange = {
                        createDirectoryValue = it
                    },
                    label = {
                        Text("Nome cartella")
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nome =
                            createDirectoryValue.trim()

                        if (
                            nome.isBlank() ||
                            nome == "." ||
                            nome == ".." ||
                            nome.contains("/")
                        ) {
                            fileStatus =
                                "Nome cartella non valido"
                        } else {
                            createDirectoryOpen = false

                            scope.launch {
                                val risultato =
                                    if (fileSftp) {
                                        fileClient
                                            .createDirectory(
                                                nome
                                            )
                                    } else {
                                        ftpClient
                                            .createDirectory(
                                                nome
                                            )
                                    }

                                if (risultato.isSuccess) {
                                    val directory =
                                        if (fileSftp) {
                                            fileClient
                                                .openDirectory(
                                                    filePath
                                                )
                                        } else {
                                            ftpClient
                                                .openDirectory(
                                                    filePath
                                                )
                                        }

                                    if (directory.isSuccess) {
                                        val remoto =
                                            directory.getOrThrow()

                                        filePath = remoto.path
                                        fileEntries =
                                            remoto.entries
                                    }

                                    fileStatus =
                                        "Cartella creata: $nome"
                                } else {
                                    fileStatus =
                                        "Errore creazione cartella: " +
                                        (
                                            risultato
                                                .exceptionOrNull()
                                                ?.message
                                                ?: "errore"
                                        )
                                }
                            }
                        }
                    }
                ) {
                    Text("Crea")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        createDirectoryOpen = false
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }


    if (propertiesBusy) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text("Proprietà")
            },
            text = {
                Row(
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )

                    Text(
                        "Lettura proprietà..."
                    )
                }
            },
            confirmButton = {}
        )
    }


    propertiesData?.let { info ->

        val dimensione =
            when {
                info.size >= 1024L * 1024L * 1024L ->
                    String.format(
                        java.util.Locale.getDefault(),
                        "%.2f GiB",
                        info.size.toDouble() /
                            (1024.0 * 1024.0 * 1024.0)
                    )

                info.size >= 1024L * 1024L ->
                    String.format(
                        java.util.Locale.getDefault(),
                        "%.2f MiB",
                        info.size.toDouble() /
                            (1024.0 * 1024.0)
                    )

                info.size >= 1024L ->
                    String.format(
                        java.util.Locale.getDefault(),
                        "%.2f KiB",
                        info.size.toDouble() /
                            1024.0
                    )

                else ->
                    "${info.size} byte"
            }

        val dataModifica =
            info.modifiedTimeMillis
                ?.let { millis ->
                    java.text.SimpleDateFormat(
                        "dd.MM.yyyy HH:mm:ss",
                        java.util.Locale.getDefault()
                    ).format(
                        java.util.Date(millis)
                    )
                }
                ?: "Non disponibile"

        AlertDialog(
            onDismissRequest = {
                propertiesData = null
            },
            title = {
                Text(
                    "Proprietà — ${info.name}"
                )
            },
            text = {
                SelectionContainer {
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            "Nome: ${info.name}"
                        )

                        Text(
                            "Tipo: ${info.type}"
                        )

                        Text(
                            "Percorso: ${info.path}"
                        )

                        Text(
                            "Dimensione: $dimensione"
                        )

                        if (info.size >= 1024L) {
                            Text(
                                "Byte: ${info.size}"
                            )
                        }

                        Text(
                            "Permessi: " +
                            info.permissions
                                .toString(8)
                                .padStart(3, '0') +
                            "  (${info.permissionsText})"
                        )

                        Text(
                            "Proprietario: " +
                            (
                                info.owner
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: "Non disponibile"
                            )
                        )

                        Text(
                            "Gruppo: " +
                            (
                                info.group
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: "Non disponibile"
                            )
                        )

                        Text(
                            "Ultima modifica: $dataModifica"
                        )

                        if (
                            info.fileCount != null
                        ) {
                            Text(
                                "File: ${info.fileCount}"
                            )
                        }

                        if (
                            info.directoryCount != null
                        ) {
                            Text(
                                "Cartelle: ${info.directoryCount}"
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        propertiesData = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }


    permissionsEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = {
                permissionsEntry = null
            },
            title = {
                Text("Permessi — ${entry.name}")
            },
            text = {
                OutlinedTextField(
                    value = permissionsValue,
                    onValueChange = { value ->
                        if (
                            value.length <= 4 &&
                            value.all { it in '0'..'7' }
                        ) {
                            permissionsValue = value
                        }
                    },
                    label = {
                        Text("Permessi (es. 600, 644, 755)")
                    },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mode =
                            permissionsValue.toIntOrNull(8)

                        if (mode == null) {
                            fileStatus = "Permessi non validi"
                        } else {
                            permissionsEntry = null

                            scope.launch {
                                val risultato =
                                    if (fileSftp) {
                                        fileClient.changePermissions(
                                            entry.name,
                                            mode
                                        )
                                    } else {
                                        ftpClient.changePermissions(
                                            entry.name,
                                            mode
                                        )
                                    }

                                if (risultato.isSuccess) {
                                    val directory =
                                        if (fileSftp) {
                                            fileClient.openDirectory(filePath)
                                        } else {
                                            ftpClient.openDirectory(filePath)
                                        }

                                    if (directory.isSuccess) {
                                        val remoto =
                                            directory.getOrThrow()

                                        filePath = remoto.path
                                        fileEntries = remoto.entries
                                    }

                                    fileStatus =
                                        "Permessi modificati: ${entry.name}"
                                } else {
                                    fileStatus =
                                        "Errore permessi: " +
                                            (
                                                risultato.exceptionOrNull()
                                                    ?.message
                                                    ?: "errore"
                                            )
                                }
                            }
                        }
                    }
                ) {
                    Text("Applica")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        permissionsEntry = null
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    val currentEditorEntry = editorEntry

    if (currentEditorEntry != null) {
        LaunchedEffect(currentEditorEntry) {
            editorFocusRequester.requestFocus()
        }

        BackHandler(
            enabled = !editorBusy
        ) {
            editorEntry = null
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .padding(
                    horizontal =
                        if (landscape) 16.dp else 12.dp,
                    vertical = 8.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(
                    enabled = !editorBusy,
                    onClick = {
                        editorEntry = null
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Torna al File Manager"
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentEditorEntry.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Text(
                        text =
                            if (fileSftp) {
                                "SFTP • ${fileHost.trim()}"
                            } else {
                                "FTP • ${fileHost.trim()}"
                            },
                        fontSize = 11.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    enabled =
                        !editorBusy &&
                        editorState.text.toString() != editorOriginalText,
                    onClick = {
                        editorState.edit {
                            replace(0, length, editorOriginalText)
                            selection = androidx.compose.ui.text.TextRange(0)
                        }
                    }
                ) {
                    Text(
                        text = "Annulla modifiche",
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                TextButton(
                    enabled = !editorBusy,
                    onClick = {
                        editorBusy = true
                        fileStatus =
                            "Salvataggio ${currentEditorEntry.name}..."

                        scope.launch {
                            val input =
                                java.io.ByteArrayInputStream(
                                    editorState.text.toString().toByteArray(
                                        Charsets.UTF_8
                                    )
                                )

                            val risultato =
                                if (fileSftp) {
                                    fileClient.uploadFile(
                                        currentEditorEntry.name,
                                        input
                                    )
                                } else {
                                    ftpClient.uploadFile(
                                        currentEditorEntry.name,
                                        input
                                    )
                                }

                            if (risultato.isSuccess) {
                                fileStatus =
                                    "Salvato: ${currentEditorEntry.name}"

                                editorOriginalText = editorState.text.toString()
                            } else {
                                fileStatus =
                                    "Errore salvataggio: " +
                                        (
                                            risultato.exceptionOrNull()
                                                ?.message
                                                ?: "errore"
                                        )
                            }

                            editorBusy = false
                        }
                    }
                ) {
                    Text(
                        text = "Salva",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedTextField(
                state = editorState,
                enabled = !editorBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusRequester(editorFocusRequester),
                keyboardOptions =
                    androidx.compose.foundation.text.KeyboardOptions(
                        showKeyboardOnFocus = false
                    ),
                textStyle =
                    androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize =
                            if (landscape) 14.sp else 13.sp,
                        lineHeight =
                            if (landscape) 19.sp else 18.sp
                    )
            )

            if (editorBusy) {
                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        return
    }

    var terminaleEspanso by
        androidx.compose.runtime.saveable.rememberSaveable {
            mutableStateOf(false)
        }

    val terminaleCompatto =
        connected &&
            !mostraFile &&
            landscape &&
            !terminaleEspanso

    val fullscreenTerminale =
        connected && terminaleEspanso

    val layoutTerminaleCompatto =
        terminaleCompatto || fullscreenTerminale

    BackHandler(
        enabled = fullscreenTerminale
    ) {
        terminaleEspanso = false
    }

    var status by remember {
        mutableStateOf("Non connesso")
    }

    var connessioneTerminaleInCorso by remember {
        mutableStateOf(false)
    }

    var terminalOutput by remember {
        mutableStateOf("")
    }

    var terminalInput by remember {
        mutableStateOf(TextFieldValue(""))
    }

    var ctrlAttivo by remember {
        mutableStateOf(false)
    }

    var altAttivo by remember {
        mutableStateOf(false)
    }

    var readJob by remember {
        mutableStateOf<Job?>(null)
    }

    val terminalScroll = rememberScrollState()

    fun formattaUltimaConnessione(
        timestamp: Long
    ): String {
        if (timestamp <= 0L) {
            return ""
        }

        val zona =
            java.time.ZoneId.systemDefault()

        val dataOra =
            java.time.Instant
                .ofEpochMilli(timestamp)
                .atZone(zona)

        val oggi =
            java.time.LocalDate.now(zona)

        val ora =
            dataOra.format(
                java.time.format.DateTimeFormatter.ofPattern(
                    "HH:mm"
                )
            )

        return when (dataOra.toLocalDate()) {
            oggi ->
                "oggi alle $ora"

            oggi.minusDays(1) ->
                "ieri alle $ora"

            else ->
                dataOra.format(
                    java.time.format.DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy 'alle' HH:mm"
                    )
                )
        }
    }

    fun saveSettings() {
        preferences.edit()
            .putString(
                "${safeKey}_protocol",
                protocol.name
            )
            .putString(
                "${safeKey}_host",
                host.trim()
            )
            .putString(
                "${safeKey}_port",
                port.trim()
            )
            .putString(
                "${safeKey}_username",
                username
            )
            .putString(
                "${safeKey}_password",
                password
            )
            .apply()
    }

    fun disconnectTerminal() {
        connected = false
        terminaleEspanso = false
        status = "Disconnesso"

        readJob?.cancel()
        readJob = null

        client.disconnect()
    }

    fun inviaRawTerminale(
        data: ByteArray,
        inviaInputPrima: Boolean = false
    ) {
        val inputDaInviare =
            if (inviaInputPrima) terminalInput.text else ""

        if (inviaInputPrima) {
            terminalInput = TextFieldValue("")
        }

        scope.launch {
            if (inputDaInviare.isNotEmpty()) {
                val inputResult =
                    client.sendRaw(
                        inputDaInviare.toByteArray(Charsets.UTF_8)
                    )

                if (inputResult.isFailure) {
                    status =
                        "Errore invio: " +
                            (
                                inputResult.exceptionOrNull()
                                    ?.message
                                    ?: "errore"
                            )
                    return@launch
                }
            }

            val result = client.sendRaw(data)

            if (result.isFailure) {
                status =
                    "Errore invio: " +
                        (
                            result.exceptionOrNull()
                                ?.message
                                ?: "errore"
                        )
            }
        }

        terminalFocusRequester.requestFocus()
        keyboardController?.show()
    }

    fun inviaTastoModificato(carattere: Char) {
        val bytes = mutableListOf<Byte>()

        if (altAttivo) {
            bytes.add(0x1B.toByte())
        }

        if (ctrlAttivo) {
            val codice = carattere.uppercaseChar().code

            if (codice in 64..95) {
                bytes.add((codice - 64).toByte())
            } else {
                bytes.addAll(
                    carattere.toString()
                        .toByteArray(Charsets.UTF_8)
                        .toList()
                )
            }
        } else {
            bytes.addAll(
                carattere.toString()
                    .toByteArray(Charsets.UTF_8)
                    .toList()
            )
        }

        ctrlAttivo = false
        altAttivo = false

        inviaRawTerminale(
            bytes.toByteArray(),
            inviaInputPrima = true
        )
    }

    fun inviaComandoTerminale() {
        val command = terminalInput.text
        terminalInput = TextFieldValue("")

        if (command.trim() == "clear") {
            val ultimoPrompt =
                terminalOutput
                    .lines()
                    .lastOrNull { riga ->
                        val t = riga.trim()
                        t.endsWith("#") ||
                            t.endsWith("$") ||
                            t.endsWith(">")
                    }
                    ?.trimEnd()
                    .orEmpty()

            terminalOutput =
                if (ultimoPrompt.isNotBlank()) {
                    "$ultimoPrompt "
                } else {
                    ""
                }

            return
        }

        scope.launch {
            val result = client.send(command)

            if (result.isFailure) {
                status =
                    "Errore invio: " +
                        (
                            result.exceptionOrNull()
                                ?.message
                                ?: "errore"
                        )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            client.disconnect()
            fileClient.disconnect()
            ftpClient.disconnect()
        }
    }

    LaunchedEffect(
        mostraFile,
        fileSftp,
        safeKey
    ) {
        if (mostraFile) {
            ultimaConnessioneFile =
                preferences.getLong(
                    if (fileSftp) {
                        "${safeKey}_last_sftp"
                    } else {
                        "${safeKey}_last_ftp"
                    },
                    0L
                )
        }
    }

    LaunchedEffect(connected, mostraFile) {
        if (connected && !mostraFile) {
            androidx.compose.runtime.withFrameNanos { }
            delay(150)
            terminalFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(terminalScroll.maxValue) {
        terminalScroll.scrollTo(
            terminalScroll.maxValue
        )
    }

    Column(
        modifier = (
            if (
                fullscreenTerminale ||
                connected ||
                fileConnected
            ) {
                modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(
                        align = androidx.compose.ui.Alignment.Top
                    )
            }
        )
            .windowInsetsPadding(
                WindowInsets.ime.exclude(
                    WindowInsets.navigationBars
                )
            )
            .padding(
                start =
                    if (layoutTerminaleCompatto) 2.dp else 14.dp,
                top =
                    if (layoutTerminaleCompatto) 2.dp else 14.dp,
                end =
                    if (layoutTerminaleCompatto) 2.dp else 14.dp,
                bottom =
                    if (connected && !mostraFile) {
                        0.dp
                    } else {
                        if (layoutTerminaleCompatto) 2.dp else 14.dp
                    }
            )
            .then(
                if (
                    !fullscreenTerminale &&
                    !connected &&
                    !fileConnected
                ) {
                    Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.62f
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.34f
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                        )
                        .padding(12.dp)
                } else {
                    Modifier
                }
            )
    ) {
        if (!layoutTerminaleCompatto) {
            ToolsHeader(
                serverName = serverName.ifBlank { defaultHost },
                serverHost = defaultHost,
                onBack = {
                    disconnectTerminal()
                    onClose()
                },
                onFullscreen =
                    if (connected && !mostraFile) {
                        {
                            terminaleEspanso = true
                            terminalFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    } else {
                        null
                    }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ToolsModeSelector(
                fileSelected = mostraFile,
                onTerminal = {
                    mostraFile = false
                },
                onFile = {
                    mostraFile = true
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (!fullscreenTerminale && !mostraFile && !connected) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .then(
                        if (!connected) {
                            Modifier.verticalScroll(rememberScrollState())
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (connected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF66BB6A)
                        )

                        Spacer(modifier = Modifier.width(9.dp))

                        Text(
                            text = "Terminale",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        ToolsConnectionStatus(
                            text = status
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (!connected) {

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ToolsBinarySelector(
                        leftTitle = "SSH",
                        rightTitle = "Telnet",
                        leftIcon = Icons.Default.Key,
                        rightIcon = Icons.Default.Terminal,
                        leftSelected =
                            protocol == TerminalProtocol.SSH,
                        accent = Color(0xFF66BB6A),
                        onLeft = {
                            if (!connected) {
                                val oldDefault =
                                    if (protocol == TerminalProtocol.SSH)
                                        "22"
                                    else
                                        "23"

                                protocol = TerminalProtocol.SSH

                                if (port == oldDefault) {
                                    port = "22"
                                }
                            }
                        },
                        onRight = {
                            if (!connected) {
                                val oldDefault =
                                    if (protocol == TerminalProtocol.SSH)
                                        "22"
                                    else
                                        "23"

                                protocol = TerminalProtocol.TELNET

                                if (port == oldDefault) {
                                    port = "23"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ToolsConnectionStatus(
                        text = status,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1.8f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        value = host,
                        onValueChange = {
                            if (!connected) host = it
                        },
                        label = {
                            Text("Host/IP")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        enabled = !connected
                    )

                    OutlinedTextField(
                        modifier = Modifier.weight(0.8f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        value = port,
                        onValueChange = {
                            if (!connected) port = it
                        },
                        label = {
                            Text("Porta")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        enabled = !connected
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        value = username,
                        onValueChange = {
                            if (!connected) username = it
                        },
                        label = {
                            Text("Username")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        enabled = !connected
                    )

                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        value = password,
                        onValueChange = {
                            if (!connected) password = it
                        },
                        label = {
                            Text("Password")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        visualTransformation =
                            if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    showPassword = !showPassword
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        if (showPassword) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                    contentDescription =
                                        if (showPassword)
                                            "Nascondi password"
                                        else
                                            "Mostra password"
                                )
                            }
                        },
                        singleLine = true,
                        enabled = !connected
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                }

                if (!connected) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !connessioneTerminaleInCorso,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F5A2C),
                            contentColor = Color(0xFFD5F2DA)
                        ),
                        onClick = {
                            val parsedPort =
                                port.trim().toIntOrNull()

                            if (
                                host.isBlank() ||
                                parsedPort == null ||
                                username.isBlank()
                            ) {
                                status =
                                    "Inserisci Host/IP, porta e username"
                                return@Button
                            }

                            saveSettings()

                            status = "Connessione..."
                            terminalOutput = ""
                            connessioneTerminaleInCorso = true

                            scope.launch {
                                val result =
                                    try {
                                        client.connect(
                                            protocol = protocol,
                                            host = host.trim(),
                                            port = parsedPort,
                                            username = username,
                                            password = password
                                        )
                                    } finally {
                                        connessioneTerminaleInCorso = false
                                    }

                                if (result.isSuccess) {
                                    val timestamp =
                                        System.currentTimeMillis()

                                    ultimaConnessioneTerminale =
                                        timestamp

                                    preferences.edit()
                                        .putLong(
                                            "${safeKey}_last_${protocol.name.lowercase()}",
                                            timestamp
                                        )
                                        .apply()

                                    connected = true
                                    status =
                                        "Connesso via ${protocol.name}"

                                    readJob = scope.launch {
                                        client.readLoop { chunk ->
                                            withContext(
                                                Dispatchers.Main
                                            ) {
                                                val chunkPulito =
                                                    chunk
                                                        .replace(
                                                            ansiEscapeRegex,
                                                            ""
                                                        )
                                                        .replace("\r\n", "\n")
                                                        .replace("\r", "")

                                                terminalOutput += chunkPulito

                                                if (
                                                    terminalOutput.length >
                                                    100_000
                                                ) {
                                                    terminalOutput =
                                                        terminalOutput
                                                            .takeLast(80_000)
                                                }
                                            }
                                        }

                                        withContext(
                                            Dispatchers.Main
                                        ) {
                                            connected = false
                                            terminaleEspanso = false

                                            if (
                                                status.startsWith(
                                                    "Connesso"
                                                )
                                            ) {
                                                status =
                                                    "Connessione terminata"
                                            }
                                        }
                                    }
                                } else {
                                    connected = false
                                    terminaleEspanso = false
                                    status =
                                        "Errore: " +
                                        (
                                            result.exceptionOrNull()
                                                ?.message
                                                ?: "connessione fallita"
                                        )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text =
                                "Connetti ${protocol.name}",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (ultimaConnessioneTerminale > 0L) {
                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "Ultima connessione ${protocol.name} riuscita: " +
                                    formattaUltimaConnessione(
                                        ultimaConnessioneTerminale
                                    ),
                            modifier =
                                Modifier.fillMaxWidth(),
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            fontSize = 11.sp,
                            textAlign =
                                androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
        }

        if (
            connected &&
            !mostraFile &&
            !fullscreenTerminale &&
            !terminaleCompatto
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "●",
                        color = Color(0xFF66BB6A),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "${protocol.name} connesso",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = {
                        disconnectTerminal()
                    },
                    modifier = Modifier.height(34.dp),
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(11.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    ),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                    contentPadding = PaddingValues(
                        horizontal = 10.dp,
                        vertical = 0.dp
                    )
                ) {
                    Text(
                        "Disconnetti",
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }

        if (connected && !mostraFile) {

            androidx.compose.animation.AnimatedVisibility(
                visible = terminaleCompatto,
                enter =
                    androidx.compose.animation.fadeIn(
                        androidx.compose.animation.core.tween(180)
                    ) +
                        androidx.compose.animation.expandVertically(
                            androidx.compose.animation.core.tween(180)
                        ),
                exit =
                    androidx.compose.animation.fadeOut(
                        androidx.compose.animation.core.tween(180)
                    ) +
                        androidx.compose.animation.shrinkVertically(
                            androidx.compose.animation.core.tween(180)
                        )
            ) {
                Column {
                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = {
                            disconnectTerminal()
                            onClose()
                        }
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                "Torna alla selezione box"
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                       text = "●",
                       color = Color(0xFF66BB6A),
                       fontSize = 11.sp
                   )

                   Spacer(modifier = Modifier.width(5.dp))

                   Text(
                       text =
                           "${protocol.name} connesso  •  " +
                               "${username}@" +
                               serverName.ifBlank { defaultHost },
                       modifier = Modifier.weight(1f),
                       color =
                           MaterialTheme.colorScheme.onSurfaceVariant,
                       fontFamily = FontFamily.Monospace,
                       fontSize = 12.sp,
                       maxLines = 1
                   )

                   IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = {
                            terminaleEspanso = true
                            terminalFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Espandi terminale"
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            disconnectTerminal()
                        },
                        modifier = Modifier.height(34.dp),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(11.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        ),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                        contentPadding = PaddingValues(
                            horizontal = 10.dp,
                            vertical = 0.dp
                        )
                    ) {
                        Text(
                            "Disconnetti",
                            fontSize = 12.sp
                        )
                    }
                }

                    Spacer(modifier = Modifier.height(2.dp))
                }
            }

            if (fullscreenTerminale) {
                androidx.compose.ui.window.Popup(
                    alignment = androidx.compose.ui.Alignment.TopEnd,
                    properties =
                        androidx.compose.ui.window.PopupProperties(
                            focusable = false
                        )
                ) {
                    IconButton(
                        modifier = Modifier
                            .padding(top = 4.dp, end = 4.dp)
                            .size(36.dp),
                        onClick = {
                            terminaleEspanso = false
                            terminalFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Riduci terminale"
                        )
                    }
                }
            }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape =
                androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            border =
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFF2E7D32)
                ),
            color = Color(0xFF0D1117)
        ) {
            if (connected) {
                val ultimaRiga =
                    terminalOutput
                        .substringAfterLast("\n")
                        .removeSuffix("\r")

                val sembraPrompt =
                    ultimaRiga
                        .trimEnd()
                        .let { riga ->
                            riga.endsWith("#") ||
                                riga.endsWith("$") ||
                                riga.endsWith(">")
                        }

                val promptCorrente =
                    if (sembraPrompt) {
                        ultimaRiga.trimEnd()
                    } else {
                        ""
                    }

                val outputStorico =
                    if (sembraPrompt) {
                        val ultimoNewline =
                            terminalOutput.lastIndexOf('\n')

                        if (ultimoNewline >= 0) {
                            terminalOutput.substring(
                                0,
                                ultimoNewline + 1
                            )
                        } else {
                            ""
                        }
                    } else {
                        terminalOutput
                    }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 8.dp,
                            vertical =
                                if (layoutTerminaleCompatto) 3.dp
                                else 12.dp
                        )
                ) {
                    val terminalMaxHeight = maxHeight

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    max = (terminalMaxHeight - 32.dp)
                                        .coerceAtLeast(0.dp)
                                )
                                .verticalScroll(terminalScroll)
                                .padding(bottom = 10.dp)
                        ) {
                            if (outputStorico.isNotEmpty()) {
                                SelectionContainer {
                                    Text(
                                        text = outputStorico,
                                        color = Color(0xFFE6EDF3),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (promptCorrente.isNotEmpty()) {
                                Text(
                                    text = "$promptCorrente ",
                                    modifier = Modifier.alignByBaseline(),
                                    color = Color(0xFFE6EDF3),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            }

                            androidx.compose.foundation.text.BasicTextField(
                                value = terminalInput,
                                onValueChange = { nuovoValore ->
                                val testoPulito =
                                    nuovoValore.text
                                        .replace("\n", "")
                                        .replace("\r", "")

                                if (
                                    (ctrlAttivo || altAttivo) &&
                                    testoPulito.length ==
                                        terminalInput.text.length + 1 &&
                                    testoPulito.startsWith(
                                        terminalInput.text
                                    )
                                ) {
                                    inviaTastoModificato(
                                        testoPulito.last()
                                    )
                                } else {
                                    val posizione =
                                        nuovoValore.selection.end
                                            .coerceAtMost(
                                                testoPulito.length
                                            )

                                    terminalInput =
                                        nuovoValore.copy(
                                            text = testoPulito,
                                            selection =
                                                TextRange(posizione)
                                        )
                                }
                            },
                            modifier = Modifier
                                    .weight(1f)
                                    .alignByBaseline()
                                    .focusRequester(terminalFocusRequester),
                                singleLine = true,
                                textStyle =
                                    androidx.compose.ui.text.TextStyle(
                                        color = Color(0xFFE6EDF3),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    ),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    autoCorrectEnabled = false,
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Send
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        inviaComandoTerminale()
                                    }
                                ),
                                cursorBrush =
                                    androidx.compose.ui.graphics.SolidColor(
                                        if (
                                            terminalInput.text.isEmpty() &&
                                            promptCorrente.isEmpty()
                                        ) {
                                            Color.Transparent
                                        } else {
                                            Color(0xFF66BB6A)
                                        }
                                    )
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Terminale pronto.",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFF8B949E),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        }

        if (connected && !mostraFile) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    val tasti =
                        listOf(
                            "CTRL", "ALT", "TAB",
                            "~", "/", "|", "-",
                            "↑", "↓", "←", "→"
                        )

                    tasti.forEach { tasto ->
                        val attivo =
                            (tasto == "CTRL" && ctrlAttivo) ||
                                (tasto == "ALT" && altAttivo)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    when (tasto) {
                                        "CTRL" -> {
                                            ctrlAttivo = !ctrlAttivo
                                            terminalFocusRequester.requestFocus()
                                            keyboardController?.show()
                                        }

                                        "ALT" -> {
                                            altAttivo = !altAttivo
                                            terminalFocusRequester.requestFocus()
                                            keyboardController?.show()
                                        }

                                        "TAB" -> {
                                            inviaRawTerminale(
                                                byteArrayOf(0x09),
                                                inviaInputPrima = true
                                            )
                                        }

                                        "↑" -> {
                                            inviaRawTerminale(
                                                byteArrayOf(
                                                    0x1B, 0x5B, 0x41
                                                ),
                                                inviaInputPrima = true
                                            )
                                        }

                                        "↓" -> {
                                            inviaRawTerminale(
                                                byteArrayOf(
                                                    0x1B, 0x5B, 0x42
                                                ),
                                                inviaInputPrima = true
                                            )
                                        }

                                        "→" -> {
                                            if (
                                                terminalInput.text
                                                    .isNotEmpty()
                                            ) {
                                                val posizione =
                                                    terminalInput.selection.max
                                                        .coerceAtMost(
                                                            terminalInput.text.length
                                                        )

                                                terminalInput =
                                                    terminalInput.copy(
                                                        selection =
                                                            TextRange(
                                                                (posizione + 1)
                                                                    .coerceAtMost(
                                                                        terminalInput.text.length
                                                                    )
                                                            )
                                                    )
                                            } else {
                                                inviaRawTerminale(
                                                    byteArrayOf(
                                                        0x1B, 0x5B, 0x43
                                                    )
                                                )
                                            }
                                        }

                                        "←" -> {
                                            if (
                                                terminalInput.text
                                                    .isNotEmpty()
                                            ) {
                                                val posizione =
                                                    terminalInput.selection.min

                                                terminalInput =
                                                    terminalInput.copy(
                                                        selection =
                                                            TextRange(
                                                                (posizione - 1)
                                                                    .coerceAtLeast(0)
                                                            )
                                                    )
                                            } else {
                                                inviaRawTerminale(
                                                    byteArrayOf(
                                                        0x1B, 0x5B, 0x44
                                                    )
                                                )
                                            }
                                        }

                                        else -> {
                                            val inizio =
                                                terminalInput.selection.min
                                            val fine =
                                                terminalInput.selection.max

                                            val nuovoTesto =
                                                terminalInput.text
                                                    .replaceRange(
                                                        inizio,
                                                        fine,
                                                        tasto
                                                    )

                                            val nuovaPosizione =
                                                inizio + tasto.length

                                            terminalInput =
                                                terminalInput.copy(
                                                    text = nuovoTesto,
                                                    selection =
                                                        TextRange(
                                                            nuovaPosizione
                                                        )
                                                )

                                            terminalFocusRequester.requestFocus()
                                            keyboardController?.show()
                                        }
                                    }
                                },
                            contentAlignment =
                                androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = tasto,
                                color =
                                    if (attivo) {
                                        Color(0xFF66BB6A)
                                    } else {
                                        Color(0xFFE6EDF3)
                                    },
                                fontFamily = FontFamily.Monospace,
                                fontSize =
                                    if (tasto.length > 1) 10.sp
                                    else 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }


        if (!fullscreenTerminale && mostraFile) {
            Card(
                modifier =
                    if (fileConnected) {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    } else {
                        Modifier.fillMaxWidth()
                    },
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier =
                        (
                            if (fileConnected) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier.fillMaxWidth()
                            }
                        )
                        .padding(14.dp)
                ) {
                    if (!fileConnected) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        ToolsBinarySelector(
                            leftTitle = "SFTP",
                            rightTitle = "FTP",
                            leftIcon = Icons.Default.Lock,
                            rightIcon = Icons.Default.Folder,
                            leftSelected = fileSftp,
                            accent = Color(0xFFFFB300),
                            onLeft = {
                                val vecchiaPorta =
                                    if (fileSftp) "22" else "21"

                                fileSftp = true

                                if (filePort == vecchiaPorta) {
                                    filePort = "22"
                                }
                            },
                            onRight = {
                                val vecchiaPorta =
                                    if (fileSftp) "22" else "21"

                                fileSftp = false

                                if (filePort == vecchiaPorta) {
                                    filePort = "21"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        ToolsConnectionStatus(
                            text = fileStatus
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1.8f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            value = fileHost,
                            onValueChange = {
                                fileHost = it
                            },
                            label = {
                                Text("Host/IP")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            modifier = Modifier.weight(0.8f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            value = filePort,
                            onValueChange = {
                                filePort = it
                            },
                            label = {
                                Text("Porta")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            value = fileUsername,
                            onValueChange = {
                                fileUsername = it
                            },
                            label = {
                                Text("Username")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            value = filePassword,
                            onValueChange = {
                                filePassword = it
                            },
                            label = {
                                Text("Password")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            visualTransformation =
                                if (mostraFilePassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        mostraFilePassword =
                                            !mostraFilePassword
                                    }
                                ) {
                                    Icon(
                                        imageVector =
                                            if (mostraFilePassword) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                        contentDescription =
                                            if (mostraFilePassword) {
                                                "Nascondi password"
                                            } else {
                                                "Mostra password"
                                            }
                                    )
                                }
                            },
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6A5416),
                            contentColor = Color(0xFFFFE0A3)
                        ),
                        onClick = {
                            val parsedPort =
                                filePort.trim().toIntOrNull()

                            if (
                                fileHost.isBlank() ||
                                parsedPort == null ||
                                fileUsername.isBlank()
                            ) {
                                fileStatus =
                                    "Inserisci Host/IP, porta e username"
                                return@Button
                            }

                            fileStatus =
                                if (fileSftp) {
                                    "Connessione SFTP..."
                                } else {
                                    "Connessione FTP..."
                                }

                            scope.launch {
                                if (fileSftp) {
                                    ftpClient.disconnect()

                                    val connessione =
                                        fileClient.connectSftp(
                                            host = fileHost.trim(),
                                            port = parsedPort,
                                            username = fileUsername,
                                            password = filePassword
                                        )

                                    if (connessione.isSuccess) {
                                        val directory =
                                            fileClient.openDirectory("/")

                                        if (directory.isSuccess) {
                                            val remoto = directory.getOrThrow()
                                            filePath = remoto.path
                                            fileEntries = remoto.entries
                                            val timestamp =
                                                System.currentTimeMillis()

                                            ultimaConnessioneFile =
                                                timestamp

                                            preferences.edit()
                                                .putLong(
                                                    "${safeKey}_last_sftp",
                                                    timestamp
                                                )
                                                .apply()

                                            fileConnected = true
                                            fileStatus =
                                                "Connesso via SFTP"
                                        } else {
                                            fileConnected = false
                                            fileStatus =
                                                "Errore directory SFTP: " +
                                                (directory.exceptionOrNull()?.message ?: "errore")
                                        }
                                    } else {
                                        fileConnected = false
                                        fileStatus =
                                            "Errore SFTP: " +
                                            (connessione.exceptionOrNull()?.message ?: "connessione fallita")
                                    }
                                } else {
                                    fileClient.disconnect()

                                    val connessione =
                                        ftpClient.connect(
                                            host = fileHost.trim(),
                                            port = parsedPort,
                                            username = fileUsername,
                                            password = filePassword
                                        )

                                    if (connessione.isSuccess) {
                                        val directory =
                                            ftpClient.openDirectory("/")

                                        if (directory.isSuccess) {
                                            val remoto = directory.getOrThrow()
                                            filePath = remoto.path
                                            fileEntries = remoto.entries
                                            val timestamp =
                                                System.currentTimeMillis()

                                            ultimaConnessioneFile =
                                                timestamp

                                            preferences.edit()
                                                .putLong(
                                                    "${safeKey}_last_ftp",
                                                    timestamp
                                                )
                                                .apply()

                                            fileConnected = true
                                            fileStatus =
                                                "Connesso via FTP"
                                        } else {
                                            ftpClient.disconnect()
                                            fileConnected = false
                                            fileStatus =
                                                "Errore directory FTP: " +
                                                (directory.exceptionOrNull()?.message ?: "errore")
                                        }
                                    } else {
                                        fileConnected = false
                                        fileStatus =
                                            "Errore FTP: " +
                                            (connessione.exceptionOrNull()?.message ?: "connessione fallita")
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text =
                                if (fileSftp) {
                                    "Connetti SFTP"
                                } else {
                                    "Connetti FTP"
                                },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (ultimaConnessioneFile > 0L) {
                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "Ultima connessione " +
                                    (if (fileSftp) "SFTP" else "FTP") +
                                    " riuscita: " +
                                    formattaUltimaConnessione(
                                        ultimaConnessioneFile
                                    ),
                            modifier = Modifier.fillMaxWidth(),
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            fontSize = 11.sp,
                            textAlign =
                                androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    } else {
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                6.dp,
                                androidx.compose.ui.Alignment.CenterHorizontally
                            ),
                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically
                        ) {

                            CompactIconButton(
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        androidx.compose.foundation.shape.RoundedCornerShape(11.dp)
                                    ),
                                enabled = filePath != "/",
                                onClick = {
                                    scope.launch {
                                        val percorsoSuperiore =
                                            filePath
                                                .trimEnd('/')
                                                .substringBeforeLast(
                                                    "/",
                                                    ""
                                                )
                                                .ifBlank { "/" }

                                        val directory =
                                            if (fileSftp) {
                                                fileClient.openDirectory(
                                                    percorsoSuperiore
                                                )
                                            } else {
                                                ftpClient.openDirectory(
                                                    percorsoSuperiore
                                                )
                                            }

                                        if (directory.isSuccess) {
                                            val remoto =
                                                directory.getOrThrow()

                                            filePath = remoto.path
                                            fileEntries = remoto.entries
                                            fileStatus =
                                                if (fileSftp) {
                                                    "Connesso via SFTP"
                                                } else {
                                                    "Connesso via FTP"
                                                }
                                        } else {
                                            fileStatus =
                                                "Errore cartella superiore: " +
                                                (
                                                    directory.exceptionOrNull()
                                                        ?.message
                                                        ?: "errore"
                                                )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Cartella superiore",
                                    tint =
                                        if (filePath != "/") {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                                .copy(alpha = 0.35f)
                                        }
                                )
                            }

                            CompactIconButton(
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        androidx.compose.foundation.shape.RoundedCornerShape(11.dp)
                                    ),
                                onClick = {
                                    createDirectoryValue = ""
                                    createDirectoryOpen = true
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Default.CreateNewFolder,
                                    contentDescription =
                                        "Nuova cartella",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                enabled = !uploadInCorso,
                                onClick = {
                                    filePickerLauncher.launch("*/*")
                                },
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(34.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(11.dp),
                                contentPadding = PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                ),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6A5416),
                                        contentColor = Color(0xFFFFE0A3)
                                    )
                            ) {
                                if (uploadInCorso) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector =
                                            Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp)
                                    )

                                    Icon(
                                        imageVector =
                                            Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )

                                    Spacer(
                                        modifier = Modifier.width(5.dp)
                                    )

                                    Text(
                                        text = "Upload",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            CompactIconButton(
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        androidx.compose.foundation.shape.RoundedCornerShape(11.dp)
                                    ),
                                onClick = {
                                    scope.launch {
                                        val directory =
                                            if (fileSftp) {
                                                fileClient.openDirectory(
                                                    filePath
                                                )
                                            } else {
                                                ftpClient.openDirectory(
                                                    filePath
                                                )
                                            }

                                        if (directory.isSuccess) {
                                            val remoto =
                                                directory.getOrThrow()

                                            filePath = remoto.path
                                            fileEntries = remoto.entries
                                            fileStatus =
                                                if (fileSftp) {
                                                    "Connesso via SFTP"
                                                } else {
                                                    "Connesso via FTP"
                                                }
                                        } else {
                                            fileStatus =
                                                "Errore aggiornamento: " +
                                                (
                                                    directory.exceptionOrNull()
                                                        ?.message
                                                        ?: "errore"
                                                )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Aggiorna directory",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape =
                                androidx.compose.foundation.shape.RoundedCornerShape(
                                    11.dp
                                ),
                            color =
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = 0.42f
                                ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFFB88A2A).copy(alpha = 0.28f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 7.dp
                                ),
                                verticalAlignment =
                                    androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "›",
                                    color = Color(0xFFD0A23A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = filePath.ifBlank { "/" },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color =
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            fileEntries
                                .filterNot { entry ->
                                    filePath == "/" &&
                                        entry.name == ".."
                                }
                                .forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (
                                                entry.isDirectory ||
                                                entry.isSymbolicLink
                                            ) {
                                                Modifier.clickable {
                                                    scope.launch {
                                                        val destinazione =
                                                            if (entry.name == "..") {
                                                                if (filePath == "/") {
                                                                    "/"
                                                                } else {
                                                                    filePath
                                                                        .trimEnd('/')
                                                                        .substringBeforeLast(
                                                                            "/",
                                                                            ""
                                                                        )
                                                                        .ifBlank { "/" }
                                                                }
                                                            } else {
                                                                if (filePath == "/") {
                                                                    "/${entry.name}"
                                                                } else {
                                                                    "${filePath.trimEnd('/')}/${entry.name}"
                                                                }
                                                            }

                                                        val directory =
                                                            if (fileSftp) {
                                                                fileClient.openDirectory(
                                                                    destinazione
                                                                )
                                                            } else {
                                                                ftpClient.openDirectory(
                                                                    destinazione
                                                                )
                                                            }

                                                        if (directory.isSuccess) {
                                                            val remoto =
                                                                directory.getOrThrow()

                                                            filePath = remoto.path
                                                            fileEntries = remoto.entries
                                                            fileStatus =
                                                                if (fileSftp) {
                                                                    "Connesso via SFTP"
                                                                } else {
                                                                    "Connesso via FTP"
                                                                }
                                                        } else if (
                                                            entry.isSymbolicLink
                                                        ) {
                                                            fileMenuEntry = entry
                                                            fileStatus =
                                                                "Collegamento simbolico: ${entry.name}"
                                                        } else {
                                                            fileStatus =
                                                                "Errore directory " +
                                                                (if (fileSftp) "SFTP: " else "FTP: ") +
                                                                (
                                                                    directory.exceptionOrNull()
                                                                        ?.message
                                                                        ?: "errore"
                                                                )
                                                        }
                                                    }
                                                }
                                            } else {
                                              Modifier.clickable(
                                                  enabled = !downloadInCorso
                                              ) {
                                                  fileMenuEntry = entry
                                              }
                                          }
                                      )
                                      .padding(
                                            horizontal = 6.dp,
                                            vertical = 7.dp
                                        ),
                                    verticalAlignment =
                                        androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector =
                                            if (entry.isDirectory) {
                                                Icons.Default.Folder
                                            } else {
                                                Icons.Default.Description
                                            },
                                        contentDescription = null,
                                        tint =
                                            if (entry.isDirectory) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme
                                                    .onSurfaceVariant
                                            },
                                        modifier = Modifier.size(22.dp)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = entry.name,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )

                                    Text(
                                        text =
                                            entry.permissions.toString(8)
                                                .padStart(3, '0'),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                                .copy(alpha = 0.72f)
                                    )

                                    if (entry.name != "..") {
                                        IconButton(
                                            modifier =
                                                Modifier.size(36.dp),
                                            onClick = {
                                                fileMenuEntry =
                                                    entry
                                            }
                                        ) {
                                            Icon(
                                                imageVector =
                                                    Icons.Default.MoreVert,
                                                contentDescription =
                                                    "Gestisci ${entry.name}"
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider()
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically
                        ) {

                            Row(
                                verticalAlignment =
                                    androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "●",
                                    color = Color(0xFFD0A23A),
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.width(5.dp))

                                Text(
                                    text =
                                        if (fileSftp)
                                            "SFTP connesso"
                                        else
                                            "FTP connesso",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            OutlinedButton(
                                modifier = Modifier.height(40.dp),
                                shape =
                                    androidx.compose.foundation.shape.RoundedCornerShape(
                                        14.dp
                                    ),
                                onClick = {
                                    fileClient.disconnect()
                                    ftpClient.disconnect()
                                    fileConnected = false
                                    fileEntries = emptyList()
                                    filePath = ""
                                    fileStatus = "Disconnesso"
                                }
                            ) {
                                Text(
                                    text = "Disconnetti",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
            }
}
