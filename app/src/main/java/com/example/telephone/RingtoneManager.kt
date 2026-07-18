package com.example.telephone

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import androidx.annotation.RawRes
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal data class RingtoneOption(val key: String, val label: String) {
    val imported: Boolean get() = key.startsWith("file:")
}

internal object CallRingtoneManager {
    private const val PREF_KEY = "ringtone_key"
    private const val PREF_LABEL = "ringtone_label"
    private const val PREF_IMPORTED_KEY = "ringtone_imported_key"
    private const val PREF_IMPORTED_LABEL = "ringtone_imported_label"
    private const val PREF_IMPORTED_LIST = "ringtone_imported_list"
    private const val IMPORT_PREFIX = "file:"
    private const val IMPORT_DIR = "ringtones"
    private val VIBRATION_PATTERN = longArrayOf(0, 700, 500)

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private val builtin = listOf(
        BuiltinRingtone("apple", "Apple", R.raw.ringtone_apple),
        BuiltinRingtone("huankuai", "欢快", R.raw.ringtone_huankuai),
        BuiltinRingtone("zuoji", "座机", R.raw.ringtone_zuoji),
        BuiltinRingtone("zuoji2", "座机2", R.raw.ringtone_zuoji2),
        BuiltinRingtone("zuoji3", "座机3", R.raw.ringtone_zuoji3),
        BuiltinRingtone("weixin", "微信", R.raw.ringtone_weixin),
    )

    fun options(context: Context): List<RingtoneOption> {
        return builtin.map { RingtoneOption(it.key, it.label) } + importedOptions(context)
    }

    fun selected(context: Context): RingtoneOption {
        val prefs = prefs(context)
        val key = prefs.getString(PREF_KEY, null) ?: builtin.first().key
        val label = prefs.getString(PREF_LABEL, null)
        return RingtoneOption(key, label ?: builtin.firstOrNull { it.key == key }?.label ?: builtin.first().label)
    }

    fun select(context: Context, option: RingtoneOption) {
        prefs(context).edit().putString(PREF_KEY, option.key).putString(PREF_LABEL, option.label).apply()
    }

    fun delete(context: Context, option: RingtoneOption): RingtoneOption {
        if (!option.imported) return selected(context)
        File(option.key.removePrefix(IMPORT_PREFIX)).delete()
        saveImportedOptions(context, importedOptions(context).filterNot { it.key == option.key })
        return if (selected(context).key == option.key) {
            RingtoneOption(builtin.first().key, builtin.first().label).also { select(context, it) }
        } else {
            selected(context)
        }
    }

    fun import(context: Context, uri: Uri): Result<RingtoneOption> = runCatching {
        val name = displayName(context, uri).ifBlank { "导入铃声" }
        val safeName = name.replace(Regex("""[^\w.\-]"""), "_")
        val dir = File(context.filesDir, IMPORT_DIR).apply { mkdirs() }
        val file = File(dir, "${System.currentTimeMillis()}_$safeName")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法打开音频文件" }
            file.outputStream().use(input::copyTo)
        }
        RingtoneOption("$IMPORT_PREFIX${file.absolutePath}", "导入：$name").also {
            saveImportedOptions(context, importedOptions(context).filterNot { option -> option.key == it.key } + it)
        }
    }

    @Synchronized
    fun play(context: Context) {
        stop()
        val appContext = context.applicationContext
        val audio = appContext.getSystemService(AudioManager::class.java)
        when (audio.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                if (audio.getStreamVolume(AudioManager.STREAM_RING) > 0) {
                    player = createPlayer(appContext).also { it.start() }
                }
                vibrate(appContext)
            }
            AudioManager.RINGER_MODE_VIBRATE -> vibrate(appContext)
        }
    }

    @Synchronized
    fun stop() {
        vibrator?.cancel()
        vibrator = null
        player?.runCatching {
            stop()
            release()
        }
        player = null
    }

    private fun createPlayer(context: Context): MediaPlayer {
        val key = selected(context).key
        val builtin = builtin.firstOrNull { it.key == key }
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            if (builtin != null) {
                context.resources.openRawResourceFd(builtin.resId).use { fd ->
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                }
            } else if (key.startsWith(IMPORT_PREFIX) && File(key.removePrefix(IMPORT_PREFIX)).exists()) {
                setDataSource(key.removePrefix(IMPORT_PREFIX))
            } else {
                context.resources.openRawResourceFd(this@CallRingtoneManager.builtin.first().resId).use { fd ->
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                }
            }
            isLooping = true
            prepare()
        }
    }

    private fun vibrate(context: Context) {
        val deviceVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!deviceVibrator.hasVibrator()) return
        vibrator = deviceVibrator
        deviceVibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0))
    }

    private fun importedOptions(context: Context): List<RingtoneOption> {
        val prefs = prefs(context)
        val stored = runCatching { JSONArray(prefs.getString(PREF_IMPORTED_LIST, "[]")) }.getOrDefault(JSONArray())
        val options = buildList {
            for (index in 0 until stored.length()) {
                val item = stored.optJSONObject(index) ?: continue
                add(RingtoneOption(item.optString("key"), item.optString("label")))
            }
            val oldKey = prefs.getString(PREF_IMPORTED_KEY, null)
            val oldLabel = prefs.getString(PREF_IMPORTED_LABEL, null)
            if (oldKey != null && oldLabel != null) add(RingtoneOption(oldKey, oldLabel))
            importDir(context).listFiles()?.forEach { file ->
                add(RingtoneOption("$IMPORT_PREFIX${file.absolutePath}", "导入：${file.name.replaceFirst(Regex("""^\d+_"""), "")}"))
            }
        }
        return options
            .filter { it.key.startsWith(IMPORT_PREFIX) && File(it.key.removePrefix(IMPORT_PREFIX)).exists() }
            .distinctBy { it.key }
    }

    private fun saveImportedOptions(context: Context, options: List<RingtoneOption>) {
        val json = JSONArray()
        options.forEach { json.put(JSONObject().put("key", it.key).put("label", it.label)) }
        prefs(context).edit().putString(PREF_IMPORTED_LIST, json.toString()).apply()
    }

    private fun displayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0).orEmpty()
        }
        return uri.lastPathSegment.orEmpty()
    }

    private fun prefs(context: Context) = context.getSharedPreferences("telephone_app", Context.MODE_PRIVATE)

    private fun importDir(context: Context) = File(context.filesDir, IMPORT_DIR)

    private data class BuiltinRingtone(val key: String, val label: String, @param:RawRes val resId: Int)
}
