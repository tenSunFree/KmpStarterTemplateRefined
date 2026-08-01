package com.sun.kmpstartertemplaterefined.feature_live_presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sun.kmpstartertemplaterefined.feature_live_presentation.engine.ChatClient
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LiveRoomRoute(
    liveId: String,
    onExit: () -> Unit,
) {
    val viewModel: LiveRoomViewModel = koinViewModel()
    val chatClient: ChatClient = koinInject()
    val state by viewModel.state.collectAsState()
    LaunchedEffect(liveId) {
        viewModel.join(liveId)
    }
    when {
        state.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        state.errorMessage != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.errorMessage ?: "加入直播失敗")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.join(liveId) }) { Text("重試") }
                    TextButton(onClick = onExit) { Text("返回") }
                }
            }
        }

        state.config != null -> {
            LiveRoomScreen(
                config = state.config!!,
                chatClient = chatClient,
                onRejoin = { viewModel.rejoin(liveId) },
                onExit = onExit,
            )
        }
    }
}