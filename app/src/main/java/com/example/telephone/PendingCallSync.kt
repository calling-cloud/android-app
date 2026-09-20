package com.example.telephone

import android.content.Context
import com.example.telephone.model.AuthExpiredException
import com.example.telephone.model.Session
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread

internal data class PendingCallSync(
    val recordId: Int,
    val customerId: Int,
    val durationSeconds: Int,
    val recordingFile: String?,
    val recordingUrl: String?,
)

internal object PendingCallSyncCache {
    private const val PrefName = "telephone_pending_call_sync"
    private const val KeyItems = "items"

    @Synchronized
    fun upsert(context: Context, item: PendingCallSync) {
        val items = allLocked(context).filterNot { it.recordId == item.recordId }.toMutableList()
        items += item
        saveLocked(context, items)
    }

    @Synchronized
    fun markUploaded(context: Context, recordId: Int, recordingUrl: String) {
        saveLocked(
            context,
            allLocked(context).map {
                if (it.recordId == recordId) it.copy(recordingUrl = recordingUrl) else it
            },
        )
    }

    @Synchronized
    fun remove(context: Context, recordId: Int) {
        saveLocked(context, allLocked(context).filterNot { it.recordId == recordId })
    }

    @Synchronized
    fun all(context: Context): List<PendingCallSync> = allLocked(context)

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PrefName, Context.MODE_PRIVATE)

    private fun allLocked(context: Context): List<PendingCallSync> {
        val array = runCatching { JSONArray(prefs(context).getString(KeyItems, "[]")) }.getOrDefault(JSONArray())
        return (0 until array.length()).mapNotNull { index ->
            runCatching {
                val item = array.getJSONObject(index)
                PendingCallSync(
                    recordId = item.getInt("recordId"),
                    customerId = item.getInt("customerId"),
                    durationSeconds = item.getInt("durationSeconds"),
                    recordingFile = item.optString("recordingFile").ifBlank { null },
                    recordingUrl = item.optString("recordingUrl").ifBlank { null },
                )
            }.getOrNull()
        }
    }

    private fun saveLocked(context: Context, items: List<PendingCallSync>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("recordId", it.recordId)
                    .put("customerId", it.customerId)
                    .put("durationSeconds", it.durationSeconds)
                    .put("recordingFile", it.recordingFile ?: "")
                    .put("recordingUrl", it.recordingUrl ?: ""),
            )
        }
        prefs(context).edit().putString(KeyItems, array.toString()).apply()
    }
}

internal object PendingCallSyncWorker {
    @Volatile
    private var running = false

    fun sync(context: Context, session: Session, onAuthExpired: () -> Unit) {
        if (running) return
        running = true
        val appContext = context.applicationContext
        thread {
            try {
                for (item in PendingCallSyncCache.all(appContext)) {
                    runCatching {
                        val recordingUrl = item.recordingUrl ?: uploadIfNeeded(appContext, session, item)
                        if (recordingUrl != item.recordingUrl && recordingUrl != null) {
                            PendingCallSyncCache.markUploaded(appContext, item.recordId, recordingUrl)
                        }
                        session.api.syncCallRecord(session.token, item.recordId, item.durationSeconds, recordingUrl)
                        PendingCallSyncCache.remove(appContext, item.recordId)
                    }.onFailure { error ->
                        if (error is AuthExpiredException) {
                            runOnMain(onAuthExpired)
                            return@thread
                        }
                    }
                }
            } finally {
                running = false
            }
        }
    }

    private fun uploadIfNeeded(context: Context, session: Session, item: PendingCallSync): String? {
        val file = item.recordingFile?.let(::File) ?: return null
        if (!file.exists() || file.length() <= 0) return null
        return session.api.uploadRecording(context, session.token, item.customerId, file) {}
    }
}
