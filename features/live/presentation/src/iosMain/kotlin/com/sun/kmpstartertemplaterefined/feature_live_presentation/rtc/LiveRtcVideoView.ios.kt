package com.sun.kmpstartertemplaterefined.feature_live_presentation.rtc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun LiveRtcClassroomView(
    modifier: Modifier,
    session: LiveRtcSession,
    screenUid: Int,
    cameraUid: Int,
    showCamera: Boolean,
    speakerEnabled: Boolean,
    isInPip: Boolean,  // Currently, iOS does not have PiP integration; just accept the parameters for now and maintain no-op functionality.
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "iOS RTC classroom placeholder", color = Color.White)
    }
}