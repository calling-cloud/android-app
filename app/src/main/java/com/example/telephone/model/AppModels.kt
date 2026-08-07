package com.example.telephone.model

import com.example.telephone.ApiClient

internal data class Session(val api: ApiClient, val token: String, val username: String, val realName: String)

internal data class Customer(
    val id: Int,
    val name: String,
    val phone: String,
    val schoolName: String,
    val gradeName: String,
    val exclusiveEmployeeId: Int? = null,
    val exclusiveMode: Int? = null,
    val maxExclusiveAssignCount: Int? = null,
)

internal data class AssignedCustomerPage(val customer: Customer?, val page: Int, val total: Int)

internal data class SchoolOption(val id: Int, val schoolName: String)

internal data class GradeOption(val gradeCode: Int, val gradeName: String, val typeCode: Int)

internal data class DialerOptions(val schools: List<SchoolOption>, val grades: List<GradeOption>)

internal data class AppTeam(val id: Int, val teamName: String, val role: Int)

internal data class AppTeamMember(val id: Int, val realName: String, val username: String, val role: Int)

internal data class Overview(
    val customerTotal: Int,
    val communicationTotal: Int,
    val dealTotal: Int,
    val conversionRate: Double,
    val statusStats: List<Pair<Int, Int>>,
)

internal data class StatisticsStats(
    val averageDurationSeconds: Double,
    val communicatedCustomers: Int,
    val communicationCount: Int,
    val conversionRate: Double,
    val dealCustomers: Int,
    val durationSeconds: Int,
    val effectiveCommunicationCount: Int,
)

internal data class StatisticsRanks(
    val averageDurationSeconds: Int,
    val communicatedCustomers: Int,
    val communicationCount: Int,
    val conversionRate: Int,
    val dealCustomers: Int,
    val durationSeconds: Int,
)

internal data class StatisticsSummary(
    val employeeId: Int,
    val stats: StatisticsStats,
    val teamId: Int,
)

internal data class StatisticsRank(
    val employeeId: Int,
    val ranks: StatisticsRanks,
    val stats: StatisticsStats,
    val teamId: Int,
    val employeeTotal: Int,
    val total: Int,
)

internal data class CallSummaryPage(val items: List<CallSummary>, val nextCursor: String?)

internal data class CallRecordQuery(
    val keyword: String = "",
    val customerStatus: Int? = null,
    val intentLevel: Int? = null,
    val callAtStart: String? = null,
    val callAtEnd: String? = null,
)

internal data class CallRecord(
    val id: Int,
    val customerId: Int,
    val customerName: String,
    val customerPhone: String,
    val customerStatus: Int,
    val schoolName: String = "",
    val gradeName: String = "",
    val callAt: String,
    val callEmployeeName: String,
    val durationSeconds: Int,
    val intentLevel: Int,
    val intentLabel: String,
    val recordingUrl: String,
    val remark: String = "",
    val canCallback: Boolean = false,
)

internal data class CallSummary(
    val id: Int,
    val customerId: Int,
    val customerName: String,
    val customerPhone: String,
    val customerStatus: Int,
    val schoolName: String = "",
    val gradeName: String = "",
    val lastCallRecordId: Int,
    val lastCallAt: String,
    val lastIntentLevel: Int,
    val intentLabel: String,
    val callCount: Int,
)

internal enum class ThemeMode(val label: String) {
    System("系统"),
    Light("亮色"),
    Dark("暗色");

    companion object {
        fun fromStorage(value: String?) = entries.firstOrNull { it.name == value } ?: System
    }
}

internal enum class CallState { Dialing, Incoming, Connected, Ended }

internal data class CallUi(
    val customer: Customer,
    val state: CallState,
    val startedAt: Long,
    val recordId: Int? = null,
    val muted: Boolean = false,
    val speaker: Boolean = false,
    val durationSeconds: Int = 0,
    val recordingUrl: String? = null,
    val recordingFile: String? = null,
    val uploadProgress: Float = 0f,
    val uploadingRecording: Boolean = false,
    val uploadError: String = "",
) {
    fun durationSeconds() = if (state == CallState.Connected) ((System.currentTimeMillis() - startedAt) / 1000).toInt() else durationSeconds
}

internal class AuthExpiredException : IllegalStateException("登录已过期")
