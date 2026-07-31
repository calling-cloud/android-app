package com.example.telephone

import android.content.Context
import android.util.Base64
import com.example.telephone.model.AuthExpiredException
import com.example.telephone.model.AppTeam
import com.example.telephone.model.AppTeamMember
import com.example.telephone.model.AssignedCustomerPage
import com.example.telephone.model.CallRecord
import com.example.telephone.model.CallRecordQuery
import com.example.telephone.model.CallSummary
import com.example.telephone.model.CallSummaryPage
import com.example.telephone.model.Customer
import com.example.telephone.model.DialerOptions
import com.example.telephone.model.GradeOption
import com.example.telephone.model.Overview
import com.example.telephone.model.Session
import com.example.telephone.model.SchoolOption
import com.example.telephone.model.StatisticsRank
import com.example.telephone.model.StatisticsSummary
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.security.KeyFactory
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import kotlin.math.roundToInt

internal class ApiClient(val baseUrl: String) {
    private var publicKeyPem: String? = null

    fun login(username: String, password: String): Session {
        val data = request("POST", "/api/auth/app-login", body = JSONObject().put("username", username).put("password", encryptSensitive(password))).getJSONObject("data")
        val user = data.getJSONObject("userInfo")
        return Session(this, data.getString("accessToken"), user.getString("username"), user.optString("realName", user.getString("username")))
    }

    fun nextCustomer(token: String, page: Int, schoolId: Int? = null, gradeCode: Int? = null): AssignedCustomerPage {
        val params = mutableListOf("pageSize" to "1", "page" to page.toString())
        schoolId?.let { params += "schoolId" to it.toString() }
        gradeCode?.let { params += "gradeCode" to it.toString() }
        val data = request("GET", "/api/app/customers?${params.queryString()}", token).getJSONObject("data")
        val items = data.optJSONArray("items") ?: JSONArray()
        return AssignedCustomerPage(
            customer = items.optJSONObject(0)?.toCustomer(),
            page = data.optInt("page", page),
            total = data.optInt("total"),
        )
    }

    fun dialerOptions(token: String): DialerOptions {
        val data = request("GET", "/api/app/options?modules=dicts,schools", token).getJSONObject("data")
        val dicts = data.optJSONObject("dicts") ?: JSONObject()
        val schools = data.optJSONArray("schools") ?: JSONArray()
        val grades = dicts.optJSONArray("grades") ?: JSONArray()
        return DialerOptions(
            schools = (0 until schools.length()).map { schools.getJSONObject(it).toSchoolOption() },
            grades = (0 until grades.length()).map { grades.getJSONObject(it).toGradeOption() },
        )
    }

    fun createCallRecord(token: String, customerId: Int): Int {
        return request(
            "POST",
            "/api/app/call-records",
            token,
            JSONObject().put("customerId", customerId),
        ).getJSONObject("data").getInt("id")
    }

    fun updateCustomerStatus(token: String, customerId: Int, status: Int) {
        request("PUT", "/api/app/customers/$customerId/status", token, JSONObject().put("status", status))
    }

    fun lookupCustomerName(token: String, phone: String): String? {
        val data = request("GET", "/api/app/customers/lookup?phone=${phone.urlEncoded()}", token).optJSONObject("data") ?: return null
        return data.optString("customerName").takeIf { it.isNotBlank() }
    }

    fun updateCallRecord(token: String, id: Int, durationSeconds: Int, intentLevel: Int, remark: String, recordingUrl: String?) {
        request(
            "PUT",
            "/api/app/call-records/$id",
            token,
            JSONObject()
                .put("durationSeconds", durationSeconds)
                .put("intentLevel", intentLevel)
                .put("remark", remark)
                .put("recordingUrl", recordingUrl),
        )
    }

    fun syncCallRecord(token: String, id: Int, durationSeconds: Int, recordingUrl: String?) {
        val body = JSONObject().put("durationSeconds", durationSeconds)
        if (recordingUrl != null) body.put("recordingUrl", recordingUrl)
        request("PUT", "/api/app/call-records/$id", token, body)
    }

    fun uploadRecording(context: Context, token: String, customerId: Int, file: File, onProgress: (Float) -> Unit): String {
        val sts = request("GET", "/api/app/oss/sts?customerId=$customerId", token).getJSONObject("data").toOssSts()
        return OssMultipartUploader.upload(context, file, sts, onProgress)
    }

    fun appTeams(token: String): List<AppTeam> {
        val items = request("GET", "/api/app/teams", token).getJSONObject("data").optJSONArray("items") ?: JSONArray()
        return (0 until items.length()).map { items.getJSONObject(it).toAppTeam() }
    }

    fun appTeamMembers(token: String, teamId: Int): List<AppTeamMember> {
        val items = request("GET", "/api/app/teams/$teamId/members", token).getJSONObject("data").optJSONArray("items") ?: JSONArray()
        return (0 until items.length()).map { items.getJSONObject(it).toAppTeamMember() }
    }

