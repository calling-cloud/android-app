package com.example.telephone

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.telephone.model.Session
import com.example.telephone.ui.CallHeartColor
import com.example.telephone.ui.CallMutedText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val callTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

internal fun isDefaultDialer(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    } else {
        context.getSystemService(TelecomManager::class.java).defaultDialerPackage == context.packageName
    }
}

internal fun defaultDialerIntent(context: Context): Intent? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            null
        }
    } else {
        val telecom = context.getSystemService(TelecomManager::class.java)
        if (telecom.defaultDialerPackage != context.packageName) {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        } else {
            null
        }
    }
}

internal fun placeCall(context: Context, phone: String) {
    val uri = Uri.parse("tel:$phone")
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(TelecomManager::class.java).placeCall(uri, Bundle())
        } else {
            context.startActivity(Intent(Intent.ACTION_CALL, uri))
        }
    }.onFailure {
        context.startActivity(Intent(Intent.ACTION_DIAL, uri))
    }
}

internal fun setSpeaker(context: Context, enabled: Boolean): Boolean {
    if (TelephoneInCallService.setCallSpeaker(enabled)) return true
    @Suppress("DEPRECATION")
    return runCatching {
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isSpeakerphoneOn = enabled
    }.isSuccess
}

internal fun setMuted(context: Context, enabled: Boolean): Boolean {
    if (TelephoneInCallService.setCallMuted(enabled)) return true
    @Suppress("DEPRECATION")
    return runCatching {
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isMicrophoneMute = enabled
    }.isSuccess
}

internal fun formatDuration(seconds: Int): String {
    val minute = seconds / 60
    val second = seconds % 60
    return "%02d:%02d".format(minute, second)
}

internal fun formatCallTime(value: String): String {
    return runCatching {
        Instant.parse(value).atZone(ZoneId.systemDefault()).format(callTimeFormatter)
    }.getOrDefault(value)
}

internal fun formatPhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    if (digits.length != 11) return phone
    return "+86 ${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7)}"
}

@Composable
internal fun intentColor(level: Int) = when (level) {
    1 -> Color(0xFF8B93A8)
    2 -> Color(0xFF4E9BFF)
    3 -> Color(0xFF22C55E)
    4 -> Color(0xFFF59E0B)
    5 -> CallHeartColor
    else -> CallMutedText
}

internal fun runOnMain(block: () -> Unit) {
    android.os.Handler(android.os.Looper.getMainLooper()).post(block)
}

internal fun loadSession(prefs: SharedPreferences): Session? {
    val baseUrl = if (BuildConfig.SERVER_URL_EDITABLE) prefs.getString("server_url", null) ?: return null else BuildConfig.DEFAULT_SERVER_URL
    val token = prefs.getString("access_token", null) ?: return null
    val username = prefs.getString("username", null) ?: return null
    val realName = prefs.getString("real_name", null) ?: username
    return Session(ApiClient(baseUrl), token, username, realName)
}

internal fun saveSession(prefs: SharedPreferences, session: Session) {
    prefs.edit()
        .putString("server_url", session.api.baseUrl)
        .putString("access_token", session.token)
        .putString("username", session.username)
        .putString("real_name", session.realName)
        .apply()
}

internal fun clearSession(prefs: SharedPreferences) {
    prefs.edit()
        .remove("access_token")
        .remove("username")
        .remove("real_name")
        .apply()
}
