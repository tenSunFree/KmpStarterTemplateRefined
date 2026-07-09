package com.sun.kmpstartertemplaterefined.feature_live_presentation.rtc

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnAttach
import com.sun.kmpstartertemplaterefined.feature_live_presentation.pip.AndroidLivePipState
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

private val mainHandler = Handler(Looper.getMainLooper())

// Use container.tag to remember the bound SurfaceView,
// to avoid cutting and rebuilding it every time you DECODING.
private data class RemoteViewBinding(
    val uid: Int,
    val surfaceView: SurfaceView,
)

// Introducing "Stable SurfaceView Reuse": Remembers the bound (uid, SurfaceView) using container.tag. DECODING no longer rebuilds it; it's only rebuilt during FROZEN/FAILED or the first time it's created (forceRecreate).
// Uses the standard SurfaceView(context) (CreateRendererView has been removed in Agora 4.x).
// Changes renderMode for screen sharing to RENDER_MODE_FIT (does not crop desktop content).
// Changes AndroidView.update{} to post{} to avoid conflicts caused by synchronous requestLayout triggered during layout pass.
// Performs a reuse bind onFirstRemoteVideoFrame to ensure the first frame is stable.
// Clears container.tag onUserOffline to avoid misjudgments during subsequent reconnections.
//
// PIP / background-resume black screen fix
// Added SurfaceHolder.Callback: when the Activity enters PIP or returns to the foreground from the background,
// Android will destroy the old Surface and create a brand-new one (even if the SurfaceView Java object itself has
// not been reclaimed). The Agora engine does not know that the underlying Surface has changed. If we do not
// proactively call setupRemoteVideo() again to rebind it, the video can get stuck on a black screen even though
// Agora keeps sending frames.
// surfaceCreated is the callback the system guarantees to invoke when the "new Surface is actually ready",
// which is more reliable than depending on Agora's onRemoteVideoStateChanged (FROZEN/FAILED), because a
// background-resume scenario does not necessarily trigger a network-state event.
//
// We also added the AndroidLivePipState.onResumed callback: MainActivity.onResume()
// actively notifies this code to force one more rebind, as an extra safeguard beyond surfaceCreated
// (some devices/ROMs may differ from the standard timing when restoring from PIP).
//
// Main Composable:One engine, one join, two views
@Composable
actual fun LiveRtcClassroomView(
    modifier: Modifier,
    session: LiveRtcSession,
    screenUid: Int,
    cameraUid: Int,
    showCamera: Boolean,
    speakerEnabled: Boolean,
    isInPip: Boolean,
) {
    val context = LocalContext.current
    // Two stable containers, created only once throughout the entire Composable lifecycle.
    val screenContainer = remember { FrameLayout(context) }
    val cameraContainer = remember { FrameLayout(context) }
    // Only one RtcEngine is created; it is rebuilt only if the channelName changes.
    val rtcEngine = remember(session.channelName) {
        val config = RtcEngineConfig().apply {
            mContext = context.applicationContext
            mAppId = session.appId
            mEventHandler = object : IRtcEngineEventHandler() {
                // Complete connection state change including reason code
                override fun onConnectionStateChanged(state: Int, reason: Int) {
                    Log.d(
                        "AgoraRTC[classroom]",
                        "連線狀態變化 state=$state reason=$reason " +
                                "(reason=9 通常代表 token 過期/失效, reason=8=token失效)"
                    )
                }

                // Agora will proactively notify you when your token is about to expire. If you don't implement `renewToken` here, you'll be disconnected and the error will be err=110/9.
                override fun onTokenPrivilegeWillExpire(token: String?) {
                    Log.w(
                        "AgoraRTC[classroom]",
                        "Token 即將過期！需要呼叫 renewToken 更新，目前尚未實作自動更新"
                    )
                }

                // Confirm the timing and reason for leaving the channel, ruling out whether you were kicked out immediately after joining.
                override fun onLeaveChannel(stats: RtcStats?) {
                    Log.d("AgoraRTC[classroom]", "離開頻道 stats=$stats")
                }

                // Successfully joined the channel
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    Log.d(
                        "AgoraRTC[classroom]", "加入頻道成功 channel=$channel localUid=$uid"
                    )
                }

                // Triggered when the App enters the channel later than the teacher's app.
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.d("AgoraRTC[classroom]", "遠端用戶加入 uid=$uid")
                    val engine = RtcEngineHolder.engine ?: return
                    when (uid) {
                        screenUid -> setupRemoteView(
                            uid = uid,
                            container = screenContainer,
                            context = context,
                            engine = engine,
                            overlay = false,
                            label = "screen",
                            renderMode = VideoCanvas.RENDER_MODE_FIT, // Screen sharing without cropping
                            forceRecreate = false,
                        )

                        cameraUid -> setupRemoteViewTexture(
                            uid = uid,
                            container = cameraContainer,
                            context = context,
                            engine = engine,
                            renderMode = VideoCanvas.RENDER_MODE_HIDDEN,
                        )
                    }
                }

                // Changes in remote video status
                // DECODING: Reuse only, do not rebuild
                // FROZEN / FAILED: Force rebuild only (forceRecreate=true)
                override fun onRemoteVideoStateChanged(
                    uid: Int, state: Int, reason: Int, elapsed: Int
                ) {
                    Log.d(
                        "AgoraRTC[classroom]", "遠端視訊狀態 uid=$uid state=$state reason=$reason"
                    )
                    val engine = RtcEngineHolder.engine ?: return
                    when (uid) {
                        screenUid -> when (state) {
                            Constants.REMOTE_VIDEO_STATE_DECODING -> {
                                // Only reuse, do not rebuild SurfaceView
                                setupRemoteView(
                                    uid = uid,
                                    container = screenContainer,
                                    context = context,
                                    engine = engine,
                                    overlay = false,
                                    label = "screen",
                                    renderMode = VideoCanvas.RENDER_MODE_FIT,
                                    forceRecreate = false,
                                )
                            }

                            Constants.REMOTE_VIDEO_STATE_FROZEN -> {
                                // FROZEN = Network jitter, Agora will recover on its own, do not touch the SurfaceView
                                // reason=1(NETWORK) Most common, will automatically return to DECODING after 1~2 seconds.
                                // If forceRecreate is used here, rebuilding the SurfaceView will trigger a new
                                // FROZEN, causing a "black→bright→black" loop every second.
                                Log.w(
                                    "AgoraRTC[classroom]",
                                    "screen FROZEN uid=$uid reason=$reason → 等 Agora 自動恢復，不重建"
                                )
                                // Do nothing and let Agora buffer back on its own.
                            }

                            Constants.REMOTE_VIDEO_STATE_FAILED -> {
                                // Only when it's truly dead does it require a forced rebuild.
                                Log.e(
                                    "AgoraRTC[classroom]",
                                    "screen FAILED uid=$uid reason=$reason → force rebind"
                                )
                                // The video is definitely interrupted, so report false to prevent the user from shrinking the window
                                // and entering PiP only to see a black screen (canEnterPip() should block this case).
                                AndroidLivePipState.setVideoPlaying(false)
                                setupRemoteView(
                                    uid = uid,
                                    container = screenContainer,
                                    context = context,
                                    engine = engine,
                                    overlay = false,
                                    label = "screen",
                                    renderMode = VideoCanvas.RENDER_MODE_FIT,
                                    forceRecreate = true,
                                )
                            }

                            Constants.REMOTE_VIDEO_STATE_STOPPED -> {
                                // The teacher manually turned off screen sharing/camera (not an abnormal disconnect);
                                // treat this as "there is currently no real video content" to avoid canEnterPip() misjudging.
                                Log.d(
                                    "AgoraRTC[classroom]",
                                    "screen STOPPED uid=$uid reason=$reason"
                                )
                                AndroidLivePipState.setVideoPlaying(false)
                            }
                        }

                        cameraUid -> {
                            if (state == Constants.REMOTE_VIDEO_STATE_DECODING) {
                                setupRemoteViewTexture(
                                    uid = uid,
                                    container = cameraContainer,
                                    context = context,
                                    engine = engine,
                                    renderMode = VideoCanvas.RENDER_MODE_HIDDEN,
                                )
                            }
                        }
                    }
                }

                // First frame arrives (for safety, to ensure stable binding of the first frame)
                override fun onFirstRemoteVideoFrame(
                    uid: Int, width: Int, height: Int, elapsed: Int
                ) {
                    Log.d(
                        "AgoraRTC[classroom]", "first remote frame uid=$uid ${width}x${height}"
                    )
                    val engine = RtcEngineHolder.engine ?: return
                    when (uid) {
                        screenUid -> {
                            AndroidLivePipState.setAspectRatio(width, height)
                            AndroidLivePipState.setVideoPlaying(true)
                            setupRemoteView(
                                uid = uid,
                                container = screenContainer,
                                context = context,
                                engine = engine,
                                overlay = false,
                                label = "screen",
                                renderMode = VideoCanvas.RENDER_MODE_FIT,
                                forceRecreate = false,
                            )
                        }

                        cameraUid -> {
                            setupRemoteViewTexture(
                                uid = uid,
                                container = cameraContainer,
                                context = context,
                                engine = engine,
                                renderMode = VideoCanvas.RENDER_MODE_HIDDEN,
                            )
                        }
                    }
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.d(
                        "AgoraRTC[classroom]", "用戶離線 uid=$uid reason=$reason"
                    )
                    mainHandler.post {
                        when (uid) {
                            screenUid -> {
                                screenContainer.removeAllViews()
                                screenContainer.tag = null  // Clear old bindings
                                // When the teacher's video goes offline, there is no real video content anymore.
                                // We must report false; otherwise canEnterPip() will incorrectly think
                                // it is still playing, and the user will only see a black screen after shrinking it.
                                AndroidLivePipState.setVideoPlaying(false)
                            }

                            cameraUid -> {
                                cameraContainer.removeAllViews()
                                cameraContainer.tag = null
                            }
                        }
                    }
                }

                override fun onError(err: Int) {
                    // Print out the Chinese explanations of common error codes as well.
                    val hint = when (err) {
                        110 -> "Token 無效：App Certificate 已啟用但沒帶token，或token的uid/channel與join時不符，或token過期"
                        109 -> "Token 已過期，需要 renewToken"
                        17 -> "加入頻道被拒絕：可能已在頻道中，或 echo test 未結束"
                        101 -> "App ID 無效"
                        102 -> "channel name 格式不合法"
                        else -> "查表: https://api-ref.agora.io"
                    }
                    Log.e("AgoraRTC[classroom]", "錯誤碼 err=$err → $hint")
                }
            }
        }
        RtcEngine.create(config).also { engine ->
            RtcEngineHolder.engine = engine
            Log.d(
                "AgoraRTC[classroom]",
                "RtcEngine created, SDK version=${RtcEngine.getSdkVersion()}"
            )
        }
    }
    // UI layer: main screen + small window in the upper right corner
    Box(modifier = modifier) {
        // Main screen: Teacher's screen sharing (uid=2000)
        // update{} is replaced with post{} to avoid conflicts
        // or black screens caused by triggering synchronous requestLayout during the Compose layout pass.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { screenContainer },
            update = { container ->
                container.post {
                    container.requestLayout()
                    container.invalidate()
                    (container.getChildAt(0) as? SurfaceView)?.invalidate()
                }
            },
        )
        // Top right corner small window: Teacher's camera (uid=1000)
        val cameraOverlayModifier = if (isInPip) {
            Modifier.width(72.dp).height(54.dp).align(Alignment.TopEnd).padding(4.dp)
        } else {
            Modifier.width(120.dp).height(90.dp).align(Alignment.TopEnd)
        }
        if (showCamera) {
            AndroidView(
                modifier = cameraOverlayModifier,
                factory = { cameraContainer },
                update = { container ->
                    container.post {
                        container.requestLayout()
                        container.invalidate()
                    }
                },
            )
        }
    }
    // Join channel / leave channel
    DisposableEffect(session.channelName) {
        rtcEngine.enableVideo()
        rtcEngine.setDefaultAudioRoutetoSpeakerphone(true)
        // Print all key parameters before joining to facilitate comparison with the values when the token is generated by the backend.
        Log.d(
            "AgoraRTC[classroom]",
            "join前參數檢查 appIdFull=[${session.appId}] appIdLength=${session.appId.length} " +
                    "channelName=${session.channelName} " +
                    "uid=${session.uid} " +
                    "tokenIsNull=${false} " +
                    "tokenIsBlank=${session.token.isBlank()} " +
                    "tokenLength=${session.token.length} " +
                    "tokenPrefix=${session.token.take(10)}***" +
                    "token=${session.token}"
        )
        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
            channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            publishCameraTrack = false
            publishMicrophoneTrack = false
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        }
        val result = rtcEngine.joinChannel(
            session.token,
            session.channelName,
            session.uid,
            options,
        )
        Log.d(
            "AgoraRTC[classroom]",
            "joinChannel result=$result (0=成功) channel=${session.channelName} uid=${session.uid}"
        )
        rtcEngine.muteAllRemoteAudioStreams(!speakerEnabled)
        rtcEngine.adjustPlaybackSignalVolume(if (speakerEnabled) 100 else 0)
        onDispose {
            // Clear all binding tags before leaving to avoid residue.
            screenContainer.removeAllViews()
            screenContainer.tag = null
            cameraContainer.removeAllViews()
            cameraContainer.tag = null
            rtcEngine.leaveChannel()
            RtcEngine.destroy()
            RtcEngineHolder.engine = null
            AndroidLivePipState.setVideoPlaying(false)
            Log.d("AgoraRTC[classroom]", "engine destroyed")
        }
    }
    // The speaker switch takes effect immediately
    LaunchedEffect(speakerEnabled) {
        rtcEngine.muteAllRemoteAudioStreams(!speakerEnabled)
        rtcEngine.adjustPlaybackSignalVolume(if (speakerEnabled) 100 else 0)
    }
    // Fix point: proactively force a rebind when the app returns from PIP/background to the foreground
    // MainActivity.onResume() calls AndroidLivePipState.notifyResumed(),
    // and this block receives that signal to perform one forceRecreate=true
    // rebind for both the screen and camera containers, ensuring the video can
    // still recover even if surfaceCreated is not triggered because of device timing issues.
    DisposableEffect(Unit) {
        val onResumedCallback: () -> Unit = {
            val engine = RtcEngineHolder.engine
            if (engine != null) {
                Log.d("AgoraRTC[classroom]", "onResumed → 強制重新綁定 screen/camera")
                setupRemoteView(
                    uid = screenUid,
                    container = screenContainer,
                    context = context,
                    engine = engine,
                    overlay = false,
                    label = "screen",
                    renderMode = VideoCanvas.RENDER_MODE_FIT,
                    forceRecreate = true,
                )
                if (showCamera) {
                    setupRemoteViewTexture(
                        uid = cameraUid,
                        container = cameraContainer,
                        context = context,
                        engine = engine,
                        renderMode = VideoCanvas.RENDER_MODE_HIDDEN,
                    )
                }
            }
        }
        AndroidLivePipState.onResumed = onResumedCallback
        onDispose {
            if (AndroidLivePipState.onResumed === onResumedCallback) {
                AndroidLivePipState.onResumed = null
            }
        }
    }
}

