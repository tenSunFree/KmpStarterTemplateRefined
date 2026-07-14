package com.sun.kmpstartertemplaterefined.core.ui.screens.main.tabs

import LiveCourseUi
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Pink = Color(0xFFFF3F68)
private val TextDark = Color(0xFF4A4A4A)
private val TextGray = Color(0xFF777777)
private val BorderGray = Color(0xFFE5E5E5)

val fakeLiveCourses = listOf(
    LiveCourseUi(
        id = "course_001",
        roomId = "funday_room_001",
        teacherName = "KarolChin",
        title = "Past Verb Pronunciation",
        category = "語言學習 (Language Learning)",
        level = "A2",
        isRequired = true,
        scheduledTime = "預計2026/05/04 19:00 開始直播",
        emoji = "👩🏻",
    ),
    LiveCourseUi(
        id = "course_002",
        roomId = "funday_room_002",
        teacherName = "MikeChang",
        title = "Past Verb Spelling",
        category = "檢定班 (MEXAM)",
        level = "B1",
        isRequired = true,
        scheduledTime = "預計2026/05/04 20:00 開始直播",
        emoji = "👨🏻",
    ),
    LiveCourseUi(
        id = "course_003",
        roomId = "ch_2026710_201133_455387",
        teacherName = "Sun",
        title = "Sun test",
        category = "Sun",
        level = "B1",
        isRequired = true,
        scheduledTime = "Sun",
        emoji = "👨🏻",
    ),
)

// onOpenLiveRoom: Upload the course when you click "Enter Live Stream".
@Composable
fun LiveStreamingTab(
    onOpenLiveRoom: (LiveCourseUi) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
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
        Text(
            text = "目前沒有正在進行的直播，\n敬請期待即將開播的課程！",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextGray,
            fontSize = 17.sp,
            lineHeight = 28.sp,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Coming soon.",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextDark,
        )
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderGray)
        Spacer(modifier = Modifier.height(16.dp))
        fakeLiveCourses.forEach { course ->
            LiveCourseCard(
                course = course,
                onEnterRoom = { onOpenLiveRoom(course) },
            )
            Spacer(modifier = Modifier.height(24.dp))
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
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 150.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = course.emoji, fontSize = 52.sp)
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
                OutlinedButton(
                    onClick = {},
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
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pink,
                    contentColor = Color.White,
                ),
                modifier = Modifier.height(40.dp),
            ) {
                Text("進入直播", fontSize = 15.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = BorderGray)
    }
}