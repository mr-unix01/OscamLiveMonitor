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
fun WebIfScreen(
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

    val urlWebIf = "http://${host.trim()}:${porta.trim()}"

    val scuroWebIf =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

    val bluWebIf =
        if (scuroWebIf) OscamPalette.BannerDark else OscamPalette.BannerLight

    val sfondoWebIf = Color.White

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bluWebIf)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 14.dp,
                    bottom = 18.dp
                ),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier.size(42.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color.White.copy(alpha = 0.16f)
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription =
                            androidx.compose.ui.res.stringResource(R.string.settings_back),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
                color = Color.White.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "WebIF OSCam",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "${host.trim()}:${porta.trim()}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.80f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp
                    )
                )
                .background(sfondoWebIf)
        ) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 12.dp),
            factory = { webContext ->
                android.webkit.WebView(webContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.setSupportZoom(true)
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true

                    webChromeClient = android.webkit.WebChromeClient()

                    webViewClient =
                        object : android.webkit.WebViewClient() {
                            private var tentativiRiconnessione = 0
                            private var riconnessioneProgrammata = false
                            private var errorePaginaCorrente = false
                            private val massimoTentativi = 3

                            private fun paginaRiconnessione(tentativo: Int): String =
                                """
                                <!doctype html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1">
                                    <style>
                                        body {
                                            margin: 0;
                                            background: #121212;
                                            color: #e8e8e8;
                                            font-family: sans-serif;
                                            display: flex;
                                            min-height: 100vh;
                                            align-items: center;
                                            justify-content: center;
                                            text-align: center;
                                        }
                                        .box { padding: 28px; }
                                        .title {
                                            color: #4CAF50;
                                            font-size: 22px;
                                            font-weight: 700;
                                            margin-bottom: 12px;
                                        }
                                        .text { font-size: 16px; line-height: 1.5; }
                                    </style>
                                </head>
                                <body>
                                    <div class="box">
                                        <div class="title">Riconnessione WebIF…</div>
                                        <div class="text">Tentativo $tentativo di $massimoTentativi</div>
                                    </div>
                                </body>
                                </html>
                                """.trimIndent()

                            private fun paginaRiprova(): String =
                                """
                                <!doctype html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1">
                                    <style>
                                        body {
                                            margin: 0;
                                            background: #121212;
                                            color: #e8e8e8;
                                            font-family: sans-serif;
                                            display: flex;
                                            min-height: 100vh;
                                            align-items: center;
                                            justify-content: center;
                                            text-align: center;
                                        }
                                        .box { padding: 28px; }
                                        .title {
                                            color: #ff7043;
                                            font-size: 22px;
                                            font-weight: 700;
                                            margin-bottom: 12px;
                                        }
                                        .text {
                                            font-size: 16px;
                                            line-height: 1.5;
                                            margin-bottom: 24px;
                                        }
                                        .button {
                                            display: inline-block;
                                            padding: 12px 24px;
                                            border: 1px solid #4CAF50;
                                            border-radius: 14px;
                                            color: #4CAF50;
                                            text-decoration: none;
                                            font-size: 17px;
                                            font-weight: 700;
                                        }
                                    </style>
                                </head>
                                <body>
                                    <div class="box">
                                        <div class="title">WebIF non raggiungibile</div>
                                        <div class="text">
                                            La connessione è stata interrotta.<br>
                                            OSCam Live Monitor ha già provato a riconnettersi.
                                        </div>
                                        <a class="button" href="oscampulse://retry">Riprova</a>
                                    </div>
                                </body>
                                </html>
                                """.trimIndent()

                            private fun gestisciErrorePrincipale(
                                view: android.webkit.WebView?
                            ) {
                                if (view == null || riconnessioneProgrammata) {
                                    return
                                }

                                errorePaginaCorrente = true
                                view.stopLoading()

                                if (tentativiRiconnessione < massimoTentativi) {
                                    tentativiRiconnessione++
                                    riconnessioneProgrammata = true

                                    view.loadDataWithBaseURL(
                                        null,
                                        paginaRiconnessione(tentativiRiconnessione),
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )

                                    view.postDelayed({
                                        riconnessioneProgrammata = false
                                        view.loadUrl(urlWebIf)
                                    }, 2500L)
                                } else {
                                    view.loadDataWithBaseURL(
                                        null,
                                        paginaRiprova(),
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            }

                            private fun riprovaManuale(
                                view: android.webkit.WebView?
                            ) {
                                if (view == null) {
                                    return
                                }

                                tentativiRiconnessione = 0
                                riconnessioneProgrammata = false
                                errorePaginaCorrente = false
                                view.loadUrl(urlWebIf)
                            }

                            override fun onPageStarted(
                                view: android.webkit.WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                if (url?.startsWith("http://") == true ||
                                    url?.startsWith("https://") == true
                                ) {
                                    errorePaginaCorrente = false
                                }

                                super.onPageStarted(view, url, favicon)
                            }

                            override fun onPageFinished(
                                view: android.webkit.WebView?,
                                url: String?
                            ) {
                                super.onPageFinished(view, url)

                                if (
                                    android.net.Uri.parse(url ?: "").host
                                        ?.equals("git.streamboard.tv", ignoreCase = true) == true
                                ) {
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            var style = document.createElement('style');
                                            style.id = 'oscam-gitlab-layout-fix';
                                            style.textContent =
                                                '.panels-container {' +
                                                'overflow: visible !important;' +
                                                'min-height: 100vh !important;' +
                                                '}' +
                                                '.content-wrapper {' +
                                                'min-height: 600px !important;' +
                                                '}';
                                            if (!document.getElementById(style.id)) {
                                                document.head.appendChild(style);
                                            }
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                }

                                if (!errorePaginaCorrente &&
                                    (url?.startsWith("http://") == true ||
                                            url?.startsWith("https://") == true)
                                ) {
                                    tentativiRiconnessione = 0
                                    riconnessioneProgrammata = false
                                }
                            }

                            override fun onReceivedError(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    gestisciErrorePrincipale(view)
                                } else {
                                    super.onReceivedError(view, request, error)
                                }
                            }

                            @Suppress("DEPRECATION")
                            override fun onReceivedError(
                                view: android.webkit.WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                if (android.os.Build.VERSION.SDK_INT <
                                    android.os.Build.VERSION_CODES.M
                                ) {
                                    gestisciErrorePrincipale(view)
                                } else {
                                    super.onReceivedError(
                                        view,
                                        errorCode,
                                        description,
                                        failingUrl
                                    )
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): Boolean {
                                if (request?.url?.scheme == "oscampulse") {
                                    riprovaManuale(view)
                                    return true
                                }

                                return false
                            }

                            @Suppress("DEPRECATION")
                            override fun shouldOverrideUrlLoading(
                                view: android.webkit.WebView?,
                                url: String?
                            ): Boolean {
                                if (url?.startsWith("oscampulse://") == true) {
                                    riprovaManuale(view)
                                    return true
                                }

                                return false
                            }

                            override fun onReceivedHttpAuthRequest(
                                view: android.webkit.WebView?,
                                handler: android.webkit.HttpAuthHandler?,
                                hostRichiesto: String?,
                                realm: String?
                            ) {
                                val hostAtteso =
                                    android.net.Uri.parse(urlWebIf).host

                                val hostAutorizzato =
                                    !hostAtteso.isNullOrBlank() &&
                                    hostRichiesto?.equals(
                                        hostAtteso,
                                        ignoreCase = true
                                    ) == true

                                if (!hostAutorizzato) {
                                    handler?.cancel()
                                    return
                                }

                                if (
                                    username.isNotBlank() ||
                                    password.isNotBlank()
                                ) {
                                    handler?.proceed(
                                        username,
                                        password
                                    )
                                } else {
                                    super.onReceivedHttpAuthRequest(
                                        view,
                                        handler,
                                        hostRichiesto,
                                        realm
                                    )
                                }
                            }
                        }

                    loadUrl(urlWebIf)
                }
            }
        )
        }
    }
}
