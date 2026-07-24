package com.sun.kmpstartertemplaterefined.feature_live_presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sun.kmpstartertemplaterefined.feature_live_domain.logics.GetLiveCoursesLogic
import com.sun.kmpstartertemplaterefined.feature_live_presentation.mapper.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LiveStreamingViewModel(
    private val getLiveCoursesLogic: GetLiveCoursesLogic,
) : ViewModel() {

    private val _state = MutableStateFlow(LiveStreamingState())
    val state = _state.asStateFlow()

    init {
        loadLiveCourses()
    }

    fun loadLiveCourses() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching { getLiveCoursesLogic(status = "scheduled,live") }
                .onSuccess { courses ->
                    _state.update {
                        it.copy(isLoading = false, courses = courses.map { c -> c.toUi() })
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "載入直播列表失敗"
                        )
                    }
                }
        }
    }

    fun retry() = loadLiveCourses()
}