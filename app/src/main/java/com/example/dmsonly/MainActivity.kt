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
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoredWebViewState = savedInstanceState?.getBundle(WEBVIEW_STATE_KEY)

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
            var wellbeingSettings by remember {
                mutableStateOf(WellbeingSettings.load(this@MainActivity))
            }
            DMsOnlyTheme(appearanceMode = appearanceMode) {
                DMsOnlyApp(
                    initialWebViewState = restoredWebViewState,
                    appearanceMode = appearanceMode,
                    wellbeingSettings = wellbeingSettings,
                    onAppearanceModeChange = { mode ->
                        appearanceMode = mode
                        getSharedPreferences(APPEARANCE_PREFERENCES, MODE_PRIVATE)
                            .edit()
                            .putString(APPEARANCE_MODE_KEY, mode.name)
                            .apply()
                    },
                    onWellbeingSettingsChange = { settings ->
                        wellbeingSettings = settings
                        WellbeingSettings.save(this@MainActivity, settings)
                    },
                    onWebViewReady = { activeWebView = it }
                )
            }
        }
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
    wellbeingSettings: WellbeingSettings,
    onAppearanceModeChange: (AppearanceMode) -> Unit,
    onWellbeingSettingsChange: (WellbeingSettings) -> Unit,
    onWebViewReady: (WebView?) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showWellbeingDialog by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var now by remember { mutableStateOf(WellbeingSettings.currentTime()) }
    LaunchedEffect(wellbeingSettings.quietHoursEnabled, wellbeingSettings.quietHoursStart, wellbeingSettings.quietHoursEnd) {
        while (true) {
            now = WellbeingSettings.currentTime()
            delay(30_000)
        }
    }
    val quietHoursActive = wellbeingSettings.isQuietHoursActive(now)
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
                                text = { Text("Appearance: ${appearanceMode.label()}") },
                                onClick = {
                                    showMenu = false
                                    showAppearanceDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Focus & privacy") },
                                onClick = {
                                    showMenu = false
                                    showWellbeingDialog = true
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
                if (quietHoursActive) {
                    QuietHoursOverlay(
                        start = wellbeingSettings.quietHoursStart,
                        end = wellbeingSettings.quietHoursEnd
                    )
                } else {
                    InstagramWebView(
                        modifier = Modifier.fillMaxSize(),
                        initialWebViewState = initialWebViewState,
                        reloadToken = reloadToken,
                        darkAppearance = darkWebAppearance,
                        dopamineFreeUi = wellbeingSettings.dopamineFreeUi,
                        ghostMode = wellbeingSettings.ghostMode,
                        onWebViewReady = {
                            webView = it
                            onWebViewReady(it)
                        },
                        onLoadingChanged = { isLoading = it },
                        onError = { errorMessage = it },
                        onRecovered = { errorMessage = null }
                    )

                }
                if (isLoading && !quietHoursActive) {
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

    if (showWellbeingDialog) {
        WellbeingDialog(
            settings = wellbeingSettings,
            onSettingsChange = onWellbeingSettingsChange,
            onDismiss = { showWellbeingDialog = false }
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

@Composable
private fun QuietHoursOverlay(start: String, end: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Quiet Hours active")
                Text("Instagram is unavailable from " + start + " to " + end + ".")
                Text("Come back when the schedule ends.")
            }
        }
    }
}

@Composable
private fun WellbeingDialog(
    settings: WellbeingSettings,
    onSettingsChange: (WellbeingSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var start by rememberSaveable(settings.quietHoursStart) { mutableStateOf(settings.quietHoursStart) }
    var end by rememberSaveable(settings.quietHoursEnd) { mutableStateOf(settings.quietHoursEnd) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Focus & privacy") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Quiet Hours")
                        Text("Block Instagram during a schedule.")
                    }
                    Switch(
                        checked = settings.quietHoursEnabled,
                        onCheckedChange = {
                            onSettingsChange(settings.copy(
                                quietHoursEnabled = it,
                                quietHoursStart = WellbeingSettings.normalizeTime(start) ?: settings.quietHoursStart,
                                quietHoursEnd = WellbeingSettings.normalizeTime(end) ?: settings.quietHoursEnd
                            ))
                        }
                    )
                }
                if (settings.quietHoursEnabled) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = start,
                            onValueChange = { start = it },
                            label = { Text("Start HH:mm") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = end,
                            onValueChange = { end = it },
                            label = { Text("End HH:mm") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dopamine-Free UI")
                        Text("Neutralize engagement and notification badges; keep DMs and Stories.")
                    }
                    Switch(
                        checked = settings.dopamineFreeUi,
                        onCheckedChange = { onSettingsChange(settings.copy(dopamineFreeUi = it)) }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ghost Mode (experimental)")
                        Text("Attempt to suppress Seen/Typing requests. Not guaranteed by Instagram Web.")
                    }
                    Switch(
                        checked = settings.ghostMode,
                        onCheckedChange = { onSettingsChange(settings.copy(ghostMode = it)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val normalizedStart = WellbeingSettings.normalizeTime(start)
                val normalizedEnd = WellbeingSettings.normalizeTime(end)
                if (normalizedStart != null && normalizedEnd != null) {
                    onSettingsChange(settings.copy(
                        quietHoursStart = normalizedStart,
                        quietHoursEnd = normalizedEnd
                    ))
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
