package com.mrunix.oscamlivemonitor

import com.mrunix.oscamlivemonitor.ui.theme.OscamPalette
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mrunix.oscamlivemonitor.ui.theme.OscamLiveMonitorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.compose.foundation.clickable

@Composable
fun LiveLogScreen(
    host: String,
    porta: String,
    username: String,
    password: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler {
        onClose()
    }

    val api = remember { OscamApi() }

    var righeLog by remember(host, porta, username, password) {
        mutableStateOf(emptyList<String>())
    }

    var ultimoId by remember(host, porta, username, password) {
        mutableStateOf("start")
    }

    var inPausa by remember { mutableStateOf(false) }
    var erroreLiveLog by remember { mutableStateOf("") }
    var erroriLiveLogConsecutivi by remember { mutableStateOf(0) }
    var riconnessioneLiveLog by remember { mutableStateOf(false) }
    var primaRichiestaCompletata by remember { mutableStateOf(false) }

    val scrollVerticale = rememberScrollState()
    val scrollOrizzontale = rememberScrollState()

    LaunchedEffect(
        host,
        porta,
        username,
        password,
        inPausa
    ) {
        if (!inPausa) {
            while (true) {
                val risposta = withContext(Dispatchers.IO) {
                    api.scaricaLiveLog(
                        host = host,
                        porta = porta,
                        username = username,
                        password = password,
                        lastId = ultimoId
                    )
                }

                primaRichiestaCompletata = true

                if (risposta.startsWith("ERRORE:")) {
                    erroriLiveLogConsecutivi += 1

                    if (erroriLiveLogConsecutivi >= 3) {
                        riconnessioneLiveLog = false
                        erroreLiveLog = risposta
                    } else {
                        riconnessioneLiveLog = true
                        erroreLiveLog = ""
                    }
                } else {
                    val risultato = api.estraiLiveLog(risposta)

                    if (!risultato.valido) {
                        erroriLiveLogConsecutivi = 3
                        riconnessioneLiveLog = false
                        erroreLiveLog =
                            "Live Log non disponibile su questo server"
                    } else {
                        erroriLiveLogConsecutivi = 0
                        riconnessioneLiveLog = false
                        erroreLiveLog = ""

                        if (risultato.ultimoId.isNotBlank()) {
                            ultimoId = risultato.ultimoId
                        }

                        if (risultato.righe.isNotEmpty()) {
                            righeLog =
                                (righeLog + risultato.righe)
                                    .takeLast(300)
                        }
                    }
                }

                kotlinx.coroutines.delay(1000)
            }
        }
    }

    LaunchedEffect(righeLog.size, inPausa) {
        if (!inPausa && righeLog.isNotEmpty()) {
            kotlinx.coroutines.delay(40)

            scrollVerticale.scrollTo(
                scrollVerticale.maxValue
            )
        }
    }

    val temaScuroLive =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

    val coloreLive =
        when {
            inPausa ->
                Color(0xFFFFB74D)

            erroreLiveLog.isNotBlank() ->
                Color(0xFFEF5350)

            riconnessioneLiveLog ->
                Color(0xFFFFB74D)

            else ->
                Color(0xFF66BB6A)
        }

    val statoLive =
        when {
            inPausa ->
                "PAUSA"

            erroreLiveLog.isNotBlank() ->
                "ERRORE"

            riconnessioneLiveLog ->
                "RICONNESSIONE"

            primaRichiestaCompletata ->
                "LIVE"

            else ->
                "CONNESSIONE"
        }

    val statoLiveVisualizzato = when (statoLive) {
        "PAUSA" -> androidx.compose.ui.res.stringResource(R.string.main_live_log_status_paused)
        "ERRORE" -> androidx.compose.ui.res.stringResource(R.string.main_live_log_status_error)
        "RICONNESSIONE" -> androidx.compose.ui.res.stringResource(R.string.main_live_log_status_reconnecting)
        "CONNESSIONE" -> androidx.compose.ui.res.stringResource(R.string.main_live_log_status_connecting)
        else -> statoLive
    }

    val testoAttesaLiveLog =
        androidx.compose.ui.res.stringResource(R.string.main_live_log_waiting)
    val testoNessunDatoLiveLog =
        androidx.compose.ui.res.stringResource(R.string.main_live_log_no_data)

    val coloreNormaleLog =
        if (temaScuroLive) {
            Color(0xFFD7E0D8)
        } else {
            Color(0xFF263229)
        }

    val testoLogColorato =
        remember(
            righeLog,
            erroreLiveLog,
            temaScuroLive,
            testoAttesaLiveLog,
            testoNessunDatoLiveLog
        ) {
            androidx.compose.ui.text.buildAnnotatedString {
                if (righeLog.isNotEmpty()) {
                    righeLog.forEachIndexed { indice, riga ->
                        val colore =
                            when {
                                riga.contains(
                                    "ERROR",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "FAIL",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "NOT FOUND",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "REJECTED",
                                    ignoreCase = true
                                ) ->
                                    Color(0xFFEF5350)

                                riga.contains(
                                    "WARN",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "TIMEOUT",
                                    ignoreCase = true
                                ) ->
                                    Color(0xFFFFB74D)

                                riga.contains(
                                    "EMM",
                                    ignoreCase = true
                                ) ->
                                    Color(0xFF42A5F5)

                                riga.contains(
                                    "ECM",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "FOUND",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "CARDOK",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "CONNECTED",
                                    ignoreCase = true
                                ) ->
                                    if (temaScuroLive) {
                                        Color(0xFF66BB6A)
                                    } else {
                                        Color(0xFF1B5E20)
                                    }

                                else ->
                                    coloreNormaleLog
                            }

                        pushStyle(
                            androidx.compose.ui.text.SpanStyle(
                                color = colore
                            )
                        )

                        append(riga)

                        pop()

                        if (indice < righeLog.lastIndex) {
                            append("\n")
                        }
                    }
                } else {
                    pushStyle(
                        androidx.compose.ui.text.SpanStyle(
                            color = coloreNormaleLog
                        )
                    )

                    append(
                        if (erroreLiveLog.isNotBlank()) {
                            testoNessunDatoLiveLog
                        } else {
                            testoAttesaLiveLog
                        }
                    )

                    pop()
                }
            }
        }

    val bluTestataLog =
        if (temaScuroLive) OscamPalette.BannerDark else OscamPalette.BannerLight

    val sfondoPannelloLog =
        if (temaScuroLive) OscamPalette.SheetDark else Color.White

    val accentoLog =
        if (temaScuroLive) OscamPalette.AccentDark else OscamPalette.BannerLight

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bluTestataLog)
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(),
            color = bluTestataLog
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 20.dp,
                        bottom = 26.dp
                    ),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Surface(
                    onClick = onClose,
                    modifier = Modifier.size(44.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(
                            text = "✕",
                            color = Color.White,
                            fontSize = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(9.dp).size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Live Log",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "${host.trim()}:${porta.trim()}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                }

                if (statoLive != "LIVE") {
                    androidx.compose.material3.Surface(
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                50
                            ),
                        color = coloreLive.copy(alpha = 0.12f),
                        border =
                            androidx.compose.foundation.BorderStroke(
                                1.dp,
                                coloreLive.copy(alpha = 0.45f)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 5.dp
                            ),
                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Surface(
                                modifier = Modifier.size(7.dp),
                                shape =
                                    androidx.compose.foundation.shape.CircleShape,
                                color = coloreLive
                            ) {}

                            Spacer(modifier = Modifier.width(5.dp))

                            Text(
                                text = statoLiveVisualizzato,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = coloreLive
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    sfondoPannelloLog,
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        15.dp
                    ),
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                    containerColor =
                        if (temaScuroLive) Color(0xFF1D3249)
                        else Color(0xFFE5F1FF),
                    contentColor = accentoLog
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, accentoLog
                ),
                onClick = {
                    inPausa = !inPausa
                }
            ) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector =
                            if (inPausa) Icons.Default.PlayArrow
                            else Icons.Default.Pause,
                        contentDescription = null,
                        tint = accentoLog,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text =
                            if (inPausa) {
                                androidx.compose.ui.res.stringResource(R.string.main_live_log_resume)
                            } else {
                                androidx.compose.ui.res.stringResource(R.string.main_live_log_pause)
                            },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentoLog
                    )
                }
            }

            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        15.dp
                    ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (temaScuroLive) Color(0xFF303842)
                    else Color(0xFFCBD8E8)
                ),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = {
                    righeLog = emptyList()
                }
            ) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.main_live_log_clear),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (erroreLiveLog.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = erroreLiveLog,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape =
                androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            color =
                if (temaScuroLive) {
                    Color(0xFF0B0F0C)
                } else {
                    Color(0xFFF4F7F4)
                },
            border =
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.65f
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 11.dp,
                            vertical = 7.dp
                        ),
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "OSCam LOG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF66BB6A)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.main_live_log_lines, righeLog.size),
                        fontSize = 10.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    color =
                        MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.45f
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(10.dp)
                        .verticalScroll(scrollVerticale)
                        .horizontalScroll(scrollOrizzontale)
                ) {
                    Text(
                        text = testoLogColorato,
                        fontFamily =
                            androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        softWrap = false
                    )
                }
            }
        }
        }
    }
}