// setupRemoteView: Stable Core Reuse
// Strategy:
// - container.tag stores RemoteViewBinding(uid, surfaceView)
// - If tag exists && uid is the same && surfaceView is still in container
// → Direct reuse, only rerun setupRemoteVideo() (rebinding Agora)
// - Otherwise (first time / forceRecreate / uid changed) → Rebuild SurfaceView
// Solves the problem of "every DECODING cancels rebuild → transitional black screen".
//
// When creating a new SurfaceView, attach a surfaceCreated listener: whenever the system recreates the underlying
// Surface because of a PIP switch, returning from the background, or a display-size change, this code proactively
// calls engine.setupRemoteVideo() again to tell Agora where to render the video, avoiding a stuck black screen.
private fun setupRemoteView(
    uid: Int,
    container: FrameLayout,
    context: Context,
    engine: RtcEngine,
    overlay: Boolean,
    label: String,
    renderMode: Int,
    forceRecreate: Boolean = false,
) {
    fun bindVideo() {
        val existing = container.tag as? RemoteViewBinding
        val surfaceView: SurfaceView =
            if (!forceRecreate && existing?.uid == uid && existing.surfaceView.parent == container) {
                // Reuse existing SurfaceView without cutting or rebuilding it.
                Log.d(
                    "AgoraRTC[classroom]", "reuse SurfaceView label=$label uid=$uid"
                )
                existing.surfaceView
            } else {
                // Initial creation / forceRecreate / uid change
                Log.d(
                    "AgoraRTC[classroom]",
                    "create SurfaceView label=$label uid=$uid force=$forceRecreate"
                )
                container.removeAllViews()
                val newSv = SurfaceView(context).apply {
                    setZOrderMediaOverlay(overlay)
                    // OPAQUE: explicitly tell the system that this Surface is opaque, avoiding an extra composition layer
                    // and an extra alpha blending pass on some devices. This saves a bit of rendering cost and makes
                    // behavior more predictable (the default TRANSLUCENT has occasional flicker/ghosting reports when
                    // two SurfaceViews are stacked).
                    holder.setFormat(android.graphics.PixelFormat.OPAQUE)
                    // Live-video apps do not want the screen to sleep automatically during playback, which could cause
                    // the system to reclaim the Surface behind the SurfaceView (sleep itself does not necessarily
                    // trigger surfaceDestroyed, but avoiding this variable is easier).
                    keepScreenOn = true
                }
                // When the system recreates the Surface (PIP switch / returning from background / size change),
                // proactively rebind to avoid Agora continuing to render to an already-destroyed old Surface.
                newSv.holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        Log.d(
                            "AgoraRTC[classroom]",
                            "surfaceCreated label=$label uid=$uid → 重新綁定 Agora"
                        )
                        engine.setupRemoteVideo(VideoCanvas(newSv, renderMode, uid))
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder, format: Int, width: Int, height: Int
                    ) {
                        // no-op: let Agora handle scaling on its own when the size changes
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        Log.w(
                            "AgoraRTC[classroom]", "surfaceDestroyed label=$label uid=$uid"
                        )
                        // Do not clear the tag proactively so the reuse logic can still hook back in on the next surfaceCreated.
                    }
                })
                container.addView(
                    newSv,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
                container.tag = RemoteViewBinding(uid, newSv)
                newSv
            }
        // Regardless of whether it's reuse or rebuild, always call setupRemoteVideo again
        // Ensure Agora knows it needs to render to this SurfaceView
        engine.setupRemoteVideo(
            VideoCanvas(surfaceView, renderMode, uid)
        )
        surfaceView.requestLayout()
        surfaceView.invalidate()
        container.requestLayout()
        container.invalidate()
    }

    mainHandler.post {
        if (container.isAttachedToWindow) {
            bindVideo()
        } else {
            container.doOnAttach { bindVideo() }
        }
    }
}