    fun statisticsSummary(token: String, teamId: Int, employeeId: Int?, startDate: String?, endDate: String?): StatisticsSummary {
        return request("GET", "/api/app/statistics/summary?${statisticsQuery(teamId, employeeId, startDate, endDate)}", token).getJSONObject("data").toStatisticsSummary()
    }

    fun statisticsTeamRank(token: String, teamId: Int, employeeId: Int?, startDate: String?, endDate: String?): StatisticsRank {
        return request("GET", "/api/app/statistics/team-rank?${statisticsQuery(teamId, employeeId, startDate, endDate)}", token).getJSONObject("data").toStatisticsRank()
    }

    fun statisticsTotalRank(token: String, teamId: Int, employeeId: Int?, startDate: String?, endDate: String?): StatisticsRank {
        return request("GET", "/api/app/statistics/total-rank?${statisticsQuery(teamId, employeeId, startDate, endDate)}", token).getJSONObject("data").toStatisticsRank()
    }

    fun overview(token: String): Overview {
        val data = request("GET", "/api/app/overview", token).getJSONObject("data")
        val status = data.optJSONArray("statusStats") ?: JSONArray()
        return Overview(
            customerTotal = data.optInt("customerTotal"),
            communicationTotal = data.optInt("communicationTotal"),
            dealTotal = data.optInt("dealTotal"),
            conversionRate = data.optDouble("conversionRate"),
            statusStats = (0 until status.length()).map {
                val row = status.getJSONObject(it)
                row.optInt("status") to row.optInt("total")
            },
        )
    }

    fun callSummaries(token: String, cursor: String? = null, query: CallRecordQuery = CallRecordQuery()): CallSummaryPage {
        val params = mutableListOf("pageSize" to "20")
        cursor?.let { params += "cursor" to it }
        if (query.keyword.isNotBlank()) params += "keyword" to query.keyword.trim()
        query.customerStatus?.let { params += "status" to it.toString() }
        query.intentLevel?.let { params += "intentLevel" to it.toString() }
        query.callAtStart?.let { params += "callAtStart" to it }
        query.callAtEnd?.let { params += "callAtEnd" to it }
        val path = "/api/app/call-records?" + params.joinToString("&") { "${it.first}=${it.second.urlEncoded()}" }
        val data = request("GET", path, token).getJSONObject("data")
        val items = data.optJSONArray("items") ?: JSONArray()
        return CallSummaryPage(
            items = (0 until items.length()).map { index -> items.getJSONObject(index).toCallSummary() },
            nextCursor = data.optString("nextCursor").ifBlank { null },
        )
    }

    fun callRecord(token: String, id: Int): CallRecord {
        return request("GET", "/api/app/call-records/$id", token).getJSONObject("data").toCallRecord()
    }

    fun customerCallRecords(token: String, customerId: Int): List<CallRecord> {
        val items = request("GET", "/api/app/customers/$customerId/call-records?pageSize=20", token).getJSONObject("data").optJSONArray("items") ?: JSONArray()
        return (0 until items.length()).map { index -> items.getJSONObject(index).toCallRecord() }
    }

    fun changePassword(token: String, oldPassword: String, newPassword: String) {
        request("PUT", "/api/app/user/password", token, JSONObject().put("oldPassword", encryptSensitive(oldPassword)).put("newPassword", encryptSensitive(newPassword)))
    }

    fun appVersion(): AppUpdateInfo {
        val data = request("GET", "/api/app/version").getJSONObject("data")
        return AppUpdateInfo(
            versionCode = data.optLong("versionCode"),
            versionName = data.optString("versionName"),
            minVersionCode = data.optLong("minVersionCode"),
            apkUrl = data.optString("apkUrl"),
            sha256 = data.optString("sha256"),
            force = data.optBoolean("force"),
            changelog = data.optString("changelog"),
        )
    }

    private fun encryptSensitive(value: String): String {
        val pem = publicKeyPem ?: request("GET", "/api/auth/public-key")
            .getJSONObject("data")
            .getString("publicKey")
            .also { publicKeyPem = it }
        val base64 = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace(Regex("\\s"), "")
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.decode(base64, Base64.DEFAULT)))
        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key,
            OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT),
        )
        return Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun request(method: String, path: String, token: String? = null, body: JSONObject? = null): JSONObject {
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                OutputStreamWriter(outputStream).use { it.write(body.toString()) }
            }
        }
        val text = if (connection.responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        val json = JSONObject(text.ifBlank { "{}" })
        if (connection.responseCode == 401) throw AuthExpiredException()
        if (connection.responseCode !in 200..299 || json.optInt("code", -1) != 0) {
            throw IllegalStateException(json.optString("message", "请求失败"))
        }
        return json
    }
}

private fun String.urlEncoded() = URLEncoder.encode(this, Charsets.UTF_8.name())

private fun List<Pair<String, String>>.queryString() = joinToString("&") { "${it.first}=${it.second.urlEncoded()}" }

