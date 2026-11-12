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

private data class RisultatoAggiornamento(
    val versioneDisponibile: String,
    val aggiornamentoDisponibile: Boolean,
    val downloadUrl: String?,
    val releaseUrl: String
)

private val githubUpdateClient = OkHttpClient()

private fun confrontaVersioni(
    versioneAttuale: String,
    versioneNuova: String
): Int {
    val attuale = versioneAttuale
        .removePrefix("v")
        .split(".")
        .map { it.toIntOrNull() ?: 0 }

    val nuova = versioneNuova
        .removePrefix("v")
        .split(".")
        .map { it.toIntOrNull() ?: 0 }

    val lunghezza = maxOf(attuale.size, nuova.size)

    for (indice in 0 until lunghezza) {
        val a = attuale.getOrElse(indice) { 0 }
        val n = nuova.getOrElse(indice) { 0 }

        if (a < n) return -1
        if (a > n) return 1
    }

    return 0
}

private suspend fun controllaAggiornamentoGitHub(
    versioneAttuale: String,
    variante: String
): RisultatoAggiornamento = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url("https://api.github.com/repos/mr-unix01/OscamLiveMonitor/releases/latest")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("User-Agent", "OSCam-Live-Monitor")
        .build()

    githubUpdateClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            if (response.code == 404) {
                throw IllegalStateException(
                    "Nessuna release stabile pubblica disponibile."
                )
            }

            throw IllegalStateException(
                "GitHub ha restituito l'errore HTTP ${response.code}."
            )
        }

        val body = response.body?.string()
            ?: throw IllegalStateException("Risposta GitHub vuota.")

        val json = JSONObject(body)

        val tag = json.optString("tag_name").ifBlank {
            throw IllegalStateException("Versione della release non disponibile.")
        }

        val versioneDisponibile = tag.removePrefix("v")
        val releaseUrl = json.optString("html_url")

        var downloadUrl: String? = null
        val assets = json.optJSONArray("assets")

        if (assets != null) {
            for (indice in 0 until assets.length()) {
                val asset = assets.optJSONObject(indice) ?: continue
                val nome = asset.optString("name")

                if (
                    nome.endsWith(".apk", ignoreCase = true) &&
                    nome.contains(variante, ignoreCase = true)
                ) {
                    downloadUrl = asset.optString("browser_download_url")
                        .takeIf { it.isNotBlank() }
                    break
                }
            }
        }

        RisultatoAggiornamento(
            versioneDisponibile = versioneDisponibile,
            aggiornamentoDisponibile =
                confrontaVersioni(versioneAttuale, versioneDisponibile) < 0,
            downloadUrl = downloadUrl,
            releaseUrl = releaseUrl
        )
    }
}

