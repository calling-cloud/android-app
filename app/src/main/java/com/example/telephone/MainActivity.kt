package com.example.telephone

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

private const val RecordDetailRoute = "recordDetail/{recordId}"

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
        val permissions = listOf(Manifest.permission.CALL_PHONE, Manifest.permission.RECORD_AUDIO)
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
            val phone = TelephoneInCallService.currentRingingPhone()
            val state = TelephoneInCallService.currentCallState()
            val current = call
            when {
                phone != null && current?.state != CallState.Connected -> {
                    message = ""
                    call = CallUi(
                        customer = Customer(0, "未知来电", phone, "", ""),
                        state = CallState.Incoming,
                        startedAt = System.currentTimeMillis(),
                    )
                }
                current?.state == CallState.Incoming && phone == null && state != android.telecom.Call.STATE_ACTIVE -> {
                    call = null
                }
                current?.state == CallState.Connected && state == null && System.currentTimeMillis() - current.startedAt > 1000 -> {
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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val routeHidesChrome = currentRoute == RecordDetailRoute
    val tab = MainTab.fromRoute(currentRoute)
    val statsViewModel: StatsViewModel = viewModel()
    val currentVersionCode = remember { AppUpdateInstaller.currentVersionCode(context) }
    var hideChrome by remember { mutableStateOf(false) }
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
                    onSelected = { selected ->
                        navController.navigate(selected.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.Dialer.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(MainTab.Dialer.route) {
                DialerScreen(session, padding, onAuthExpired = onLogout, onChromeHiddenChange = { hideChrome = it })
            }
            composable(MainTab.Stats.route) {
                StatsScreen(session, padding, onAuthExpired = onLogout, viewModel = statsViewModel)
            }
            composable(MainTab.Records.route) {
                RecordsScreen(
                    session = session,
                    padding = padding,
                    onAuthExpired = onLogout,
                    onChromeHiddenChange = { hideChrome = it },
                    onOpenDetail = { recordId -> navController.navigate("recordDetail/$recordId") },
                )
            }
            composable(
                route = RecordDetailRoute,
                arguments = listOf(navArgument("recordId") { type = NavType.IntType }),
            ) { entry ->
                val recordId = entry.arguments?.getInt("recordId") ?: return@composable
                RecordsScreen(
                    session = session,
                    padding = padding,
                    onAuthExpired = onLogout,
                    onChromeHiddenChange = { hideChrome = it },
                    detailId = recordId,
                    onCloseDetail = { navController.popBackStack() },
                )
            }
            composable(MainTab.Profile.route) {
                ProfileScreen(session, themeMode, onThemeModeChange, onLogout, padding)
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
