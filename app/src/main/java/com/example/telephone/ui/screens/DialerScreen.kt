package com.example.telephone.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.telephone.CallRecordingManager
import com.example.telephone.PendingCallSync
import com.example.telephone.PendingCallSyncCache
import com.example.telephone.TelephoneInCallService
import com.example.telephone.UnknownPhone
import com.example.telephone.callDisplayName
import com.example.telephone.formatCallTime
import com.example.telephone.formatDuration
import com.example.telephone.isFinishedSystemCallState
import com.example.telephone.isUnknownCallName
import com.example.telephone.model.AuthExpiredException
import com.example.telephone.model.AssignedCustomerPage
import com.example.telephone.model.CallRecord
import com.example.telephone.model.CallState
import com.example.telephone.model.CallUi
import com.example.telephone.model.Customer
import com.example.telephone.model.GradeOption
import com.example.telephone.model.Session
import com.example.telephone.model.SchoolOption
import com.example.telephone.placeCall
import com.example.telephone.runOnMain
import com.example.telephone.setMuted
import com.example.telephone.setSpeaker
import com.example.telephone.ui.CallActiveBlue
import com.example.telephone.ui.CallActionContent
import com.example.telephone.ui.CallBackground
import com.example.telephone.ui.CallButtonColor
import com.example.telephone.ui.CallMutedText
import com.example.telephone.ui.CallSurfaceColor
import com.example.telephone.ui.CallText
import com.example.telephone.ui.UiButtonRadius
import com.example.telephone.ui.UiCardPadding
import com.example.telephone.ui.UiListPadding
import com.example.telephone.ui.components.AppCard
import com.example.telephone.ui.components.AutoSearchChoiceRow
import com.example.telephone.ui.components.CallPanel
import com.example.telephone.ui.components.CallResultForm
import com.example.telephone.ui.components.CustomerCard
import java.io.File
import kotlin.concurrent.thread

