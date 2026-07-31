package com.example.telephone.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.telephone.CallRecordingManager
import com.example.telephone.PendingCallSync
import com.example.telephone.PendingCallSyncCache
import com.example.telephone.TelephoneInCallService
import com.example.telephone.formatCallTime
import com.example.telephone.formatDuration
import com.example.telephone.formatPhone
import com.example.telephone.intentColor
import com.example.telephone.model.AuthExpiredException
import com.example.telephone.model.CallRecord
import com.example.telephone.model.CallRecordQuery
import com.example.telephone.model.CallState
import com.example.telephone.model.CallSummary
import com.example.telephone.model.CallUi
import com.example.telephone.model.Customer
import com.example.telephone.model.Session
import com.example.telephone.placeCall
import com.example.telephone.runOnMain
import com.example.telephone.setMuted
import com.example.telephone.setSpeaker
import com.example.telephone.ui.CallAcceptColor
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
import com.example.telephone.ui.UiIconSize
import com.example.telephone.ui.UiListPadding
import com.example.telephone.ui.components.AutoSearchChoiceRow
import com.example.telephone.ui.components.AppCard
import com.example.telephone.ui.components.Avatar
import com.example.telephone.ui.components.CallPanel
import com.example.telephone.ui.components.CallResultForm
import com.example.telephone.ui.components.EmptyState
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.concurrent.thread
import kotlinx.coroutines.launch