@Composable
fun InformazioniScreen(
    onClose: () -> Unit,
    onOpenGitHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mostraLicenze by rememberSaveable { mutableStateOf(false) }

    if (mostraLicenze) {
        OpenSourceLicensesScreen(
            onClose = { mostraLicenze = false },
            modifier = modifier
        )
        return
    }

    androidx.activity.compose.BackHandler {
        onClose()
    }

    val context = LocalContext.current

    val packageInfo = remember(context) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
        }
    }

    val versioneApp = packageInfo.versionName ?: "—"

    val buildApp =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        }

    val coroutineScope = rememberCoroutineScope()
    var controlloAggiornamentoInCorso by remember { mutableStateOf(false) }
    var risultatoAggiornamento by remember {
        mutableStateOf<RisultatoAggiornamento?>(null)
    }
    var erroreAggiornamento by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(erroreAggiornamento) {
        if (erroreAggiornamento == "Nessuna release stabile pubblica disponibile.") {
            kotlinx.coroutines.delay(3000)
            erroreAggiornamento = null
        }
    }

    LaunchedEffect(risultatoAggiornamento) {
        if (risultatoAggiornamento?.aggiornamentoDisponibile == false) {
            kotlinx.coroutines.delay(3000)
            risultatoAggiornamento = null
        }
    }

    val scuroInfo =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current
    val bluInfo =
        if (scuroInfo) OscamPalette.BannerDark else OscamPalette.BannerLight
    val sfondoInfo =
        if (scuroInfo) OscamPalette.SheetDark else Color.White

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(sfondoInfo)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bluInfo)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp,
                        bottom = 26.dp
                    ),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.clickable(onClick = onClose),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                        tint = Color.White,
                        modifier = Modifier
                            .padding(11.dp)
                            .size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = stringResource(R.string.info_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        sfondoInfo,
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp
                        )
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    )
            ) {
        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.oscam_drawer_logo),
                contentDescription = stringResource(R.string.info_logo),
                modifier = Modifier
                    .size(96.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(
                    androidx.compose.ui.text.SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                append("OSCam")
                pop()
                pushStyle(
                    androidx.compose.ui.text.SpanStyle(
                        color = if (scuroInfo)
                            OscamPalette.AccentDark
                        else
                            OscamPalette.BannerLight
                    )
                )
                append(" Live Monitor")
                pop()
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = stringResource(R.string.info_subtitle),
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            shape =
                androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (scuroInfo) Color(0xFF303F52) else Color(0xFFCBD8E8)
            ),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(
                defaultElevation = 0.dp
            ),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor =
                    if (scuroInfo) Color(0xFF1E2A3B) else Color(0xFFF0F5FC)
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.info_version),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = versioneApp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = if (scuroInfo) Color(0xFF303F52) else Color(0xFFCBD8E8)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.info_build),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = buildApp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = if (scuroInfo) Color(0xFF303F52) else Color(0xFFCBD8E8)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.info_author),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "mr-unix",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape =
                androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            onClick = {
                coroutineScope.launch {
                    kotlinx.coroutines.delay(300)
                    onOpenGitHub()
                }
            }
        ) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(
                    id = R.drawable.ic_github
                ),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(stringResource(R.string.info_repository))
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            onClick = { mostraLicenze = true }
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.info_open_source_licenses))
        }

        Spacer(modifier = Modifier.height(10.dp))

        androidx.compose.material3.Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape =
                androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            enabled = !controlloAggiornamentoInCorso,
            border = androidx.compose.foundation.BorderStroke(
                1.4.dp,
                if (com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current)
                    OscamPalette.AccentDark.copy(alpha = 0.75f)
                else
                    OscamPalette.BannerLight.copy(alpha = 0.85f)
            ),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor =
                    if (com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current)
                        OscamPalette.AccentDark.copy(alpha = 0.10f)
                    else
                        OscamPalette.AccentDark.copy(alpha = 0.20f),
                contentColor =
                    if (com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current)
                        OscamPalette.AccentDark
                    else
                        OscamPalette.BannerLight,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            onClick = {
                controlloAggiornamentoInCorso = true
                risultatoAggiornamento = null
                erroreAggiornamento = null

                coroutineScope.launch {
                    kotlinx.coroutines.delay(300)
                    try {
                        risultatoAggiornamento =
                            controllaAggiornamentoGitHub(
                                versioneAttuale = versioneApp,
                                variante = BuildConfig.VARIANTE_APP
                            )
                    } catch (e: Exception) {
                        erroreAggiornamento =
                            e.message ?: context.getString(R.string.info_check_error)
                    } finally {
                        controlloAggiornamentoInCorso = false
                    }
                }
            }
        ) {
            if (controlloAggiornamentoInCorso) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.info_checking))
            } else {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.info_check_updates))
            }
        }

        risultatoAggiornamento?.let { risultato ->
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text =
                    if (risultato.aggiornamentoDisponibile) {
                        stringResource(R.string.info_new_version, risultato.versioneDisponibile)
                    } else {
                        stringResource(R.string.info_up_to_date)
                    },
                fontSize = 13.sp,
                fontWeight =
                    if (risultato.aggiornamentoDisponibile) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                color =
                    if (risultato.aggiornamentoDisponibile) {
                        Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (
                risultato.aggiornamentoDisponibile &&
                risultato.downloadUrl != null
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(risultato.downloadUrl)
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(stringResource(R.string.info_download))
                }
            }
        }

        erroreAggiornamento?.let { errore ->
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = errore,
                fontSize = 13.sp,
                color =
                    if (errore == "Nessuna release stabile pubblica disponibile.") {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.info_disclaimer),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
