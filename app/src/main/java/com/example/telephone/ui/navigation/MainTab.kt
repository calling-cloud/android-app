package com.example.telephone.ui.navigation

enum class MainTab(val route: String, val title: String, val icon: Int) {
    Dialer("dialer", "拨号", android.R.drawable.ic_menu_call),
    Stats("stats", "统计", android.R.drawable.ic_menu_sort_by_size),
    Records("records", "记录", android.R.drawable.ic_menu_recent_history),
    Profile("profile", "我的", android.R.drawable.ic_menu_myplaces);
}
