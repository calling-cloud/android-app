package com.example.telephone

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.telephone.model.CallState
import com.example.telephone.model.CallUi
import com.example.telephone.model.Customer
import com.example.telephone.model.Session
import com.example.telephone.model.ThemeMode
import com.example.telephone.ui.CallActiveBlue
import com.example.telephone.ui.CallBackground
import com.example.telephone.ui.CallMutedText
import com.example.telephone.ui.CallSurfaceColor
import com.example.telephone.ui.CallText
import com.example.telephone.ui.DarkAppPalette
import com.example.telephone.ui.LightAppPalette
import com.example.telephone.ui.LocalAppPalette
import com.example.telephone.ui.components.CallPanel
import com.example.telephone.ui.navigation.MainBottomBar
import com.example.telephone.ui.navigation.MainTab
import com.example.telephone.ui.screens.DialerScreen
import com.example.telephone.ui.screens.LoginScreen
import com.example.telephone.ui.screens.ProfileScreen
import com.example.telephone.ui.screens.RecordsScreen
import com.example.telephone.ui.screens.StatsScreen
import com.example.telephone.ui.screens.StatsViewModel
import com.example.telephone.ui.theme.TelephoneTheme
import com.example.telephone.update.AppUpdateDialog
import com.example.telephone.update.checkAppUpdate
import com.example.telephone.update.startAppUpdateDownload

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        setContent {
            ThemedTelephoneApp(this)
        }
    }
}

@Composable
private fun ThemedTelephoneApp(activity: ComponentActivity) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("telephone_app", Context.MODE_PRIVATE) }
    var themeMode by remember { mutableStateOf(ThemeMode.fromStorage(prefs.getString("theme_mode", null))) }
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    TelephoneTheme(darkTheme = darkTheme, dynamicColor = false) {
        CompositionLocalProvider(LocalAppPalette provides if (darkTheme) DarkAppPalette else LightAppPalette) {
            val navigationBarColor = CallSurfaceColor.toArgb()
            SideEffect {
                activity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        navigationBarColor,
                        navigationBarColor,
                    ) { darkTheme },
                )
            }
            RuntimePermissionsRequest()
            DefaultDialerRequest()
            if (!IncomingCallGate()) {
                TelephoneApp(
                    activity = activity,
                    prefs = prefs,
                    themeMode = themeMode,
                    darkTheme = darkTheme,
                    onThemeModeChange = {
                        themeMode = it
                        prefs.edit().putString("theme_mode", it.name).apply()
                    },
                )
            }
        }
    }
}

@Composable
private fun RuntimePermissionsRequest() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    LaunchedEffect(Unit) {
        val permissions = buildList {
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
            .filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (permissions.isNotEmpty()) launcher.launch(permissions.toTypedArray())
    }
}

@Composable
private fun DefaultDialerRequest() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        defaultDialerIntent(context)?.let(launcher::launch)
    }
}

@Composable
private fun IncomingCallGate(): Boolean {
    val context = LocalContext.current
    var call by remember { mutableStateOf<CallUi?>(null) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val phone = TelephoneInCallService.currentCallPhone()
            val state = TelephoneInCallService.currentCallState()
            val current = call
            val callState = when (state) {
                android.telecom.Call.STATE_RINGING -> CallState.Incoming
                android.telecom.Call.STATE_DIALING -> CallState.Dialing
                android.telecom.Call.STATE_ACTIVE -> CallState.Connected
                else -> null
            }
            val targetPhone = phone ?: "未知号码"
            when {
                callState != null -> {
                    message = ""
                    val startedAt = if (callState == CallState.Connected && current?.state != CallState.Connected) {
                        System.currentTimeMillis()
                    } else {
                        current?.startedAt ?: System.currentTimeMillis()
                    }
                    call = if (current?.customer?.phone == targetPhone) {
                        current.copy(state = callState, startedAt = startedAt)
                    } else {
                        CallUi(
                            customer = Customer(0, if (callState == CallState.Incoming) "未知来电" else "未知通话", targetPhone, "", ""),
                            state = callState,
                            startedAt = System.currentTimeMillis(),
                        )
                    }
                }
                current != null && state == null && System.currentTimeMillis() - current.startedAt > 1000 -> {
                    call = null
                }
            }
            kotlinx.coroutines.delay(300)
        }
    }

    val current = call ?: return false
    CallPanel(
        call = current,
        onAnswer = {
            if (!TelephoneInCallService.answerCurrentCall()) {
                message = "接听失败，请确认本应用是默认电话应用"
                return@CallPanel
            }
            message = ""
            CallRingtoneManager.stop()
            call = current.copy(state = CallState.Connected, startedAt = System.currentTimeMillis())
        },
        onToggleMute = {
            val muted = !current.muted
            if (setMuted(context, muted)) call = current.copy(muted = muted)
        },
        onToggleSpeaker = {
            val speaker = !current.speaker
            if (setSpeaker(context, speaker)) call = current.copy(speaker = speaker)
        },
        onHangup = {
            if (!TelephoneInCallService.hangUpCurrentCall()) {
                message = "挂断失败，请确认本应用是默认电话应用"
                return@CallPanel
            }
            CallRingtoneManager.stop()
            call = null
        },
        message = message,
    )
    return true
}

