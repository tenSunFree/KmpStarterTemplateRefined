package com.sun.kmpstartertemplaterefined.core.ui.screens.main.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sun.kmpstartertemplaterefined.core.ui.screens.main.components.MainTopBar  // ← 新增
import com.sun.kmpstartertemplaterefined.feature_live_presentation.LiveStreamingTab
import com.sun.kmpstartertemplaterefined.feature_live_presentation.LiveStreamingViewModel
import com.sun.kmpstartertemplaterefined.feature_navigation.StarterNavigator
import com.sun.kmpstartertemplaterefined.feature_navigation.screens.StarterScreens
import org.koin.compose.viewmodel.koinViewModel

private val Pink = Color(0xFFFF3F68)
private val BorderGray = Color(0xFFE5E5E5)
private val TabUnselected = Color(0xFFC5C5C5)

@Composable
fun EasyLearningScreen() {
    val navigator = StarterNavigator.getCurrent()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("直播室", "FunTV", "影片", "音樂", "童話", "專欄", "大補帖")

    Column(modifier = Modifier.fillMaxSize()) {
        MainTopBar()
        TabRow(
            tabs = tabs,
            selectedIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it },
        )
        when (selectedTabIndex) {
            0 -> {
                val viewModel: LiveStreamingViewModel = koinViewModel()
                LiveStreamingTab(
                    viewModel = viewModel,
                    onOpenLiveRoom = { liveId ->
                        navigator.navigateTo(StarterScreens.LiveRoom(liveId = liveId))
                    },
                )
            }
            1 -> FunTvTab()
            2 -> VideoTab()
            3 -> MusicTab()
            4 -> StoryTab()
            5 -> ColumnTab()
            6 -> StudyPackTab()
        }
    }
}

@Composable
private fun TabRow(tabs: List<String>, selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    val scrollState = rememberScrollState()
    Column {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Pink else TabUnselected,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(52.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSelected) Pink else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(color = BorderGray, thickness = 0.8.dp)
    }
}