// The camera viewport uses TextureView instead: TextureView uses normal view layer compositing (GPU texture),
// unlike SurfaceView, which is an independent hardware layer, when stacked with the main screen's SurfaceView,
// it won't compete for Z-order, and it can display stably even in the extremely small virtual display of the system PiP, which is scaled as a whole.
//
// Just like the original SurfaceView, manually create a new TextureView(context),
// Don't use RtcEngine's helper, because the API differs slightly between different Agora SDK versions; manually creating it is the safest approach.
private fun setupRemoteViewTexture(
    uid: Int,
    container: FrameLayout,
    context: Context,
    engine: RtcEngine,
    renderMode: Int,
) {
    fun bindVideo() {
        container.removeAllViews()
        val textureView = TextureView(context).apply {
            keepScreenOn = true
        }
        container.addView(
            textureView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        engine.setupRemoteVideo(VideoCanvas(textureView, renderMode, uid))
        Log.d("AgoraRTC[classroom]", "bind TextureView uid=$uid")
    }

    mainHandler.post {
        if (container.isAttachedToWindow) {
            bindVideo()
        } else {
            container.doOnAttach { bindVideo() }
        }
    }
}

// Single engine owner
//
// It used to be internal, but LiveBackgroundAudioService (located in the androidApp module)
// needs to call muteLocalVideoStream/muteAllRemoteVideoStreams in the background to save bandwidth,
// so it must be accessible from outside the module to the same engine instance and was changed to public.
// If you do not plan to implement "foreground-service-only audio background playback," you can keep it internal.
object RtcEngineHolder {
    var engine: RtcEngine? = null
}