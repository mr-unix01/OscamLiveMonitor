package com.mrunix.oscamlivemonitor

import android.content.res.Configuration
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    var mostraFile by remember {
        mutableStateOf(false)
    }

    var fileSftp by remember {
        mutableStateOf(true)
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

    var editorEntry by remember {
        mutableStateOf<RemoteFile?>(null)
    }

    var editorText by remember {
        mutableStateOf("")
    }

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
                    Text("Scegli operazione")

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            fileMenuEntry = null
                            editorBusy = true
                            fileStatus = "Apertura ${entry.name}..."

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
                                    val bytes = buffer.toByteArray()

                                    if (bytes.size > 2 * 1024 * 1024) {
                                        fileStatus =
                                            "File troppo grande per l'editor"
                                    } else if (
                                        bytes.take(1024)
                                            .any { it == 0.toByte() }
                                    ) {
                                        fileStatus =
                                            "File binario: modifica non disponibile"
                                    } else {
                                        val testo =
                                            bytes.toString(Charsets.UTF_8)

                                        editorOriginalText = testo
                                        editorText = testo
                                        editorEntry = entry
                                        fileStatus =
                                            "File aperto: ${entry.name}"
                                    }
                                } else {
                                    fileStatus =
                                        "Errore apertura: " +
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
                        Text("Modifica")
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

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            downloadNome = entry.name
                            fileMenuEntry = null
                            saveFileLauncher.launch(entry.name)
                        }
                    ) {
                        Text("Scarica")
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
                        imageVector = Icons.Default.ArrowBack,
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
                        editorText != editorOriginalText,
                    onClick = {
                        editorText = editorOriginalText
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
                                    editorText.toByteArray(
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

                                editorOriginalText = editorText
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
                value = editorText,
                onValueChange = {
                    editorText = it
                },
                enabled = !editorBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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

    val fullscreenTerminale =
        connected && landscape

    var status by remember {
        mutableStateOf("Non connesso")
    }

    var terminalOutput by remember {
        mutableStateOf("")
    }

    var terminalInput by remember {
        mutableStateOf("")
    }

    var terminalFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0)
            )
        )
    }

    var readJob by remember {
        mutableStateOf<Job?>(null)
    }

    val terminalScroll = rememberScrollState()

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
        status = "Disconnesso"

        readJob?.cancel()
        readJob = null

        client.disconnect()
    }

    DisposableEffect(Unit) {
        onDispose {
            client.disconnect()
            fileClient.disconnect()
            ftpClient.disconnect()
        }
    }

    LaunchedEffect(
        terminalOutput,
        terminalInput
    ) {
        val testoCompleto =
            terminalOutput + terminalInput

        terminalFieldValue =
            TextFieldValue(
                text = testoCompleto,
                selection = TextRange(testoCompleto.length)
            )

        androidx.compose.runtime.withFrameNanos { }

        terminalScroll.scrollTo(
            terminalScroll.maxValue
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(
                if (fullscreenTerminale) 2.dp else 14.dp
            )
    ) {
        if (!fullscreenTerminale) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        disconnectTerminal()
                        onClose()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Torna alla selezione box"
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Strumenti",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = serverName.ifBlank {
                            defaultHost
                        },
                        fontSize = 13.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!connected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = !mostraFile,
                        onClick = {
                            mostraFile = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text("Terminale")
                        }
                    )

                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = mostraFile,
                        onClick = {
                            mostraFile = true
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text("File")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (!fullscreenTerminale && !mostraFile) {
        Card(
            modifier =
                if (!connected) {
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                } else {
                    Modifier.fillMaxWidth()
                },
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

                    if (connected) {
                        Text(
                            text = "Connesso via ${protocol.name}",
                            color = Color(0xFF66BB6A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!connected) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected =
                            protocol == TerminalProtocol.SSH,
                        onClick = {
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
                        label = {
                            Text("SSH")
                        }
                    )

                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected =
                            protocol == TerminalProtocol.TELNET,
                        onClick = {
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
                        label = {
                            Text("Telnet")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = host,
                    onValueChange = {
                        if (!connected) host = it
                    },
                    label = {
                        Text("Host/IP")
                    },
                    singleLine = true,
                    enabled = !connected
                )

                Spacer(modifier = Modifier.height(7.dp))

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = port,
                    onValueChange = {
                        if (!connected) port = it
                    },
                    label = {
                        Text("Porta")
                    },
                    singleLine = true,
                    enabled = !connected
                )

                Spacer(modifier = Modifier.height(7.dp))

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = username,
                    onValueChange = {
                        if (!connected) username = it
                    },
                    label = {
                        Text("Username")
                    },
                    singleLine = true,
                    enabled = !connected
                )

                Spacer(modifier = Modifier.height(7.dp))

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = password,
                    onValueChange = {
                        if (!connected) password = it
                    },
                    label = {
                        Text("Password")
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

                Spacer(modifier = Modifier.height(12.dp))
                }

                if (!connected) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
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

                            scope.launch {
                                val result =
                                    client.connect(
                                        protocol = protocol,
                                        host = host.trim(),
                                        port = parsedPort,
                                        username = username,
                                        password = password
                                    )

                                if (result.isSuccess) {
                                    connected = true
                                    status =
                                        "Connesso via ${protocol.name}"

                                    readJob = scope.launch {
                                        client.readLoop { chunk ->
                                            withContext(
                                                Dispatchers.Main
                                            ) {
                                                val chunkPulito =
                                                    chunk.replace(
                                                        Regex(
                                                            "\\u001B\\[[;?0-9]*[ -/]*[@-~]"
                                                        ),
                                                        ""
                                                    )

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
                        Text(
                            text =
                                "Connetti ${protocol.name}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            disconnectTerminal()
                        }
                    ) {
                        Text("Disconnetti")
                    }
                }

                if (!connected) {
                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = status,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
        }

        if (connected && !mostraFile) {

            if (fullscreenTerminale) {

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
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription =
                                "Torna alla selezione box"
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text =
                            "${protocol.name}  •  " +
                            "${username}@" +
                            serverName.ifBlank { defaultHost },
                        modifier = Modifier.weight(1f),
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        maxLines = 1
                    )

                    TextButton(
                        onClick = {
                            disconnectTerminal()
                        },
                        contentPadding = PaddingValues(
                            horizontal = 8.dp,
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

            } else {

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "●",
                        color = Color(0xFF66BB6A),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text =
                            "${protocol.name}  •  " +
                            "${username}@" +
                            serverName.ifBlank { defaultHost },
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
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
                androidx.compose.foundation.text.BasicTextField(
                    value = terminalFieldValue,
                    onValueChange = { nuovoValore ->

                        if (!nuovoValore.text.startsWith(terminalOutput)) {
                            return@BasicTextField
                        }

                        val nuovoInput =
                            nuovoValore.text
                                .removePrefix(terminalOutput)

                        if (
                            nuovoInput.contains("\n") ||
                            nuovoInput.contains("\r")
                        ) {
                            val command =
                                nuovoInput
                                    .replace("\n", "")
                                    .replace("\r", "")

                            terminalInput = ""

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

                                terminalFieldValue =
                                    TextFieldValue(
                                        text = terminalOutput,
                                        selection =
                                            TextRange(
                                                terminalOutput.length
                                            )
                                    )
                            } else {
                                scope.launch {
                                    val result =
                                        client.send(command)

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
                        } else {
                            terminalInput = nuovoInput

                            val testoCompleto =
                                terminalOutput + nuovoInput

                            terminalFieldValue =
                                TextFieldValue(
                                    text = testoCompleto,
                                    selection =
                                        TextRange(
                                            testoCompleto.length
                                        )
                                )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(terminalScroll)
                        .padding(
                            horizontal = 8.dp,
                            vertical =
                                if (fullscreenTerminale) 3.dp
                                else 12.dp
                        ),
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
                        imeAction = ImeAction.None
                    ),
                    cursorBrush =
                        androidx.compose.ui.graphics.SolidColor(
                            Color(0xFF66BB6A)
                        )
                )
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

        if (!fullscreenTerminale && mostraFile) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFFFFB300)
                        )

                        Spacer(modifier = Modifier.width(9.dp))

                        Text(
                            text = "File Manager",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!fileConnected) {
                        Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = fileSftp,
                            onClick = {
                                val vecchiaPorta =
                                    if (fileSftp) "22" else "21"

                                fileSftp = true

                                if (filePort == vecchiaPorta) {
                                    filePort = "22"
                                }
                            },
                            label = {
                                Text("SFTP")
                            }
                        )

                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = !fileSftp,
                            onClick = {
                                val vecchiaPorta =
                                    if (fileSftp) "22" else "21"

                                fileSftp = false

                                if (filePort == vecchiaPorta) {
                                    filePort = "21"
                                }
                            },
                            label = {
                                Text("FTP")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = fileHost,
                        onValueChange = {
                            fileHost = it
                        },
                        label = {
                            Text("Host/IP")
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = filePort,
                        onValueChange = {
                            filePort = it
                        },
                        label = {
                            Text("Porta")
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = fileUsername,
                        onValueChange = {
                            fileUsername = it
                        },
                        label = {
                            Text("Username")
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = filePassword,
                        onValueChange = {
                            filePassword = it
                        },
                        label = {
                            Text("Password")
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
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
                                            fileConnected = true
                                            fileStatus = "Connesso via SFTP"
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
                                            fileConnected = true
                                            fileStatus = "Connesso via FTP"
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

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = fileStatus,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text =
                                    (if (fileSftp) "SFTP" else "FTP") +
                                    "  •  ${fileHost.trim()}",
                                color = Color(0xFF66BB6A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
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
                                            Color(0xFF66BB6A)
                                        } else {
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                                .copy(alpha = 0.35f)
                                        }
                                )
                            }

                            Button(
                                enabled = !uploadInCorso,
                                onClick = {
                                    filePickerLauncher.launch("*/*")
                                },
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                ),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1976D2)
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

                            IconButton(
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
                                    tint = Color(0xFF66BB6A)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape =
                                androidx.compose.foundation.shape.RoundedCornerShape(
                                    12.dp
                                ),
                            color =
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = filePath.ifBlank { "/" },
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 9.dp
                                ),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            fileEntries.forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (entry.isDirectory) {
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
                                            vertical = 9.dp
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
                                                Color(0xFFFFB300)
                                            } else {
                                                MaterialTheme.colorScheme
                                                    .onSurfaceVariant
                                            },
                                        modifier = Modifier.size(24.dp)
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
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )
                                }

                                HorizontalDivider()
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                fileClient.disconnect()
                                fileConnected = false
                                fileEntries = emptyList()
                                filePath = ""
                                fileStatus = "Disconnesso"
                            }
                        ) {
                            Text("Disconnetti")
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = fileStatus,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
            }
}
