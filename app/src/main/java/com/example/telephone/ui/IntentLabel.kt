package com.example.telephone.ui

internal fun intentLevelLabel(level: Int) = when (level) {
    0 -> "未知"
    1 -> "基本无意向"
    2 -> "较低意向"
    3 -> "中等意向"
    4 -> "较高意向"
    5 -> "强烈意向"
    else -> "未知"
}
