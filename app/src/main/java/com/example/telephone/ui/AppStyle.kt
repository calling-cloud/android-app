package com.example.telephone.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val UiTabHeight = 58.dp
internal val UiButtonHeight = 48.dp
internal val UiButtonRadius = 24.dp
internal val UiIconSize = 20.dp
internal val UiSmallIconSize = 18.dp
internal val UiCardPadding = 14.dp
internal val UiListPadding = 14.dp
internal val UiTabFontSize = 11.sp

internal val CallBackground: Color @Composable get() = LocalAppPalette.current.background
internal val CallSurfaceColor: Color @Composable get() = LocalAppPalette.current.surface
internal val CallButtonColor: Color @Composable get() = LocalAppPalette.current.button
internal val CallActiveBlue: Color @Composable get() = LocalAppPalette.current.primary
internal val CallActionContent: Color @Composable get() = LocalAppPalette.current.actionContent
internal val CallHangupColor: Color @Composable get() = LocalAppPalette.current.danger
internal val CallAcceptColor: Color @Composable get() = LocalAppPalette.current.success
internal val CallHeartColor: Color @Composable get() = LocalAppPalette.current.heart
internal val CallInactiveHeart: Color @Composable get() = LocalAppPalette.current.inactiveHeart
internal val CallMutedText: Color @Composable get() = LocalAppPalette.current.mutedText
internal val CallAvatarRing: Color @Composable get() = LocalAppPalette.current.avatarRing
internal val CallAvatarColor: Color @Composable get() = LocalAppPalette.current.avatar
internal val CallInputColor: Color @Composable get() = LocalAppPalette.current.input
internal val CallInputBorder: Color @Composable get() = LocalAppPalette.current.inputBorder
internal val CallPlaceholderText: Color @Composable get() = LocalAppPalette.current.placeholder
internal val CallText: Color @Composable get() = LocalAppPalette.current.text
internal val CallTabSelectedText: Color @Composable get() = LocalAppPalette.current.tabSelectedText
internal val CallTabUnselectedText: Color @Composable get() = LocalAppPalette.current.tabUnselectedText

internal val LocalAppPalette = staticCompositionLocalOf { DarkAppPalette }

internal data class AppPalette(
    val background: Color,
    val surface: Color,
    val button: Color,
    val primary: Color,
    val actionContent: Color,
    val danger: Color,
    val success: Color,
    val heart: Color,
    val inactiveHeart: Color,
    val mutedText: Color,
    val avatarRing: Color,
    val avatar: Color,
    val input: Color,
    val inputBorder: Color,
    val placeholder: Color,
    val text: Color,
    val tabSelectedText: Color,
    val tabUnselectedText: Color,
)

internal val DarkAppPalette = AppPalette(
    background = Color(0xFF171A45),
    surface = Color(0xFF222650),
    button = Color(0xFF2D315B),
    primary = Color(0xFF050C8F),
    actionContent = Color.White,
    danger = Color(0xFFD71920),
    success = Color(0xFF138A42),
    heart = Color(0xFFE1251B),
    inactiveHeart = Color(0xFF596084),
    mutedText = Color(0xFFB6BADB),
    avatarRing = Color(0xFF33386E),
    avatar = Color(0xFF687092),
    input = Color(0xFF25294F),
    inputBorder = Color(0xFF3D426C),
    placeholder = Color(0xFF70769D),
    text = Color.White,
    tabSelectedText = Color.White,
    tabUnselectedText = Color(0xFFB6BADB),
)

internal val LightAppPalette = AppPalette(
    background = Color(0xFFF5F7FB),
    surface = Color.White,
    button = Color(0xFFE8ECF5),
    primary = Color(0xFF1F4FD8),
    actionContent = Color.White,
    danger = Color(0xFFD71920),
    success = Color(0xFF15803D),
    heart = Color(0xFFE1251B),
    inactiveHeart = Color(0xFFB4BBCB),
    mutedText = Color(0xFF687089),
    avatarRing = Color(0xFFDDE5F7),
    avatar = Color(0xFF3E63DD),
    input = Color(0xFFF0F3FA),
    inputBorder = Color(0xFFD9DEEA),
    placeholder = Color(0xFF8A92A6),
    text = Color(0xFF12172A),
    tabSelectedText = Color(0xFF1F4FD8),
    tabUnselectedText = Color(0xFF3D465C),
)
