package com.example.telephone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.telephone.model.CallState
import com.example.telephone.model.CallUi
import com.example.telephone.model.Customer
import com.example.telephone.ui.DarkAppPalette
import com.example.telephone.ui.LightAppPalette
import com.example.telephone.ui.LocalAppPalette
import com.example.telephone.ui.components.CallResultForm
import com.example.telephone.ui.theme.TelephoneTheme

class DebugCallResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val customerName = intent.getStringExtra(ExtraCustomerName) ?: "测试客户"
        val customerPhone = intent.getStringExtra(ExtraCustomerPhone) ?: "13800138000"
        val durationSeconds = intent.getIntExtra(ExtraDurationSeconds, 185).coerceAtLeast(0)

        setContent {
            val darkTheme = isSystemInDarkTheme()
            var message by remember { mutableStateOf("") }

            TelephoneTheme(darkTheme = darkTheme, dynamicColor = false) {
                CompositionLocalProvider(LocalAppPalette provides if (darkTheme) DarkAppPalette else LightAppPalette) {
                    CallResultForm(
                        call = CallUi(
                            customer = Customer(
                                id = 1,
                                name = customerName,
                                phone = customerPhone,
                                schoolName = "测试学校",
                                gradeName = "测试年级",
                            ),
                            state = CallState.Ended,
                            startedAt = System.currentTimeMillis(),
                            recordId = 1,
                            durationSeconds = durationSeconds,
                            uploadProgress = 1f,
                            uploadError = "未生成录音文件（Debug 模拟）",
                        ),
                        message = message,
                        onMarkInvalid = { message = "已模拟标记为无效" },
                        onMarkDeal = { message = "已模拟标记为成交" },
                        onSubmit = { _, _ -> finish() },
                    )
                }
            }
        }
    }

    private companion object {
        const val ExtraCustomerName = "customer_name"
        const val ExtraCustomerPhone = "customer_phone"
        const val ExtraDurationSeconds = "duration_seconds"
    }
}
