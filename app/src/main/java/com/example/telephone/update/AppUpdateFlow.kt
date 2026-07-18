package com.example.telephone.update

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.telephone.ApiClient
import com.example.telephone.AppUpdateInfo
import com.example.telephone.AppUpdateInstaller
import com.example.telephone.runOnMain
import com.example.telephone.ui.CallActionContent
import com.example.telephone.ui.CallActiveBlue
import com.example.telephone.ui.CallButtonColor
import com.example.telephone.ui.CallHangupColor
import com.example.telephone.ui.CallMutedText
import com.example.telephone.ui.CallText
import com.example.telephone.ui.UiButtonHeight
import com.example.telephone.ui.UiButtonRadius
import com.example.telephone.ui.UiCardPadding
import com.example.telephone.ui.components.AppCard
import kotlin.concurrent.thread

internal fun checkAppUpdate(
    api: ApiClient,
    currentVersionCode: Long,
    onFound: (AppUpdateInfo) -> Unit,
    onCurrent: () -> Unit = {},
    onError: (String) -> Unit = {},
    onDone: () -> Unit = {},
) {
    thread {
        runCatching { api.appVersion() }
            .onSuccess { info ->
                runOnMain {
                    if (info.hasUpdate(currentVersionCode)) onFound(info) else onCurrent()
                }
            }
            .onFailure { error -> runOnMain { onError(error.message ?: "检查更新失败") } }
        runOnMain(onDone)
    }
}

internal fun startAppUpdateDownload(
    context: Context,
    update: AppUpdateInfo,
    onDownloading: (Boolean) -> Unit,
    onProgress: (Float) -> Unit,
    onMessage: (String) -> Unit,
) {
    onDownloading(true)
    onMessage("")
    onProgress(0f)
    thread {
        runCatching {
            AppUpdateInstaller.download(context, update) { progress ->
                runOnMain { onProgress(progress) }
            }
        }.onSuccess { file ->
            runOnMain {
                onDownloading(false)
                val installing = AppUpdateInstaller.install(context, file)
                onMessage(if (installing) "请在系统安装器中完成更新" else "请允许安装未知应用后再次点击更新")
            }
        }.onFailure { error ->
            runOnMain {
                onDownloading(false)
                onMessage(error.message ?: "更新失败")
            }
        }
    }
}

@Composable
internal fun AppUpdateDialog(
    update: AppUpdateInfo,
    currentVersionCode: Long,
    downloading: Boolean,
    progress: Float,
    message: String,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val forced = update.isForced(currentVersionCode)
    Dialog(onDismissRequest = { if (!forced && !downloading) onDismiss() }) {
        AppCard {
            Column(Modifier.fillMaxWidth().padding(UiCardPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (forced) "需要更新" else "发现新版本",
                    color = CallText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "版本 ${update.versionName.ifBlank { update.versionCode.toString() }}",
                    color = CallMutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = update.changelog.ifBlank { "建议更新到最新版本。" },
                    color = CallText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (downloading) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)),
                        color = CallActiveBlue,
                        trackColor = CallButtonColor,
                    )
                    Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", color = CallMutedText, style = MaterialTheme.typography.labelMedium)
                }
                if (message.isNotBlank()) {
                    Text(message, color = if (message.startsWith("请")) CallMutedText else CallHangupColor, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    if (!forced) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !downloading,
                            modifier = Modifier.weight(1f).height(UiButtonHeight),
                            shape = RoundedCornerShape(UiButtonRadius),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CallText),
                        ) {
                            Text("稍后")
                        }
                    }
                    Button(
                        onClick = onUpdate,
                        enabled = !downloading,
                        modifier = Modifier.weight(1f).height(UiButtonHeight),
                        shape = RoundedCornerShape(UiButtonRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = CallActiveBlue, contentColor = CallActionContent),
                    ) {
                        Text(if (downloading) "下载中" else "立即更新", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
