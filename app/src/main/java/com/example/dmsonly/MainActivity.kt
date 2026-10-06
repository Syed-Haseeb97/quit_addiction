package com.example.dmsonly

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.dmsonly.ui.theme.DMsOnlyTheme
import com.example.dmsonly.web.InstagramWebView
import com.example.dmsonly.web.clearInstagramSession

class MainActivity : ComponentActivity() {

    private var activeWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val webView = activeWebView
                    if (webView?.canGoBack() == true) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        setContent {
            DMsOnlyTheme {
                DMsOnlyApp(
                    onWebViewReady = { activeWebView = it }
                )
            }
        }
    }
}

@Composable
private fun DMsOnlyApp(
    onWebViewReady: (WebView) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DMs Only") },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                onClick = {
                                    showMenu = false
                                    errorMessage = null
                                    reloadToken++
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Instagram session") },
                                onClick = {
                                    showMenu = false
                                    showClearDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                InstagramWebView(
                    modifier = Modifier.fillMaxSize(),
                    reloadToken = reloadToken,
                    onWebViewReady = {
                        webView = it
                        onWebViewReady(it)
                    },
                    onLoadingChanged = { isLoading = it },
                    onError = { errorMessage = it },
                    onRecovered = { errorMessage = null }
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                errorMessage?.let { message ->
                    ErrorOverlay(
                        message = message,
                        onRetry = {
                            errorMessage = null
                            reloadToken++
                        }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Instagram session?") },
            text = {
                Text(
                    "This clears this app's WebView cookies, cache, and history. " +
                        "It does not change your Instagram account."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        clearInstagramSession(webView) {
                            errorMessage = null
                            isLoading = true
                        }
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    onRetry: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(message)
                TextButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}
