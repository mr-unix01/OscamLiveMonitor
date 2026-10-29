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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Eliminare server?")
        },
        text = {
            Text(
                "Vuoi eliminare definitivamente " +
                    "\"$nomeServer\"?"
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Elimina")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Annulla")
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Riordina server",
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
                        color =
                            MaterialTheme.colorScheme.surfaceVariant
                                .copy(alpha = 0.38f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape =
                                    androidx.compose.foundation.shape.RoundedCornerShape(
                                        12.dp
                                    ),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline
                                        .copy(alpha = 0.35f)
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.width(12.dp)
                            )

                            Text(
                                text = elemento.nome,
                                modifier = Modifier.weight(1f),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            IconButton(
                                enabled = indice > 0,
                                modifier = Modifier.semantics {
                                    contentDescription =
                                        "Sposta server su"
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
                                modifier = Modifier.semantics {
                                    contentDescription =
                                        "Sposta server giù"
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

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        modifier = Modifier
                            .width(58.dp)
                            .height(50.dp),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                14.dp
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF353B45),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp),
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription =
                                "Fine riordino, ordine già salvato",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
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
    onDelete: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape =
                        androidx.compose.foundation.shape.CircleShape,
                    color =
                        Color(0xFF353B45)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Modifica server OSCam",
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
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = nomeServer,
                        onValueChange = onNomeChange,
                        label = {
                            Text("Nome server")
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
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = hostServer,
                        onValueChange = onHostChange,
                        label = {
                            Text("Host/IP")
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
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = portaServer,
                        onValueChange = onPortaChange,
                        label = {
                            Text("Porta")
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
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = usernameServer,
                        onValueChange = onUsernameChange,
                        label = {
                            Text("Username")
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
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = passwordServer,
                        onValueChange = onPasswordChange,
                        label = {
                            Text("Password")
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

                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                16.dp
                            ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(
                                alpha = 0.55f
                            )
                        ),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor =
                                    MaterialTheme.colorScheme.error,
                                containerColor =
                                    MaterialTheme.colorScheme.error.copy(
                                        alpha = 0.08f
                                    )
                            ),
                        onClick = onDelete
                    ) {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Elimina server",
                                color = MaterialTheme.colorScheme.error
                            )
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
                        contentDescription = "Salva server"
                    },
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        14.dp
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF353B45),
                    contentColor = Color.White
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
                        contentDescription = "Annulla"
                    },
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        14.dp
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF353B45),
                    contentColor = Color.White
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape =
                        androidx.compose.foundation.shape.CircleShape,
                    color =
                        Color(0xFF353B45)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Aggiungi server OSCam",
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
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = nomeServer,
                        onValueChange = onNomeChange,
                        label = { Text("Nome server") },
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
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = hostServer,
                        onValueChange = onHostChange,
                        label = { Text("Host/IP") },
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
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = portaServer,
                        onValueChange = onPortaChange,
                        label = { Text("Porta") },
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
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = usernameServer,
                        onValueChange = onUsernameChange,
                        label = { Text("Username") },
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
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        value = passwordServer,
                        onValueChange = onPasswordChange,
                        label = { Text("Password") },
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

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                14.dp
                            ),
                        enabled = !testConnessioneInCorso,
                        onClick = onTestConnection
                    ) {
                        if (testConnessioneInCorso) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text("Connessione...")
                        } else {
                            Text("Test connessione")
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
                        contentDescription = "Salva server"
                    },
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        14.dp
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF353B45),
                    contentColor = Color.White
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
                        contentDescription = "Annulla"
                    },
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        14.dp
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF353B45),
                    contentColor = Color.White
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
