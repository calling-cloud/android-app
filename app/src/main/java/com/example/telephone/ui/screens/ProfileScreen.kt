package com.example.telephone.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.telephone.AppUpdateInfo
import com.example.telephone.AppUpdateInstaller
import com.example.telephone.CallRingtoneManager
import com.example.telephone.RingtoneOption
import com.example.telephone.defaultDialerIntent
import com.example.telephone.isDefaultDialer
import com.example.telephone.model.AuthExpiredException
import com.example.telephone.model.Session
import com.example.telephone.model.ThemeMode
import com.example.telephone.runOnMain
import com.example.telephone.ui.CallActionContent
import com.example.telephone.ui.CallActiveBlue
import com.example.telephone.ui.CallBackground
import com.example.telephone.ui.CallButtonColor
import com.example.telephone.ui.CallHangupColor
import com.example.telephone.ui.CallMutedText
import com.example.telephone.ui.CallText
import com.example.telephone.ui.UiButtonHeight
import com.example.telephone.ui.UiButtonRadius
import com.example.telephone.ui.UiCardPadding
import com.example.telephone.ui.UiListPadding
import com.example.telephone.ui.components.AppCard
import com.example.telephone.ui.components.Avatar
import com.example.telephone.ui.components.DarkTextField
import com.example.telephone.update.AppUpdateDialog
import com.example.telephone.update.checkAppUpdate
import com.example.telephone.update.startAppUpdateDownload
import kotlin.concurrent.thread