@Composable
internal fun RecordsScreen(
    session: Session,
    padding: PaddingValues,
    onAuthExpired: () -> Unit,
    onChromeHiddenChange: (Boolean) -> Unit,
    refreshToken: Int,
    detailId: Int? = null,
    onOpenDetail: (Int) -> Unit = {},
    onCloseDetail: () -> Unit = {},
) {
    val context = LocalContext.current
    val isDetailRoute = detailId != null
    val records = remember { mutableStateListOf<CallSummary>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var hasMore by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<Int?>(null) }
    var selectedIntent by remember { mutableStateOf<Int?>(null) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var filterError by remember { mutableStateOf("") }
    var detail by remember(detailId) { mutableStateOf<CallRecord?>(null) }
    var detailHistory by remember { mutableStateOf<List<CallRecord>>(emptyList()) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf("") }
    var pendingDetailStatus by remember { mutableStateOf<Int?>(null) }
    var callbackCall by remember { mutableStateOf<CallUi?>(null) }
    var callbackMessage by remember { mutableStateOf("") }
    var callbackStatusMarking by remember { mutableStateOf(false) }
    var playingId by remember { mutableStateOf<Int?>(null) }
    var loadingRecordingId by remember { mutableStateOf<Int?>(null) }
    var isPlayingRecording by remember { mutableStateOf(false) }
    var playingPositionMs by remember { mutableIntStateOf(0) }
    var playingDurationMs by remember { mutableIntStateOf(0) }
    var playErrorId by remember { mutableStateOf<Int?>(null) }
    var playError by remember { mutableStateOf("") }
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    loadingRecordingId = null
                    if (player.duration > 0) playingDurationMs = player.duration.toInt()
                } else if (playbackState == Player.STATE_ENDED) {
                    isPlayingRecording = false
                    playingPositionMs = 0
                    playingId = null
                    loadingRecordingId = null
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayingRecording = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                loadingRecordingId = null
                playingId?.let { playErrorId = it }
                playingId = null
                isPlayingRecording = false
                playError = "录音无法播放"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    fun activeQuery() = CallRecordQuery(
        keyword = keyword,
        customerStatus = selectedStatus,
        intentLevel = selectedIntent,
        callAtStart = startDate?.startInstant(),
        callAtEnd = endDate?.endInstant(),
    )

    fun loadRecords(cursor: String?, append: Boolean) {
        if (append && (isLoadingMore || !hasMore)) return
        if (append) {
            isLoadingMore = true
        } else {
            isRefreshing = true
            nextCursor = null
            hasMore = true
        }
        val query = activeQuery()
        thread {
            runCatching { session.api.callSummaries(session.token, cursor, query) }
                .onSuccess { result ->
                    runOnMain {
                        if (!append) records.clear()
                        records.addAll(result.items)
                        nextCursor = result.nextCursor
                        hasMore = result.nextCursor != null
                    }
                }
                .onFailure { runOnMain { if (it is AuthExpiredException) onAuthExpired() } }
            runOnMain {
                isRefreshing = false
                isLoadingMore = false
            }
        }
    }

    fun applyFilters(nextKeyword: String, nextStatus: Int?, nextIntent: Int?, nextStart: LocalDate?, nextEnd: LocalDate?): Boolean {
        if ((nextStart == null) != (nextEnd == null)) {
            filterError = "请选择完整时间区间"
            return false
        }
        if (nextStart != null && nextEnd != null) {
            if (nextStart.isAfter(nextEnd)) {
                filterError = "开始时间不能晚于结束时间"
                return false
            }
            if (nextEnd.isAfter(nextStart.plusMonths(1))) {
                filterError = "时间跨度最长一个月"
                return false
            }
        }
        keyword = nextKeyword.trim()
        selectedStatus = nextStatus
        selectedIntent = nextIntent
        startDate = nextStart
        endDate = nextEnd
        filterError = ""
        loadRecords(null, append = false)
        return true
    }

    fun loadDetail(id: Int) {
        detailLoading = true
        detailError = ""
        detailHistory = emptyList()
        thread {
            runCatching {
                val record = session.api.callRecord(session.token, id)
                record to session.api.customerCallRecords(session.token, record.customerId)
            }
                .onSuccess { (record, history) ->
                    runOnMain {
                        detail = record
                        detailHistory = history
                    }
                }
                .onFailure { error -> runOnMain { if (error is AuthExpiredException) onAuthExpired() else detailError = error.message ?: "详情加载失败" } }
            runOnMain { detailLoading = false }
        }
    }

    fun markDetailStatus(status: Int) {
        val record = detail ?: return
        detailLoading = true
        detailError = ""
        thread {
            runCatching {
                session.api.updateCustomerStatus(session.token, record.customerId, status)
                val updated = session.api.callRecord(session.token, record.id)
                updated to session.api.customerCallRecords(session.token, updated.customerId)
            }.onSuccess { (updated, history) ->
                runOnMain {
                    detail = updated
                    detailHistory = history
                    records.indices.forEach { index ->
                        if (records[index].customerId == updated.customerId) {
                            records[index] = records[index].copy(customerStatus = updated.customerStatus)
                        }
                    }
                }
            }.onFailure { error ->
                runOnMain { if (error is AuthExpiredException) onAuthExpired() else detailError = error.message ?: "标记失败" }
            }
            runOnMain { detailLoading = false }
        }
    }

    fun markCallbackStatus(status: Int, currentCall: CallUi) {
        val query = activeQuery()
        callbackMessage = ""
        callbackStatusMarking = true
        thread {
            runCatching {
                session.api.updateCustomerStatus(session.token, currentCall.customer.id, status)
                session.api.callSummaries(session.token, query = query)
            }.onSuccess { result ->
                runOnMain {
                    callbackMessage = if (status == 4) "已标记为无效" else "已标记为成交"
                    records.clear()
                    records.addAll(result.items)
                    nextCursor = result.nextCursor
                    hasMore = result.nextCursor != null
                    detail = detail?.let { if (it.customerId == currentCall.customer.id) it.copy(customerStatus = status) else it }
                    detailHistory = detailHistory.map { if (it.customerId == currentCall.customer.id) it.copy(customerStatus = status) else it }
                }
            }.onFailure { error ->
                runOnMain { if (error is AuthExpiredException) onAuthExpired() else callbackMessage = error.message ?: "标记失败" }
            }
            runOnMain { callbackStatusMarking = false }
        }
    }

    fun playOrPause(record: CallRecord) {
        playError = ""
        playErrorId = null
        if (playingId == record.id) {
            if (player.isPlaying) player.pause() else player.play()
            return
        }
        playingId = record.id
        loadingRecordingId = record.id
        playingPositionMs = 0
        playingDurationMs = record.durationSeconds * 1000
        runCatching {
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(MediaItem.fromUri(recordingUri(record.recordingUrl)))
            player.prepare()
            player.play()
        }.onFailure {
            loadingRecordingId = null
            playingId = null
            isPlayingRecording = false
            playErrorId = record.id
            playError = "录音无法播放"
        }
    }

    fun seekRecording(positionMs: Int) {
        player.seekTo(positionMs.toLong())
        playingPositionMs = positionMs
    }

    fun uploadRecording(recordId: Int, customerId: Int, file: File) {
        thread {
            runCatching {
                session.api.uploadRecording(context, session.token, customerId, file) { progress ->
                    runOnMain {
                        if (callbackCall?.recordId == recordId && callbackCall?.state == CallState.Ended) {
                            callbackCall = callbackCall!!.copy(uploadProgress = progress)
                        }
                    }
                }
            }.onSuccess { url ->
                PendingCallSyncCache.markUploaded(context, recordId, url)
                runOnMain {
                    if (callbackCall?.recordId == recordId && callbackCall?.state == CallState.Ended) {
                        callbackCall = callbackCall!!.copy(recordingUrl = url, uploadingRecording = false, uploadProgress = 1f, uploadError = "")
                    }
                }
            }.onFailure { error ->
                runOnMain {
                    if (callbackCall?.recordId == recordId && callbackCall?.state == CallState.Ended) {
                        if (error is AuthExpiredException) onAuthExpired()
                        callbackCall = callbackCall!!.copy(uploadingRecording = false, uploadError = error.message ?: "录音上传失败")
                    }
                }
            }
        }
    }

    fun finishCallback(recording: File?) {
        val current = callbackCall ?: return
        if (current.state == CallState.Ended) return
        val recordId = current.recordId
        val hasRecording = recording != null && recording.length() > 0
        val shouldUpload = hasRecording && recordId != null
        if (recordId != null) {
            PendingCallSyncCache.upsert(
                context,
                PendingCallSync(recordId, current.customer.id, current.durationSeconds(), recording?.absolutePath, current.recordingUrl),
            )
        }
        callbackCall = current.copy(
            state = CallState.Ended,
            durationSeconds = current.durationSeconds(),
            recordingFile = recording?.absolutePath,
            uploadingRecording = shouldUpload,
            uploadProgress = if (shouldUpload) 0f else 1f,
            uploadError = if (hasRecording) "" else "未生成录音文件",
        )
        if (shouldUpload) uploadRecording(recordId!!, current.customer.id, recording!!)
    }

    fun startCallback(customer: Customer) {
        callbackCall = CallUi(customer = customer, state = CallState.Dialing, startedAt = System.currentTimeMillis())
        callbackMessage = ""
        callbackStatusMarking = false
        thread {
            runCatching { session.api.createCallRecord(session.token, customer.id) }
                .onSuccess { recordId ->
                    runOnMain {
                        callbackCall = callbackCall?.copy(recordId = recordId, startedAt = System.currentTimeMillis())
                        placeCall(context, customer.phone, customer.name)
                    }
                }
                .onFailure { error ->
                    runOnMain {
                        callbackCall = null
                        if (error is AuthExpiredException) onAuthExpired() else detailError = error.message ?: "创建通话记录失败"
                    }
                }
        }
    }

    LaunchedEffect(refreshToken, detailId) {
        if (callbackCall != null) return@LaunchedEffect
        if (detailId != null) {
            loadDetail(detailId)
        } else {
            loadRecords(null, append = false)
        }
    }
    LaunchedEffect(playingId) {
        while (playingId != null) {
            runCatching {
                playingPositionMs = player.currentPosition.toInt()
                if (player.duration > 0) playingDurationMs = player.duration.toInt()
            }
            kotlinx.coroutines.delay(500)
        }
    }
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    val showBackToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isRefreshing && !isLoadingMore && hasMore) {
            loadRecords(nextCursor, append = true)
        }
    }
    LaunchedEffect(callbackCall != null, isDetailRoute) {
        onChromeHiddenChange(callbackCall != null || isDetailRoute)
    }
    DisposableEffect(Unit) {
        onDispose { onChromeHiddenChange(false) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val activeCall = callbackCall
            val systemState = TelephoneInCallService.currentCallState()
            if (activeCall?.state == CallState.Dialing) {
                when {
                    systemState == android.telecom.Call.STATE_ACTIVE -> {
                        CallRecordingManager.start(context)
                        callbackCall = activeCall.copy(state = CallState.Connected, startedAt = System.currentTimeMillis())
                    }
                    systemState == null && System.currentTimeMillis() - activeCall.startedAt > 5000 -> {
                        callbackCall = null
                    }
                }
            } else if (activeCall?.state == CallState.Connected && systemState == null) {
                val recording = CallRecordingManager.stop() ?: CallRecordingManager.takeLastFinishedFile()
                finishCallback(recording)
            }
            kotlinx.coroutines.delay(500)
        }
    }

    callbackCall?.let { currentCall ->
        Box(Modifier.fillMaxSize().background(CallBackground).padding(padding)) {
            if (currentCall.state == CallState.Ended) {
                CallResultForm(
                    call = currentCall,
                    message = callbackMessage,
                    marking = callbackStatusMarking,
                    onMarkInvalid = { markCallbackStatus(4, currentCall) },
                    onMarkDeal = { markCallbackStatus(3, currentCall) },
                ) { intent, remark ->
                    val recordId = currentCall.recordId
                    if (recordId == null) {
                        callbackCall = null
                        return@CallResultForm
                    }
                    val query = activeQuery()
                    callbackCall = null
                    thread {
                        runCatching {
                            session.api.updateCallRecord(
                                token = session.token,
                                id = recordId,
                                durationSeconds = currentCall.durationSeconds,
                                intentLevel = intent,
                                remark = remark,
                                recordingUrl = currentCall.recordingUrl,
                            )
                            PendingCallSyncCache.remove(context, recordId)
                            session.api.callSummaries(session.token, query = query)
                        }.onSuccess { result ->
                            runOnMain {
                                records.clear()
                                records.addAll(result.items)
                                nextCursor = result.nextCursor
                                hasMore = result.nextCursor != null
                            }
                        }.onFailure { error ->
                            runOnMain { if (error is AuthExpiredException) onAuthExpired() else detailError = error.message ?: "提交失败" }
                        }
                    }
                }
            } else {
                CallPanel(
                    call = currentCall,
                    onAnswer = {},
                    onToggleMute = {
                        val muted = !currentCall.muted
                        if (setMuted(context, muted)) callbackCall = currentCall.copy(muted = muted)
                    },
                    onToggleSpeaker = {
                        val speaker = !currentCall.speaker
                        if (setSpeaker(context, speaker)) callbackCall = currentCall.copy(speaker = speaker)
                    },
                    onHangup = {
                        if (!TelephoneInCallService.hangUpCurrentCall()) return@CallPanel
                        if (currentCall.state == CallState.Connected) {
                            val recording = CallRecordingManager.stop() ?: CallRecordingManager.takeLastFinishedFile()
                            finishCallback(recording)
                        } else {
                            callbackCall = null
                        }
                    },
                )
            }
        }
        return
    }

    detail?.let { record ->
        pendingDetailStatus?.let { status ->
            AlertDialog(
                onDismissRequest = { pendingDetailStatus = null },
                shape = RoundedCornerShape(8.dp),
                containerColor = CallBackground,
                titleContentColor = CallText,
                textContentColor = CallMutedText,
                title = { Text("提示") },
                text = { Text(if (status == 4) "确认将当前客户标记为无效吗？" else "确认将当前客户标记为成交吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingDetailStatus = null
                            markDetailStatus(status)
                        },
                        shape = RoundedCornerShape(UiButtonRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = CallActiveBlue, contentColor = CallActionContent),
                    ) { Text("确认") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDetailStatus = null }) {
                        Text("取消", color = CallMutedText)
                    }
                },
            )
        }
        CallRecordDetailScreen(
            record = record,
            history = detailHistory,
            loading = detailLoading,
            error = detailError,
            playingId = playingId,
            isPlayingRecording = isPlayingRecording,
            loadingRecordingId = loadingRecordingId,
            playingPositionMs = playingPositionMs,
            playingDurationMs = playingDurationMs,
            playErrorId = playErrorId,
            playError = playError,
            onBack = {
                detail = null
                detailError = ""
                onCloseDetail()
            },
            onCallback = { startCallback(Customer(record.customerId, record.customerName, record.customerPhone, record.schoolName, record.gradeName)) },
            onMarkInvalid = { pendingDetailStatus = 4 },
            onMarkDeal = { pendingDetailStatus = 3 },
            onPlayClick = ::playOrPause,
            onSeek = ::seekRecording,
        )
        return
    }

    if (isDetailRoute) {
        CallRecordDetailLoadingScreen(
            loading = detailLoading,
            error = detailError,
            onBack = onCloseDetail,
        )
        return
    }

    Column(Modifier.fillMaxSize().background(CallBackground).padding(padding)) {
        Box(Modifier.fillMaxSize()) {
            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { loadRecords(null, append = false) }, modifier = Modifier.fillMaxSize()) {
                LazyColumn(state = listState, contentPadding = PaddingValues(UiListPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "filters", contentType = "filters") {
                CallRecordFilters(
                    keyword = keyword,
                    selectedStatus = selectedStatus,
                    selectedIntent = selectedIntent,
                    startDate = startDate,
                    endDate = endDate,
                    error = filterError,
                    onApply = ::applyFilters,
                    onReset = {
                        filterError = ""
                        keyword = ""
                        selectedStatus = null
                        selectedIntent = null
                        startDate = null
                        endDate = null
                        loadRecords(null, append = false)
                    },
                )
            }
            if (records.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    EmptyState(if (isRefreshing) "正在刷新" else "暂无通话记录")
                }
            }
            items(records, key = { it.id }, contentType = { "record" }) { record ->
                CallRecordListItem(
                    record = record,
                    onOpen = { onOpenDetail(record.lastCallRecordId) },
                )
            }
            if (isLoadingMore) {
                item(key = "loading", contentType = "loading") {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CallActiveBlue)
                    }
                }
            }
            if (records.isNotEmpty() && !hasMore) {
                item(key = "end", contentType = "end") {
                    Text("没有更多了", Modifier.fillMaxWidth().padding(16.dp), color = CallMutedText, textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                }
            }
                }
            }
            if (showBackToTop) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (listState.firstVisibleItemIndex > 30) listState.scrollToItem(30)
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                    containerColor = CallActiveBlue,
                    contentColor = CallActionContent,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, "返回顶部", modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallRecordDetailLoadingScreen(loading: Boolean, error: String, onBack: () -> Unit) {
    Scaffold(
        containerColor = CallBackground,
        topBar = {
            TopAppBar(
                title = { Text("通话详情", color = CallText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = CallText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CallBackground),
            )
        },
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .background(CallBackground)
                .padding(inner),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(color = CallActiveBlue)
            } else {
                Text(error.ifBlank { "详情加载失败" }, color = CallMutedText)
            }
        }
    }
}

