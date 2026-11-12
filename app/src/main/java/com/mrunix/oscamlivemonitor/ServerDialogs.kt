package com.mrunix.oscamlivemonitor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke

@Composable
internal fun EliminaServerDialog(
    nomeServer: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val descrizioneAnnulla = stringResource(R.string.settings_cancel)
    val descrizioneElimina = stringResource(R.string.home_delete)

    val scuroElimina =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

    val sfondoDialogo =
        if (scuroElimina) Color(0xFF181B21) else Color.White

    val accentoElimina =
        if (scuroElimina) Color(0xFFFF8A80) else Color(0xFFB3261E)

    val sfondoIconaElimina =
        if (scuroElimina) {
            Color(0xFFFF8A80).copy(alpha = 0.16f)
        } else {
            Color(0xFFB3261E).copy(alpha = 0.10f)
        }

    val bordoNeutro =
        if (scuroElimina) Color(0xFF303842) else Color(0xFFE2E8F0)

    val sfondoAnnulla =
        if (scuroElimina) Color(0xFF171A1F) else Color.White

    val sfondoElimina =
        if (scuroElimina) {
            Color(0xFFFF8A80).copy(alpha = 0.14f)
        } else {
            Color(0xFFFDECEC)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        containerColor = sfondoDialogo,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
                    color = sfondoIconaElimina
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = accentoElimina,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = stringResource(R.string.server_delete_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = stringResource(
                    R.string.server_delete_message,
                    nomeServer
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        dismissButton = {
            Button(
                modifier = Modifier
                    .width(56.dp)
                    .height(50.dp)
                    .semantics {
                        contentDescription = descrizioneAnnulla
                    },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, bordoNeutro),
                colors = ButtonDefaults.buttonColors(
                    containerColor = sfondoAnnulla,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(0.dp),
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier
                    .height(50.dp)
                    .semantics {
                        contentDescription = descrizioneElimina
                    },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, accentoElimina),
                colors = ButtonDefaults.buttonColors(
                    containerColor = sfondoElimina,
                    contentColor = accentoElimina
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                onClick = onConfirm
            ) {
                Text(
                    text = stringResource(R.string.home_delete),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
internal fun RiordinaServerDialog(
    server: List<OscamServer>,
    onDismiss: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit
) {
    val descrizioneSpostaSu = stringResource(R.string.server_move_up)
    val descrizioneSpostaGiu = stringResource(R.string.server_move_down)

    val scuroRiordina =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor =
            if (scuroRiordina) Color(0xFF171A1F) else Color.White,
        title = {
            Text(
                text = stringResource(R.string.server_reorder_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                server.forEachIndexed { indice, elemento ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        color = if (scuroRiordina)
                            Color(0xFF1D3249)
                        else
                            Color(0xFFE5F1FF)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 10.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape =
                                    androidx.compose.foundation.shape.RoundedCornerShape(
                                        12.dp
                                    ),
                                color = Color.Transparent
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (scuroRiordina)
                                                Color(0xFF4DA3FF)
                                            else
                                                Color(0xFF1565C0)
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text = elemento.nome,
                                modifier = Modifier.weight(1f),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )

                            IconButton(
                                enabled = indice > 0,
                                modifier = Modifier.size(40.dp).semantics {
                                    contentDescription =
                                        descrizioneSpostaSu
                                },
                                onClick = {
                                    onMoveUp(indice)
                                }
                            ) {
                                Text(
                                    text = "↑",
                                    fontSize = 22.sp,
                                    color =
                                        if (indice > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                                .copy(alpha = 0.25f)
                                        }
                                )
                            }

                            IconButton(
                                enabled = indice < server.lastIndex,
                                modifier = Modifier.size(40.dp).semantics {
                                    contentDescription =
                                        descrizioneSpostaGiu
                                },
                                onClick = {
                                    onMoveDown(indice)
                                }
                            ) {
                                Text(
                                    text = "↓",
                                    fontSize = 22.sp,
                                    color =
                                        if (indice < server.lastIndex) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                                .copy(alpha = 0.25f)
                                        }
                                )
                            }
                        }
                    }
                }

            }
        },
        confirmButton = {
            Button(
                modifier = Modifier
                    .width(58.dp)
                    .height(50.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (scuroRiordina) Color(0xFF4DA3FF)
                    else Color(0xFF1565C0)
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (scuroRiordina) Color(0xFF1D3249)
                        else Color(0xFFE5F1FF),
                    contentColor =
                        if (scuroRiordina) Color(0xFF4DA3FF)
                        else Color(0xFF1565C0)
                ),
                contentPadding = PaddingValues(0.dp),
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = stringResource(R.string.server_reorder_done),
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        shape =
            androidx.compose.foundation.shape.RoundedCornerShape(
                28.dp
            ),
        tonalElevation = 0.dp
    )
}

@Composable
internal fun ModificaServerDialog(
    nomeServer: String,
    onNomeChange: (String) -> Unit,
    hostServer: String,
    onHostChange: (String) -> Unit,
    portaServer: String,
    onPortaChange: (String) -> Unit,
    usernameServer: String,
    onUsernameChange: (String) -> Unit,
    passwordServer: String,
    onPasswordChange: (String) -> Unit,
    mostraPassword: Boolean,
    onTogglePassword: () -> Unit,
    dialogSnackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val descrizioneSalva = stringResource(R.string.server_save)
    val descrizioneAnnulla = stringResource(R.string.settings_cancel)

    val scuroModifica =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current
    val accentoModifica =
        if (scuroModifica) Color(0xFF4DA3FF) else Color(0xFF1565C0)
    val bordoModifica =
        if (scuroModifica) Color(0xFF303842) else Color(0xFFCBD8E8)
    val sfondoIconaModifica =
        if (scuroModifica) Color(0xFF1D3249) else Color(0xFFE5F1FF)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor =
            if (scuroModifica) Color(0xFF171A1F) else Color.White,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape =
                        androidx.compose.foundation.shape.CircleShape,
                    color =
                        sfondoIconaModifica
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = accentoModifica,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.server_edit_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1
                )
            }
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    OutlinedTextField(
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentoModifica,
                            unfocusedBorderColor = bordoModifica,
                            cursorColor = accentoModifica,
                            focusedLabelColor = accentoModifica
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = nomeServer,
                        onValueChange = onNomeChange,
                        label = {
                            Text(stringResource(R.string.server_name))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null
                            )
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentoModifica,
                            unfocusedBorderColor = bordoModifica,
                            cursorColor = accentoModifica,
                            focusedLabelColor = accentoModifica
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = hostServer,
                        onValueChange = onHostChange,
                        label = {
                            Text(stringResource(R.string.server_host_ip))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null
                            )
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentoModifica,
                            unfocusedBorderColor = bordoModifica,
                            cursorColor = accentoModifica,
                            focusedLabelColor = accentoModifica
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = portaServer,
                        onValueChange = onPortaChange,
                        label = {
                            Text(stringResource(R.string.server_port))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null
                            )
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentoModifica,
                            unfocusedBorderColor = bordoModifica,
                            cursorColor = accentoModifica,
                            focusedLabelColor = accentoModifica
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = usernameServer,
                        onValueChange = onUsernameChange,
                        label = {
                            Text(stringResource(R.string.server_username))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentoModifica,
                            unfocusedBorderColor = bordoModifica,
                            cursorColor = accentoModifica,
                            focusedLabelColor = accentoModifica
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = passwordServer,
                        onValueChange = onPasswordChange,
                        label = {
                            Text(stringResource(R.string.server_password))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        visualTransformation =
                            if (mostraPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        trailingIcon = {
                            IconButton(
                                onClick = onTogglePassword
                            ) {
                                Icon(
                                    imageVector =
                                        if (mostraPassword) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                    contentDescription =
                                        if (mostraPassword) {
                                            stringResource(R.string.server_hide_password)
                                        } else {
                                            stringResource(R.string.server_show_password)
                                        }
                                )
                            }
                        },
                        singleLine = true
                    )


                }

                SnackbarHost(
                    hostState = dialogSnackbarHostState,
                    modifier = Modifier.align(
                        Alignment.BottomCenter
                    )
                )
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier
                    .width(56.dp)
                    .height(50.dp)
                    .semantics {
                        contentDescription = descrizioneSalva
                    },
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        14.dp
                    ),
                border = BorderStroke(1.dp, accentoModifica),
                colors = ButtonDefaults.buttonColors(
                    containerColor = sfondoIconaModifica,
                    contentColor = accentoModifica
                ),
                contentPadding = PaddingValues(0.dp),
                onClick = onSave
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        dismissButton = {
            Button(
                modifier = Modifier
                    .width(56.dp)
                    .height(50.dp)
                    .semantics {
                        contentDescription = descrizioneAnnulla
                    },
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        14.dp
                    ),
                border = BorderStroke(1.dp, bordoModifica),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (scuroModifica) Color(0xFF171A1F) else Color.White,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(0.dp),
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    )
}

@Composable
internal fun AggiungiServerDialog(
    nomeServer: String,
    onNomeChange: (String) -> Unit,
    hostServer: String,
    onHostChange: (String) -> Unit,
    portaServer: String,
    onPortaChange: (String) -> Unit,
    usernameServer: String,
    onUsernameChange: (String) -> Unit,
    passwordServer: String,
    onPasswordChange: (String) -> Unit,
    mostraPassword: Boolean,
    onTogglePassword: () -> Unit,
    testConnessioneInCorso: Boolean,
    dialogSnackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit
) {
    val descrizioneSalva = stringResource(R.string.server_save)
    val descrizioneAnnulla = stringResource(R.string.settings_cancel)

    val scuroAggiungi =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current
    val accentoAggiungi =
        if (scuroAggiungi) Color(0xFF4DA3FF) else Color(0xFF1565C0)
    val bordoAggiungi =
        if (scuroAggiungi) Color(0xFF303842) else Color(0xFFCBD8E8)
    val sfondoIconaAggiungi =
        if (scuroAggiungi) Color(0xFF1D3249) else Color(0xFFE5F1FF)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor =
            if (scuroAggiungi) Color(0xFF171A1F) else Color.White,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape =
                        androidx.compose.foundation.shape.CircleShape,
                    color =
                        sfondoIconaAggiungi
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = accentoAggiungi,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.server_add_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1
                )
            }
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    OutlinedTextField(
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentoAggiungi,
                            unfocusedBorderColor = bordoAggiungi,
                            cursorColor = accentoAggiungi,
                            focusedLabelColor = accentoAggiungi
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = nomeServer,
                        onValueChange = onNomeChange,
                        label = { Text(stringResource(R.string.server_name)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null
                            )
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentoAggiungi,
                            unfocusedBorderColor = bordoAggiungi,
                            cursorColor = accentoAggiungi,
                            focusedLabelColor = accentoAggiungi
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = hostServer,
                        onValueChange = onHostChange,
                        label = { Text(stringResource(R.string.server_host_ip)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null
                            )
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentoAggiungi,
                            unfocusedBorderColor = bordoAggiungi,
                            cursorColor = accentoAggiungi,
                            focusedLabelColor = accentoAggiungi
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = portaServer,
                        onValueChange = onPortaChange,
                        label = { Text(stringResource(R.string.server_port)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null
                            )
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentoAggiungi,
                            unfocusedBorderColor = bordoAggiungi,
                            cursorColor = accentoAggiungi,
                            focusedLabelColor = accentoAggiungi
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = usernameServer,
                        onValueChange = onUsernameChange,
                        label = { Text(stringResource(R.string.server_username)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentoAggiungi,
                            unfocusedBorderColor = bordoAggiungi,
                            cursorColor = accentoAggiungi,
                            focusedLabelColor = accentoAggiungi
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = passwordServer,
                        onValueChange = onPasswordChange,
                        label = { Text(stringResource(R.string.server_password)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        visualTransformation =
                            if (mostraPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        trailingIcon = {
                            IconButton(
                                onClick = onTogglePassword
                            ) {
                                Icon(
                                    imageVector =
                                        if (mostraPassword) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                    contentDescription =
                                        if (mostraPassword) {
                                            stringResource(R.string.server_hide_password)
                                        } else {
                                            stringResource(R.string.server_show_password)
                                        }
                                )
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                14.dp
                            ),
                        enabled = !testConnessioneInCorso,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = accentoAggiungi
                        ),
                        border = BorderStroke(1.dp, bordoAggiungi),
                        onClick = onTestConnection
                    ) {
                        if (testConnessioneInCorso) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(stringResource(R.string.server_connecting))
                        } else {
                            Text(stringResource(R.string.server_test_connection))
                        }
                    }
                }

                SnackbarHost(
                    hostState = dialogSnackbarHostState,
                    modifier = Modifier.align(
                        Alignment.BottomCenter
                    )
                )
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier
                    .width(56.dp)
                    .height(50.dp)
                    .semantics {
                        contentDescription = descrizioneSalva
                    },
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        14.dp
                    ),
                border = BorderStroke(1.dp, accentoAggiungi),
                colors = ButtonDefaults.buttonColors(
                    containerColor = sfondoIconaAggiungi,
                    contentColor = accentoAggiungi
                ),
                contentPadding = PaddingValues(0.dp),
                onClick = onSave
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        dismissButton = {
            Button(
                modifier = Modifier
                    .width(56.dp)
                    .height(50.dp)
                    .semantics {
                        contentDescription = descrizioneAnnulla
                    },
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        14.dp
                    ),
                border = BorderStroke(1.dp, bordoAggiungi),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (scuroAggiungi) Color(0xFF171A1F) else Color.White,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(0.dp),
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    )
}
