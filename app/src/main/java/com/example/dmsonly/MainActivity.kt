package com.example.dmsonly

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dmsonly.ui.theme.AppearanceMode
import com.example.dmsonly.ui.theme.DMsOnlyTheme
import com.example.dmsonly.web.InstagramBackNavigation
import com.example.dmsonly.web.InstagramWebView
import com.example.dmsonly.web.clearInstagramSession

private const val WEBVIEW_STATE_KEY = "webview_state"
private const val APPEARANCE_PREFERENCES = "appearance_preferences"
private const val APPEARANCE_MODE_KEY = "appearance_mode"

class MainActivity : ComponentActivity() {

    private var activeWebView: WebView? = null
    private var restoredWebViewState: Bundle? = null
    private lateinit var appTimeTracker: AppTimeTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoredWebViewState = savedInstanceState?.getBundle(WEBVIEW_STATE_KEY)
        appTimeTracker = AppTimeTracker(getSharedPreferences(AppTimeTracker.PREFERENCES, MODE_PRIVATE))

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val webView = activeWebView
                    if (webView == null) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        return
                    }

                    if (InstagramBackNavigation.goBack(webView)) {
                        return
                    }

                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        )

        setContent {
            var appearanceMode by remember {
                mutableStateOf(
                    AppearanceMode.fromStoredValue(
                        getSharedPreferences(APPEARANCE_PREFERENCES, MODE_PRIVATE)
                            .getString(APPEARANCE_MODE_KEY, null)
                    )
                )
            }
            DMsOnlyTheme(appearanceMode = appearanceMode) {
                DMsOnlyApp(
                    initialWebViewState = restoredWebViewState,
                    appearanceMode = appearanceMode,
                    onAppearanceModeChange = { mode ->
                        appearanceMode = mode
                        getSharedPreferences(APPEARANCE_PREFERENCES, MODE_PRIVATE)
                            .edit()
                            .putString(APPEARANCE_MODE_KEY, mode.name)
                            .apply()
                    },
                    appTimeTracker = appTimeTracker,
                    onWebViewReady = { activeWebView = it }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appTimeTracker.start()
    }

    override fun onPause() {
        appTimeTracker.stop()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        activeWebView?.let { webView ->
            val webViewState = Bundle()
            webView.saveState(webViewState)
            outState.putBundle(WEBVIEW_STATE_KEY, webViewState)
        }
        super.onSaveInstanceState(outState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DMsOnlyApp(
    initialWebViewState: Bundle?,
    appearanceMode: AppearanceMode,
    onAppearanceModeChange: (AppearanceMode) -> Unit,
    appTimeTracker: AppTimeTracker,
    onWebViewReady: (WebView) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showAppTimeDialog by remember { mutableStateOf(false) }
    var appTimeSnapshot by remember { mutableStateOf<AppTimeSnapshot?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val systemDarkTheme = isSystemInDarkTheme()
    val darkWebAppearance = appearanceMode.resolveDarkTheme(systemDarkTheme)

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
                                text = { Text("App time") },
                                onClick = {
                                    showMenu = false
                                    appTimeSnapshot = appTimeTracker.snapshot()
                                    showAppTimeDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Appearance: ${appearanceMode.label()}") },
                                onClick = {
                                    showMenu = false
                                    showAppearanceDialog = true
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
                .padding(padding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                InstagramWebView(
                    modifier = Modifier.fillMaxSize(),
                    initialWebViewState = initialWebViewState,
                    reloadToken = reloadToken,
                    darkAppearance = darkWebAppearance,
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

    if (showAppTimeDialog) {
        val snapshot = appTimeSnapshot ?: appTimeTracker.snapshot()
        AlertDialog(
            onDismissRequest = { showAppTimeDialog = false },
            title = { Text("App time") },
            text = {
                Column {
                    Text("Today: ${formatDuration(snapshot.todaySeconds)}")
                    Text("Yesterday: ${formatDuration(snapshot.yesterdaySeconds)}")
                    Text("Last 7 days: ${formatDuration(snapshot.last7DaysSeconds)}")
                    Text(
                        modifier = Modifier.padding(top = 12.dp),
                        text = "Tracked only while DMs Only is in the foreground. " +
                            "Usage stays on this device."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appTimeTracker.reset()
                        appTimeSnapshot = appTimeTracker.snapshot()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppTimeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text("Appearance") },
            text = {
                Column {
                    AppearanceMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = appearanceMode == mode,
                                    onClick = {
                                        onAppearanceModeChange(mode)
                                        showAppearanceDialog = false
                                    },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appearanceMode == mode,
                                onClick = null
                            )
                            Text(mode.label())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppearanceDialog = false }) {
                    Text("Close")
                }
            }
        )
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

private fun AppearanceMode.label(): String = when (this) {
    AppearanceMode.SYSTEM -> "System default"
    AppearanceMode.LIGHT -> "Light"
    AppearanceMode.DARK -> "Dark"
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
