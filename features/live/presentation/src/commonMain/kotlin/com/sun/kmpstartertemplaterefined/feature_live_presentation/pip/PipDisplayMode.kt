package com.sun.kmpstartertemplaterefined.feature_live_presentation.pip

enum class PipDisplayMode(val label: String, val description: String) {
    ScreenOnly(
        label = "只顯示畫面",
        description = "縮小成小視窗時，只顯示老師的螢幕分享，最穩定",
    ),
    ScreenWithCameraOverlay(
        label = "畫面 + 鏡頭",
        description = "縮小成小視窗時，同時顯示老師的螢幕分享與攝影機（進階選項）",
    ),
}