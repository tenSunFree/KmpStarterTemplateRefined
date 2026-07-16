package com.sun.kmpstartertemplaterefined.androidapp

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.sun.kmpstartertemplaterefined.App
import com.sun.kmpstartertemplaterefined.feature_live_presentation.background.AndroidLiveBackgroundPlaybackBridge
import com.sun.kmpstartertemplaterefined.feature_live_presentation.pip.AndroidLivePipNotificationBridge
import com.sun.kmpstartertemplaterefined.feature_live_presentation.pip.AndroidLivePipState
import com.sun.kmpstartertemplaterefined.feature_live_presentation.rtc.RtcEngineHolder
import com.sun.kmpstartertemplaterefined.utils.logging.Log

class MainActivity : ComponentActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())

    /** Whether the system PiP has actually been accessed. Only if the PiP has been accessed can subsequent `onStop / PiP false` represent "System X is shut down". */
    private var hasEnteredPipOnce = false

    /**
     * A temporary flag indicating when PiP changes from true to false. False may represent:
     * 1. The user taps the PiP window to return to the main app screen (should not exit)
     * 2. The user taps the system's native X to close PiP (should exit)
     * This cannot be determined immediately upon reaching false; confirmation must be waited for onResume or onStop.
     */
    private var pendingSystemPipDismiss = false

    /** true = onResume has been run; the Activity is indeed currently in the foreground. */
    private var isActivityResumed = false

    /** Prevents delayed checks in onStop and onPictureInPictureModeChanged from triggering the same exit twice. */
    private var hasHandledPipDismiss = false

    /**
     * Crash fix: request notification permission here, not inside any Composable
     *
     * registerForActivityResult() here is initialized as an Activity class property,
     * not called from inside an @Composable function like rememberLauncherForActivityResult().
     * This approach uses ComponentActivity's own ActivityResultRegistry and registers directly
     * and synchronously when the Activity is created, without going through Compose's
     * CompositionLocal resolution mechanism. As a result, it is not affected by Navigation3's
     * SceneSetupNavEntryDecorator (implemented internally with movableContentOf), and it
     * guarantees that the crash caused by LocalActivityResultRegistryOwner not being found will not occur.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            AndroidLivePipState.setNotificationPermissionGranted(granted)
        }

    /** Prevent the system dialog from popping up repeatedly if onStateChanged is triggered multiple times after the user rejects once. */
    private var hasRequestedNotificationPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Synchronize the current permission state once at startup so notificationPermissionGrantedState
        // does not stay at its default value false——if the user has already granted it before
        // (for example, before reinstalling the app, or because Android 13 and below do not require
        // this permission), the correct state should be reflected immediately here.
        AndroidLivePipState.setNotificationPermissionGranted(hasNotificationPermission())
        // Whenever the live state changes (entering the room / receiving the first frame / leaving the room),
        // immediately sync it to the system's PictureInPictureParams to ensure autoEnterEnabled only works
        // when we are actually live.
        // Also proactively check whether the PiP notification should be dismissed—for example, when a live
        // session ends while in PiP (teacher disconnects or the user ends it manually), this will remove the
        // now-meaningless notification even if onPictureInPictureModeChanged has not yet been triggered by the system.
        //
        // This is also where the notification permission request is triggered: requesting it when entering
        // the live room (while still in the full-screen foreground, not at the moment of shrinking into PiP)
        // is because the system permission dialog needs foreground UI to display properly. Requesting too late
        // (for example, right when the user presses Home) makes the dialog experience unstable. ensureNotificationPermission()
        // has a one-time guard, and any state change such as setLiveRoomActive/setVideoPlaying/setCourseTitle
        // will trigger this callback, but only the first time will the system dialog actually appear.
        AndroidLivePipState.onStateChanged = {
            refreshPipParams()
            syncPipNotification()
            ensureNotificationPermission()
        }
        // Wire the commonMain LiveBackgroundPlayback.start()/stop() calls to the real Android ForegroundService.
        // Keep this in androidApp rather than feature_live_presentation because the Service class itself is defined
        // in the androidApp module (at the same level as MainActivity), preserving a one-way dependency.
        AndroidLiveBackgroundPlaybackBridge.onStart = { courseTitle ->
            LiveBackgroundAudioService.start(applicationContext, courseTitle)
        }
        AndroidLiveBackgroundPlaybackBridge.onStop = {
            LiveBackgroundAudioService.stop(applicationContext)
        }
        // If the PiP notification mute state changes (whether the user toggles it inside the app or
        // Compose confirms the change after tapping the mute button in the notification), the currently
        // displayed notification must update its text as well, otherwise the button label and the actual
        // state will be inconsistent.
        AndroidLivePipNotificationBridge.onMutedStateChanged = { muted ->
            LivePipNotificationManager.updateMuteState(
                context = applicationContext,
                courseTitle = AndroidLivePipState.courseTitle,
                isMuted = muted,
            )
        }
        // New back gesture / back button interception: because the Manifest already sets
        // android:enableOnBackInvokedCallback="true", this project uses
        // OnBackPressedDispatcher and should no longer override the deprecated onBackPressed().
        onBackPressedDispatcher.addCallback(this) {
            if (AndroidLivePipState.canEnterPip()) {
                enterPip()
            } else {
                // Not in the live room; keep the default back behavior (finish the Activity / go to the previous page)
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
        setContent {
            // Explicitly re-provide LocalActivityResultRegistryOwner (and LocalOnBackPressedDispatcherOwner as well).
            //
            // In theory, ComponentActivity.setContent{} automatically provides these two CompositionLocals,
            // so there is no need to do it manually. But this project mounts screens using Navigation3's
            // movableContentOf mechanism (multiple layers of NavEntryDecorator such as SceneSetupNavEntryDecorator),
            // and in this scenario the CompositionLocal inheritance chain can break in edge cases—when LiveRoomScreen
            // calls rememberPipNotificationPermissionGranted() (which internally uses rememberLauncherForActivityResult),
            // it crashes with an IllegalStateException because LocalActivityResultRegistryOwner cannot be found.
            //
            // By wrapping one more layer here, every subtree is guaranteed to receive it regardless of how many
            // Decorators or MovableContent layers are below, solving the root cause without having to patch every
            // screen that uses rememberLauncherForActivityResult. MainActivity itself implements
            // ActivityResultRegistryOwner / OnBackPressedDispatcherOwner, so providing this is fully valid.
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides this,
                LocalOnBackPressedDispatcherOwner provides this,
            ) {
                AndroidSideEffects()
                App()
            }
        }
    }

    @Composable
    private fun AndroidSideEffects() {
        val view = LocalView.current
        SideEffect {
            val window = (view.context as ComponentActivity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    /**
     * Triggered when the user presses Home / switches to another app / uses a gesture to return to the home screen.
     * This is the primary time to shrink into a floating window.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (AndroidLivePipState.canEnterPip()) {
            enterPip()
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        // If this onResume was caused by "expanding the PiP window back to the main screen", it means it was not closed by X, so the pending check is cancelled.
        pendingSystemPipDismiss = false
        AndroidLivePipState.notifyResumed()
    }

    override fun onPause() {
        super.onPause()
        isActivityResumed = false
    }

    /**
     * Triggered when the Activity is completely invisible (including cases where it is closed by the system's native PiP via X).
     * This is the most direct and reliable point in time to determine if "System X closed the PiP," without needing to guess based on delays.
     * Because once it reaches this point, it means the Activity has definitely not been brought back to the foreground.
     */
    override fun onStop() {
        super.onStop()
        val closedBySystemX =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    hasEnteredPipOnce &&
                    !isInPictureInPictureMode &&
                    AndroidLivePipState.canEnterPip()
        if (pendingSystemPipDismiss || closedBySystemX) {
            handleSystemPipDismiss(reason = "onStop")
        }
    }

    /**
     * Enter PiP. Each call rereads the latest aspect ratio to avoid the issue where
     * screen sharing (16:9) versus camera-only (which may use another ratio) is not updated in sync.
     */
    private fun enterPip() {
        // PiP is only supported starting from Android 8.0 (API 26)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val aspectRatio = Rational(
            AndroidLivePipState.aspectRatioWidth,
            AndroidLivePipState.aspectRatioHeight,
        )
        val params = PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
        try {
            enterPictureInPictureMode(params)
        } catch (e: IllegalStateException) {
            // The device or system settings do not allow PiP (for example, the user disabled the app's PiP permission in system settings).
            // Fail silently so the app can move to the background normally without affecting user actions or crashing the app.
        }
    }

    /**
     * On Android 12+ (API 31), set autoEnterEnabled as well so that when the user gestures back to the home screen,
     * the system can more naturally enter PiP automatically (and not always go through onUserLeaveHint).
     *
     * Note: this is not set only once in onCreate; it is called every time the live state changes to ensure that
     * autoEnterEnabled is turned off after leaving the live room and does not mistakenly trigger PiP on other screens.
     */
    fun refreshPipParams() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canEnter = AndroidLivePipState.canEnterPip()
        val aspectRatio = Rational(
            AndroidLivePipState.aspectRatioWidth,
            AndroidLivePipState.aspectRatioHeight,
        )
        val builder = PictureInPictureParams.Builder().setAspectRatio(aspectRatio)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(canEnter)
        }
        try {
            setPictureInPictureParams(builder.build())
        } catch (e: IllegalStateException) {
            // Same as above: fail silently is fine
        }
    }

    /**
     * Callback when the system actually switches into or out of PiP mode.
     *
     * Important Clarification (Tested and Corrected): This callback will be triggered in both cases: "User presses the expand button to return to full screen" and "User presses the system button (X) to close PiP".
     * `isInPictureInPictureMode = false` will be triggered in both cases; the system will not tell you which case it is.
     * The main judgment is handled by `onStop()` (more direct and reliable); the delay check here is only a backup.
     * Covers a few cases where the call order is not standard in OEM systems.
     */
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        AndroidLivePipState.isInPipModeState.value = isInPictureInPictureMode
        syncPipNotification()
        if (isInPictureInPictureMode) {
            hasEnteredPipOnce = true
            pendingSystemPipDismiss = false
            hasHandledPipDismiss = false
            return
        }
        // false: Mark pending initially, the actual result is left to onResume() (cancel) or onStop() (confirm).
        // The following delayed check is just a backup: in case onStop() is not triggered as expected due to system timing,
        // a second check will be performed after 350ms.
        if (hasEnteredPipOnce && AndroidLivePipState.canEnterPip()) {
            pendingSystemPipDismiss = true
            mainHandler.postDelayed({
                if (pendingSystemPipDismiss && !isActivityResumed && AndroidLivePipState.canEnterPip()) {
                    handleSystemPipDismiss(reason = "delayed fallback check")
                }
            }, 350L)
        }
    }

    /**
     * The user pressed the X button on the system PiP to close the small window (instead of expanding it back to full screen).
     * This is treated as "exiting live stream viewing," following the exact same path as the "End Viewing" notification:
     * notifyStopRequested() → LiveRoomScreen.exitLiveRoom() → onBack()
     * → LiveRtcClassroomView.onDispose releases engine resources.
     */
    private fun handleSystemPipDismiss(reason: String) {
        if (hasHandledPipDismiss) return
        // Don't rely solely on canEnterPip() — during the system's PiP shutdown process,
        // isLiveRoomActive/isVideoPlaying may have already been modified by other paths,
        // As long as the engine is still running, it should be allowed to be released.
        val hasLiveEngine = RtcEngineHolder.engine != null
        if (!AndroidLivePipState.canEnterPip() && !hasLiveEngine) return
        hasHandledPipDismiss = true
        pendingSystemPipDismiss = false
        hasEnteredPipOnce = false
        Log.d("MainActivity", "The system PiP has been closed by the user, exiting the live stream.reason=$reason")
        LivePipNotificationManager.cancel(applicationContext)
        LiveBackgroundAudioService.stop(applicationContext)
        // Key point: The Activity layer is released directly without waiting for Compose onDispose, and the sound stops immediately when X is pressed.
        RtcEngineHolder.releaseCurrentSession(reason = "system PiP dismissed: $reason")
        AndroidLivePipState.isInPipModeState.value = false
        AndroidLivePipState.setLiveRoomActive(false)
        // Reserved: Properly pop LiveRoomScreen from the navigation stack when Compose becomes visible again.
        AndroidLivePipNotificationBridge.notifyStopRequested()
    }

    /**
     * === PiP notification display policy ===
     * When watching full screen (not in PiP): never show the notification to avoid distracting the user.
     * When reduced to PiP and still actively playing: show the notification.
     * In all other cases (not live, disconnected while in PiP, already left the live room): dismiss the notification.
     *
     * This method is called at two times:
     * 1. onPictureInPictureModeChanged —— when the PiP window itself enters or exits.
     * 2. AndroidLivePipState.onStateChanged —— when the live state changes
     *    (for example, the teacher disconnects in PiP, or the user taps "stop playback" in the notification
     *    causing setLiveRoomActive(false)), ensuring there is no leftover state where the user is no longer
     *    in PiP but the notification is still hanging around.
     */
    private fun syncPipNotification() {
        val shouldShow =
            AndroidLivePipState.isInPipModeState.value && AndroidLivePipState.canEnterPip()
        if (shouldShow) {
            LivePipNotificationManager.show(
                context = applicationContext,
                courseTitle = AndroidLivePipState.courseTitle,
                isMuted = AndroidLivePipNotificationBridge.isMuted,
            )
        } else {
            LivePipNotificationManager.cancel(applicationContext)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Called when entering the live room (via AndroidLivePipState.onStateChanged).
     * The system dialog only appears when permission has not yet been granted and it has not been asked once
     * during this Activity lifecycle, preventing the user from being repeatedly prompted every time the live
     * state changes (for example, receiving the first frame or entering/exiting PiP).
     *
     * If the user denies it, live playback itself is completely unaffected—only the control notification will
     * not appear when shrinking into PiP, which is a reasonable graceful degradation (LivePipNotificationManager.show
     * already checks permissions internally and skips showing the notification without crashing when permission is missing).
     */
    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasRequestedNotificationPermission) return
        if (hasNotificationPermission()) {
            AndroidLivePipState.setNotificationPermissionGranted(true)
            return
        }
        hasRequestedNotificationPermission = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        LiveBackgroundAudioService.stop(applicationContext)
        LivePipNotificationManager.cancel(applicationContext)
        AndroidLivePipState.onStateChanged = null
        AndroidLivePipState.onResumed = null
        AndroidLivePipNotificationBridge.onMutedStateChanged = null
    }
}