private fun statisticsQuery(teamId: Int, employeeId: Int?, startDate: String?, endDate: String?): String {
    val params = mutableListOf("teamId" to teamId.toString())
    employeeId?.let { params += "employeeId" to it.toString() }
    startDate?.let { params += "startDate" to it }
    endDate?.let { params += "endDate" to it }
    return params.joinToString("&") { "${it.first}=${it.second.urlEncoded()}" }
}

private fun JSONObject.toAppTeam() = AppTeam(
    id = optInt("id"),
    teamName = optString("teamName"),
    role = optInt("role"),
)

private fun JSONObject.toAppTeamMember() = AppTeamMember(
    id = optInt("id"),
    realName = optString("realName"),
    username = optString("username"),
    role = optInt("role"),
)

private fun JSONObject.toSchoolOption() = SchoolOption(
    id = optInt("id"),
    schoolName = optString("schoolName"),
)

private fun JSONObject.toGradeOption() = GradeOption(
    gradeCode = optInt("gradeCode"),
    gradeName = optString("gradeName"),
    typeCode = optInt("typeCode"),
)

private fun JSONObject.toStatisticsStats(): com.example.telephone.model.StatisticsStats {
    val rawAverageDurationSeconds = optDouble("averageDurationSeconds", 0.0)
    val averageDurationSeconds = if (rawAverageDurationSeconds.isFinite()) rawAverageDurationSeconds else 0.0
    val durationSeconds = optInt("durationSeconds")
    val apiEffectiveCommunicationCount = optInt("effectiveCommunicationCount")
    val effectiveCommunicationCount = if (apiEffectiveCommunicationCount > 0 || durationSeconds <= 0 || averageDurationSeconds <= 0) {
        apiEffectiveCommunicationCount
    } else {
        (durationSeconds / averageDurationSeconds).roundToInt()
    }
    return com.example.telephone.model.StatisticsStats(
        averageDurationSeconds = averageDurationSeconds,
        communicatedCustomers = optInt("communicatedCustomers"),
        communicationCount = optInt("communicationCount"),
        conversionRate = optDouble("conversionRate"),
        dealCustomers = optInt("dealCustomers"),
        durationSeconds = durationSeconds,
        effectiveCommunicationCount = effectiveCommunicationCount,
    )
}

private fun JSONObject.toStatisticsRanks() = com.example.telephone.model.StatisticsRanks(
    averageDurationSeconds = optInt("averageDurationSeconds"),
    communicatedCustomers = optInt("communicatedCustomers"),
    communicationCount = optInt("communicationCount"),
    conversionRate = optInt("conversionRate"),
    dealCustomers = optInt("dealCustomers"),
    durationSeconds = optInt("durationSeconds"),
)

private fun JSONObject.toStatisticsSummary() = StatisticsSummary(
    employeeId = optInt("employeeId"),
    stats = getJSONObject("stats").toStatisticsStats(),
    teamId = optInt("teamId"),
)

private fun JSONObject.toStatisticsRank() = StatisticsRank(
    employeeId = optInt("employeeId"),
    ranks = getJSONObject("ranks").toStatisticsRanks(),
    stats = getJSONObject("stats").toStatisticsStats(),
    teamId = optInt("teamId"),
    employeeTotal = optInt("employeeTotal", optInt("teamTotal", optInt("total"))),
    total = optInt("total"),
)

private fun JSONObject.toCustomer() = Customer(
    id = optInt("id"),
    name = optString("customerName"),
    phone = optString("phone"),
    schoolName = optString("schoolName"),
    gradeName = optString("gradeName"),
)

private fun JSONObject.toCallRecord() = CallRecord(
    id = optInt("id"),
    customerId = optInt("customerId"),
    customerName = optString("customerName"),
    customerPhone = optString("customerPhone"),
    customerStatus = optInt("customerStatus"),
    schoolName = optString("schoolName"),
    gradeName = optString("gradeName"),
    callAt = optString("callAt"),
    callEmployeeName = optString("callEmployeeName"),
    durationSeconds = optInt("durationSeconds"),
    intentLevel = optInt("intentLevel"),
    intentLabel = optString("intentLabel"),
    recordingUrl = cleanString("recordingUrl"),
    remark = cleanString("remark"),
    canCallback = optBoolean("canCallback"),
)

private fun JSONObject.toCallSummary() = CallSummary(
    id = optInt("id"),
    customerId = optInt("customerId"),
    customerName = optString("customerName"),
    customerPhone = optString("customerPhone"),
    customerStatus = optInt("customerStatus"),
    schoolName = optString("schoolName"),
    gradeName = optString("gradeName"),
    lastCallRecordId = optInt("lastCallRecordId"),
    lastCallAt = optString("lastCallAt"),
    lastIntentLevel = optInt("lastIntentLevel"),
    intentLabel = optString("intentLabel"),
    callCount = optInt("callCount"),
)

private fun JSONObject.cleanString(key: String): String {
    val value = optString(key).trim()
    return if (value.equals("null", ignoreCase = true)) "" else value
}
