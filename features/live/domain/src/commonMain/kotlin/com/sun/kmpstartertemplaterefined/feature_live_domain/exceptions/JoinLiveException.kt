package com.sun.kmpstartertemplaterefined.feature_live_domain.exceptions

sealed class JoinLiveException(message: String) : Exception(message) {
    data object Unauthorized : JoinLiveException("登入狀態已失效，請重新登入")
    data object LiveNotStarted : JoinLiveException("直播尚未開始")
    data object LiveNotFound : JoinLiveException("找不到此直播")
    data object MalformedRequest : JoinLiveException("請求格式錯誤，請更新 App")
    data class Unknown(val code: Int?, val raw: String?) :
        JoinLiveException(raw ?: "加入直播失敗，請稍後再試")
}