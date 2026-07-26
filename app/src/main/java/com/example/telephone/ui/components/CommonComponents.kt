package com.example.telephone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.telephone.formatDuration
import com.example.telephone.formatPhone
import com.example.telephone.model.CallState
import com.example.telephone.model.CallUi
import com.example.telephone.model.Customer
import com.example.telephone.ui.CallAcceptColor
import com.example.telephone.ui.CallActionContent
import com.example.telephone.ui.CallActiveBlue
import com.example.telephone.ui.CallAvatarColor
import com.example.telephone.ui.CallAvatarRing
import com.example.telephone.ui.CallBackground
import com.example.telephone.ui.CallButtonColor
import com.example.telephone.ui.CallHangupColor
import com.example.telephone.ui.CallHeartColor
import com.example.telephone.ui.CallInactiveHeart
import com.example.telephone.ui.CallInputBorder
import com.example.telephone.ui.CallInputColor
import com.example.telephone.ui.CallMutedText
import com.example.telephone.ui.CallPlaceholderText
import com.example.telephone.ui.CallSurfaceColor
import com.example.telephone.ui.CallText
import com.example.telephone.ui.UiButtonHeight
import com.example.telephone.ui.UiButtonRadius
import com.example.telephone.ui.UiCardPadding
import com.example.telephone.ui.UiIconSize
import com.example.telephone.ui.intentLevelLabel

@Composable
internal fun CustomerCard(
    customer: Customer?,
    loading: Boolean,
    message: String,
    page: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
    onMarkInvalid: () -> Unit,
    onMarkDeal: () -> Unit,
    onCopyPhone: ((Customer) -> Unit)? = null,
    onCall: (Customer) -> Unit,
) {
    val progressPage = if (total > 0) page.coerceAtMost(total) else 0
    AppCard {
        Column(Modifier.fillMaxWidth().padding(UiCardPadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("待联系客户", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CallText)
                    Text("当前进度：($progressPage/$total)", color = CallMutedText, style = MaterialTheme.typography.bodySmall)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(CallActiveBlue)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("拨号池", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            when {
                loading -> CircularProgressIndicator(color = CallActiveBlue)
                customer == null -> {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        Text(message.ifBlank { "暂无客户" }, color = CallMutedText)
                    }
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(customer.name, 64)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(customer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = CallText)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    formatPhone(customer.phone),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = CallMutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (onCopyPhone != null) {
                                    IconButton(
                                        onClick = { onCopyPhone(customer) },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, "复制电话", modifier = Modifier.size(18.dp), tint = CallMutedText)
                                    }
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        InfoPill(customer.schoolName.ifBlank { "未知学校" }, Modifier.weight(1f))
                        InfoPill(customer.gradeName.ifBlank { "未知年级" }, Modifier.weight(1f))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CallInputColor),
                    ) {
                        Column(Modifier.padding(UiCardPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("跟进提示", color = CallText, fontWeight = FontWeight.Bold)
                            Text("确认客户需求、记录意向度，通话结束后补充备注。", color = CallMutedText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    enabled = !loading && page > 1,
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f).height(UiButtonHeight).defaultMinSize(minWidth = 0.dp),
                    shape = RoundedCornerShape(UiButtonRadius),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CallText),
                ) {
                    Icon(Icons.Filled.ChevronLeft, "上一位", modifier = Modifier.size(UiIconSize))
                }
                OutlinedButton(
                    enabled = !loading && page > 1,
                    onClick = onReset,
                    modifier = Modifier.weight(1f).height(UiButtonHeight).defaultMinSize(minWidth = 0.dp),
                    shape = RoundedCornerShape(UiButtonRadius),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CallText),
                ) {
                    Icon(Icons.Filled.Autorenew, "重置", modifier = Modifier.size(UiIconSize))
                }
                OutlinedButton(
                    enabled = !loading && customer != null,
                    onClick = onMarkInvalid,
                    modifier = Modifier.weight(1f).height(UiButtonHeight).defaultMinSize(minWidth = 0.dp),
                    shape = RoundedCornerShape(UiButtonRadius),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CallText),
                ) {
                    Icon(Icons.Filled.PersonOff, "标记为无效", modifier = Modifier.size(UiIconSize))
                }
                OutlinedButton(
                    enabled = !loading && customer != null,
                    onClick = onMarkDeal,
                    modifier = Modifier.weight(1f).height(UiButtonHeight).defaultMinSize(minWidth = 0.dp),
                    shape = RoundedCornerShape(UiButtonRadius),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CallText),
                ) {
                    Icon(Icons.Filled.Gavel, "标记为成交", modifier = Modifier.size(UiIconSize))
                }
                OutlinedButton(
                    enabled = !loading && total > 0 && page < total,
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(UiButtonHeight).defaultMinSize(minWidth = 0.dp),
                    shape = RoundedCornerShape(UiButtonRadius),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CallText),
                ) {
                    Icon(Icons.Filled.ChevronRight, "下一位", modifier = Modifier.size(UiIconSize))
                }
            }
            Button(
                enabled = customer != null,
                onClick = { customer?.let(onCall) },
                modifier = Modifier.fillMaxWidth().height(UiButtonHeight),
                shape = RoundedCornerShape(UiButtonRadius),
                colors = ButtonDefaults.buttonColors(containerColor = CallActiveBlue, contentColor = CallActionContent, disabledContainerColor = CallButtonColor),
            ) {
                Icon(painterResource(android.R.drawable.ic_menu_call), "拨号", modifier = Modifier.size(UiIconSize))
                Spacer(Modifier.width(8.dp))
                Text("拨号")
            }
        }
    }
}

