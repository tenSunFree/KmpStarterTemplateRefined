// features/live/presentation/src/commonMain/kotlin/.../feature_live_presentation/LiveRoomViewModel.kt
package com.sun.kmpstartertemplaterefined.feature_live_presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sun.kmpstartertemplaterefined.core.platform.currentClientType
import com.sun.kmpstartertemplaterefined.feature_live_domain.exceptions.JoinLiveException
import com.sun.kmpstartertemplaterefined.feature_live_domain.logics.JoinLiveLogic
import com.sun.kmpstartertemplaterefined.feature_live_domain.models.JoinLiveConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveRoomState(
    val isLoading: Boolean = true,
    val config: JoinLiveConfig? = null,
    val errorMessage: String? = null,
)

class LiveRoomViewModel(
    private val joinLiveLogic: JoinLiveLogic,
) : ViewModel() {

    private val _state = MutableStateFlow(LiveRoomState())
    val state = _state.asStateFlow()

    fun join(liveId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching { joinLiveLogic(liveId = liveId, clientType = currentClientType()) }
                .onSuccess { config ->
                    _state.update { it.copy(isLoading = false, config = config) }
                }
                .onFailure { throwable ->
                    val message = (throwable as? JoinLiveException)?.message ?: "加入直播失敗，請稍後再試"
                    _state.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }

    /** 給 LiveRoomScreen 的 onRejoin 用：token 快過期時重新拿設定 */
    suspend fun rejoin(liveId: String): JoinLiveConfig? =
        runCatching { joinLiveLogic(liveId = liveId, clientType = currentClientType()) }.getOrNull()
}