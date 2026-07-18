package com.example.telephone

import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CallRecordingManager {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var lastFinishedFile: File? = null

    @Synchronized
    fun start(context: Context): File? {
        if (recorder != null) return outputFile
        val dir = File(context.getExternalFilesDir(null), "recordings").apply { mkdirs() }
        val file = File(dir, "call_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.m4a")
        return runCatching {
            @Suppress("DEPRECATION")
            val mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            outputFile = file
            file
        }.getOrNull()
    }

    @Synchronized
    fun stop(): File? {
        val file = outputFile
        recorder?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        recorder = null
        outputFile = null
        if (file != null) lastFinishedFile = file
        return file
    }

    @Synchronized
    fun takeLastFinishedFile(): File? {
        val file = lastFinishedFile
        lastFinishedFile = null
        return file
    }
}