@Composable
internal fun InfoPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CallButtonColor)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = CallText, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CallSurfaceColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(android.R.drawable.ic_menu_info_details), text, tint = CallMutedText, modifier = Modifier.size(UiIconSize))
            Text(text, color = CallMutedText)
        }
    }
}

@Composable
internal fun CallPanel(call: CallUi, onAnswer: () -> Unit, onToggleMute: () -> Unit, onToggleSpeaker: () -> Unit, onHangup: () -> Unit, message: String = "") {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(call.state) {
        while (call.state == CallState.Connected) {
            kotlinx.coroutines.delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val duration = if (call.state == CallState.Connected) ((now - call.startedAt) / 1000).toInt() else call.durationSeconds
    ImmersiveCallSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))
            Avatar(name = call.customer.name, size = 104)
            Spacer(Modifier.height(22.dp))
            Text(
                call.customer.name,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CallText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(formatPhone(call.customer.phone), style = MaterialTheme.typography.bodyLarge, color = CallMutedText)
            Spacer(Modifier.height(10.dp))
            Text(
                text = when (call.state) {
                    CallState.Dialing -> "拨号中"
                    CallState.Incoming -> "来电中"
                    CallState.Connected -> formatDuration(duration)
                    CallState.Ended -> formatDuration(duration)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CallMutedText,
            )
            Spacer(Modifier.weight(1f))
            if (call.state == CallState.Incoming) {
                Row(horizontalArrangement = Arrangement.spacedBy(64.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundAction(android.R.drawable.ic_menu_call, "挂断", selected = true, actionColor = CallHangupColor, size = 76, onClick = onHangup)
                    RoundAction(android.R.drawable.ic_menu_call, "接听", selected = true, actionColor = CallAcceptColor, size = 76, onClick = onAnswer)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(52.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundAction(android.R.drawable.ic_lock_silent_mode, if (call.muted) "取消静音" else "静音", selected = call.muted, onClick = onToggleMute)
                    RoundAction(android.R.drawable.ic_lock_silent_mode_off, if (call.speaker) "关闭免提" else "免提", selected = call.speaker, onClick = onToggleSpeaker)
                }
                Spacer(Modifier.height(48.dp))
                RoundAction(android.R.drawable.ic_menu_call, "挂断", selected = true, actionColor = CallHangupColor, size = 76, onClick = onHangup)
            }
            if (message.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(message, color = CallHangupColor, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
internal fun RoundAction(icon: Int, label: String, selected: Boolean = false, actionColor: Color = CallActiveBlue, size: Int = 60, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(if (selected) actionColor else CallButtonColor),
        ) {
            Icon(painterResource(icon), label, tint = if (selected) CallActionContent else CallText, modifier = Modifier.size(if (size > 60) 28.dp else 24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CallText)
    }
}

@Composable
internal fun CallResultForm(call: CallUi, message: String = "", marking: Boolean = false, onMarkInvalid: () -> Unit, onMarkDeal: () -> Unit, onSubmit: (Int, String) -> Unit) {
    var intent by remember { mutableIntStateOf(1) }
    var remark by remember { mutableStateOf("") }
    var pendingStatus by remember { mutableIntStateOf(0) }
    if (pendingStatus != 0) {
        AlertDialog(
            onDismissRequest = { pendingStatus = 0 },
            shape = RoundedCornerShape(8.dp),
            containerColor = CallBackground,
            titleContentColor = CallText,
            textContentColor = CallMutedText,
            title = { Text("提示") },
            text = { Text(if (pendingStatus == 4) "确认将当前客户标记为无效吗？" else "确认将当前客户标记为成交吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        val status = pendingStatus
                        pendingStatus = 0
                        if (status == 4) onMarkInvalid() else onMarkDeal()
                    },
                    shape = RoundedCornerShape(UiButtonRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = CallActiveBlue, contentColor = CallActionContent),
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { pendingStatus = 0 }) {
                    Text("取消", color = CallMutedText)
                }
            },
        )
    }
    ImmersiveCallSurface {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(56.dp))
                Avatar(name = call.customer.name, size = 76)
                Spacer(Modifier.height(14.dp))
                Text(
                    call.customer.name,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CallText,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(formatPhone(call.customer.phone), style = MaterialTheme.typography.bodyMedium, color = CallMutedText)
                Spacer(Modifier.height(8.dp))
                Text("通话已结束", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CallHangupColor)
                Spacer(Modifier.height(6.dp))
                Text(formatDuration(call.durationSeconds), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CallMutedText)
                Spacer(Modifier.height(22.dp))
                RecordingUploadProgress(call)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("客户意向度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CallText)
                    Text("$intent ${intentLevelLabel(intent)}", style = MaterialTheme.typography.labelMedium, color = CallMutedText, maxLines = 1)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    (1..5).forEach { star ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (intent == star) CallActiveBlue else CallButtonColor)
                                .clickable { intent = star },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = star.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (intent == star) CallActionContent else CallText,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "通话备注",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CallText,
                    textAlign = TextAlign.Start,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("请输入客户跟进细节、需求点或下次联系计划...") },
                    minLines = 5,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CallText,
                        unfocusedTextColor = CallText,
                        focusedBorderColor = CallInputBorder,
                        unfocusedBorderColor = CallInputBorder,
                        focusedContainerColor = CallInputColor,
                        unfocusedContainerColor = CallInputColor,
                        focusedPlaceholderColor = CallPlaceholderText,
                        unfocusedPlaceholderColor = CallPlaceholderText,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                if (message.isNotBlank()) {
                    Text(message, color = if (message.startsWith("已")) CallMutedText else CallHangupColor, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(64.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .background(CallBackground),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { pendingStatus = 4 },
                            enabled = !marking,
                            modifier = Modifier.weight(1f).height(UiButtonHeight).defaultMinSize(minWidth = 0.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(UiButtonRadius),
                            colors = ButtonDefaults.buttonColors(containerColor = CallButtonColor, contentColor = CallText),
                        ) {
                            Icon(Icons.Filled.PersonOff, "标记为无效", modifier = Modifier.size(UiIconSize))
                        }
                        Button(
                            onClick = { pendingStatus = 3 },
                            enabled = !marking,
                            modifier = Modifier.weight(1f).height(UiButtonHeight).defaultMinSize(minWidth = 0.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(UiButtonRadius),
                            colors = ButtonDefaults.buttonColors(containerColor = CallButtonColor, contentColor = CallText),
                        ) {
                            Icon(Icons.Filled.Gavel, "标记为成交", modifier = Modifier.size(UiIconSize))
                        }
                    }
                    Button(
                        onClick = { onSubmit(intent, remark) },
                        enabled = !call.uploadingRecording && !marking,
                        modifier = Modifier.weight(1f).height(UiButtonHeight).defaultMinSize(minWidth = 0.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(UiButtonRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = CallActiveBlue, contentColor = CallActionContent),
                    ) {
                        Text(
                            if (call.uploadingRecording) "录音上传中" else "保存并退出",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        }
                    }
                }
            }
    }
}

@Composable
internal fun RecordingUploadProgress(call: CallUi) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("录音上传", color = CallText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("${(call.uploadProgress * 100).toInt()}%", color = CallMutedText, style = MaterialTheme.typography.labelMedium)
            }
            LinearProgressIndicator(
                progress = { call.uploadProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)),
                color = CallActiveBlue,
                trackColor = CallButtonColor,
            )
            Text(
                when {
                    call.uploadingRecording -> "正在分片上传，每片最大 2MB"
                    call.recordingUrl?.isNotBlank() == true -> "上传完成"
                    call.uploadError.isNotBlank() -> call.uploadError
                    else -> "无录音文件"
                },
                color = if (call.uploadError.isNotBlank()) CallHangupColor else CallMutedText,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
internal fun ImmersiveCallSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CallBackground),
    ) {
        content()
    }
}

@Composable
internal fun AppCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CallSurfaceColor),
    ) {
        content()
    }
}

@Composable
internal fun AutoSearchChoiceRow(
    label: String,
    options: List<Pair<Int?, String>>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    searchThreshold: Int = 8,
    resetKey: Any? = Unit,
) {
    val searchable = options.size > searchThreshold
    var query by rememberSaveable(label, resetKey) { mutableStateOf("") }
    LaunchedEffect(searchable) {
        if (!searchable && query.isNotBlank()) query = ""
    }
    val visibleOptions = remember(options, query, selected, searchable) {
        val trimmed = query.trim()
        if (!searchable || trimmed.isBlank()) {
            options
        } else {
            val stickyOption = options.firstOrNull { it.first == null }
            val selectedOption = selected?.let { current -> options.firstOrNull { it.first == current } }
            buildList {
                stickyOption?.let { add(it) }
                if (selectedOption != null && selectedOption != stickyOption) {
                    add(selectedOption)
                }
                options.asSequence()
                    .filter { it.second.contains(trimmed, ignoreCase = true) }
                    .filterNot { it == stickyOption || it == selectedOption }
                    .forEach { add(it) }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = CallMutedText, style = MaterialTheme.typography.labelMedium)
        if (searchable) {
            ChoiceSearchField(query, onValueChange = { query = it })
        }
        ChoiceChipRow(visibleOptions, selected, onSelect)
    }
}

@Composable
private fun ChoiceSearchField(value: String, onValueChange: (String) -> Unit) {
    Row(
        Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(CallButtonColor)
            .padding(start = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(android.R.drawable.ic_menu_search),
            contentDescription = "搜索",
            tint = CallMutedText,
            modifier = Modifier.size(14.dp),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isBlank()) {
                Text("搜索", color = CallMutedText, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.labelMedium.copy(color = CallText),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotBlank()) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = "清除搜索",
                    tint = CallMutedText,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ChoiceChipRow(options: List<Pair<Int?, String>>, selected: Int?, onSelect: (Int?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, text) ->
            ChoiceChip(text, selected == value) { onSelect(value) }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(30.dp),
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) CallActiveBlue else CallButtonColor,
            contentColor = if (selected) CallActionContent else CallText,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
internal fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    showLeadingMark: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        placeholder = if (placeholder.isBlank()) null else ({ Text(placeholder) }),
        leadingIcon = if (!showLeadingMark) null else ({
            Box(
                Modifier
                    .height(20.dp)
                    .width(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CallActiveBlue),
            )
        }),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = CallText,
            unfocusedTextColor = CallText,
            focusedLabelColor = CallActiveBlue,
            unfocusedLabelColor = CallMutedText,
            cursorColor = CallText,
            focusedBorderColor = CallActiveBlue,
            unfocusedBorderColor = CallInputBorder,
            focusedLeadingIconColor = CallActiveBlue,
            unfocusedLeadingIconColor = CallMutedText,
            focusedContainerColor = CallInputColor,
            unfocusedContainerColor = CallInputColor,
            focusedPlaceholderColor = CallPlaceholderText,
            unfocusedPlaceholderColor = CallPlaceholderText,
        ),
    )
}

@Composable
internal fun Avatar(name: String, size: Int) {
    val textStyle = if (size <= 64) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(CallAvatarRing),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size((size - 10).dp)
                .clip(CircleShape)
                .background(CallAvatarColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1).ifBlank { "客" }, color = Color.White, style = textStyle, fontWeight = FontWeight.Bold)
        }
    }
}
