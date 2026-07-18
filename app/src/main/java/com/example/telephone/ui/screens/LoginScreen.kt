package com.example.telephone.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.telephone.ApiClient
import com.example.telephone.BuildConfig
import com.example.telephone.R
import com.example.telephone.model.Session
import com.example.telephone.runOnMain
import com.example.telephone.ui.CallActionContent
import com.example.telephone.ui.CallActiveBlue
import com.example.telephone.ui.CallButtonColor
import com.example.telephone.ui.CallHangupColor
import com.example.telephone.ui.CallMutedText
import com.example.telephone.ui.CallText
import com.example.telephone.ui.UiButtonRadius
import com.example.telephone.ui.UiSmallIconSize
import com.example.telephone.ui.CallInputBorder
import com.example.telephone.ui.CallInputColor
import com.example.telephone.ui.CallPlaceholderText
import com.example.telephone.ui.CallSurfaceColor
import com.example.telephone.ui.CallBackground
import com.example.telephone.ui.theme.TelephoneTheme
import kotlin.concurrent.thread

@Composable
internal fun LoginScreen(
    initialServerUrl: String = BuildConfig.DEFAULT_SERVER_URL,
    serverUrlEditable: Boolean = BuildConfig.SERVER_URL_EDITABLE,
    onLoggedIn: (Session) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("telephone_app", android.content.Context.MODE_PRIVATE) }
    val rememberedUsername = remember { prefs.getString("remember_username", "").orEmpty() }
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var username by remember { mutableStateOf(rememberedUsername) }
    var password by remember { mutableStateOf("") }
    var rememberUsername by remember { mutableStateOf(rememberedUsername.isNotBlank()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CallBackground),
    ) {
        LoginBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.8f))
            LoginHeader()
            Spacer(Modifier.height(26.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = CallSurfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (serverUrlEditable) {
                        LoginTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it.trimEnd('/') },
                            placeholder = BuildConfig.DEFAULT_SERVER_URL,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        )
                    }
                    LoginTextField(username, { username = it }, "请输入账号")
                    LoginTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "请输入密码",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberUsername,
                            onCheckedChange = { rememberUsername = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CallActiveBlue,
                                checkmarkColor = CallActionContent,
                                uncheckedColor = CallMutedText,
                            ),
                        )
                        Text("记住账号", color = CallText, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    if (error.isNotBlank()) Text(error, color = CallHangupColor)
                    Button(
                        enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(UiButtonRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = CallActiveBlue, contentColor = CallActionContent, disabledContainerColor = CallButtonColor),
                        onClick = {
                            loading = true
                            error = ""
                            val loginUsername = username.trim()
                            thread {
                                runCatching { ApiClient(if (serverUrlEditable) serverUrl else BuildConfig.DEFAULT_SERVER_URL).login(loginUsername, password) }
                                    .onSuccess { session ->
                                        val editor = prefs.edit()
                                        if (rememberUsername) editor.putString("remember_username", loginUsername) else editor.remove("remember_username")
                                        editor.apply()
                                        runOnMain { onLoggedIn(session) }
                                    }
                                    .onFailure { runOnMain { error = it.message ?: "登录失败" } }
                                runOnMain { loading = false }
                            }
                        },
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(UiSmallIconSize), strokeWidth = 2.dp, color = CallActionContent)
                        } else {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("进入工作台", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    "进入",
                                    modifier = Modifier.align(Alignment.CenterEnd).size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.Lock, "数据安全", tint = CallActiveBlue, modifier = Modifier.size(22.dp))
                Spacer(Modifier.size(8.dp))
                Text("数据安全 · 稳定可靠", color = CallMutedText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun LoginBackground() {
    val line = CallActiveBlue.copy(alpha = 0.08f)
    val wave = CallActiveBlue.copy(alpha = 0.06f)
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.16f)
        listOf(110f, 190f, 285f, 385f).forEach { radius ->
            drawCircle(line, radius = radius, center = center, style = Stroke(width = 2f))
        }
        val path = Path().apply {
            moveTo(0f, size.height * 0.90f)
            quadraticTo(size.width * 0.28f, size.height * 0.87f, size.width * 0.50f, size.height * 0.94f)
            quadraticTo(size.width * 0.78f, size.height * 0.84f, size.width, size.height * 0.91f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, wave)
    }
}

@Composable
private fun LoginHeader() {
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CallSurfaceColor),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "销售通话",
            modifier = Modifier.size(60.dp),
        )
    }
    Spacer(Modifier.height(20.dp))
    Text(
        "TELEPHONE",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Black,
        color = CallText,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .size(width = 28.dp, height = 3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(CallActiveBlue),
    )
    Spacer(Modifier.height(10.dp))
    Text(
        "客户拨打、录音、统计一站完成",
        modifier = Modifier.fillMaxWidth(),
        color = CallMutedText,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        placeholder = { Text(placeholder) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = CallText,
            unfocusedTextColor = CallText,
            focusedBorderColor = CallInputBorder,
            unfocusedBorderColor = CallInputBorder,
            cursorColor = CallText,
            focusedContainerColor = CallInputColor,
            unfocusedContainerColor = CallInputColor,
            focusedPlaceholderColor = CallPlaceholderText,
            unfocusedPlaceholderColor = CallPlaceholderText,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    TelephoneTheme { LoginScreen {} }
}
