package com.example.telephone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.telephone.model.AppTeam
import com.example.telephone.model.AppTeamMember
import com.example.telephone.model.AuthExpiredException
import com.example.telephone.model.Session
import com.example.telephone.model.StatisticsRank
import com.example.telephone.model.StatisticsStats
import com.example.telephone.runOnMain
import com.example.telephone.ui.CallActionContent
import com.example.telephone.ui.CallActiveBlue
import com.example.telephone.ui.CallBackground
import com.example.telephone.ui.CallButtonColor
import com.example.telephone.ui.CallHangupColor
import com.example.telephone.ui.CallMutedText
import com.example.telephone.ui.CallText
import com.example.telephone.ui.UiSmallIconSize
import com.example.telephone.ui.components.AppCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.concurrent.thread

internal class StatsViewModel : ViewModel() {
    var teams by mutableStateOf<List<AppTeam>>(emptyList())
        private set
    var teamId by mutableStateOf<Int?>(null)
        private set
    var employeeId by mutableStateOf<Int?>(null)
        private set
    var startDate by mutableStateOf<LocalDate?>(null)
        private set
    var endDate by mutableStateOf<LocalDate?>(null)
        private set
    var members by mutableStateOf<List<AppTeamMember>>(emptyList())
        private set
    var summary by mutableStateOf<StatisticsStats?>(null)
        private set
    var teamRank by mutableStateOf<StatisticsRank?>(null)
        private set
    var totalRank by mutableStateOf<StatisticsRank?>(null)
        private set
    var error by mutableStateOf("")
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    private var loadedToken: String? = null
    private var teamsLoaded = false
    private var teamsLoading = false
    private var statsRequestId = 0

    fun ensureLoaded(session: Session, onAuthExpired: () -> Unit) {
        if (loadedToken != session.token) reset(session.token)
        if (!teamsLoaded && !teamsLoading) loadTeams(session, onAuthExpired)
    }

    fun refresh(session: Session, onAuthExpired: () -> Unit) {
        if (teams.isEmpty()) loadTeams(session, onAuthExpired) else loadStats(session, onAuthExpired)
    }

    fun selectTeam(session: Session, onAuthExpired: () -> Unit, id: Int) {
        if (teamId == id) return
        teamId = id
        employeeId = null
        loadMembers(session, onAuthExpired, id)
    }

    fun selectEmployee(session: Session, onAuthExpired: () -> Unit, id: Int) {
        if (employeeId == id) return
        employeeId = id
        loadStats(session, onAuthExpired)
    }

    fun selectDates(session: Session, onAuthExpired: () -> Unit, start: LocalDate?, end: LocalDate?) {
        if (startDate == start && endDate == end) return
        startDate = start
        endDate = end
        loadStats(session, onAuthExpired)
    }

    private fun reset(token: String) {
        loadedToken = token
        teamsLoaded = false
        teamsLoading = false
        statsRequestId++
        teams = emptyList()
        teamId = null
        employeeId = null
        startDate = null
        endDate = null
        members = emptyList()
        summary = null
        teamRank = null
        totalRank = null
        error = ""
        isRefreshing = false
    }

    private fun fail(t: Throwable, fallback: String, onAuthExpired: () -> Unit) {
        if (t is AuthExpiredException) onAuthExpired() else error = t.message ?: fallback
    }

    private fun loadTeams(session: Session, onAuthExpired: () -> Unit) {
        val token = session.token
        teamsLoading = true
        error = ""
        thread {
            runCatching { session.api.appTeams(token) }
                .onSuccess { list ->
                    runOnMain {
                        if (loadedToken != token) return@runOnMain
                        teams = list
                        teamsLoaded = true
                        teamsLoading = false
                        val nextTeamId = teamId ?: list.firstOrNull()?.id
                        teamId = nextTeamId
                        nextTeamId?.let { loadMembers(session, onAuthExpired, it) }
                    }
                }
                .onFailure {
                    runOnMain {
                        if (loadedToken != token) return@runOnMain
                        teamsLoaded = true
                        teamsLoading = false
                        fail(it, "团队加载失败", onAuthExpired)
                    }
                }
        }
    }

    private fun loadMembers(session: Session, onAuthExpired: () -> Unit, id: Int) {
        val token = session.token
        members = emptyList()
        thread {
            runCatching { session.api.appTeamMembers(token, id) }
                .onSuccess { list ->
                    runOnMain {
                        if (loadedToken != token || teamId != id) return@runOnMain
                        members = list
                        val nextEmployeeId = employeeId?.takeIf { current -> list.any { it.id == current } } ?: list.firstOrNull()?.id
                        employeeId = nextEmployeeId
                        nextEmployeeId?.let { loadStats(session, onAuthExpired) }
                    }
                }
                .onFailure {
                    runOnMain {
                        if (loadedToken != token || teamId != id) return@runOnMain
                        fail(it, "员工加载失败", onAuthExpired)
                    }
                }
        }
    }

