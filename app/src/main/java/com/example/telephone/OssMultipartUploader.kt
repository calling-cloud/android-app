package com.example.telephone

import android.content.Context
import com.alibaba.sdk.android.oss.ClientConfiguration
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider
import com.alibaba.sdk.android.oss.model.ObjectMetadata
import com.alibaba.sdk.android.oss.model.ResumableUploadRequest
import org.json.JSONObject
import java.io.File

private const val OssPartSize = 2 * 1024 * 1024L

data class OssSts(
    val accessKeyId: String,
    val accessKeySecret: String,
    val bucket: String,
    val endpoint: String,
    val key: String,
    val stsToken: String,
    val url: String,
)

fun JSONObject.toOssSts() = OssSts(
    accessKeyId = getString("accessKeyId"),
    accessKeySecret = getString("accessKeySecret"),
    bucket = getString("bucket"),
    endpoint = getString("endpoint"),
    key = getString("key"),
    stsToken = getString("stsToken"),
    url = getString("url"),
)

object OssMultipartUploader {
    fun upload(context: Context, file: File, sts: OssSts, onProgress: (Float) -> Unit): String {
        val client = OSSClient(
            context.applicationContext,
            sts.endpoint,
            OSSStsTokenCredentialProvider(sts.accessKeyId, sts.accessKeySecret, sts.stsToken),
            ClientConfiguration().apply {
                connectionTimeout = 10_000
                socketTimeout = 30_000
                maxErrorRetry = 2
            },
        )
        val request = ResumableUploadRequest(
            sts.bucket,
            sts.key,
            file.absolutePath,
            ObjectMetadata().apply { contentType = "audio/mpeg" },
        ).apply {
            partSize = OssPartSize
            setProgressCallback { _, currentSize, totalSize ->
                onProgress(if (totalSize > 0) (currentSize.toFloat() / totalSize).coerceIn(0f, 1f) else 0f)
            }
        }
        client.resumableUpload(request)
        onProgress(1f)
        return sts.url
    }
}