@Composable
private fun TelephoneApp(activity: ComponentActivity, prefs: SharedPreferences, themeMode: ThemeMode, darkTheme: Boolean, onThemeModeChange: (ThemeMode) -> Unit) {
    var session by remember { mutableStateOf(loadSession(prefs)) }
    val loginNavigationBarColor = CallActiveBlue.copy(alpha = 0.06f).compositeOver(CallBackground)
    val appNavigationBarColor = (if (session == null) loginNavigationBarColor else CallSurfaceColor).toArgb()
    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                appNavigationBarColor,
                appNavigationBarColor,
            ) { darkTheme },
        )
    }
    val logout: () -> Unit = {
        clearSession(prefs)
        session = null
    }
    if (session == null) {
        LoginScreen(
            initialServerUrl = if (BuildConfig.SERVER_URL_EDITABLE) prefs.getString("server_url", null) ?: BuildConfig.DEFAULT_SERVER_URL else BuildConfig.DEFAULT_SERVER_URL,
            serverUrlEditable = BuildConfig.SERVER_URL_EDITABLE,
        ) {
            saveSession(prefs, it)
            session = it
        }
    } else {
        HomeScreen(
            session = session!!,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            onLogout = logout,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(session: Session, themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Dialer) }
    var detailRecordId by rememberSaveable { mutableStateOf<Int?>(null) }
    var dialerRefreshToken by rememberSaveable { mutableIntStateOf(0) }
    var recordsRefreshToken by rememberSaveable { mutableIntStateOf(0) }
    val routeHidesChrome = detailRecordId != null
    val tab = selectedTab
    val statsViewModel: StatsViewModel = viewModel()
    val currentVersionCode = remember { AppUpdateInstaller.currentVersionCode(context) }
    var dialerHidesChrome by remember { mutableStateOf(false) }
    var recordsHidesChrome by remember { mutableStateOf(false) }
    val hideChrome = when (selectedTab) {
        MainTab.Dialer -> dialerHidesChrome
        MainTab.Records -> recordsHidesChrome
        else -> false
    }
    var update by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var updateProgress by remember { mutableStateOf(0f) }
    var updateDownloading by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }

    LaunchedEffect(session.token) {
        checkAppUpdate(
            api = session.api,
            currentVersionCode = currentVersionCode,
            onFound = {
                update = it
                updateMessage = ""
            },
        )
    }
    LaunchedEffect(session.token) {
        while (true) {
            PendingCallSyncWorker.sync(context, session)
            kotlinx.coroutines.delay(60_000)
        }
    }
    BackHandler(detailRecordId != null) {
        detailRecordId = null
    }

    Scaffold(
        containerColor = CallBackground,
        topBar = {
            if (!hideChrome && !routeHidesChrome) {
                TopAppBar(
                    title = {
                        Column {
                            Text(tab.title, color = CallText, fontWeight = FontWeight.Bold)
                            Text(session.realName, color = CallMutedText, style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CallBackground),
                )
            }
        },
        bottomBar = {
            if (!hideChrome && !routeHidesChrome) {
                MainBottomBar(
                    selected = tab,
                    onSelected = { selectedTab = it },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            KeepAliveTab(MainTab.Dialer, selectedTab, onShow = { dialerRefreshToken++ }) {
                DialerScreen(
                    session,
                    padding,
                    onAuthExpired = onLogout,
                    onChromeHiddenChange = { dialerHidesChrome = it },
                    refreshToken = dialerRefreshToken,
                )
            }
            KeepAliveTab(MainTab.Stats, selectedTab) {
                StatsScreen(session, padding, onAuthExpired = onLogout, viewModel = statsViewModel)
            }
            KeepAliveTab(MainTab.Records, selectedTab, onShow = { recordsRefreshToken++ }) {
                RecordsScreen(
                    session = session,
                    padding = padding,
                    onAuthExpired = onLogout,
                    onChromeHiddenChange = { recordsHidesChrome = it },
                    refreshToken = recordsRefreshToken,
                    onOpenDetail = { recordId -> detailRecordId = recordId },
                )
            }
            KeepAliveTab(MainTab.Profile, selectedTab) {
                ProfileScreen(session, themeMode, onThemeModeChange, onLogout, padding)
            }
            detailRecordId?.let { recordId ->
                Box(Modifier.fillMaxSize().zIndex(2f)) {
                    RecordsScreen(
                    session = session,
                    padding = padding,
                    onAuthExpired = onLogout,
                    onChromeHiddenChange = {},
                    refreshToken = recordsRefreshToken,
                    detailId = recordId,
                    onCloseDetail = { detailRecordId = null },
                )
            }
        }
        }
    }

    update?.let { info ->
        AppUpdateDialog(
            update = info,
            currentVersionCode = currentVersionCode,
            downloading = updateDownloading,
            progress = updateProgress,
            message = updateMessage,
            onUpdate = {
                startAppUpdateDownload(
                    context = context,
                    update = info,
                    onDownloading = { updateDownloading = it },
                    onProgress = { updateProgress = it },
                    onMessage = { updateMessage = it },
                )
            },
            onDismiss = { update = null },
        )
    }
}

@Composable
private fun KeepAliveTab(tab: MainTab, selectedTab: MainTab, onShow: () -> Unit = {}, content: @Composable () -> Unit) {
    val visible = tab == selectedTab
    val latestOnShow = rememberUpdatedState(onShow)
    LaunchedEffect(visible) {
        if (visible) latestOnShow.value()
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = if (visible) 1f else 0f }
            .zIndex(if (visible) 1f else 0f)
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics {}),
    ) {
        content()
    }
}