@Composable
internal fun DialerScreen(
    session: Session,
    padding: PaddingValues,
    onAuthExpired: () -> Unit,
    onChromeHiddenChange: (Boolean) -> Unit,
    refreshToken: Int,
) {
    val context = LocalContext.current
    var customer by remember { mutableStateOf<Customer?>(null) }
    var customerPage by remember { mutableIntStateOf(1) }
    var customerTotal by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var callMessage by remember { mutableStateOf("") }
    var callSubmitting by remember { mutableStateOf(false) }
    var customerStatusMarking by remember { mutableStateOf(false) }
    var call by remember { mutableStateOf<CallUi?>(null) }
    var callCustomerStatusChanged by remember { mutableStateOf(false) }
    var callStatusMarking by remember { mutableStateOf(false) }
    var pendingConfirm by remember { mutableStateOf<String?>(null) }
    var callRecords by remember { mutableStateOf<List<CallRecord>>(emptyList()) }
    var callRecordsTotal by remember { mutableIntStateOf(0) }
    var callRecordsLoading by remember { mutableStateOf(false) }
    var callRecordsMessage by remember { mutableStateOf("") }
    var schoolOptions by remember { mutableStateOf<List<SchoolOption>>(emptyList()) }
    var gradeOptions by remember { mutableStateOf<List<GradeOption>>(emptyList()) }
    var selectedSchoolId by remember { mutableStateOf<Int?>(null) }
    var selectedGradeCode by remember { mutableStateOf<Int?>(null) }
    var filterResetToken by remember { mutableIntStateOf(0) }

    fun copyCustomerPhone(target: Customer) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("客户电话", target.phone))
        Toast.makeText(context, "电话号码已复制", Toast.LENGTH_SHORT).show()
    }

    fun loadCustomerCallRecords(customerId: Int) {
        callRecords = emptyList()
        callRecordsTotal = 0
        callRecordsLoading = true
        callRecordsMessage = ""
        thread {
            runCatching { session.api.customerCallRecords(session.token, customerId) }
                .onSuccess { page ->
                    runOnMain {
                        if (customer?.id == customerId) {
                            callRecords = page.items
                            callRecordsTotal = page.total
                            callRecordsMessage = when {
                                page.message.isNotBlank() -> page.message
                                page.items.isEmpty() -> "暂无通话记录"
                                else -> ""
                            }
                        }
                    }
                }
                .onFailure {
                    runOnMain {
                        if (customer?.id == customerId) {
                            if (it is AuthExpiredException) onAuthExpired() else callRecordsMessage = it.message ?: "获取通话记录失败"
                        }
                    }
                }
            runOnMain {
                if (customer?.id == customerId) callRecordsLoading = false
            }
        }
    }

    fun showCustomerPage(page: AssignedCustomerPage) {
        customer = page.customer
        customerPage = page.page
        customerTotal = page.total
        message = if (page.customer == null) "暂无可拨客户" else ""
        if (page.customer == null) {
            callRecords = emptyList()
            callRecordsTotal = 0
            callRecordsLoading = false
            callRecordsMessage = ""
        } else {
            loadCustomerCallRecords(page.customer.id)
        }
    }

    fun loadCustomer(page: Int) {
        loading = true
        message = ""
        thread {
            runCatching { session.api.nextCustomer(session.token, page.coerceAtLeast(1), selectedSchoolId, selectedGradeCode) }
                .onSuccess { runOnMain { showCustomerPage(it) } }
                .onFailure { runOnMain { if (it is AuthExpiredException) onAuthExpired() else message = it.message ?: "获取客户失败" } }
            runOnMain { loading = false }
        }
    }

    fun loadNext() {
        if (!loading && customerTotal > 0 && customerPage < customerTotal) loadCustomer(customerPage + 1)
    }

    fun loadPrevious() {
        if (!loading && customerPage > 1) loadCustomer(customerPage - 1)
    }

    fun resetCustomer() {
        if (!loading) loadCustomer(1)
    }

    fun markCustomerStatus(status: Int) {
        val target = customer ?: return
        val page = customerPage
        customerStatusMarking = true
        message = ""
        thread {
            runCatching {
                session.api.updateCustomerStatus(session.token, target.id, status)
                val next = session.api.nextCustomer(session.token, page, selectedSchoolId, selectedGradeCode)
                if (next.customer == null && next.total > 0 && page > next.total) {
                    session.api.nextCustomer(session.token, next.total, selectedSchoolId, selectedGradeCode)
                } else {
                    next
                }
            }.onSuccess {
                runOnMain { showCustomerPage(it) }
            }.onFailure {
                runOnMain {
                    if (it is AuthExpiredException) {
                        onAuthExpired()
                    } else {
                        val errorMessage = it.message ?: "标记失败"
                        message = errorMessage
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
            runOnMain { customerStatusMarking = false }
        }
    }

    fun markEndedCallStatus(status: Int) {
        val ended = call ?: return
        callMessage = ""
        callStatusMarking = true
        thread {
            runCatching {
                val recordId = ended.recordId ?: error("缺少通话记录，无法标记")
                session.api.syncCallRecord(session.token, recordId, ended.durationSeconds, ended.recordingUrl)
                session.api.updateCustomerStatus(session.token, ended.customer.id, status)
            }.onSuccess {
                runOnMain {
                    callCustomerStatusChanged = true
                    callMessage = if (status == 4) "已标记为无效" else "已标记为成交"
                }
            }.onFailure { error ->
                runOnMain {
                    if (error is AuthExpiredException) {
                        onAuthExpired()
                    } else {
                        val errorMessage = error.message ?: "标记失败"
                        callMessage = errorMessage
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
            runOnMain { callStatusMarking = false }
        }
    }

    fun uploadRecording(recordId: Int, customerId: Int, file: File) {
        thread {
            runCatching {
                session.api.uploadRecording(context, session.token, customerId, file) { progress ->
                    runOnMain {
                        if (call?.recordId == recordId && call?.state == CallState.Ended) {
                            call = call!!.copy(uploadProgress = progress)
                        }
                    }
                }
            }.onSuccess { url ->
                PendingCallSyncCache.markUploaded(context, recordId, url)
                runOnMain {
                    if (call?.recordId == recordId && call?.state == CallState.Ended) {
                        call = call!!.copy(recordingUrl = url, uploadingRecording = false, uploadProgress = 1f, uploadError = "")
                    }
                }
            }.onFailure { error ->
                runOnMain {
                    if (call?.recordId == recordId && call?.state == CallState.Ended) {
                        if (error is AuthExpiredException) onAuthExpired()
                        call = call!!.copy(uploadingRecording = false, uploadError = error.message ?: "录音上传失败")
                    }
                }
            }
        }
    }

    fun finishConnectedCall(recording: File?) {
        val current = call ?: return
        if (current.customer.id == 0 || current.state == CallState.Ended) {
            call = null
            return
        }
        val recordId = current.recordId
        val hasRecording = recording != null && recording.length() > 0
        val shouldUpload = hasRecording && recordId != null
        if (recordId != null) {
            PendingCallSyncCache.upsert(
                context,
                PendingCallSync(recordId, current.customer.id, current.durationSeconds(), recording?.absolutePath, current.recordingUrl),
            )
        }
        call = current.copy(
            state = CallState.Ended,
            durationSeconds = current.durationSeconds(),
            recordingFile = recording?.absolutePath,
            uploadingRecording = shouldUpload,
            uploadProgress = if (shouldUpload) 0f else 1f,
            uploadError = if (hasRecording) "" else "未生成录音文件",
        )
        if (shouldUpload) uploadRecording(recordId!!, current.customer.id, recording!!)
    }

    fun loadDialerOptions() {
        thread {
            runCatching { session.api.dialerOptions(session.token) }
                .onSuccess { options ->
                    runOnMain {
                        schoolOptions = options.schools
                        gradeOptions = options.grades
                    }
                }
                .onFailure { runOnMain { if (it is AuthExpiredException) onAuthExpired() else message = it.message ?: "获取筛选项失败" } }
        }
    }

    LaunchedEffect(refreshToken) {
        if (call == null) {
            loadCustomer(customerPage.coerceAtLeast(1))
        }
    }
    LaunchedEffect(Unit) { loadDialerOptions() }
    LaunchedEffect(Unit) {
        while (true) {
            val incomingPhone = TelephoneInCallService.currentRingingPhone()
            if (incomingPhone != null && call?.state != CallState.Incoming) {
                callMessage = ""
                call = CallUi(
                    customer = Customer(0, callDisplayName(context, incomingPhone, "未知来电"), incomingPhone, "", ""),
                    state = CallState.Incoming,
                    startedAt = System.currentTimeMillis(),
                )
            } else if (incomingPhone == null && call?.state == CallState.Incoming) {
                call = null
            }
            val activeCall = call
            val systemState = TelephoneInCallService.currentCallState()
            if (activeCall?.state == CallState.Dialing) {
                when {
                    systemState == android.telecom.Call.STATE_ACTIVE -> {
                        CallRecordingManager.start(context)
                        call = activeCall.copy(state = CallState.Connected, startedAt = System.currentTimeMillis())
                    }
                    isFinishedSystemCallState(systemState) && System.currentTimeMillis() - activeCall.startedAt > 5000 -> {
                        call = null
                    }
                }
            } else if (activeCall?.state == CallState.Connected && isFinishedSystemCallState(systemState)) {
                val recording = CallRecordingManager.stop() ?: CallRecordingManager.takeLastFinishedFile()
                finishConnectedCall(recording)
            }
            kotlinx.coroutines.delay(500)
        }
    }
    LaunchedEffect(call?.customer?.phone, call?.customer?.name, session.token) {
        val current = call ?: return@LaunchedEffect
        if (current.customer.id != 0 || current.customer.phone == UnknownPhone || !isUnknownCallName(current.customer.name)) return@LaunchedEffect
        val phone = current.customer.phone
        thread {
            runCatching { session.api.lookupCustomerName(session.token, phone) }
                .onSuccess { name ->
                    if (name.isNullOrBlank()) return@onSuccess
                    runOnMain {
                        if (call?.customer?.phone == phone && isUnknownCallName(call!!.customer.name)) {
                            call = call!!.copy(customer = call!!.customer.copy(name = name))
                        }
                    }
                }
                .onFailure { error ->
                    runOnMain { if (error is AuthExpiredException) onAuthExpired() }
                }
        }
    }
    LaunchedEffect(call != null) { onChromeHiddenChange(call != null) }
    DisposableEffect(Unit) {
        onDispose { onChromeHiddenChange(false) }
    }

    pendingConfirm?.let { action ->
        val text = when (action) {
            "reset" -> "确认重置到第一位客户吗？"
            "invalid" -> "确认将当前客户标记为无效吗？标记后将不再出现在拨号池中"
            else -> "确认将当前客户标记为成交吗？"
        }
        AlertDialog(
            modifier = Modifier.width(320.dp),
            onDismissRequest = { pendingConfirm = null },
            shape = RoundedCornerShape(8.dp),
            containerColor = CallSurfaceColor,
            titleContentColor = CallText,
            textContentColor = CallMutedText,
            title = { Text("提示") },
            text = { Text(text) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingConfirm = null
                        when (action) {
                            "reset" -> resetCustomer()
                            "invalid" -> markCustomerStatus(4)
                            "deal" -> markCustomerStatus(3)
                        }
                    },
                    shape = RoundedCornerShape(UiButtonRadius),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CallActiveBlue, contentColor = CallActionContent),
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingConfirm = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = CallMutedText),
                ) { Text("取消") }
            },
        )
    }

    Box(Modifier.fillMaxSize().background(CallBackground).padding(padding)) {
        if (call == null) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(UiListPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    DialerFilters(
                        schools = schoolOptions,
                        grades = gradeOptions,
                        selectedSchoolId = selectedSchoolId,
                        selectedGradeCode = selectedGradeCode,
                        resetKey = filterResetToken,
                        onSchoolSelected = {
                            selectedSchoolId = it
                            loadCustomer(1)
                        },
                        onGradeSelected = {
                            selectedGradeCode = it
                            loadCustomer(1)
                        },
                        onReset = {
                            filterResetToken++
                            selectedSchoolId = null
                            selectedGradeCode = null
                            loadCustomer(1)
                        },
                    )
                }
                item {
                    CustomerCard(
                        customer = customer,
                        loading = loading,
                        callLoading = callSubmitting,
                        statusLoading = customerStatusMarking,
                        message = message,
                        page = customerPage,
                        total = customerTotal,
                        onPrevious = { loadPrevious() },
                        onNext = { loadNext() },
                        onReset = { pendingConfirm = "reset" },
                        onMarkInvalid = { pendingConfirm = "invalid" },
                        onMarkDeal = { pendingConfirm = "deal" },
                        onCopyPhone = { copyCustomerPhone(it) },
                    ) {
                        val target = it
                        callSubmitting = true
                        callMessage = ""
                        callCustomerStatusChanged = false
                        callStatusMarking = false
                        thread {
                            runCatching { session.api.createCallRecord(session.token, target.id) }
                                .onSuccess { recordId ->
                                    runOnMain {
                                        callSubmitting = false
                                        call = CallUi(customer = target, state = CallState.Dialing, startedAt = System.currentTimeMillis(), recordId = recordId)
                                        placeCall(context, target.phone, target.name)
                                    }
                                }
                                .onFailure { error ->
                                    runOnMain {
                                        callSubmitting = false
                                        call = null
                                        if (error is AuthExpiredException) onAuthExpired() else message = error.message ?: "创建通话记录失败"
                                    }
                                }
                        }
                    }
                }
                if (customer != null) {
                    item {
                        CustomerCallTimeline(
                            records = callRecords,
                            total = callRecordsTotal,
                            loading = callRecordsLoading,
                            message = callRecordsMessage,
                        )
                    }
                }
            }
        } else if (call!!.state == CallState.Ended) {
            CallResultForm(
                call = call!!,
                message = callMessage,
                marking = callStatusMarking,
                onMarkInvalid = { markEndedCallStatus(4) },
                onMarkDeal = { markEndedCallStatus(3) },
            ) { intent, remark ->
                val ended = call!!
                val recordId = ended.recordId
                if (recordId == null) {
                    message = "缺少通话记录ID，无法更新"
                    call = null
                    return@CallResultForm
                }
                call = null
                val nextPage = if (callCustomerStatusChanged) customerPage else customerPage + 1
                callCustomerStatusChanged = false
                customer = null
                loading = true
                thread {
                    runCatching {
                        session.api.updateCallRecord(
                            token = session.token,
                            id = recordId,
                            durationSeconds = ended.durationSeconds,
                            intentLevel = intent,
                            remark = remark,
                            recordingUrl = ended.recordingUrl,
                        )
                        PendingCallSyncCache.remove(context, recordId)
                        if (customerTotal > 0 && nextPage <= customerTotal) session.api.nextCustomer(session.token, nextPage, selectedSchoolId, selectedGradeCode) else null
                    }.onSuccess { next ->
                        runOnMain {
                            if (next == null) {
                                message = if (customerTotal > 0) "已到最后一位" else "暂无可拨客户"
                                callRecords = emptyList()
                                callRecordsTotal = 0
                                callRecordsLoading = false
                                callRecordsMessage = ""
                            } else {
                                showCustomerPage(next)
                            }
                        }
                    }
                        .onFailure { runOnMain { if (it is AuthExpiredException) onAuthExpired() else message = it.message ?: "提交失败" } }
                    runOnMain { loading = false }
                }
            }
        } else {
            CallPanel(
                call = call!!,
                onAnswer = {
                    if (!TelephoneInCallService.answerCurrentCall()) {
                        callMessage = "接听失败，请确认本应用是默认电话应用"
                        return@CallPanel
                    }
                    call = call!!.copy(state = CallState.Connected, startedAt = System.currentTimeMillis())
                },
                onToggleMute = {
                    val muted = !call!!.muted
                    if (setMuted(context, muted)) call = call!!.copy(muted = muted)
                },
                onToggleSpeaker = {
                    val speaker = !call!!.speaker
                    if (setSpeaker(context, speaker)) call = call!!.copy(speaker = speaker)
                },
                onHangup = {
                    if (!TelephoneInCallService.hangUpCurrentCall()) {
                        callMessage = "未获取到系统通话，请先将本应用设为默认电话应用"
                        return@CallPanel
                    }
                    if (call!!.state == CallState.Connected) {
                        val recording = CallRecordingManager.stop() ?: CallRecordingManager.takeLastFinishedFile()
                        finishConnectedCall(recording)
                    } else {
                        call = null
                    }
                },
                message = callMessage,
            )
        }
    }
}

