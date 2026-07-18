package com.example.telephone.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telephone.ui.CallActiveBlue
import com.example.telephone.ui.CallMutedText
import com.example.telephone.ui.CallSurfaceColor
import com.example.telephone.ui.CallTabSelectedText
import com.example.telephone.ui.CallTabUnselectedText
import com.example.telephone.ui.UiIconSize
import com.example.telephone.ui.UiTabFontSize
import com.example.telephone.ui.UiTabHeight

@Composable
fun MainBottomBar(
    selected: MainTab,
    onSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CallSurfaceColor)
            .navigationBarsPadding(),
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(UiTabHeight),
            containerColor = CallSurfaceColor,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            MainTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selected == tab,
                    onClick = { onSelected(tab) },
                    icon = {
                        Icon(
                            painterResource(tab.icon),
                            tab.title,
                            modifier = Modifier.size(UiIconSize),
                        )
                    },
                    label = {
                        Text(
                            tab.title,
                            fontSize = UiTabFontSize,
                            lineHeight = 12.sp,
                            maxLines = 1,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = CallTabSelectedText,
                        unselectedIconColor = CallMutedText,
                        unselectedTextColor = CallTabUnselectedText,
                        indicatorColor = CallActiveBlue,
                    ),
                )
            }
        }
    }
}
