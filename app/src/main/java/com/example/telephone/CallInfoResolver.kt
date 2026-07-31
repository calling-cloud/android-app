package com.example.telephone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

internal const val UnknownPhone = "未知号码"

internal fun contactNameForPhone(context: Context, phone: String): String? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
    val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
    return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() } else null
    }
}

internal fun callDisplayName(context: Context, phone: String, fallback: String) =
    AppPlacedCallTracker.nameFor(phone)
        ?: contactNameForPhone(context, phone)
        ?: fallback

internal fun isUnknownCallName(name: String) = name == "未知来电" || name == "未知通话"