@Composable
private fun DialerFilters(
    schools: List<SchoolOption>,
    grades: List<GradeOption>,
    selectedSchoolId: Int?,
    selectedGradeCode: Int?,
    resetKey: Any?,
    onSchoolSelected: (Int?) -> Unit,
    onGradeSelected: (Int?) -> Unit,
    onReset: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val schoolName = schools.firstOrNull { it.id == selectedSchoolId }?.schoolName ?: "全部学校"
    val gradeName = grades.firstOrNull { it.gradeCode == selectedGradeCode }?.gradeName ?: "全部年级"
    val active = selectedSchoolId != null || selectedGradeCode != null
    val summary = if (active) "$schoolName · $gradeName" else "全部学校 · 全部年级"
    AppCard {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("客户筛选", color = CallText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(summary, color = CallMutedText, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) CallActiveBlue else CallButtonColor,
                        contentColor = if (active) CallActionContent else CallText,
                    ),
                ) {
                    Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, if (expanded) "收起" else "筛选", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(if (expanded) "收起" else "筛选", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (expanded) {
                AutoSearchChoiceRow(
                    "学校",
                    listOf<Pair<Int?, String>>(null to "全部") + schools.map { it.id as Int? to it.schoolName },
                    selectedSchoolId,
                    onSchoolSelected,
                    resetKey = resetKey,
                )
                AutoSearchChoiceRow(
                    "年级",
                    listOf<Pair<Int?, String>>(null to "全部") + grades.map { it.gradeCode as Int? to it.gradeName },
                    selectedGradeCode,
                    onGradeSelected,
                    resetKey = resetKey,
                )
                if (active) {
                    Button(
                        onClick = {
                            onReset()
                        },
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                        shape = RoundedCornerShape(17.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CallButtonColor, contentColor = CallText),
                    ) {
                        Text("清空筛选", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerCallTimeline(records: List<CallRecord>, total: Int, loading: Boolean, message: String) {
    AppCard {
        Column(Modifier.fillMaxWidth().padding(UiCardPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("通话时间线（${total}次）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CallText)
            when {
                loading -> CircularProgressIndicator(color = CallActiveBlue, modifier = Modifier.size(24.dp))
                records.isEmpty() -> Text(message.ifBlank { "暂无通话记录" }, color = CallMutedText, style = MaterialTheme.typography.bodyMedium)
                else -> records.forEachIndexed { index, record ->
                    TimelineRecord(record = record, last = index == records.lastIndex)
                }
            }
        }
    }
}

@Composable
private fun TimelineRecord(record: CallRecord, last: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(CallActiveBlue),
            )
            if (!last) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(68.dp)
                        .background(CallMutedText.copy(alpha = 0.35f)),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(bottom = if (last) 0.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${record.callEmployeeName.ifBlank { "未知拨号人" }} · ${formatCallTime(record.callAt)}",
                color = CallText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "意向度：${record.intentLabel.ifBlank { "未知" }} · 时长：${formatDuration(record.durationSeconds)}",
                color = CallMutedText,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "备注：${record.remark.ifBlank { "无" }}",
                color = CallMutedText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