    private fun loadStats(session: Session, onAuthExpired: () -> Unit) {
        val id = teamId ?: return
        val employee = employeeId ?: return
        val start = startDate?.toString()
        val end = endDate?.toString()
        val token = session.token
        val requestId = ++statsRequestId
        isRefreshing = true
        error = ""
        summary = null
        teamRank = null
        totalRank = null
        thread {
            runCatching {
                StatsBundle(
                    session.api.statisticsSummary(token, id, employee, start, end).stats,
                    session.api.statisticsTeamRank(token, id, employee, start, end),
                    session.api.statisticsTotalRank(token, id, employee, start, end),
                )
            }.onSuccess { bundle ->
                runOnMain {
                    if (loadedToken != token || requestId != statsRequestId) return@runOnMain
                    summary = bundle.summary
                    teamRank = bundle.teamRank
                    totalRank = bundle.totalRank
                }
            }.onFailure {
                runOnMain {
                    if (loadedToken != token || requestId != statsRequestId) return@runOnMain
                    fail(it, "统计加载失败", onAuthExpired)
                }
            }
            runOnMain {
                if (loadedToken == token && requestId == statsRequestId) isRefreshing = false
            }
        }
    }
}

@Composable
internal fun StatsScreen(session: Session, padding: PaddingValues, onAuthExpired: () -> Unit, viewModel: StatsViewModel) {
    LaunchedEffect(session.token) { viewModel.ensureLoaded(session, onAuthExpired) }

    PullToRefreshBox(isRefreshing = viewModel.isRefreshing, onRefresh = { viewModel.refresh(session, onAuthExpired) }, modifier = Modifier.fillMaxSize().background(CallBackground).padding(padding)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                StatsFilters(
                    teams = viewModel.teams,
                    members = viewModel.members,
                    teamId = viewModel.teamId,
                    employeeId = viewModel.employeeId,
                    startDate = viewModel.startDate,
                    endDate = viewModel.endDate,
                    onTeam = { viewModel.selectTeam(session, onAuthExpired, it) },
                    onEmployee = { viewModel.selectEmployee(session, onAuthExpired, it) },
                    onDates = { start, end -> viewModel.selectDates(session, onAuthExpired, start, end) },
                )
            }
            if (viewModel.error.isNotBlank()) item { Text(viewModel.error, color = CallHangupColor) }
            viewModel.summary?.let { item { SummaryCards(it, viewModel.teamRank, viewModel.totalRank) } }
            if (viewModel.teams.isEmpty() && viewModel.error.isBlank()) {
                item { Text("暂无可查看团队", color = CallMutedText, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun StatsFilters(
    teams: List<AppTeam>,
    members: List<AppTeamMember>,
    teamId: Int?,
    employeeId: Int?,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onTeam: (Int) -> Unit,
    onEmployee: (Int) -> Unit,
    onDates: (LocalDate?, LocalDate?) -> Unit,
) {
    var picking by remember { mutableStateOf<StatsDateField?>(null) }
    var expanded by remember { mutableStateOf(false) }
    AppCard {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("统计筛选", color = CallText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(filterSummary(teams, members, teamId, employeeId, startDate, endDate), color = CallMutedText, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(onClick = { expanded = !expanded }, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp), colors = ButtonDefaults.buttonColors(containerColor = CallButtonColor, contentColor = CallText)) {
                    Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, if (expanded) "收起" else "筛选", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(if (expanded) "收起" else "筛选", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (expanded) {
                ChoiceRow("团队", teams.map { it.id to it.teamName }, teamId, onTeam)
                ChoiceRow("员工", members.map { it.id to memberName(it) }, employeeId, onEmployee)
                ChoiceRow(
                    "时间",
                    listOf(0 to "全部", 1 to "今天", 7 to "近7天", 30 to "近30天"),
                    quickDays(startDate, endDate) ?: if (startDate == null && endDate == null) 0 else null,
                ) { days ->
                    if (days == 0) onDates(null, null) else onDates(LocalDate.now().minusDays((days - 1).toLong()), LocalDate.now())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateButton("开始", startDate, Modifier.weight(1f), selected = startDate != null) { picking = StatsDateField.Start }
                    DateButton("结束", endDate, Modifier.weight(1f), selected = endDate != null) { picking = StatsDateField.End }
                }
            }
        }
    }
    picking?.let { field ->
        ThemedDatePickerDialog(
            initialDate = if (field == StatsDateField.Start) startDate ?: LocalDate.now() else endDate ?: startDate ?: LocalDate.now(),
            onDismiss = { picking = null },
            onSelected = {
                if (field == StatsDateField.Start) onDates(it, endDate) else onDates(startDate, it)
                picking = null
            },
        )
    }
}

@Composable
private fun SummaryCards(stats: StatisticsStats, teamRank: StatisticsRank?, totalRank: StatisticsRank?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("沟通客户", stats.communicatedCustomers, rankInfo(teamRank, totalRank) { it.ranks.communicatedCustomers }, Modifier.weight(1f))
            StatCard("沟通次数", stats.communicationCount, rankInfo(teamRank, totalRank) { it.ranks.communicationCount }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("成交客户", stats.dealCustomers, rankInfo(teamRank, totalRank) { it.ranks.dealCustomers }, Modifier.weight(1f))
            StatCard("转化率", "${stats.conversionRate}%", rankInfo(teamRank, totalRank) { it.ranks.conversionRate }, Modifier.weight(1f))
        }
        StatCard("通话时长", formatDuration(stats.durationSeconds), rankInfo(teamRank, totalRank) { it.ranks.durationSeconds }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatCard(title: String, value: Any, rank: RankInfo, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = CallMutedText, style = MaterialTheme.typography.labelMedium)
            Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = CallText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(rank.text, color = CallMutedText, style = MaterialTheme.typography.labelSmall)
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(5.dp)).background(CallButtonColor)) {
                Box(Modifier.fillMaxWidth(rank.progress).height(5.dp).background(CallActiveBlue))
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, options: List<Pair<Int, String>>, selected: Int?, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = CallMutedText, style = MaterialTheme.typography.labelMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) ->
                FilterButton(text, selected == value) { onSelect(value) }
            }
        }
    }
}

@Composable
private fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun DateButton(label: String, date: LocalDate?, modifier: Modifier = Modifier, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) CallActiveBlue else CallButtonColor,
            contentColor = if (selected) CallActionContent else CallText,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        Icon(painterResource(android.R.drawable.ic_menu_month), label, modifier = Modifier.size(UiSmallIconSize))
        Spacer(Modifier.width(4.dp))
        Text(if (date == null) label else "$label ${date}", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemedDatePickerDialog(initialDate: LocalDate, onDismiss: () -> Unit, onSelected: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
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
                state.selectedDateMillis?.let {
                    onSelected(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                }
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

private enum class StatsDateField { Start, End }

private data class StatsBundle(val summary: StatisticsStats, val teamRank: StatisticsRank, val totalRank: StatisticsRank)

private data class RankInfo(val text: String, val progress: Float)

private fun rankInfo(teamRank: StatisticsRank?, totalRank: StatisticsRank?, rank: (StatisticsRank) -> Int): RankInfo {
    val total = totalRank?.rankTotal() ?: 0
    val value = totalRank?.let(rank) ?: 0
    return RankInfo(
        "${rankLine("团队排行", teamRank, rank)}\n${rankLine("总排行", totalRank, rank)}",
        if (value > 0 && total > 0) ((total - value + 1).toFloat() / total).coerceIn(0f, 1f) else 0f,
    )
}

private fun rankLine(label: String, data: StatisticsRank?, rank: (StatisticsRank) -> Int): String {
    val total = data?.rankTotal() ?: 0
    val value = data?.let(rank) ?: 0
    return "$label ${if (value > 0 && total > 0) "$value/$total" else "--/--"}名"
}

private fun StatisticsRank.rankTotal() = employeeTotal.takeIf { it > 0 } ?: total

private fun memberName(member: AppTeamMember) = member.realName.ifBlank { member.username }

private fun filterSummary(teams: List<AppTeam>, members: List<AppTeamMember>, teamId: Int?, employeeId: Int?, startDate: LocalDate?, endDate: LocalDate?): String {
    val team = teams.firstOrNull { it.id == teamId }?.teamName ?: "未选团队"
    val employee = members.firstOrNull { it.id == employeeId }?.let(::memberName) ?: "未选员工"
    return "$team · $employee · ${dateSummary(startDate, endDate)}"
}

private fun dateSummary(startDate: LocalDate?, endDate: LocalDate?) = when {
    startDate == null && endDate == null -> "全部时间"
    startDate != null && endDate != null -> "$startDate 至 $endDate"
    startDate != null -> "$startDate 起"
    else -> "至 $endDate"
}

private fun quickDays(startDate: LocalDate?, endDate: LocalDate?): Int? {
    if (endDate != LocalDate.now()) return null
    return when (startDate) {
        LocalDate.now() -> 1
        LocalDate.now().minusDays(6) -> 7
        LocalDate.now().minusDays(29) -> 30
        else -> null
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    return if (hours > 0) "${hours}小时${minutes}分" else "${minutes}分${seconds % 60}秒"
}