@Composable
internal fun ProfileScreen(
    session: Session,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAuthExpired: () -> Unit,
    onLogout: () -> Unit,
    padding: PaddingValues,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentVersionCode = remember { AppUpdateInstaller.currentVersionCode(context) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var defaultDialer by remember { mutableStateOf(isDefaultDialer(context)) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var updateProgress by remember { mutableStateOf(0f) }
    var updateDownloading by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    var showRingtoneDialog by remember { mutableStateOf(false) }
    var selectedRingtone by remember { mutableStateOf(CallRingtoneManager.selected(context)) }
    var pendingRingtone by remember { mutableStateOf(selectedRingtone) }
    var deleteRingtone by remember { mutableStateOf<RingtoneOption?>(null) }
    var ringtoneMessage by remember { mutableStateOf("") }
    val defaultDialerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        defaultDialer = isDefaultDialer(context)
    }
    val ringtoneImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        ringtoneMessage = "正在导入铃声..."
        thread {
            CallRingtoneManager.import(context, uri)
                .onSuccess { runOnMain { pendingRingtone = it; ringtoneMessage = "已导入，点击确认生效" } }
                .onFailure { runOnMain { ringtoneMessage = it.message ?: "导入失败" } }
        }
    }

    fun checkUpdate() {
        checkingUpdate = true
        updateMessage = ""
        checkAppUpdate(
            api = session.api,
            currentVersionCode = currentVersionCode,
            onFound = {
                update = it
                updateMessage = ""
            },
            onCurrent = { updateMessage = "当前已是最新版本" },
            onError = { updateMessage = it },
            onDone = { checkingUpdate = false },
        )
    }

    if (showPasswordDialog) {
        Dialog(onDismissRequest = { if (!loading) showPasswordDialog = false }) {
            AppCard {
                Column(Modifier.fillMaxWidth().padding(UiCardPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("修改密码", fontWeight = FontWeight.Bold, color = CallText, style = MaterialTheme.typography.titleMedium)
                        Text("输入原密码和不少于 6 位的新密码。", color = CallMutedText, style = MaterialTheme.typography.bodySmall)
                    }
                    DarkTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = "原密码",
                        placeholder = "请输入原密码",
                        showLeadingMark = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    DarkTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "新密码",
                        placeholder = "至少 6 位",
                        showLeadingMark = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    if (message.isNotBlank()) Text(message, color = CallMutedText)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            enabled = !loading,
                            onClick = { showPasswordDialog = false },
                            modifier = Modifier.weight(1f).height(UiButtonHeight),
                            shape = RoundedCornerShape(UiButtonRadius),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CallText),
                        ) { Text("取消") }
                        Button(
                            enabled = !loading && oldPassword.isNotBlank() && newPassword.length >= 6,
                            onClick = {
                                loading = true
                                message = ""
                                thread {
                                    runCatching { session.api.changePassword(session.token, oldPassword, newPassword) }
                                        .onSuccess { runOnMain { oldPassword = ""; newPassword = ""; message = "密码已修改"; showPasswordDialog = false } }
                                        .onFailure { runOnMain { if (it is AuthExpiredException) onAuthExpired() else message = it.message ?: "修改失败" } }
                                    runOnMain { loading = false }
                                }
                            },
                            modifier = Modifier.weight(1f).height(UiButtonHeight),
                            shape = RoundedCornerShape(UiButtonRadius),
                            colors = ButtonDefaults.buttonColors(containerColor = CallActiveBlue, contentColor = CallActionContent, disabledContainerColor = CallButtonColor),
                        ) { Text(if (loading) "保存中" else "保存") }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        Dialog(onDismissRequest = { showLogoutDialog = false }) {
            AppCard {
                Column(Modifier.fillMaxWidth().padding(UiCardPadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("退出登录", color = CallText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("确认退出当前账号吗？退出后需要重新登录。", color = CallMutedText, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showLogoutDialog = false },
                            modifier = Modifier.weight(1f).height(UiButtonHeight),
                            shape = RoundedCornerShape(UiButtonRadius),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CallText),
                        ) { Text("取消") }
                        Button(
                            onClick = {
                                showLogoutDialog = false
                                onLogout()
                            },
                            modifier = Modifier.weight(1f).height(UiButtonHeight),
                            shape = RoundedCornerShape(UiButtonRadius),
                            colors = ButtonDefaults.buttonColors(containerColor = CallHangupColor, contentColor = CallActionContent),
                        ) { Text("退出") }
                    }
                }
            }
        }
    }

    if (showRingtoneDialog) {
        RingtoneDialog(
            options = CallRingtoneManager.options(context),
            selected = pendingRingtone,
            message = ringtoneMessage,
            onSelect = {
                pendingRingtone = it
                ringtoneMessage = ""
            },
            onImport = { ringtoneImportLauncher.launch(arrayOf("audio/*")) },
            onDelete = { deleteRingtone = it },
            onConfirm = {
                CallRingtoneManager.select(context, pendingRingtone)
                selectedRingtone = pendingRingtone
                ringtoneMessage = ""
                showRingtoneDialog = false
            },
            onDismiss = { showRingtoneDialog = false },
        )
    }

    deleteRingtone?.let { option ->
        Dialog(onDismissRequest = { deleteRingtone = null }) {
            AppCard {
                Column(Modifier.fillMaxWidth().padding(UiCardPadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("删除铃声", color = CallText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("确认删除 ${option.label} 吗？", color = CallMutedText, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { deleteRingtone = null },
                            modifier = Modifier.weight(1f).height(UiButtonHeight),
                            shape = RoundedCornerShape(UiButtonRadius),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CallText),
                        ) { Text("取消") }
                        Button(
                            onClick = {
                                val current = CallRingtoneManager.delete(context, option)
                                selectedRingtone = current
                                if (pendingRingtone.key == option.key) pendingRingtone = current
                                ringtoneMessage = "已删除 ${option.label}"
                                deleteRingtone = null
                            },
                            modifier = Modifier.weight(1f).height(UiButtonHeight),
                            shape = RoundedCornerShape(UiButtonRadius),
                            colors = ButtonDefaults.buttonColors(containerColor = CallHangupColor, contentColor = CallActionContent),
                        ) { Text("删除") }
                    }
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

    LazyColumn(Modifier.fillMaxSize().background(CallBackground).padding(padding), contentPadding = PaddingValues(UiListPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AppCard {
                Column(Modifier.fillMaxWidth().padding(UiCardPadding), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(session.realName, 72)
                    Spacer(Modifier.height(10.dp))
                    Text(session.realName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = CallText)
                    Text(session.username, color = CallMutedText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            AppCard {
                Column(Modifier.fillMaxWidth().padding(UiCardPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("外观", fontWeight = FontWeight.Bold, color = CallText)
                        Text("选择亮色、暗色，或跟随系统设置。", color = CallMutedText, style = MaterialTheme.typography.bodySmall)
                    }
                    ThemeModeSelector(themeMode, onThemeModeChange)
                }
            }
        }
        item {
            SettingsAction(
                "默认电话应用",
                if (defaultDialer) "已设置，可接管通话和自动录音" else "设置后才能接管通话和自动录音",
            ) {
                defaultDialerIntent(context)?.let(defaultDialerLauncher::launch)
                defaultDialer = isDefaultDialer(context)
            }
        }
        item {
            SettingsAction("来电铃声", selectedRingtone.label) {
                pendingRingtone = selectedRingtone
                ringtoneMessage = ""
                showRingtoneDialog = true
            }
        }
        item { SettingsAction("修改密码", "更新当前账号的登录密码") { showPasswordDialog = true } }
        item {
            SettingsAction(
                "检查更新",
                when {
                    checkingUpdate -> "正在检查..."
                    updateMessage.isNotBlank() -> updateMessage
                    else -> "查看是否有新版本"
                },
            ) {
                if (!checkingUpdate) checkUpdate()
            }
        }
        item {
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(UiButtonHeight),
                shape = RoundedCornerShape(UiButtonRadius),
                colors = ButtonDefaults.buttonColors(containerColor = CallHangupColor, contentColor = CallActionContent),
            ) { Text("退出登录") }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RingtoneDialog(
    options: List<RingtoneOption>,
    selected: RingtoneOption,
    message: String,
    onSelect: (RingtoneOption) -> Unit,
    onImport: () -> Unit,
    onDelete: (RingtoneOption) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        AppCard {
            Column(Modifier.fillMaxWidth().padding(UiCardPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text("来电铃声", fontWeight = FontWeight.Bold, color = CallText, style = MaterialTheme.typography.titleMedium)
                    Text("选择内置铃声，或从文件导入音频。", color = CallMutedText, style = MaterialTheme.typography.bodySmall)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CallButtonColor)
                                .clickable(onClick = onImport)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.FileUpload, "导入铃声", tint = CallActiveBlue)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("导入铃声", color = CallText, fontWeight = FontWeight.Bold)
                                Text("从文件系统选择可播放音频", color = CallMutedText, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    options.forEach { option ->
                        item(key = option.key) {
                            val checked = option.key == selected.key
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (checked) CallButtonColor else Color.Transparent)
                                    .combinedClickable(
                                        onClick = { onSelect(option) },
                                        onLongClick = { if (option.imported) onDelete(option) },
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.MusicNote, option.label, tint = if (checked) CallActiveBlue else CallMutedText)
                                Spacer(Modifier.width(10.dp))
                                Text(option.label, modifier = Modifier.weight(1f), color = CallText, fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal)
                                if (checked) Icon(Icons.Filled.Check, "已选中", tint = CallActiveBlue)
                            }
                        }
                    }
                }
                if (message.isNotBlank()) Text(message, color = CallMutedText, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(UiButtonHeight),
                        shape = RoundedCornerShape(UiButtonRadius),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CallText),
                    ) { Text("取消") }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(UiButtonHeight),
                        shape = RoundedCornerShape(UiButtonRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = CallActiveBlue, contentColor = CallActionContent),
                    ) { Text("确认") }
                }
            }
        }
    }
}

@Composable
private fun SettingsAction(title: String, subtitle: String, onClick: () -> Unit) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(UiCardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = CallText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = CallMutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(value: ThemeMode, onChange: (ThemeMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEach { mode ->
            val selected = value == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selected) CallActiveBlue else CallButtonColor)
                    .clickable { onChange(mode) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(mode.label, color = if (selected) Color.White else CallText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