@Composable
private fun CallRecordListItem(
    record: CallSummary,
    onOpen: () -> Unit,
) {
    val phone = remember(record.customerPhone) { formatPhone(record.customerPhone) }
    val callTime = formatCallTime(record.lastCallAt)
    val schoolGrade = remember(record.schoolName, record.gradeName) { schoolGradeText(record.schoolName, record.gradeName) }
    AppCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(UiCardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CallButtonColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(android.R.drawable.ic_menu_call), "记录", tint = CallText, modifier = Modifier.size(UiIconSize))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(record.customerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = CallText)
                Spacer(Modifier.height(4.dp))
                if (schoolGrade.isNotBlank()) {
                    Text(schoolGrade, color = CallMutedText, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                }
                Text(phone, color = CallMutedText, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(callTime, color = CallMutedText, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${record.callCount}次通话", color = CallMutedText, style = MaterialTheme.typography.labelMedium)
                    CustomerStatusBadge(record.customerStatus)
                    IntentBadge(record.lastIntentLevel, record.intentLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallRecordDetailScreen(
    record: CallRecord,
    history: List<CallRecord>,
    loading: Boolean,
    error: String,
    isPlayingRecording: Boolean,
    playingId: Int?,
    loadingRecordingId: Int?,
    playingPositionMs: Int,
    playingDurationMs: Int,
    playErrorId: Int?,
    playError: String,
    onBack: () -> Unit,
    onCallback: () -> Unit,
    onMarkInvalid: () -> Unit,
    onMarkDeal: () -> Unit,
    onPlayClick: (CallRecord) -> Unit,
    onSeek: (Int) -> Unit,
) {
    val schoolGrade = remember(record.schoolName, record.gradeName) { schoolGradeText(record.schoolName, record.gradeName) }
    val canMarkStatus = record.canCallback && record.customerStatus != 3 && record.customerStatus != 4
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CallBackground,
        topBar = {
            TopAppBar(
                title = { Text("通话详情", color = CallText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = CallText,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CallBackground),
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(CallBackground)
                .padding(innerPadding)
                .padding(UiListPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                Column(Modifier.fillMaxWidth().padding(UiCardPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(record.customerName, 58)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(record.customerName, color = CallText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (schoolGrade.isNotBlank()) Text(schoolGrade, color = CallMutedText, style = MaterialTheme.typography.bodyMedium)
                            Text(formatPhone(record.customerPhone), color = CallMutedText)
                        }
                    }
                    DetailLine("最后通话时间", formatCallTime(record.callAt))
                    if (schoolGrade.isNotBlank()) DetailLine("学校年级", schoolGrade)
                    DetailLine("客户状态", customerStatusLabel(record.customerStatus))
                    DetailLine("最近意向度", record.intentLabel.ifBlank { "未知" })
                    if (record.remark.isNotBlank()) DetailLine("备注", record.remark)
                    if (error.isNotBlank()) Text(error, color = CallHangupColor, style = MaterialTheme.typography.bodySmall)
                    if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = CallActiveBlue)
                }
            }
            AppCard(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.fillMaxSize().padding(UiCardPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("客户通话时间线", color = CallText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (history.isEmpty()) {
                        Text(if (loading) "正在加载" else "暂无通话记录", color = CallMutedText, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Box(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                            CallRecordTimeline(
                                history = history,
                                playingId = playingId,
                                isPlayingRecording = isPlayingRecording,
                                loadingRecordingId = loadingRecordingId,
                                playingPositionMs = playingPositionMs,
                                playingDurationMs = playingDurationMs,
                                playErrorId = playErrorId,
                                playError = playError,
                                onPlayClick = onPlayClick,
                                onSeek = onSeek,
                            )
                        }
                    }
                }
            }
            if (canMarkStatus) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onMarkInvalid,
                        enabled = !loading,
                        modifier = Modifier.weight(1f).height(UiButtonHeight),
                        shape = RoundedCornerShape(UiButtonRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = CallButtonColor, contentColor = CallText),
                    ) {
                        Icon(Icons.Filled.PersonOff, "标记为无效", modifier = Modifier.size(UiIconSize))
                        Spacer(Modifier.width(8.dp))
                        Text("标记为无效", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onMarkDeal,
                        enabled = !loading,
                        modifier = Modifier.weight(1f).height(UiButtonHeight),
                        shape = RoundedCornerShape(UiButtonRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = CallButtonColor, contentColor = CallText),
                    ) {
                        Icon(Icons.Filled.Gavel, "标记为成交", modifier = Modifier.size(UiIconSize))
                        Spacer(Modifier.width(8.dp))
                        Text("标记为成交", fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (record.canCallback) {
                Button(
                    onClick = onCallback,
                    modifier = Modifier.fillMaxWidth().height(UiButtonHeight),
                    shape = RoundedCornerShape(UiButtonRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = CallActiveBlue, contentColor = CallActionContent),
                ) {
                    Icon(painterResource(android.R.drawable.ic_menu_call), "回拨", modifier = Modifier.size(UiIconSize))
                    Spacer(Modifier.width(8.dp))
                    Text("回拨", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = CallMutedText, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            modifier = Modifier.padding(start = 16.dp).weight(1f),
            color = CallText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun RecordingPlayer(
    isCurrentRecording: Boolean,
    isPlayingRecording: Boolean,
    isLoadingRecording: Boolean,
    positionMs: Int,
    durationMs: Int,
    playError: String,
    onPlayClick: () -> Unit,
    onSeek: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("录音", color = CallMutedText, style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onPlayClick, modifier = Modifier.size(40.dp)) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isCurrentRecording || isLoadingRecording) CallActiveBlue else CallButtonColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoadingRecording) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CallActionContent)
                    } else {
                        Icon(
                            painterResource(if (isPlayingRecording) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play),
                            "播放录音",
                            tint = if (isCurrentRecording) CallActionContent else CallText,
                            modifier = Modifier.size(UiIconSize),
                        )
                    }
                }
            }
        }
        if (isCurrentRecording) {
            RecordingProgress(positionMs, durationMs, onSeek = onSeek)
        }
        if (playError.isNotBlank()) {
            Text(playError, color = CallHangupColor, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun RecordingProgress(positionMs: Int, durationMs: Int, modifier: Modifier = Modifier, onSeek: (Int) -> Unit) {
    val max = durationMs.coerceAtLeast(1).toFloat()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Slider(
            value = positionMs.coerceIn(0, durationMs.coerceAtLeast(0)).toFloat(),
            onValueChange = { onSeek(it.toInt()) },
            valueRange = 0f..max,
            colors = SliderDefaults.colors(
                thumbColor = CallActiveBlue,
                activeTrackColor = CallActiveBlue,
                inactiveTrackColor = CallButtonColor,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(positionMs / 1000), color = CallMutedText, style = MaterialTheme.typography.labelSmall)
            Text(formatDuration(durationMs / 1000), color = CallMutedText, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CallRecordTimeline(
    history: List<CallRecord>,
    playingId: Int?,
    isPlayingRecording: Boolean,
    loadingRecordingId: Int?,
    playingPositionMs: Int,
    playingDurationMs: Int,
    playErrorId: Int?,
    playError: String,
    onPlayClick: (CallRecord) -> Unit,
    onSeek: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        history.forEachIndexed { index, item ->
            val isCurrentRecording = playingId == item.id
            val hasRecording = item.recordingUrl.isNotBlank()
            val connectorHeight = when {
                index == history.lastIndex -> 0.dp
                hasRecording && isCurrentRecording -> 148.dp
                hasRecording -> 104.dp
                item.remark.isNotBlank() -> 88.dp
                else -> 72.dp
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(CallActiveBlue))
                    if (connectorHeight > 0.dp) {
                        Box(Modifier.width(2.dp).height(connectorHeight).background(CallButtonColor))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("拨号时间：${formatCallTime(item.callAt)}", color = CallText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("拨号人：${item.callEmployeeName.ifBlank { "未知" }}", color = CallMutedText, style = MaterialTheme.typography.bodySmall)
                    Text("意向度：${item.intentLabel.ifBlank { "未知" }} · 通话时长：${formatDuration(item.durationSeconds)}", color = CallMutedText, style = MaterialTheme.typography.bodySmall)
                    if (hasRecording) {
                        RecordingPlayer(
                            isCurrentRecording = isCurrentRecording,
                            isPlayingRecording = isCurrentRecording && isPlayingRecording,
                            isLoadingRecording = loadingRecordingId == item.id,
                            positionMs = if (isCurrentRecording) playingPositionMs else 0,
                            durationMs = if (isCurrentRecording) playingDurationMs else item.durationSeconds * 1000,
                            playError = if (playErrorId == item.id) playError else "",
                            onPlayClick = { onPlayClick(item) },
                            onSeek = onSeek,
                        )
                    } else {
                        Text("录音：无", color = CallMutedText, style = MaterialTheme.typography.bodySmall)
                    }
                    if (item.remark.isNotBlank()) {
                        Text("备注：${item.remark}", color = CallMutedText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallRecordFilters(
    keyword: String,
    selectedStatus: Int?,
    selectedIntent: Int?,
    startDate: LocalDate?,
    endDate: LocalDate?,
    error: String,
    onApply: (String, Int?, Int?, LocalDate?, LocalDate?) -> Boolean,
    onReset: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var filterResetToken by remember { mutableIntStateOf(0) }
    var draftKeyword by remember(keyword) { mutableStateOf(keyword) }
    var draftStatus by remember(selectedStatus) { mutableStateOf(selectedStatus) }
    var draftIntent by remember(selectedIntent) { mutableStateOf(selectedIntent) }
    var draftStart by remember(startDate) { mutableStateOf(startDate) }
    var draftEnd by remember(endDate) { mutableStateOf(endDate) }
    var pickingDate by remember { mutableStateOf<DateField?>(null) }
    val hasActiveFilters = keyword.isNotBlank() || selectedStatus != null || selectedIntent != null || startDate != null || endDate != null
    AppCard {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactSearchField(
                    value = draftKeyword,
                    onValueChange = { draftKeyword = it },
                    modifier = Modifier.weight(1f),
                    onSearch = { onApply(draftKeyword, draftStatus, draftIntent, draftStart, draftEnd) },
                )
                Button(onClick = { expanded = !expanded }, modifier = Modifier.height(38.dp), shape = RoundedCornerShape(19.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (hasActiveFilters) CallActiveBlue else CallButtonColor, contentColor = if (hasActiveFilters) CallActionContent else CallText)) {
                    Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, if (expanded) "收起" else "展开", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(if (expanded) "收起" else "展开", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (expanded) {
                AutoSearchChoiceRow(
                    label = "快捷时间",
                    options = listOf<Pair<Int?, String>>(null to "不限", 1 to "今天", 7 to "近7天", 30 to "近30天"),
                    selected = quickDays(draftStart, draftEnd),
                    resetKey = filterResetToken,
                    onSelect = { days ->
                        if (days == null) {
                            draftStart = null
                            draftEnd = null
                        } else {
                            draftStart = LocalDate.now().minusDays((days - 1).toLong())
                            draftEnd = LocalDate.now()
                        }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateButton("开始", draftStart, Modifier.weight(1f)) {
                        pickingDate = DateField.Start
                    }
                    DateButton("结束", draftEnd, Modifier.weight(1f)) {
                        pickingDate = DateField.End
                    }
                }
                AutoSearchChoiceRow(
                    label = "客户状态",
                    options = listOf<Pair<Int?, String>>(null to "全部", 1 to "待分配", 2 to "跟进中", 3 to "已成交", 4 to "无效"),
                    selected = draftStatus,
                    resetKey = filterResetToken,
                    onSelect = { draftStatus = it },
                )
                AutoSearchChoiceRow(
                    label = "意向度",
                    options = listOf<Pair<Int?, String>>(null to "全部", 0 to "未知", 1 to "基本无意向", 2 to "较低意向", 3 to "中等意向", 4 to "较高意向", 5 to "强烈意向"),
                    selected = draftIntent,
                    resetKey = filterResetToken,
                    onSelect = { draftIntent = it },
                )
                if (error.isNotBlank()) {
                    Text(error, color = CallHangupColor, style = MaterialTheme.typography.labelMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (onApply(draftKeyword, draftStatus, draftIntent, draftStart, draftEnd)) expanded = false
                    }, modifier = Modifier.weight(1f).height(36.dp), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp), colors = ButtonDefaults.buttonColors(containerColor = CallActiveBlue, contentColor = CallActionContent)) {
                        Text("应用", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = {
                        draftKeyword = ""
                        draftStatus = null
                        draftIntent = null
                        draftStart = null
                        draftEnd = null
                        filterResetToken++
                        expanded = false
                        onReset()
                    }, modifier = Modifier.weight(1f).height(36.dp), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp), colors = ButtonDefaults.buttonColors(containerColor = CallButtonColor, contentColor = CallText)) {
                        Text("重置", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    pickingDate?.let { field ->
        ThemedDatePickerDialog(
            initialDate = when (field) {
                DateField.Start -> draftStart ?: LocalDate.now()
                DateField.End -> draftEnd ?: draftStart ?: LocalDate.now()
            },
            onDismiss = { pickingDate = null },
            onSelected = {
                if (field == DateField.Start) draftStart = it else draftEnd = it
                pickingDate = null
            },
        )
    }
}

@Composable
private fun CompactSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, onSearch: () -> Unit) {
    Row(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(CallButtonColor)
            .padding(start = 12.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isBlank()) {
                Text("姓名/手机号", color = CallMutedText, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = CallText),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        IconButton(onClick = onSearch, modifier = Modifier.size(34.dp)) {
            Icon(painterResource(android.R.drawable.ic_menu_search), "查询", tint = CallText, modifier = Modifier.size(17.dp))
        }
    }
}

private enum class DateField { Start, End }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemedDatePickerDialog(initialDate: LocalDate, onDismiss: () -> Unit, onSelected: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate.datePickerMillis())
    val colors = DatePickerDefaults.colors(
        containerColor = CallBackground,
        titleContentColor = CallText,
        headlineContentColor = CallText,
        weekdayContentColor = CallMutedText,
        subheadContentColor = CallText,
        navigationContentColor = CallText,
        yearContentColor = CallText,
        currentYearContentColor = CallActiveBlue,
        selectedYearContentColor = CallActionContent,
        selectedYearContainerColor = CallActiveBlue,
        dayContentColor = CallText,
        selectedDayContentColor = CallActionContent,
        selectedDayContainerColor = CallActiveBlue,
        todayContentColor = CallActiveBlue,
        todayDateBorderColor = CallActiveBlue,
        dividerColor = CallButtonColor,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onSelected(datePickerMillisToLocalDate(it)) }
            }) {
                Text("确定", color = CallActiveBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = CallMutedText)
            }
        },
        colors = colors,
    ) {
        DatePicker(state = state, colors = colors)
    }
}

@Composable
private fun DateButton(label: String, date: LocalDate?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(34.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = CallButtonColor, contentColor = CallText), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
        Text(if (date == null) label else "$label ${date}", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun customerStatusLabel(status: Int) = when (status) {
    1 -> "待分配"
    2 -> "跟进中"
    3 -> "已成交"
    4 -> "无效"
    else -> "未知"
}

private fun schoolGradeText(schoolName: String, gradeName: String) =
    listOf(schoolName, gradeName).filter { it.isNotBlank() }.joinToString(" · ")

private fun quickDays(startDate: LocalDate?, endDate: LocalDate?): Int? {
    if (endDate != LocalDate.now()) return null
    return when (startDate) {
        LocalDate.now() -> 1
        LocalDate.now().minusDays(6) -> 7
        LocalDate.now().minusDays(29) -> 30
        else -> null
    }
}

private fun recordingUri(source: String): Uri = when {
    source.startsWith("http://") || source.startsWith("https://") || source.startsWith("content://") || source.startsWith("file://") -> Uri.parse(source)
    else -> Uri.fromFile(File(source))
}

private fun LocalDate.startInstant() = atStartOfDay(ZoneId.systemDefault()).toInstant().toString()

private fun LocalDate.endInstant() = atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toString()

private fun LocalDate.datePickerMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun datePickerMillisToLocalDate(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun CustomerStatusBadge(status: Int) {
    val (background, content) = customerStatusColors(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(customerStatusLabel(status), color = content, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun customerStatusColors(status: Int): Pair<Color, Color> = when (status) {
    2 -> CallActiveBlue to CallActionContent
    3 -> CallAcceptColor to CallActionContent
    4 -> CallMutedText.copy(alpha = 0.18f) to CallMutedText
    else -> CallButtonColor to CallMutedText
}

@Composable
private fun RecordingStatusBadge(hasRecording: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (hasRecording) CallAcceptColor else CallButtonColor)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(if (hasRecording) "有录音" else "无录音", color = if (hasRecording) CallActionContent else CallMutedText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IntentBadge(level: Int, label: String) {
    val color = intentColor(level)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(label.ifBlank { "未知" }, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
