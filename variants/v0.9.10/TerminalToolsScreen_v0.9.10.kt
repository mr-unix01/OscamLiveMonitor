package com.mrunix.oscamlivemonitor

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Terminal
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
                        contentDescription = "Torna alla Dashboard"
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
        }

        if (!fullscreenTerminale) {
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

        if (connected) {

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
                                "Torna alla Dashboard"
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
            }
}
