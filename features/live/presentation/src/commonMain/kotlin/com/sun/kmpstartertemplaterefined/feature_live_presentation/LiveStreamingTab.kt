package com.sun.kmpstartertemplaterefined.feature_live_presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sun.kmpstartertemplaterefined.feature_live_domain.models.JoinLiveConfig
import com.sun.kmpstartertemplaterefined.feature_live_presentation.model.LiveCourseUi

private val Pink = Color(0xFFFF3F68)
private val TextDark = Color(0xFF4A4A4A)
private val TextGray = Color(0xFF777777)
private val BorderGray = Color(0xFFE5E5E5)

@Composable
fun LiveStreamingTab(
    viewModel: LiveStreamingViewModel,
    onOpenLiveRoom: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            // ---------- 標題區塊 ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Live Streaming",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "ⓘ", fontSize = 18.sp, color = Color(0xFFAAAAAA))
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "篩選",
                    tint = Color(0xFF555555),
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ---------- 內容區塊：依 State 分支渲染 ----------
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Pink)
                    }
                }
                state.errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    ) {
                        Text("載入失敗：${state.errorMessage}", color = Color.Red, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = viewModel::retry, colors = ButtonDefaults.buttonColors(containerColor = Pink)) {
                            Text("重試")
                        }
                    }
                }
                state.courses.isEmpty() -> {
                    Text(
                        "目前沒有正在進行的直播，\n敬請期待即將開播的課程！",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = TextGray, fontSize = 17.sp, lineHeight = 28.sp,
                    )
                }
                else -> {
                    state.courses.forEach { course ->
                        LiveCourseCard(
                            course = course,
                            onEnterRoom = { onOpenLiveRoom(course.liveId) },
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LiveCourseCard(
    course: LiveCourseUi,
    onEnterRoom: () -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 縮圖區（先用 emoji/預設圖佔位，之後可換成 AsyncImage 載入 thumbnailUrl）
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 150.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (course.teacherAvatarUrl == null) "🧑🏻" else "",
                    fontSize = 52.sp,
                )
                // 之後可替換成：
                // AsyncImage(model = course.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = course.teacherName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.weight(1f),
                    )
                    if (course.isRequired) {
                        Text(
                            text = "必修",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Pink)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = course.level,
                        color = Pink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = course.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    lineHeight = 22.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = course.category,
                    fontSize = 14.sp,
                    color = TextGray,
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 課本下載按鈕：只有在有 textbookUrl 時才顯示
                if (course.textbookUrl != null) {
                    OutlinedButton(
                        onClick = { /* TODO: 開啟 course.textbookUrl，例如用 WebView 或系統瀏覽器 */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, BorderGray),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            tint = TextDark,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Textbook", color = TextDark, fontSize = 15.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = course.scheduledTime,
                color = TextGray,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )

            Button(
                onClick = onEnterRoom,
                enabled = course.canJoin,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pink,
                    contentColor = Color.White,
                    disabledContainerColor = BorderGray,
                    disabledContentColor = TextGray,
                ),
                modifier = Modifier.height(40.dp),
            ) {
                Text(if (course.canJoin) "進入直播" else "尚未開放", fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = BorderGray)
    }
}